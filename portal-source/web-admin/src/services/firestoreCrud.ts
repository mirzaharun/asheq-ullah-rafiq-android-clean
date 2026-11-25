import {
  DocumentData,
  FirestoreError,
  QueryConstraint,
  Unsubscribe,
  collection,
  deleteDoc,
  doc,
  getCountFromServer,
  getDoc,
  getDocs,
  onSnapshot,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import { db } from '../lib/firebase';

const VALID_ID_REGEX = /^[A-Za-z0-9_-]{1,128}$/;

export function isValidFirestoreId(id: string): boolean {
  return VALID_ID_REGEX.test(id);
}

export function assertValidFirestoreId(id: string, label = 'Document ID'): string {
  const trimmed = id.trim();
  if (!isValidFirestoreId(trimmed)) {
    throw new Error(
      `${label} অবৈধ: শুধুমাত্র ইংরেজি অক্ষর, সংখ্যা, '-' এবং '_' অনুমোদিত।`
    );
  }
  return trimmed;
}

export function isPermissionDeniedError(err: unknown): boolean {
  if (err && typeof err === 'object' && 'code' in err) {
    const code = String((err as { code?: unknown }).code);
    return code === 'permission-denied' || code === 'PERMISSION_DENIED';
  }
  return false;
}

export function formatFirestoreError(err: unknown, lang: 'bn' | 'en' = 'bn'): string {
  if (err instanceof FirestoreError || (err && typeof err === 'object' && 'code' in err)) {
    const code = String((err as { code?: unknown }).code || '');
    if (code === 'permission-denied' || code === 'PERMISSION_DENIED') {
      return lang === 'bn'
        ? 'অনুমতি প্রত্যাখ্যাত (Firestore Rules): আপনার রোলে এই তথ্যে প্রবেশাধিকার নেই।'
        : 'Permission denied by Firestore security rules for your role.';
    }
    if (code === 'unavailable') {
      return lang === 'bn'
        ? 'নেটওয়ার্ক বা ফায়ারস্টোর সংযোগ সাময়িকভাবে বিচ্ছিন্ন।'
        : 'Firestore service temporarily unavailable.';
    }
    if (code === 'failed-precondition') {
      return lang === 'bn'
        ? 'ফায়ারস্টোর ইনডেক্স বা পূর্বশর্ত পূরণ হয়নি।'
        : 'Firestore query precondition failed.';
    }
    if (code === 'not-found') {
      return lang === 'bn' ? 'কাঙ্ক্ষিত ডকুমেন্টটি পাওয়া যায়নি।' : 'Document not found.';
    }
  }
  if (err instanceof Error) return err.message;
  return lang === 'bn' ? 'অজানা ডাটাবেজ ত্রুটি ঘটেছে।' : 'Unknown database error occurred.';
}

export function subscribeCollection<T extends { id: string }>(
  collectionName: string,
  constraints: QueryConstraint[],
  onData: (items: T[]) => void,
  onError: (err: unknown) => void
): Unsubscribe {
  const ref = collection(db, collectionName);
  const q = constraints.length > 0 ? query(ref, ...constraints) : ref;
  return onSnapshot(
    q,
    (snapshot) => {
      const rows = snapshot.docs.map((d) => ({
        id: d.id,
        ...(d.data() as Record<string, unknown>),
      })) as T[];
      onData(rows);
    },
    (error) => {
      onError(error);
    }
  );
}

export function subscribeSingletonDoc<T = DocumentData>(
  collectionName: string,
  docId: string,
  onData: (data: (T & { id: string }) | null) => void,
  onError: (err: unknown) => void
): Unsubscribe {
  const ref = doc(db, collectionName, docId);
  return onSnapshot(
    ref,
    (snap) => {
      if (!snap.exists()) {
        onData(null);
      } else {
        onData({ id: snap.id, ...(snap.data() as T) });
      }
    },
    (error) => {
      onError(error);
    }
  );
}

export async function fetchCollection<T extends { id: string }>(
  collectionName: string,
  constraints: QueryConstraint[] = []
): Promise<T[]> {
  const ref = collection(db, collectionName);
  const q = constraints.length > 0 ? query(ref, ...constraints) : ref;
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({
    id: d.id,
    ...(d.data() as Record<string, unknown>),
  })) as T[];
}

export async function fetchSingletonDoc<T = DocumentData>(
  collectionName: string,
  docId: string
): Promise<(T & { id: string }) | null> {
  const snap = await getDoc(doc(db, collectionName, docId));
  if (!snap.exists()) return null;
  return { id: snap.id, ...(snap.data() as T) };
}

export async function upsertDoc(
  collectionName: string,
  docId: string,
  payload: Record<string, unknown>,
  merge = true
): Promise<void> {
  const safeId = assertValidFirestoreId(docId);
  await setDoc(
    doc(db, collectionName, safeId),
    {
      ...payload,
      updatedAt: serverTimestamp(),
    },
    { merge }
  );
}

export async function createDoc(
  collectionName: string,
  docId: string,
  payload: Record<string, unknown>
): Promise<void> {
  const safeId = assertValidFirestoreId(docId);
  const ref = doc(db, collectionName, safeId);
  await runTransaction(db, async (transaction) => {
    const existing = await transaction.get(ref);
    if (existing.exists()) {
      throw new Error('A record with this document ID already exists.');
    }
    transaction.set(ref, {
      ...payload,
      updatedAt: serverTimestamp(),
    });
  });
}

export async function updateDocFields(
  collectionName: string,
  docId: string,
  fields: Record<string, unknown>
): Promise<void> {
  const safeId = assertValidFirestoreId(docId);
  await updateDoc(doc(db, collectionName, safeId), {
    ...fields,
    updatedAt: serverTimestamp(),
  });
}

export async function removeDoc(collectionName: string, docId: string): Promise<void> {
  const safeId = assertValidFirestoreId(docId);
  await deleteDoc(doc(db, collectionName, safeId));
}

export async function fetchCollectionCount(collectionName: string): Promise<number> {
  const snap = await getCountFromServer(collection(db, collectionName));
  return snap.data().count;
}
