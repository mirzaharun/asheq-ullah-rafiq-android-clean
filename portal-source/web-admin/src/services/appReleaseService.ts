import {
  collection,
  doc,
  getDoc,
  onSnapshot,
  query,
  runTransaction,
  serverTimestamp,
  updateDoc,
  where,
} from 'firebase/firestore';
import { getDownloadURL, ref, uploadBytesResumable } from 'firebase/storage';
import { db, storage } from '../lib/firebase';
import { AppRelease } from '../types/portal';
import { recordAuditLog } from './auditService';

export const APP_RELEASES_COLLECTION = 'app_releases';
export const MAX_APK_SIZE_BYTES = 50 * 1024 * 1024;
export const APK_CONTENT_TYPE = 'application/vnd.android.package-archive';
export const CURRENT_ANDROID_VERSION_CODE = 3;

export type AppReleaseStatus = 'draft' | 'published' | 'unpublished';

export function formatApkSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatReleaseDate(value: unknown, lang: 'bn' | 'en'): string {
  if (!value) return '—';
  let date: Date | null = null;
  if (value instanceof Date) {
    date = value;
  } else if (typeof value === 'number') {
    date = new Date(value);
  } else if (
    typeof value === 'object' &&
    value !== null &&
    'toDate' in value &&
    typeof value.toDate === 'function'
  ) {
    date = value.toDate();
  }
  if (!date || Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(lang === 'bn' ? 'bn-BD' : 'en-US', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

interface UploadAppReleaseParams {
  file: File;
  versionCode: number;
  versionName: string;
  releaseNotesBn: string;
  releaseNotesEn: string;
  actorUid: string;
  actorEmail?: string;
  onProgress?: (percent: number) => void;
}

function parseAppRelease(id: string, value: Record<string, unknown>): AppRelease | null {
  const status = value.status;
  if (
    !Number.isInteger(value.versionCode) ||
    typeof value.versionName !== 'string' ||
    typeof value.releaseNotesBn !== 'string' ||
    typeof value.releaseNotesEn !== 'string' ||
    (status !== 'draft' && status !== 'published' && status !== 'unpublished') ||
    typeof value.downloadUrl !== 'string' ||
    typeof value.storagePath !== 'string' ||
    typeof value.fileName !== 'string' ||
    !Number.isInteger(value.fileSizeBytes) ||
    typeof value.sha256 !== 'string' ||
    typeof value.uploadedBy !== 'string'
  ) {
    return null;
  }

  return {
    id,
    versionCode: value.versionCode as number,
    versionName: value.versionName,
    releaseNotesBn: value.releaseNotesBn,
    releaseNotesEn: value.releaseNotesEn,
    status,
    downloadUrl: value.downloadUrl,
    storagePath: value.storagePath,
    fileName: value.fileName,
    fileSizeBytes: value.fileSizeBytes as number,
    sha256: value.sha256,
    uploadedBy: value.uploadedBy,
    uploadedByEmail:
      typeof value.uploadedByEmail === 'string' ? value.uploadedByEmail : '',
    createdAt: value.createdAt,
    releaseDate: value.releaseDate,
    publishedAt: value.publishedAt,
  };
}

export function subscribeAppReleases(
  publishedOnly: boolean,
  onData: (releases: AppRelease[]) => void,
  onError: (error: Error) => void
) {
  const releasesRef = collection(db, APP_RELEASES_COLLECTION);
  const releasesQuery = publishedOnly
    ? query(releasesRef, where('status', '==', 'published'))
    : releasesRef;

  return onSnapshot(
    releasesQuery,
    (snapshot) => {
      const parsed = snapshot.docs.map((item) =>
        parseAppRelease(item.id, item.data() as Record<string, unknown>)
      );
      if (parsed.some((release) => release === null)) {
        onError(new Error('A release record has invalid or incomplete metadata.'));
      }
      onData(
        parsed
          .filter((release): release is AppRelease => release !== null)
          .sort((a, b) => b.versionCode - a.versionCode)
      );
    },
    onError
  );
}

function formatError(error: unknown): string {
  return error instanceof Error ? error.message : 'Unknown Firebase error.';
}

async function writeReleaseAudit(
  actorUid: string,
  actorEmail: string | undefined,
  action: string,
  releaseId: string
): Promise<void> {
  try {
    await recordAuditLog({
      actorUid,
      actorEmail,
      action,
      targetCollection: APP_RELEASES_COLLECTION,
      targetId: releaseId,
    });
  } catch (error) {
    throw new Error(
      `The release change was saved, but its audit log could not be written: ${formatError(error)}`
    );
  }
}

export async function uploadAppRelease({
  file,
  versionCode,
  versionName,
  releaseNotesBn,
  releaseNotesEn,
  actorUid,
  actorEmail,
  onProgress,
}: UploadAppReleaseParams): Promise<void> {
  const normalizedVersionName = versionName.trim();
  if (!Number.isInteger(versionCode) || versionCode < 1 || versionCode > 2_100_000_000) {
    throw new Error('Version code must be a positive Android version code.');
  }
  if (!normalizedVersionName || normalizedVersionName.length > 64) {
    throw new Error('Version name is required and must be 64 characters or fewer.');
  }
  if (!releaseNotesBn.trim() && !releaseNotesEn.trim()) {
    throw new Error('Add release notes in at least one language.');
  }
  if (releaseNotesBn.length > 4000 || releaseNotesEn.length > 4000) {
    throw new Error('Release notes must be 4,000 characters or fewer per language.');
  }
  if (!file.name.toLowerCase().endsWith('.apk') || file.size <= 0) {
    throw new Error('Select a non-empty APK file.');
  }
  if (file.size > MAX_APK_SIZE_BYTES) {
    throw new Error('APK files must be 50 MB or smaller.');
  }
  if (!globalThis.crypto?.subtle || !globalThis.crypto.randomUUID) {
    throw new Error('Secure browser cryptography is required to upload APK releases.');
  }

  const releaseId = `v${versionCode}`;
  const releaseRef = doc(db, APP_RELEASES_COLLECTION, releaseId);
  if ((await getDoc(releaseRef)).exists()) {
    throw new Error(`Version code ${versionCode} already has a release record.`);
  }

  const sha256Bytes = await globalThis.crypto.subtle.digest(
    'SHA-256',
    await file.arrayBuffer()
  );
  const sha256 = Array.from(new Uint8Array(sha256Bytes))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
  const storagePath = `app_release_packages/${versionCode}/${globalThis.crypto.randomUUID()}.apk`;
  const safeBaseName = file.name
    .replace(/[^A-Za-z0-9._-]/g, '_')
    .replace(/[.]apk$/i, '')
    .slice(0, 145);
  const safeFileName = `${safeBaseName || `release-${versionCode}`}.apk`;
  const packageRef = ref(storage, storagePath);

  await new Promise<void>((resolve, reject) => {
    const task = uploadBytesResumable(packageRef, file, {
      contentType: APK_CONTENT_TYPE,
      contentDisposition: `attachment; filename="${safeFileName}"`,
    });
    task.on(
      'state_changed',
      (snapshot) => {
        if (snapshot.totalBytes > 0) {
          onProgress?.(
            Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100)
          );
        }
      },
      reject,
      resolve
    );
  });

  const downloadUrl = await getDownloadURL(packageRef);
  try {
    await runTransaction(db, async (transaction) => {
      const existing = await transaction.get(releaseRef);
      if (existing.exists()) {
        throw new Error(`Version code ${versionCode} already has a release record.`);
      }
      transaction.set(releaseRef, {
        versionCode,
        versionName: normalizedVersionName,
        releaseNotesBn: releaseNotesBn.trim(),
        releaseNotesEn: releaseNotesEn.trim(),
        status: 'draft',
        downloadUrl,
        storagePath,
        fileName: safeFileName,
        fileSizeBytes: file.size,
        sha256,
        uploadedBy: actorUid,
        uploadedByEmail: actorEmail || '',
        createdAt: serverTimestamp(),
        releaseDate: null,
        publishedAt: null,
        updatedAt: serverTimestamp(),
      });
    });
  } catch (error) {
    throw new Error(
      `The APK upload finished, but its release record could not be saved (package path: ${storagePath}): ${formatError(error)}`
    );
  }

  await writeReleaseAudit(actorUid, actorEmail, 'APK_UPLOAD', releaseId);
}

export async function setAppReleasePublished(
  release: AppRelease,
  published: boolean,
  actorUid: string,
  actorEmail?: string
): Promise<void> {
  const releaseRef = doc(db, APP_RELEASES_COLLECTION, release.id);
  await updateDoc(releaseRef, {
    status: published ? 'published' : 'unpublished',
    publishedAt: published ? serverTimestamp() : null,
    releaseDate: published ? release.releaseDate || serverTimestamp() : release.releaseDate || null,
    updatedAt: serverTimestamp(),
  });
  await writeReleaseAudit(
    actorUid,
    actorEmail,
    published ? 'APK_PUBLISH' : 'APK_UNPUBLISH',
    release.id
  );
}
