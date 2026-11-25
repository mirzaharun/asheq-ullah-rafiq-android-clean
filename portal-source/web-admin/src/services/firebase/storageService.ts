import {
  deleteObject,
  getDownloadURL,
  ref,
  uploadBytesResumable,
  type UploadTask,
} from 'firebase/storage';
import { storage } from './app';
import type { Language } from '../../utils/i18n';

export type UploadCategory =
  | 'profile'
  | 'project'
  | 'gallery'
  | 'video'
  | 'document'
  | 'attachment';

interface CategoryRules {
  folder: string;
  maxSizeBytes: number;
  maxSizeLabel: string;
  allowedMimeTypes: string[];
  allowedExtensions: string[];
}

const UPLOAD_RULES: Record<UploadCategory, CategoryRules> = {
  profile: {
    folder: 'uploads/profiles',
    maxSizeBytes: 5 * 1024 * 1024,
    maxSizeLabel: '5 MB',
    allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.webp'],
  },
  project: {
    folder: 'uploads/projects',
    maxSizeBytes: 5 * 1024 * 1024,
    maxSizeLabel: '5 MB',
    allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.webp'],
  },
  gallery: {
    folder: 'uploads/gallery',
    maxSizeBytes: 5 * 1024 * 1024,
    maxSizeLabel: '5 MB',
    allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.webp'],
  },
  video: {
    folder: 'uploads/videos',
    maxSizeBytes: 50 * 1024 * 1024,
    maxSizeLabel: '50 MB',
    allowedMimeTypes: ['video/mp4', 'video/webm'],
    allowedExtensions: ['.mp4', '.webm'],
  },
  document: {
    folder: 'uploads/documents',
    maxSizeBytes: 10 * 1024 * 1024,
    maxSizeLabel: '10 MB',
    allowedMimeTypes: ['application/pdf'],
    allowedExtensions: ['.pdf'],
  },
  attachment: {
    folder: 'uploads/attachments',
    maxSizeBytes: 10 * 1024 * 1024,
    maxSizeLabel: '10 MB',
    allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'],
    allowedExtensions: ['.jpg', '.jpeg', '.png', '.webp', '.pdf'],
  },
};

export function validateUploadFile(
  file: File,
  category: UploadCategory,
  lang: Language
): string | null {
  const rules = UPLOAD_RULES[category];
  const lowerName = file.name.toLowerCase();
  const hasAllowedExt = rules.allowedExtensions.some((ext) => lowerName.endsWith(ext));
  const hasAllowedMime = rules.allowedMimeTypes.includes(file.type);

  if (!hasAllowedExt || (file.type && !hasAllowedMime)) {
    return lang === 'bn'
      ? `অনুমোদিত ফাইল ফরম্যাট নয়। গ্রহণযোগ্য ফরম্যাট: ${rules.allowedExtensions.join(', ')}`
      : `Unsupported file format. Allowed formats: ${rules.allowedExtensions.join(', ')}`;
  }

  if (file.size <= 0) {
    return lang === 'bn' ? 'খালি ফাইল আপলোড করা যাবে না।' : 'Cannot upload an empty file.';
  }

  if (file.size > rules.maxSizeBytes) {
    return lang === 'bn'
      ? `ফাইলের আকার সর্বোচ্চ ${rules.maxSizeLabel} এর মধ্যে হতে হবে।`
      : `File size must not exceed ${rules.maxSizeLabel}.`;
  }

  return null;
}

export function isUnsafeLocalUrl(url: string): boolean {
  const trimmed = url.trim().toLowerCase();
  return trimmed.startsWith('file://') || trimmed.startsWith('blob:') || trimmed.startsWith('javascript:');
}

function sanitizeFileName(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, '_').slice(-80);
}

export function startValidatedStorageUpload(params: {
  file: File;
  category: UploadCategory;
  lang: Language;
  onProgress: (percent: number) => void;
  onComplete: (downloadUrl: string, storagePath: string) => void;
  onError: (message: string) => void;
}): UploadTask | null {
  const { file, category, lang, onProgress, onComplete, onError } = params;
  const validationError = validateUploadFile(file, category, lang);
  if (validationError) {
    onError(validationError);
    return null;
  }

  const rules = UPLOAD_RULES[category];
  const storagePath = `${rules.folder}/${Date.now()}_${sanitizeFileName(file.name)}`;
  const storageRef = ref(storage, storagePath);
  const task = uploadBytesResumable(storageRef, file, {
    contentType: file.type || undefined,
  });

  task.on(
    'state_changed',
    (snapshot) => {
      const pct =
        snapshot.totalBytes > 0
          ? Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100)
          : 0;
      onProgress(pct);
    },
    (error) => {
      const code = (error as { code?: string }).code ?? '';
      if (code === 'storage/canceled') {
        onError(lang === 'bn' ? 'আপলোড বাতিল করা হয়েছে।' : 'Upload was canceled.');
        return;
      }
      if (code === 'storage/unauthorized') {
        onError(
          lang === 'bn'
            ? 'Firebase Storage Rules অনুযায়ী আপনার এই ফাইল আপলোডের অনুমতি নেই।'
            : 'Upload blocked by Firebase Storage Security Rules (storage/unauthorized).'
        );
        return;
      }
      onError(
        error.message ||
          (lang === 'bn' ? 'ফাইল আপলোড ব্যর্থ হয়েছে।' : 'File upload failed.')
      );
    },
    async () => {
      try {
        const url = await getDownloadURL(task.snapshot.ref);
        onComplete(url, storagePath);
      } catch (err) {
        onError(
          err instanceof Error
            ? err.message
            : lang === 'bn'
            ? 'ডাউনলোড লিংক পাওয়া যায়নি।'
            : 'Failed to retrieve uploaded file URL.'
        );
      }
    }
  );

  return task;
}

export async function removeStorageObjectByPath(storagePath: string): Promise<void> {
  if (!storagePath) return;
  await deleteObject(ref(storage, storagePath));
}
