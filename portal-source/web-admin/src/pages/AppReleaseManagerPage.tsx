import React, { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Download,
  PackageCheck,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  UploadCloud,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import {
  CURRENT_ANDROID_VERSION_CODE,
  formatApkSize,
  formatReleaseDate,
  setAppReleasePublished,
  subscribeAppReleases,
  uploadAppRelease,
} from '../services/appReleaseService';
import { AppRelease } from '../types/portal';

function releaseStatusLabel(status: AppRelease['status'], lang: 'bn' | 'en'): string {
  if (status === 'published') return lang === 'bn' ? 'প্রকাশিত' : 'Published';
  if (status === 'unpublished') return lang === 'bn' ? 'অপ্রকাশিত' : 'Unpublished';
  return lang === 'bn' ? 'খসড়া' : 'Draft';
}

function formatError(error: unknown): string {
  return error instanceof Error ? error.message : 'An unexpected error occurred.';
}

export const AppReleaseManagerPage: React.FC = () => {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();
  const [releases, setReleases] = useState<AppRelease[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [versionName, setVersionName] = useState('');
  const [versionCode, setVersionCode] = useState<number | ''>('');
  const [releaseNotesBn, setReleaseNotesBn] = useState('');
  const [releaseNotesEn, setReleaseNotesEn] = useState('');
  const [apkFile, setApkFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [busyReleaseId, setBusyReleaseId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setLoadError(null);
    return subscribeAppReleases(
      false,
      setReleases,
      (error) => setLoadError(formatError(error))
    );
  }, []);

  const latestPublishedVersionCode = useMemo(
    () =>
      releases.reduce(
        (latest, release) =>
          release.status === 'published'
            ? Math.max(latest, release.versionCode)
            : latest,
        CURRENT_ANDROID_VERSION_CODE
      ),
    [releases]
  );
  const publishedCount = useMemo(
    () => releases.filter((release) => release.status === 'published').length,
    [releases]
  );

  const handleUpload = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setActionError(null);
    setSuccess(null);
    if (!adminUser) {
      setActionError(t('সুপার এডমিন সেশন পাওয়া যায়নি।', 'Super Admin session is unavailable.'));
      return;
    }
    if (!apkFile) {
      setActionError(t('একটি APK ফাইল নির্বাচন করুন।', 'Select an APK file.'));
      return;
    }
    setUploadProgress(0);
    try {
      await uploadAppRelease({
        file: apkFile,
        versionCode: Number(versionCode),
        versionName,
        releaseNotesBn,
        releaseNotesEn,
        actorUid: adminUser.uid,
        actorEmail: adminUser.email,
        onProgress: setUploadProgress,
      });
      setSuccess(
        t(
          'APK আপলোড হয়েছে এবং খসড়া হিসেবে সংরক্ষিত। প্রকাশ করতে নিচের তালিকা ব্যবহার করুন।',
          'APK uploaded as a draft. Publish it from the release list when ready.'
        )
      );
      setVersionName('');
      setVersionCode('');
      setReleaseNotesBn('');
      setReleaseNotesEn('');
      setApkFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (error) {
      setActionError(formatError(error));
    } finally {
      setUploadProgress(null);
    }
  };

  const handlePublish = async (release: AppRelease, published: boolean) => {
    setActionError(null);
    setSuccess(null);
    if (!adminUser) {
      setActionError(t('সুপার এডমিন সেশন পাওয়া যায়নি।', 'Super Admin session is unavailable.'));
      return;
    }
    setBusyReleaseId(release.id);
    try {
      await setAppReleasePublished(
        release,
        published,
        adminUser.uid,
        adminUser.email
      );
      setSuccess(
        published
          ? t('রিলিজটি প্রকাশিত হয়েছে।', 'Release published.')
          : t('রিলিজটি অপ্রকাশিত হয়েছে।', 'Release unpublished.')
      );
    } catch (error) {
      setActionError(formatError(error));
    } finally {
      setBusyReleaseId(null);
    }
  };

  return (
    <div className="space-y-5">
      <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <div className="mb-1 flex items-center gap-2 text-xs text-slate-500">
            <ShieldCheck className="h-4 w-4 text-emerald-700" />
            <span>{t('শুধুমাত্র সুপার এডমিন', 'Super Admin only')}</span>
          </div>
          <h2 className="text-xl font-bold text-slate-900">
            {t('অ্যান্ড্রয়েড অ্যাপ ম্যানেজমেন্ট', 'Android App Management')}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {t(
              'APK আপলোড করুন, সংস্করণ পরিচালনা করুন এবং প্রকাশিত সংস্করণ ব্যবহারকারীদের জন্য দিন।',
              'Upload APKs, manage versions, and publish downloads for app users.'
            )}
          </p>
        </div>
        <a
          href="/download"
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-emerald-200 bg-white px-3 py-2 text-sm font-semibold text-emerald-800 hover:bg-emerald-50"
        >
          <Download className="h-4 w-4" />
          {t('পাবলিক ডাউনলোড পেজ', 'Public download page')}
        </a>
      </header>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <span className="rounded-xl bg-emerald-50 p-2 text-emerald-700">
              <PackageCheck className="h-5 w-5" />
            </span>
            <div>
              <p className="text-xs text-slate-500">{t('মোট সংস্করণ', 'Total versions')}</p>
              <p className="text-xl font-bold text-slate-900">{releases.length}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <span className="rounded-xl bg-sky-50 p-2 text-sky-700">
              <Download className="h-5 w-5" />
            </span>
            <div>
              <p className="text-xs text-slate-500">{t('প্রকাশিত সংস্করণ', 'Published versions')}</p>
              <p className="text-xl font-bold text-slate-900">{publishedCount}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <span className="rounded-xl bg-violet-50 p-2 text-violet-700">
              <Smartphone className="h-5 w-5" />
            </span>
            <div>
              <p className="text-xs text-slate-500">{t('সর্বশেষ প্রকাশিত কোড', 'Latest published code')}</p>
              <p className="text-xl font-bold text-slate-900">{latestPublishedVersionCode}</p>
            </div>
          </div>
        </div>
      </div>

      {loadError && (
        <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          {t('রিলিজ তালিকা লোড করা যায়নি:', 'Could not load releases:')} {loadError}
        </div>
      )}
      {actionError && (
        <div role="alert" className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{actionError}</span>
        </div>
      )}
      {success && (
        <div role="status" className="flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{success}</span>
        </div>
      )}

      <form
        onSubmit={(event) => void handleUpload(event)}
        className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6"
      >
        <div className="mb-4 flex items-center gap-2">
          <UploadCloud className="h-5 w-5 text-emerald-700" />
          <h3 className="font-bold text-slate-900">
            {t('নতুন APK আপলোড', 'Upload a new APK')}
          </h3>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block text-sm font-medium text-slate-700">
            {t('অ্যাপ সংস্করণ', 'App version')}
            <input
              required
              maxLength={64}
              value={versionName}
              onChange={(event) => setVersionName(event.target.value)}
              placeholder="1.0.3"
              className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            {t('সংস্করণ কোড', 'Version code')}
            <input
              required
              type="number"
              min={CURRENT_ANDROID_VERSION_CODE + 1}
              max={2_100_000_000}
              step="1"
              value={versionCode}
              onChange={(event) =>
                setVersionCode(event.target.value ? Number(event.target.value) : '')
              }
              className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
            />
            <span className="mt-1 block text-xs text-slate-500">
              {t(
                `বর্তমান Android সংস্করণ কোড ${CURRENT_ANDROID_VERSION_CODE}-এর চেয়ে বেশি হতে হবে।`,
                `Must be greater than the current Android version code (${CURRENT_ANDROID_VERSION_CODE}).`
              )}
            </span>
          </label>
          <label className="block text-sm font-medium text-slate-700 md:col-span-2">
            {t('রিলিজ নোট (বাংলা)', 'Release notes (Bangla)')}
            <textarea
              rows={3}
              maxLength={4000}
              value={releaseNotesBn}
              onChange={(event) => setReleaseNotesBn(event.target.value)}
              className="mt-1.5 w-full resize-y rounded-xl border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
            />
          </label>
          <label className="block text-sm font-medium text-slate-700 md:col-span-2">
            {t('রিলিজ নোট (ইংরেজি)', 'Release notes (English)')}
            <textarea
              rows={3}
              maxLength={4000}
              value={releaseNotesEn}
              onChange={(event) => setReleaseNotesEn(event.target.value)}
              className="mt-1.5 w-full resize-y rounded-xl border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
            />
            <span className="mt-1 block text-xs text-slate-500">
              {t('অন্তত একটি ভাষায় রিলিজ নোট আবশ্যক।', 'Release notes are required in at least one language.')}
            </span>
          </label>
          <label className="block text-sm font-medium text-slate-700 md:col-span-2">
            {t('স্বাক্ষরিত APK ফাইল (সর্বোচ্চ ৫০ MB)', 'Signed APK file (max 50 MB)')}
            <input
              ref={fileInputRef}
              required
              type="file"
              accept=".apk,application/vnd.android.package-archive"
              onChange={(event) => setApkFile(event.target.files?.[0] ?? null)}
              className="mt-1.5 block w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-emerald-50 file:px-3 file:py-1.5 file:font-semibold file:text-emerald-800"
            />
            {apkFile && (
              <span className="mt-1 block text-xs text-slate-500">
                {apkFile.name} · {formatApkSize(apkFile.size)}
              </span>
            )}
          </label>
        </div>
        {uploadProgress !== null && (
          <div className="mt-4" aria-live="polite">
            <div className="mb-1 flex justify-between text-xs text-slate-600">
              <span>{t('APK আপলোড হচ্ছে', 'Uploading APK')}</span>
              <span>{uploadProgress}%</span>
            </div>
            <progress
              className="h-2 w-full accent-emerald-700"
              max={100}
              value={uploadProgress}
            />
          </div>
        )}
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="max-w-2xl text-xs leading-relaxed text-amber-800">
            {t(
              'শুধুমাত্র production signing key দিয়ে স্বাক্ষরিত release APK আপলোড করুন। Debug-signed APK প্রকাশ করবেন না।',
              'Upload only a release APK signed with the production signing key. Do not publish a debug-signed APK.'
            )}
          </p>
          <button
            type="submit"
            disabled={uploadProgress !== null || !adminUser}
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-900 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <UploadCloud className="h-4 w-4" />
            {uploadProgress !== null
              ? t(`আপলোড হচ্ছে ${uploadProgress}%`, `Uploading ${uploadProgress}%`)
              : t('খসড়া হিসেবে আপলোড', 'Upload as draft')}
          </button>
        </div>
      </form>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between gap-3 border-b border-slate-200 px-4 py-4 sm:px-6">
          <div>
            <h3 className="font-bold text-slate-900">{t('সংস্করণসমূহ', 'Release versions')}</h3>
            <p className="mt-0.5 text-xs text-slate-500">
              {t('প্রকাশনা প্রত্যাহার করলে APK ফাইল ও অডিট ইতিহাস অক্ষত থাকবে।', 'Unpublishing keeps the APK and audit history intact.')}
            </p>
          </div>
          <RefreshCw className="h-4 w-4 text-slate-400" aria-hidden="true" />
        </div>
        {releases.length === 0 ? (
          <div className="px-6 py-10 text-center text-sm text-slate-500">
            {t('এখনও কোনো APK সংস্করণ আপলোড করা হয়নি।', 'No APK versions have been uploaded yet.')}
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {releases.map((release) => {
              const isBusy = busyReleaseId === release.id;
              const canPublish =
                release.versionCode > latestPublishedVersionCode &&
                release.versionCode > CURRENT_ANDROID_VERSION_CODE;
              return (
                <article key={release.id} className="p-4 sm:p-6">
                  <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <h4 className="text-base font-bold text-slate-900">
                          {release.versionName}
                        </h4>
                        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-mono text-slate-700">
                          code {release.versionCode}
                        </span>
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                            release.status === 'published'
                              ? 'bg-emerald-100 text-emerald-800'
                              : release.status === 'unpublished'
                                ? 'bg-slate-100 text-slate-600'
                                : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {releaseStatusLabel(release.status, lang)}
                        </span>
                      </div>
                      <p className="mt-2 text-xs text-slate-500">
                        {release.fileName} · {formatApkSize(release.fileSizeBytes)} ·{' '}
                        {t('রিলিজ তারিখ', 'Release date')}:{' '}
                        {formatReleaseDate(release.releaseDate || release.createdAt, lang)}
                      </p>
                      {(release.releaseNotesBn || release.releaseNotesEn) && (
                        <p className="mt-3 whitespace-pre-wrap text-sm text-slate-700">
                          {lang === 'bn'
                            ? release.releaseNotesBn || release.releaseNotesEn
                            : release.releaseNotesEn || release.releaseNotesBn}
                        </p>
                      )}
                      <p className="mt-2 break-all font-mono text-[11px] text-slate-400">
                        SHA-256: {release.sha256}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      {release.status === 'published' ? (
                        <>
                          <a
                            href={release.downloadUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                          >
                            <Download className="h-4 w-4" />
                            {t('ডাউনলোড', 'Download')}
                          </a>
                          <button
                            type="button"
                            disabled={isBusy}
                            onClick={() => void handlePublish(release, false)}
                            className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100 disabled:opacity-60"
                          >
                            {t('প্রকাশনা প্রত্যাহার', 'Unpublish')}
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          disabled={isBusy || !canPublish}
                          onClick={() => void handlePublish(release, true)}
                          title={
                            canPublish
                              ? undefined
                              : t(
                                  'প্রকাশিত সংস্করণ ও বর্তমান ইনস্টল করা সংস্করণের চেয়ে নতুন কোড প্রয়োজন।',
                                  'A version code newer than the published and installed versions is required.'
                                )
                          }
                          className="rounded-xl bg-emerald-800 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-900 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {isBusy
                            ? t('সংরক্ষণ হচ্ছে…', 'Saving…')
                            : t('প্রকাশ করুন', 'Publish')}
                        </button>
                      )}
                    </div>
                  </div>
                  {release.status !== 'published' && !canPublish && (
                    <p className="mt-3 text-xs text-amber-700">
                      {t(
                        'এই সংস্করণ কোডটি বর্তমান ইনস্টল করা বা প্রকাশিত সংস্করণের চেয়ে নতুন নয়।',
                        'This version code is not newer than the currently installed or published version.'
                      )}
                    </p>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};
