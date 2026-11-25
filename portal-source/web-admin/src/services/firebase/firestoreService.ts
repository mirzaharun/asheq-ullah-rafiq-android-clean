import {
  collection,
  getCountFromServer,
  limit,
  onSnapshot,
  query,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './app';
import { OperationType, reportFirestoreError } from './errorHandler';
import type {
  AuditLogRecord,
  CitizenRequestRecord,
  DashboardMetrics,
  DevelopmentProjectDoc,
  NoticeDoc,
} from '../../types/firestore';

const PENDING_STATUSES = new Set([
  'submitted',
  'under review',
  'additional information required',
  'forwarded to relevant office',
  'in progress',
  'pending',
]);

const RESOLVED_STATUSES = new Set(['resolved', 'completed', 'approved']);

async function safeCount(collectionName: string): Promise<number | null> {
  try {
    const snap = await getCountFromServer(collection(db, collectionName));
    return snap.data().count;
  } catch (err) {
    reportFirestoreError(err, OperationType.LIST, collectionName);
    return null;
  }
}

export async function fetchDashboardSnapshotCounts(): Promise<DashboardMetrics> {
  const [
    totalCitizens,
    totalProjects,
    publishedNotices,
    totalApplications,
    galleryItemsCount,
    bloodDonorsCount,
    watercraftRoutesCount,
    citizenGrievancesCount,
    appointmentsCount,
  ] = await Promise.all([
    safeCount('users'),
    safeCount('development_projects'),
    safeCount('notices'),
    safeCount('applications'),
    safeCount('gallery_items'),
    safeCount('blood_donors'),
    safeCount('watercraft_schedules'),
    safeCount('citizen_grievances'),
    safeCount('appointments'),
  ]);

  return {
    totalCitizens,
    totalProjects,
    publishedNotices,
    totalApplications,
    pendingApplications: null,
    resolvedApplications: null,
    galleryItemsCount,
    bloodDonorsCount,
    watercraftRoutesCount,
    citizenGrievancesCount,
    appointmentsCount,
  };
}

export function subscribeApplicationsSummary(
  onData: (data: {
    total: number;
    pending: number;
    resolved: number;
    latest: CitizenRequestRecord[];
  }) => void,
  onError: (message: string) => void
): Unsubscribe {
  const path = 'applications';
  return onSnapshot(
    query(collection(db, path)),
    (snapshot) => {
      let pending = 0;
      let resolved = 0;
      const records: CitizenRequestRecord[] = [];

      snapshot.forEach((docSnap) => {
        const raw = docSnap.data();
        const statusStr = typeof raw.status === 'string' ? raw.status.trim().toLowerCase() : 'submitted';
        if (RESOLVED_STATUSES.has(statusStr)) {
          resolved += 1;
        } else if (PENDING_STATUSES.has(statusStr) || statusStr === '') {
          pending += 1;
        }
        records.push({
          id: docSnap.id,
          sourceCollection: 'applications',
          trackingNumber: typeof raw.trackingNumber === 'string' ? raw.trackingNumber : docSnap.id,
          applicantName: typeof raw.applicantName === 'string' ? raw.applicantName : undefined,
          subject: typeof raw.subject === 'string' ? raw.subject : typeof raw.title === 'string' ? raw.title : undefined,
          category: typeof raw.category === 'string' ? raw.category : undefined,
          union: typeof raw.union === 'string' ? raw.union : undefined,
          status: typeof raw.status === 'string' ? raw.status : 'Submitted',
          createdAt: raw.createdAt ?? null,
          updatedAt: raw.updatedAt ?? null,
        });
      });

      onData({
        total: snapshot.size,
        pending,
        resolved,
        latest: records.slice(0, 8),
      });
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}

export function subscribeCitizenGrievances(
  onData: (items: CitizenRequestRecord[]) => void,
  onError: (message: string) => void
): Unsubscribe {
  const path = 'citizen_grievances';
  return onSnapshot(
    query(collection(db, path), limit(10)),
    (snapshot) => {
      const items: CitizenRequestRecord[] = snapshot.docs.map((docSnap) => {
        const raw = docSnap.data();
        return {
          id: docSnap.id,
          sourceCollection: 'citizen_grievances',
          trackingNumber: typeof raw.trackingNumber === 'string' ? raw.trackingNumber : docSnap.id,
          applicantName: typeof raw.applicantName === 'string' ? raw.applicantName : typeof raw.name === 'string' ? raw.name : undefined,
          subject: typeof raw.subject === 'string' ? raw.subject : typeof raw.title === 'string' ? raw.title : undefined,
          category: typeof raw.category === 'string' ? raw.category : undefined,
          union: typeof raw.union === 'string' ? raw.union : undefined,
          status: typeof raw.status === 'string' ? raw.status : 'Submitted',
          createdAt: raw.createdAt ?? null,
          updatedAt: raw.updatedAt ?? null,
        };
      });
      onData(items);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}

export function subscribeRecentProjects(
  onData: (projects: DevelopmentProjectDoc[], totalCount: number) => void,
  onError: (message: string) => void
): Unsubscribe {
  const path = 'development_projects';
  return onSnapshot(
    query(collection(db, path)),
    (snapshot) => {
      const projects: DevelopmentProjectDoc[] = snapshot.docs.map((docSnap) => ({
        ...(docSnap.data() as DevelopmentProjectDoc),
        id: docSnap.id,
      }));
      onData(projects.slice(0, 5), snapshot.size);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}

export function subscribeRecentNotices(
  onData: (notices: NoticeDoc[], totalCount: number) => void,
  onError: (message: string) => void
): Unsubscribe {
  const path = 'notices';
  return onSnapshot(
    query(collection(db, path)),
    (snapshot) => {
      const notices: NoticeDoc[] = snapshot.docs.map((docSnap) => ({
        ...(docSnap.data() as NoticeDoc),
        id: docSnap.id,
      }));
      onData(notices.slice(0, 5), snapshot.size);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}

export function subscribeAuditLogs(
  onData: (logs: AuditLogRecord[]) => void,
  onError: (message: string) => void
): Unsubscribe {
  const path = 'audit_logs';
  return onSnapshot(
    query(collection(db, path), limit(10)),
    (snapshot) => {
      const logs: AuditLogRecord[] = snapshot.docs.map((docSnap) => {
        const raw = docSnap.data();
        return {
          id: docSnap.id,
          actorUid: typeof raw.actorUid === 'string' ? raw.actorUid : undefined,
          actorEmail: typeof raw.actorEmail === 'string' ? raw.actorEmail : undefined,
          actorRole: typeof raw.actorRole === 'string' ? raw.actorRole : undefined,
          action: typeof raw.action === 'string' ? raw.action : 'Updated record',
          targetCollection: typeof raw.targetCollection === 'string' ? raw.targetCollection : undefined,
          targetDocId: typeof raw.targetDocId === 'string' ? raw.targetDocId : undefined,
          summaryBn: typeof raw.summaryBn === 'string' ? raw.summaryBn : undefined,
          summaryEn: typeof raw.summaryEn === 'string' ? raw.summaryEn : undefined,
          createdAt: raw.createdAt ?? null,
        };
      });
      onData(logs);
    },
    (error) => {
      const info = reportFirestoreError(error, OperationType.LIST, path);
      onError(info.error);
    }
  );
}
