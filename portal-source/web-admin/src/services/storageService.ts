import {
  deleteObject,
  getDownloadURL,
  ref,
  uploadBytesResumable,
} from 'firebase/storage';
import { storage } from '../lib/firebase';

const MAX_CMS_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB per storage.rules

const MODULE_FOLDER_MAP: Record<string, string> = {
  education_resources: 'education_documents',
  economy_opportunities: 'employment_circulars',
  welfare_programs: 'welfare_documents',
  digital_services: 'service_media',
  union_services: 'service_media',
  tourism_spots: 'tourism_media',
  government_reports: 'report_documents',
  special_events: 'event_media',
  daily_schedules: 'event_media',
  development_projects: 'development_media',
  gallery_items: 'gallery',
  media_items: 'media',
  profile_info: 'profile',
  config: 'banners',
  media_assets: 'banners',
};

const DOCUMENT_MIME_TYPES = new Set([
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.oasis.opendocument.text',
  'text/plain',
]);

export function resolveStorageFolder(moduleKey: string, fieldKey?: string): string {
  if (moduleKey === 'config' && fieldKey === 'apkUrl') {
    return 'app_releases';
  }
  if (moduleKey === 'development_projects' && fieldKey === 'pdfUrl') {
    return 'development_documents';
  }
  return MODULE_FOLDER_MAP[moduleKey] || 'cms_media';
}

export function formatFileSizeMb(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function isAllowedCmsMimeType(
  mimeType: string,
  moduleKey?: string,
  fieldKey?: string
): boolean {
  if (
    mimeType === 'application/vnd.android.package-archive' &&
    moduleKey === 'config' &&
    fieldKey === 'apkUrl'
  ) {
    return true;
  }
  return (
    mimeType.startsWith('image/') ||
    mimeType.startsWith('video/') ||
    mimeType === 'application/pdf' ||
    DOCUMENT_MIME_TYPES.has(mimeType)
  );
}

export interface UploadCmsFileResult {
  downloadUrl: string;
  storagePath: string;
  fileSizeMb: string;
  contentType: string;
}

export async function uploadCmsAsset(
  moduleKey: string,
  file: File,
  onProgress?: (percent: number) => void,
  fieldKey?: string
): Promise<UploadCmsFileResult> {
  if (!isAllowedCmsMimeType(file.type, moduleKey, fieldKey)) {
    throw new Error(
      'অবৈধ ফাইল ফরম্যাট: ছবি, ভিডিও, PDF, DOC/DOCX/TXT অথবা config-এর apkUrl-এ APK অনুমোদিত।'
    );
  }
  if (file.size > MAX_CMS_FILE_SIZE_BYTES) {
    throw new Error('ফাইলের আকার ৫০ মেগাবাইটের বেশি হতে পারবে না (সর্বোচ্চ ৫০ MB)।');
  }

  const folder = resolveStorageFolder(moduleKey, fieldKey);
  const safeName = file.name.replace(/[^A-Za-z0-9._-]/g, '_');
  const storagePath = `${folder}/${Date.now()}_${safeName}`;
  const storageRef = ref(storage, storagePath);

  await new Promise<void>((resolve, reject) => {
    const task = uploadBytesResumable(storageRef, file, {
      contentType: file.type,
    });
    task.on(
      'state_changed',
      (snap) => {
        if (onProgress && snap.totalBytes > 0) {
          onProgress(Math.round((snap.bytesTransferred / snap.totalBytes) * 100));
        }
      },
      (err) => reject(err),
      () => resolve()
    );
  });

  const downloadUrl = await getDownloadURL(storageRef);
  return {
    downloadUrl,
    storagePath,
    fileSizeMb: formatFileSizeMb(file.size),
    contentType: file.type,
  };
}

export async function deleteStorageUrlIfOwned(url: string): Promise<void> {
  if (!url || !url.includes('firebasestorage.googleapis.com')) return;
  try {
    const storageRef = ref(storage, url);
    await deleteObject(storageRef);
  } catch {
    // Non-fatal if already removed or external
  }
}
