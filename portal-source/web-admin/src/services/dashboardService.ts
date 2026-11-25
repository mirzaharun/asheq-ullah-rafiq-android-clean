import { Unsubscribe } from 'firebase/firestore';
import { PORTAL_MODULES } from '../config/modules';
import {
  AdminRole,
  AuditLogEntry,
  BloodDonor,
  CitizenApplication,
  CitizenGrievance,
  CollectionCountStat,
  DashboardLiveStats,
  DevelopmentProject,
  GeneralFeedback,
  NoticeFeedItem,
  SeaTruckUpdate,
  WeatherData,
} from '../types/portal';
import { canAccessModule, canManageApplications } from './authService';
import { subscribeRecentAuditLogs } from './auditService';
import {
  fetchCollectionCount,
  formatFirestoreError,
  isPermissionDeniedError,
  subscribeCollection,
  subscribeSingletonDoc,
} from './firestoreCrud';

export function createInitialDashboardStats(role: AdminRole): DashboardLiveStats {
  const manageApps = canManageApplications(role);

  return {
    lastSyncedAt: null,
    projects: [],
    projectsError: null,
    noticesCount: null,
    urgentNoticesCount: null,
    noticesError: null,
    applications: [],
    applicationsError: null,
    applicationsRestricted: !manageApps,
    grievances: [],
    grievancesError: null,
    grievancesRestricted: !manageApps,
    feedbackCount: null,
    feedbackError: null,
    feedbackRestricted: !manageApps,
    bloodDonors: [],
    bloodDonorsError: null,
    digitalServicesCount: null,
    tourismSpotsCount: null,
    citizenPollsCount: null,
    galleryCount: null,
    mediaArchiveCount: null,
    weather: null,
    seaTruck: null,
    auditLogs: [],
    auditLogsError: null,
    collectionInventory: PORTAL_MODULES.map((mod) => ({
      key: mod.key,
      collection: mod.collection,
      labelBn: mod.titleBn,
      labelEn: mod.titleEn,
      count: null,
      restricted: !canAccessModule(role, mod),
    })),
  };
}

/**
 * Attaches live Firestore listeners & server count aggregations strictly scoped
 * to the verified AdminRole so security rules are respected and zero fake data is shown.
 */
export function subscribeDashboardStats(
  role: AdminRole,
  lang: 'bn' | 'en',
  onUpdate: (next: DashboardLiveStats) => void
): Unsubscribe {
  let state = createInitialDashboardStats(role);
  let disposed = false;

  const emit = (patch: Partial<DashboardLiveStats>) => {
    if (disposed) return;
    state = {
      ...state,
      ...patch,
      lastSyncedAt: new Date(),
    };
    onUpdate(state);
  };

  const unsubs: Unsubscribe[] = [];

  // 1. Public-read CMS live listeners
  unsubs.push(
    subscribeCollection<DevelopmentProject>(
      'development_projects',
      [],
      (items) => emit({ projects: items, projectsError: null }),
      (err) => emit({ projectsError: formatFirestoreError(err, lang) })
    )
  );

  unsubs.push(
    subscribeCollection<NoticeFeedItem>(
      'notices',
      [],
      (items) =>
        emit({
          noticesCount: items.length,
          urgentNoticesCount: items.filter((n) => Boolean(n.isUrgent)).length,
          noticesError: null,
        }),
      (err) => emit({ noticesError: formatFirestoreError(err, lang) })
    )
  );

  unsubs.push(
    subscribeCollection<BloodDonor>(
      'blood_donors',
      [],
      (items) => emit({ bloodDonors: items, bloodDonorsError: null }),
      (err) => emit({ bloodDonorsError: formatFirestoreError(err, lang) })
    )
  );

  unsubs.push(
    subscribeSingletonDoc<WeatherData>(
      'weather_data',
      'current',
      (data) => emit({ weather: data }),
      () => {
        // Ignore non-critical banner error
      }
    )
  );

  unsubs.push(
    subscribeSingletonDoc<SeaTruckUpdate>(
      'sea_truck_updates',
      'current',
      (data) => emit({ seaTruck: data }),
      () => {
        // Ignore non-critical banner error
      }
    )
  );

  // 2. Citizen Applications, Grievances & Feedback: allowed ONLY for super_admin & application_manager
  if (canManageApplications(role)) {
    unsubs.push(
      subscribeCollection<CitizenApplication>(
        'applications',
        [],
        (items) =>
          emit({
            applications: items,
            applicationsError: null,
            applicationsRestricted: false,
          }),
        (err) =>
          emit({
            applicationsRestricted: isPermissionDeniedError(err),
            applicationsError: formatFirestoreError(err, lang),
          })
      )
    );

    unsubs.push(
      subscribeCollection<CitizenGrievance>(
        'citizen_grievances',
        [],
        (items) =>
          emit({
            grievances: items,
            grievancesError: null,
            grievancesRestricted: false,
          }),
        (err) =>
          emit({
            grievancesRestricted: isPermissionDeniedError(err),
            grievancesError: formatFirestoreError(err, lang),
          })
      )
    );

    unsubs.push(
      subscribeCollection<GeneralFeedback>(
        'general_feedback',
        [],
        (items) =>
          emit({
            feedbackCount: items.length,
            feedbackError: null,
            feedbackRestricted: false,
          }),
        (err) =>
          emit({
            feedbackRestricted: isPermissionDeniedError(err),
            feedbackError: formatFirestoreError(err, lang),
          })
      )
    );
  }

  // 3. Audit logs: allowed for all 3 admin roles per firestore.rules
  unsubs.push(
    subscribeRecentAuditLogs(
      12,
      (logs: AuditLogEntry[]) => emit({ auditLogs: logs, auditLogsError: null }),
      (err) => emit({ auditLogsError: formatFirestoreError(err, lang) })
    )
  );

  // 4. Server-side aggregate counts across all registered modules respecting RBAC
  Promise.all(
    PORTAL_MODULES.map(async (mod): Promise<CollectionCountStat> => {
      if (!canAccessModule(role, mod)) {
        return {
          key: mod.key,
          collection: mod.collection,
          labelBn: mod.titleBn,
          labelEn: mod.titleEn,
          count: null,
          restricted: true,
        };
      }
      try {
        const count = await fetchCollectionCount(mod.collection);
        return {
          key: mod.key,
          collection: mod.collection,
          labelBn: mod.titleBn,
          labelEn: mod.titleEn,
          count,
          restricted: false,
        };
      } catch (err) {
        return {
          key: mod.key,
          collection: mod.collection,
          labelBn: mod.titleBn,
          labelEn: mod.titleEn,
          count: null,
          restricted: isPermissionDeniedError(err),
          error: formatFirestoreError(err, lang),
        };
      }
    })
  ).then((inventory) => {
    const findCount = (key: string) =>
      inventory.find((i) => i.key === key)?.count ?? null;
    emit({
      collectionInventory: inventory,
      galleryCount: findCount('gallery_items'),
      mediaArchiveCount: findCount('media_items'),
      digitalServicesCount: findCount('digital_services'),
      tourismSpotsCount: findCount('tourism_spots'),
      citizenPollsCount: findCount('citizen_polls'),
    });
  });

  return () => {
    disposed = true;
    unsubs.forEach((u) => u());
  };
}
