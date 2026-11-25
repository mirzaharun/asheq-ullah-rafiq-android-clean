import React, { useEffect, useState } from 'react';
import { Download, ExternalLink, Smartphone } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useLanguage } from '../contexts/LanguageContext';
import {
  formatApkSize,
  formatReleaseDate,
  subscribeAppReleases,
} from '../services/appReleaseService';
import { AppRelease } from '../types/portal';

function formatError(error: unknown): string {
  return error instanceof Error ? error.message : 'An unexpected error occurred.';
}

export const PublicAppDownloadPage: React.FC = () => {
  const { lang, t } = useLanguage();
  const [releases, setReleases] = useState<AppRelease[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    return subscribeAppReleases(
      true,
      (items) => {
        setReleases(items);
        setLoading(false);
        setError(null);
      },
      (loadError) => {
        setLoading(false);
        setError(formatError(loadError));
      }
    );
  }, []);

  const latestRelease = releases[0];

  return (
    <main className="min-h-screen bg-gradient-to-b from-emerald-950 via-emerald-900 to-slate-50 px-4 py-10 sm:px-6">
      <div className="mx-auto max-w-4xl">
        <header className="text-center text-white">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20">
            <Smartphone className="h-8 w-8" />
          </div>
          <p className="mt-5 text-sm font-semibold tracking-wide text-emerald-200">
            {t('আশেক উল্লাহ রফিক', 'Asheq Ullah Rafiq')}
          </p>
          <h1 className="mt-2 text-3xl font-bold sm:text-4xl">
            {t('অ্যান্ড্রয়েড অ্যাপ ডাউনলোড', 'Android App Download')}
          </h1>
          <p className="mx-auto mt-3 max-w-2xl text-sm leading-relaxed text-emerald-100 sm:text-base">
            {t(
              'অফিসিয়াল পোর্টাল অ্যাপের সর্বশেষ প্রকাশিত সংস্করণ এবং পূর্ববর্তী সংস্করণ ডাউনলোড করুন।',
              'Download the latest published version of the official portal app or browse earlier releases.'
            )}
          </p>
        </header>

        <section className="mt-8 rounded-3xl border border-white/60 bg-white p-5 shadow-xl sm:p-8">
          {loading ? (
            <p className="py-8 text-center text-sm text-slate-500">
              {t('প্রকাশিত সংস্করণ খোঁজা হচ্ছে…', 'Loading published releases…')}
            </p>
          ) : error ? (
            <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
              {t('ডাউনলোড তথ্য লোড করা যায়নি:', 'Could not load download information:')} {error}
            </div>
          ) : !latestRelease ? (
            <div className="py-8 text-center">
              <Smartphone className="mx-auto h-10 w-10 text-slate-300" />
              <h2 className="mt-3 font-bold text-slate-800">
                {t('কোনো প্রকাশিত সংস্করণ নেই', 'No published version yet')}
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                {t('নতুন সংস্করণ প্রকাশিত হলে এখানে পাওয়া যাবে।', 'A download will appear here when a release is published.')}
              </p>
            </div>
          ) : (
            <>
              <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-center">
                <div>
                  <span className="inline-flex rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-800">
                    {t('সর্বশেষ সংস্করণ', 'Latest version')}
                  </span>
                  <h2 className="mt-3 text-2xl font-bold text-slate-900">
                    {latestRelease.versionName}
                    <span className="ml-2 text-sm font-medium text-slate-500">
                      (code {latestRelease.versionCode})
                    </span>
                  </h2>
                  <p className="mt-1 text-sm text-slate-500">
                    {formatApkSize(latestRelease.fileSizeBytes)} ·{' '}
                    {t('প্রকাশিত', 'Released')}{' '}
                    {formatReleaseDate(latestRelease.releaseDate, lang)}
                  </p>
                </div>
                <a
                  href={latestRelease.downloadUrl}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-800 px-5 py-3 text-sm font-bold text-white shadow-sm hover:bg-emerald-900"
                >
                  <Download className="h-5 w-5" />
                  {t('APK ডাউনলোড', 'Download APK')}
                </a>
              </div>
              {(latestRelease.releaseNotesBn || latestRelease.releaseNotesEn) && (
                <div className="mt-6 rounded-2xl bg-slate-50 p-4">
                  <h3 className="text-sm font-bold text-slate-800">
                    {t('এই সংস্করণে', "What's new")}
                  </h3>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-700">
                    {lang === 'bn'
                      ? latestRelease.releaseNotesBn || latestRelease.releaseNotesEn
                      : latestRelease.releaseNotesEn || latestRelease.releaseNotesBn}
                  </p>
                </div>
              )}

              {releases.length > 1 && (
                <div className="mt-8">
                  <h3 className="mb-3 text-sm font-bold text-slate-800">
                    {t('পূর্ববর্তী সংস্করণ', 'Previous versions')}
                  </h3>
                  <div className="divide-y divide-slate-100 rounded-2xl border border-slate-200">
                    {releases.slice(1).map((release) => (
                      <div
                        key={release.id}
                        className="flex flex-col justify-between gap-3 p-4 sm:flex-row sm:items-center"
                      >
                        <div>
                          <p className="text-sm font-semibold text-slate-800">
                            {release.versionName}
                            <span className="ml-2 text-xs font-normal text-slate-500">
                              (code {release.versionCode})
                            </span>
                          </p>
                          <p className="mt-1 text-xs text-slate-500">
                            {formatApkSize(release.fileSizeBytes)} ·{' '}
                            {formatReleaseDate(release.releaseDate, lang)}
                          </p>
                        </div>
                        <a
                          href={release.downloadUrl}
                          className="inline-flex items-center gap-2 self-start rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 sm:self-auto"
                        >
                          <Download className="h-4 w-4" />
                          {t('ডাউনলোড', 'Download')}
                        </a>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </section>

        <footer className="mt-6 flex flex-col items-center justify-between gap-3 text-xs text-slate-500 sm:flex-row">
          <p>
            {t(
              'শুধুমাত্র প্রকাশিত APK ফাইল এই পেজে দেখানো হয়।',
              'Only published APK files are shown on this page.'
            )}
          </p>
          <Link
            to="/login"
            className="inline-flex items-center gap-1 font-semibold text-emerald-800 hover:text-emerald-950"
          >
            {t('এডমিন প্যানেল', 'Admin panel')}
            <ExternalLink className="h-3.5 w-3.5" />
          </Link>
        </footer>
      </div>
    </main>
  );
};
