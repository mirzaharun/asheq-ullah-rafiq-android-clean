import {
  Unsubscribe,
  addDoc,
  collection,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
} from 'firebase/firestore';
import { db } from '../lib/firebase';
import { AuditLogEntry } from '../types/portal';

export interface RecordAuditParams {
  actorUid: string;
  actorEmail?: string;
  action: string;
  targetCollection: string;
  targetId?: string;
}

export async function recordAuditLog(params: RecordAuditParams): Promise<void> {
  await addDoc(collection(db, 'audit_logs'), {
    actorUid: params.actorUid,
    actorEmail: params.actorEmail || '',
    action: params.action,
    targetCollection: params.targetCollection,
    targetId: params.targetId || '',
    createdAt: serverTimestamp(),
  });
}

export function subscribeRecentAuditLogs(
  maxItems: number,
  onData: (logs: AuditLogEntry[]) => void,
  onError: (err: unknown) => void
): Unsubscribe {
  const q = query(
    collection(db, 'audit_logs'),
    orderBy('createdAt', 'desc'),
    limit(maxItems)
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const items: AuditLogEntry[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          actorUid: typeof data.actorUid === 'string' ? data.actorUid : '',
          actorEmail: typeof data.actorEmail === 'string' ? data.actorEmail : undefined,
          action: typeof data.action === 'string' ? data.action : 'unknown',
          targetCollection:
            typeof data.targetCollection === 'string' ? data.targetCollection : '',
          targetId: typeof data.targetId === 'string' ? data.targetId : undefined,
          createdAt: data.createdAt,
        };
      });
      onData(items);
    },
    (error) => {
      onError(error);
    }
  );
}
