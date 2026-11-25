import { useRef, useState, type ChangeEvent } from 'react';
import { AlertCircle, FileText, UploadCloud, X } from 'lucide-react';
import type { UploadTask } from 'firebase/storage';
import { useLanguage } from '../../hooks/useLanguage';
import {
  isUnsafeLocalUrl,
  startValidatedStorageUpload,
  type UploadCategory,
} from '../../services/firebase/storageService';

interface FileUploadFieldProps {
  label: string;
  value: string;
  onChange: (url: string) => void;
  category: UploadCategory;
  accept?: string;
  helperText?: string;
}

export function FileUploadField({
  label,
  value,
  onChange,
  category,
  accept,
  helperText,
}: FileUploadFieldProps) {
  const { lang } = useLanguage();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const taskRef = useRef<UploadTask | null>(null);

  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setError(null);
    setProgress(0);
    setUploading(true);

    const task = startValidatedStorageUpload({
      file,
      category,
      lang,
      onProgress: (pct) => setProgress(pct),
      onComplete: (downloadUrl) => {
        setUploading(false);
        setProgress(100);
        onChange(downloadUrl);
      },
      onError: (message) => {
        setUploading(false);
        setError(message);
      },
    });

    if (!task) {
      setUploading(false);
    } else {
      taskRef.current = task;
    }

    e.target.value = '';
  };

  const handleCancelUpload = () => {
    taskRef.current?.cancel();
    taskRef.current = null;
    setUploading(false);
    setProgress(0);
  };

  const handleUrlInput = (nextUrl: string) => {
    if (isUnsafeLocalUrl(nextUrl)) {
      setError(
        lang === 'bn'
          ? 'নিরাপত্তার স্বার্থে স্থানীয় file:// বা blob: URL ব্যবহার করা যাবে না।'
          : 'Local file:// or blob: URLs are blocked for security.'
      );
      return;
    }
    setError(null);
    onChange(nextUrl);
  };

  const isHttpImage =
    /^https?:\/\//i.test(value) &&
    category !== 'document' &&
    category !== 'video';

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-2">
        <label className="block text-xs font-semibold text-slate-700">
          {label}
        </label>
        {helperText ? (
          <span className="text-[11px] text-slate-500">{helperText}</span>
        ) : null}
      </div>

      <div className="flex flex-col sm:flex-row gap-2">
        <input
          type="text"
          value={value}
          onChange={(e) => handleUrlInput(e.target.value)}
          placeholder="https://... অথবা নিচে থেকে ফাইল আপলোড করুন"
          className="flex-1 px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
        />

        <input
          ref={inputRef}
          type="file"
          accept={accept}
          onChange={handleFileChange}
          className="hidden"
        />

        {uploading ? (
          <button
            type="button"
            onClick={handleCancelUpload}
            className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 whitespace-nowrap cursor-pointer"
          >
            <X className="w-3.5 h-3.5" />
            <span>{lang === 'bn' ? 'বাতিল' : 'Cancel'}</span>
          </button>
        ) : (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 whitespace-nowrap cursor-pointer"
          >
            <UploadCloud className="w-3.5 h-3.5" />
            <span>{lang === 'bn' ? 'ফাইল আপলোড' : 'Upload File'}</span>
          </button>
        )}
      </div>

      {uploading ? (
        <div className="space-y-1">
          <div className="w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-emerald-600 transition-all duration-150"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-[11px] font-mono text-slate-600 tabular-nums">
            {lang === 'bn' ? `আপলোড হচ্ছে: ${progress}%` : `Uploading: ${progress}%`}
          </p>
        </div>
      ) : null}

      {error ? (
        <div className="flex items-start gap-2 text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg p-2.5">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      ) : null}

      {value ? (
        <div className="flex items-center gap-3 p-2 bg-slate-50 border border-slate-200 rounded-lg">
          {isHttpImage ? (
            <img
              src={value}
              alt={label}
              referrerPolicy="no-referrer"
              className="w-12 h-12 rounded object-cover border border-slate-200 bg-white shrink-0"
            />
          ) : (
            <div className="w-10 h-10 rounded bg-white border border-slate-200 flex items-center justify-center text-slate-500 shrink-0">
              <FileText className="w-4 h-4" />
            </div>
          )}
          <div className="min-w-0 flex-1">
            <p className="text-xs font-mono text-slate-600 truncate">{value}</p>
            {value.startsWith('drawable:') ? (
              <p className="text-[11px] text-slate-500">
                {lang === 'bn'
                  ? 'অ্যান্ড্রয়েড লোকাল রিসোর্স রেফারেন্স'
                  : 'Android drawable resource reference'}
              </p>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
