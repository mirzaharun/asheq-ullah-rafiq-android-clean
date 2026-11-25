import {
  collection,
  doc,
  limit,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './app';
import { OperationType, reportFirestoreError } from './errorHandler';
import type { AdminIdentity } from '../../types/auth';
import type { AuditLogRecord } from '../../types/firestore';

export async function recordAuditEvent(params: {
  actor: AdminIdentity;
  action: string;
  targetCollection: string;
  targetDocId: string;
  summaryBn: string;
  summaryEn: string;
}): Promise<{ recorded: boolean; error?: string }> {
  const { actor, action, targetCollection, targetDocId, summaryBn, summaryEn } = params;
  const logId = `log_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const path = `audit_logs/${logId}`;

  try {
    await setDoc(doc(db, 'audit_logs', logId), {
      actorUid: actor.uid,
      actorEmail: actor.email,
      actorRole: actor.role,
      action: action.slice(0, 160),
      targetCollection: targetCollection.slice(0, 120),
      targetDocId: targetDocId.slice(0, 128),
      summaryBn: summaryBn.slice(0, 500),
      summaryEn: summaryEn.slice(0, 500),
      createdAt: serverTimestamp(),
    });
    return { recorded: true };
  } catch (err) {
    const info = reportFirestoreError(err, OperationType.CREATE, path);
    return { recorded: false, error: info.error };
  }
}

export function subscribeAuditLogFeed(
  maxItems: number,
  onData: (logs: AuditLogRecord[]) => void,
  onError: (msg: string) => void
): Unsubscribe {
  const path = 'audit_logs';
  return onSnapshot(
    query(collection(db, path), limit(maxItems)),
    (snapshot) => {
      const items: AuditLogRecord[] = snapshot.docs.map((docSnap) => {
        const raw = docSnap.data();
        return {
          id: docSnap.id,
          actorUid: typeof raw.actorUid === 'string' ? raw.actorUid : undefined,
          actorEmail: typeof raw.actorEmail === 'string' ? raw.actorEmail : undefined,
          actorRole: typeof raw.actorRole === 'string' ? raw.actorRole : undefined,
          action: typeof raw.action === 'string' ? raw.action : 'EVENT',
          targetCollection:
            typeof raw.targetCollection === 'string' ? raw.targetCollection : undefined,
          targetDocId: typeof raw.targetDocId === 'string' ? raw.targetDocId : undefined,
          summaryBn: typeof raw.summaryBn === 'string' ? raw.summaryBn : undefined,
          summaryEn: typeof raw.summaryEn === 'string' ? raw.summaryEn : undefined,
          createdAt: raw.createdAt ?? null,
        };
      });

      items.sort((a, b) => {
        const ta =
          a.createdAt && typeof a.createdAt === 'object' && 'toMillis' in a.createdAt
            ? a.createdAt.toMillis()
            : 0;
        const tb =
          b.createdAt && typeof b.createdAt === 'object' && 'toMillis' in b.createdAt
            ? b.createdAt.toMillis()
            : 0;
        return tb - ta;
      });

      onData(items);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}
