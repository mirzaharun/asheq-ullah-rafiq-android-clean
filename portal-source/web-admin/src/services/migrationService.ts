import { doc, getDoc, serverTimestamp, setDoc } from 'firebase/firestore';
import {
  DEFAULT_MIGRATION_BUNDLES,
  ModuleSeedBundle,
  getSeedBundleForModule,
} from '../data/defaultMigrationData';
import { db } from '../lib/firebase';
import { AdminUserProfile } from '../types/portal';
import { recordAuditLog } from './auditService';

export interface ModuleMigrationOutcome {
  moduleKey: string;
  collection: string;
  labelBn: string;
  labelEn: string;
  seededCount: number;
  skippedCount: number;
  errors: string[];
}

export interface FullMigrationReport {
  outcomes: ModuleMigrationOutcome[];
  totalSeeded: number;
  totalSkipped: number;
  totalErrors: number;
}

/**
 * Idempotently seeds missing documents for a single CMS module.
 * Checks getDoc() first and NEVER overwrites existing Firestore documents.
 */
export async function seedMissingDefaultsForModule(
  moduleKey: string,
  actor: AdminUserProfile,
  logIndividualAudit = true
): Promise<ModuleMigrationOutcome> {
  const bundle = getSeedBundleForModule(moduleKey);
  if (!bundle) {
    return {
      moduleKey,
      collection: moduleKey,
      labelBn: moduleKey,
      labelEn: moduleKey,
      seededCount: 0,
      skippedCount: 0,
      errors: [],
    };
  }
  const outcome = await seedBundleIdempotently(bundle);
  if (logIndividualAudit && outcome.seededCount > 0) {
    await recordAuditLog({
      actorUid: actor.uid,
      actorEmail: actor.email,
      action: 'SEED_MODULE_DEFAULTS',
      targetCollection: bundle.collection,
      targetId: `seeded_${outcome.seededCount}`,
    }).catch(() => undefined);
  }
  return outcome;
}

async function seedBundleIdempotently(
  bundle: ModuleSeedBundle
): Promise<ModuleMigrationOutcome> {
  let seededCount = 0;
  let skippedCount = 0;
  const errors: string[] = [];

  for (const record of bundle.records) {
    try {
      const ref = doc(db, bundle.collection, record.id);
      const snap = await getDoc(ref);
      if (snap.exists()) {
        skippedCount += 1;
        continue;
      }
      await setDoc(ref, {
        ...record.data,
        updatedAt: serverTimestamp(),
      });
      seededCount += 1;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      errors.push(`${bundle.collection}/${record.id}: ${msg}`);
    }
  }

  return {
    moduleKey: bundle.moduleKey,
    collection: bundle.collection,
    labelBn: bundle.labelBn,
    labelEn: bundle.labelEn,
    seededCount,
    skippedCount,
    errors,
  };
}

/**
 * Runs idempotent migration across all 28+ CMS modules/singletons in exact parity
 * with Android DefaultPortalContent and PortalRepository defaults.
 */
export async function runFullPortalContentMigration(
  actor: AdminUserProfile
): Promise<FullMigrationReport> {
  const outcomes: ModuleMigrationOutcome[] = [];
  for (const bundle of DEFAULT_MIGRATION_BUNDLES) {
    const outcome = await seedBundleIdempotently(bundle);
    outcomes.push(outcome);
  }

  const totalSeeded = outcomes.reduce((acc, o) => acc + o.seededCount, 0);
  const totalSkipped = outcomes.reduce((acc, o) => acc + o.skippedCount, 0);
  const totalErrors = outcomes.reduce((acc, o) => acc + o.errors.length, 0);

  await recordAuditLog({
    actorUid: actor.uid,
    actorEmail: actor.email,
    action: 'MIGRATE_DEFAULT_CONTENT',
    targetCollection: 'portal_cms_all',
    targetId: `seeded_${totalSeeded}_skipped_${totalSkipped}`,
  }).catch(() => undefined);

  return {
    outcomes,
    totalSeeded,
    totalSkipped,
    totalErrors,
  };
}
