import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Download,
  Edit3,
  ExternalLink,
  Eye,
  EyeOff,
  FileCode2,
  Plus,
  ShieldAlert,
  Smartphone,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteAppRelease,
  mapAppReleaseDoc,
  saveAppRelease,
  subscribeCollection,
  toggleAppReleasePublish,
} from '../../services/firebase/crudService';
import { isUnsafeLocalUrl } from '../../services/firebase/storageService';
import type { AppReleaseDoc } from '../../types/firestore';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

function createDefaultRelease(docId = 'latest'): AppReleaseDoc {
  const today = new Date().toISOString().slice(0, 10);
  return {
    id: docId,
    app_name: 'Asheq Ullah Rafiq',
    package_name: 'com.ashequllah.portal',
    latest_version: '2.0.0',
    version_code: 2,
    release_title_bn: 'নতুন সংস্করণ ও লাইভ নোটিফিকেশন আপডেট',
    release_title_en: 'Live Notification Center & Content Sync Update',
    release_notes_bn:
      '• হোমপেজে লাইভ নোটিফিকেশন বেল ও অপঠিত ব্যাজ\n• ফায়ারস্টোর রিয়েল-টাইম কনটেন্ট সিঙ্ক\n• স্বয়ংক্রিয় অ্যাপ আপডেট চেকার',
    release_notes_en:
      '• Live Notification Bell & unread count badge\n• Real-time Firestore content sync across modules\n• Built-in App Version & APK Update Checker',
    download_url: '',
    release_tag: 'v2.0.0',
    release_date: today,
    minimum_supported_version: 1,
    force_update: false,
    update_available: true,
    is_published: false,
    file_name: 'AsheqUllahRafiq.apk',
    file_size: '',
    checksum_sha256: '',
    is_latest: docId === 'latest',
  };
}

function isValidApkDownloadUrl(url: string): { valid: boolean; reasonBn: string; reasonEn: string } {
  const trimmed = url.trim();
  if (!trimmed) {
    return {
      valid: false,
      reasonBn: 'APK ডাউনলোড URL অবশ্যই দিতে হবে।',
      reasonEn: 'APK download URL is required.',
    };
  }
  if (isUnsafeLocalUrl(trimmed) || !/^https:\/\//i.test(trimmed)) {
    return {
      valid: false,
      reasonBn: 'ডাউনলোড URL অবশ্যই নিরাপদ https:// ঠিকানা হতে হবে।',
      reasonEn: 'Download URL must use HTTPS (https://...).',
    };
  }
  const lower = trimmed.toLowerCase();
  if (lower.includes('firebasestorage.googleapis.com') || lower.includes('firebasestorage.app')) {
    return {
      valid: false,
      reasonBn: 'Spark প্ল্যানে Firebase Storage-এ APK হোস্ট করা যাবে না; GitHub Releases URL ব্যবহার করুন।',
      reasonEn: 'Firebase Storage APK URLs are blocked on Spark plan; use GitHub Releases.',
    };
  }
  if (trimmed.includes('GITHUB_OWNER') || trimmed.includes('GITHUB_REPOSITORY')) {
    return {
      valid: false,
      reasonBn: 'দয়া করে আপনার আসল GitHub Owner এবং Repository নাম বসান।',
      reasonEn: 'Replace GITHUB_OWNER and GITHUB_REPOSITORY placeholders with real repository values.',
    };
  }
  return { valid: true, reasonBn: '', reasonEn: '' };
}

export function AppVersionManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [releases, setReleases] = useState<AppReleaseDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [editorOpen, setEditorOpen] = useState(false);
  const [workflowOpen, setWorkflowOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<AppReleaseDoc>(() => createDefaultRelease('latest'));
  const [syncToLatest, setSyncToLatest] = useState(true);
  const [autoNotify, setAutoNotify] = useState(true);
  const [ghOwner, setGhOwner] = useState('');
  const [ghRepo, setGhRepo] = useState('');
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<AppReleaseDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'app_release',
      mapAppReleaseDoc,
      (list) => {
        const sorted = [...list].sort((a, b) => {
          if (a.id === 'latest') return -1;
          if (b.id === 'latest') return 1;
          return (b.version_code || 0) - (a.version_code || 0);
        });
        setReleases(sorted);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setLoading(false);
      }
    );
    return () => unsub();
  }, []);

  const isSuperAdmin = admin?.role === 'super_admin';

  const latestRelease = useMemo(
    () => releases.find((r) => r.id === 'latest') || null,
    [releases]
  );

  const historyReleases = useMemo(
    () => releases.filter((r) => r.id !== 'latest'),
    [releases]
  );

  const openCreateRelease = () => {
    const nextCode = latestRelease ? latestRelease.version_code + 1 : 2;
    const nextId = latestRelease ? `release_v_${nextCode}_${Date.now().toString().slice(-4)}` : 'latest';
    setForm({
      ...createDefaultRelease(nextId),
      version_code: nextCode,
      download_url: latestRelease?.download_url || '',
    });
    setSyncToLatest(true);
    setAutoNotify(true);
    setIsNew(true);
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEditRelease = (item: AppReleaseDoc) => {
    setForm({ ...item });
    setSyncToLatest(item.id === 'latest');
    setAutoNotify(false);
    setIsNew(false);
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleApplyGitHubUrl = () => {
    const owner = ghOwner.trim();
    const repo = ghRepo.trim();
    if (!owner || !repo) return;
    const url = `https://github.com/${owner}/${repo}/releases/latest/download/AsheqUllahRafiq.apk`;
    setForm((prev) => ({
      ...prev,
      download_url: url,
      file_name: 'AsheqUllahRafiq.apk',
    }));
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !isSuperAdmin) return;

    const urlCheck = isValidApkDownloadUrl(form.download_url);
    if (!urlCheck.valid) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? urlCheck.reasonBn : urlCheck.reasonEn,
      });
      return;
    }

    if (form.checksum_sha256.trim() && !/^[a-fA-F0-9]{64}$/.test(form.checksum_sha256.trim())) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'SHA-256 চেকসাম অবশ্যই ৬৪ অক্ষরের বৈধ হেক্সাডেসিমেল স্ট্রিং হতে হবে।'
            : 'SHA-256 checksum must be a valid 64-character hexadecimal string.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveAppRelease({
        release: form,
        syncToLatest,
        createNotification: autoNotify && form.is_published,
        isNew,
        actor: admin,
      });
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `অ্যাপ সংস্করণ ${form.latest_version} (Code ${form.version_code}) সফলভাবে সংরক্ষিত হয়েছে।`
            : `App release v${form.latest_version} (Code ${form.version_code}) saved successfully.`,
      });
      setEditorOpen(false);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save app release.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleTogglePublish = async (item: AppReleaseDoc) => {
    if (!admin || !isSuperAdmin) return;
    try {
      await toggleAppReleasePublish(
        item.id,
        !item.is_published,
        `v${item.latest_version}`,
        admin
      );
      setFeedback({
        type: 'success',
        text: !item.is_published
          ? lang === 'bn'
            ? 'অ্যাপ রিলিজ প্রকাশিত হয়েছে।'
            : 'Release published.'
          : lang === 'bn'
          ? 'অ্যাপ রিলিজ অপ্রকাশিত করা হয়েছে।'
          : 'Release unpublished.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update publish status.',
      });
    }
  };

  const handleConfirmDelete = async () => {
    if (!admin || !isSuperAdmin || !deleteTarget) return;
    setDeleting(true);
    try {
      await deleteAppRelease(deleteTarget.id, `v${deleteTarget.latest_version}`, admin);
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'রিলিজ ইতিহাস মুছে ফেলা হয়েছে।' : 'Release history entry deleted.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete release entry.',
      });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? 'অ্যাপ সংস্করণ ব্যবস্থাপনা' : 'App Version Manager'}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {lang === 'bn'
              ? 'GitHub Releases ভিত্তিক স্থায়ী APK ডাউনলোড ঠিকানা, সংস্করণ কোড ও স্বয়ংক্রিয় আপডেট নিয়ন্ত্রণ করুন।'
              : 'Manage live APK releases, permanent GitHub Releases URLs, force-update rules, and release history.'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setWorkflowOpen(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors whitespace-nowrap cursor-pointer"
          >
            <FileCode2 className="w-4 h-4" />
            <span>{lang === 'bn' ? 'রিলিজ ওয়ার্কফ্লো গাইড' : 'Release Workflow Guide'}</span>
          </button>

          {isSuperAdmin ? (
            <button
              type="button"
              onClick={openCreateRelease}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors whitespace-nowrap cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>{lang === 'bn' ? 'নতুন রিলিজ যোগ করুন' : 'New Release'}</span>
            </button>
          ) : null}
        </div>
      </div>

      {!isSuperAdmin ? (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-2.5 text-xs text-amber-900">
          <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <p>
            {lang === 'bn'
              ? 'নিরাপত্তার স্বার্থে শুধুমাত্র সুপার অ্যাডমিন অ্যাপ রিলিজ মেটাডাটা পরিবর্তন ও প্রকাশ করতে পারবেন।'
              : 'For release integrity, only Super Admin accounts can create, modify, or publish app_release metadata.'}
          </p>
        </div>
      ) : null}

      {feedback ? (
        <div
          role="alert"
          className={`p-3.5 rounded-xl border flex items-start gap-2.5 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-900'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-4 h-4 text-red-700 shrink-0 mt-0.5" />
          )}
          <span>{feedback.text}</span>
        </div>
      ) : null}

      {loadError ? (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800">
          {loadError}
        </div>
      ) : null}

      {/* Active Live Release Card (app_release/latest) */}
      <section className="bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 pb-5 border-b border-slate-200">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 shrink-0">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {lang === 'bn' ? 'সক্রিয় লাইভ সংস্করণ (app_release/latest)' : 'Current Live Release (app_release/latest)'}
              </h3>
              <p className="mt-0.5 text-xs text-slate-500 font-mono">
                com.ashequllah.portal · {latestRelease?.file_name || 'AsheqUllahRafiq.apk'}
              </p>
            </div>
          </div>

          {latestRelease && isSuperAdmin ? (
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => handleTogglePublish(latestRelease)}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg border transition-colors cursor-pointer ${
                  latestRelease.is_published
                    ? 'text-slate-700 bg-slate-50 border-slate-300 hover:bg-slate-100'
                    : 'text-emerald-800 bg-emerald-50 border-emerald-200 hover:bg-emerald-100'
                }`}
              >
                {latestRelease.is_published ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                <span>
                  {latestRelease.is_published
                    ? lang === 'bn'
                      ? 'অপ্রকাশিত করুন'
                      : 'Unpublish'
                    : lang === 'bn'
                    ? 'প্রকাশ করুন'
                    : 'Publish'}
                </span>
              </button>
              <button
                type="button"
                onClick={() => openEditRelease(latestRelease)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-blue-700 bg-blue-50 border border-blue-200 hover:bg-blue-100 rounded-lg transition-colors cursor-pointer"
              >
                <Edit3 className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'সম্পাদনা' : 'Edit Latest'}</span>
              </button>
            </div>
          ) : null}
        </div>

        {loading ? (
          <div className="py-8 text-sm text-slate-500">
            {lang === 'bn' ? 'সংস্করণ তথ্য লোড হচ্ছে...' : 'Loading release metadata...'}
          </div>
        ) : !latestRelease ? (
          <div className="py-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <p className="text-sm font-medium text-slate-800">
                {lang === 'bn'
                  ? 'এখনো app_release/latest নথি সেট করা হয়নি।'
                  : 'No active app_release/latest document configured yet.'}
              </p>
              <p className="text-xs text-slate-500">
                {lang === 'bn'
                  ? 'নতুন রিলিজ তৈরি করে আপনার GitHub Releases APK URL যুক্ত করুন।'
                  : 'Create the initial release entry and configure your permanent GitHub Releases APK download URL.'}
              </p>
            </div>
            {isSuperAdmin ? (
              <button
                type="button"
                onClick={() => {
                  setForm(createDefaultRelease('latest'));
                  setSyncToLatest(true);
                  setAutoNotify(false);
                  setIsNew(true);
                  setEditorOpen(true);
                }}
                className="px-4 py-2 text-xs font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg whitespace-nowrap cursor-pointer"
              >
                {lang === 'bn' ? 'প্রথম রিলিজ কনফিগার করুন' : 'Configure Latest Release'}
              </button>
            ) : null}
          </div>
        ) : (
          <div className="pt-5 space-y-5">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <p className="text-xs text-slate-500">{lang === 'bn' ? 'সর্বশেষ সংস্করণ' : 'Latest Version'}</p>
                <p className="mt-1 text-xl font-bold text-slate-900 font-mono tabular-nums">
                  v{latestRelease.latest_version}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">{lang === 'bn' ? 'ভার্সন কোড' : 'Version Code'}</p>
                <p className="mt-1 text-xl font-bold text-slate-900 font-mono tabular-nums">
                  {toLocalizedNumber(latestRelease.version_code, lang)}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">{lang === 'bn' ? 'ন্যূনতম সমর্থিত কোড' : 'Min Supported Code'}</p>
                <p className="mt-1 text-xl font-bold text-slate-900 font-mono tabular-nums">
                  {toLocalizedNumber(latestRelease.minimum_supported_version, lang)}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">{lang === 'bn' ? 'প্রকাশের অবস্থা' : 'Release Status'}</p>
                <p className={`mt-1 text-sm font-semibold ${latestRelease.is_published ? 'text-emerald-700' : 'text-amber-700'}`}>
                  {latestRelease.is_published
                    ? lang === 'bn'
                      ? 'প্রকাশিত (Live)'
                      : 'Published (Live)'
                    : lang === 'bn'
                    ? 'অপ্রকাশিত (Draft)'
                    : 'Draft / Unpublished'}
                  {latestRelease.force_update
                    ? lang === 'bn'
                      ? ' · বাধ্যতামূলক আপডেট'
                      : ' · Force Update'
                    : ''}
                </p>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 grid grid-cols-1 lg:grid-cols-2 gap-4 text-xs">
              <div className="space-y-1.5">
                <p className="font-semibold text-slate-700">
                  {lang === 'bn' ? 'স্থায়ী APK ডাউনলোড লিংক (GitHub Releases)' : 'Permanent APK Download URL'}
                </p>
                {latestRelease.download_url ? (
                  <a
                    href={latestRelease.download_url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 font-mono text-blue-700 hover:underline break-all"
                  >
                    <Download className="w-3.5 h-3.5 shrink-0" />
                    <span>{latestRelease.download_url}</span>
                    <ExternalLink className="w-3 h-3 shrink-0" />
                  </a>
                ) : (
                  <p className="text-amber-700">
                    {lang === 'bn' ? 'কোনো ডাউনলোড URL সেট করা নেই।' : 'Download URL not configured.'}
                  </p>
                )}
                {latestRelease.checksum_sha256 ? (
                  <p className="font-mono text-slate-500 break-all">
                    SHA-256: {latestRelease.checksum_sha256}
                  </p>
                ) : null}
              </div>

              <div className="space-y-1.5">
                <p className="font-semibold text-slate-700">
                  {lang === 'bn' ? 'রিলিজ নোটস (সংক্ষিপ্ত)' : 'Release Notes'}
                </p>
                <p className="text-slate-600 whitespace-pre-line leading-relaxed">
                  {lang === 'bn'
                    ? latestRelease.release_notes_bn || latestRelease.release_notes_en
                    : latestRelease.release_notes_en || latestRelease.release_notes_bn}
                </p>
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Release History Table */}
      <section className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-900">
            {lang === 'bn' ? 'পূর্ববর্তী রিলিজ ইতিহাস' : 'Release History Archive'}
          </h3>
          <span className="text-xs font-mono text-slate-500 tabular-nums">
            {toLocalizedNumber(historyReleases.length, lang)}{' '}
            {lang === 'bn' ? 'টি সংস্করণ' : 'entries'}
          </span>
        </div>

        {historyReleases.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn'
              ? 'এখনো কোনো পূর্ববর্তী সংস্করণের ইতিহাস সংরক্ষিত হয়নি।'
              : 'No archived release history entries recorded yet.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="p-3.5">{lang === 'bn' ? 'সংস্করণ ও ট্যাগ' : 'Version & Tag'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'ভার্সন কোড' : 'Code'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'রিলিজ তারিখ' : 'Release Date'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'অবস্থা' : 'Status'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'ফাইল সাইজ' : 'Size'}</th>
                  <th className="p-3.5 text-right">{lang === 'bn' ? 'কার্যক্রম' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {historyReleases.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="p-3.5">
                      <p className="font-semibold text-slate-900 font-mono">
                        v{item.latest_version} · {item.release_tag || item.id}
                      </p>
                      <p className="text-xs text-slate-500 line-clamp-1">
                        {lang === 'bn' ? item.release_title_bn : item.release_title_en}
                      </p>
                    </td>
                    <td className="p-3.5 font-mono text-xs tabular-nums text-slate-700">
                      {item.version_code}
                    </td>
                    <td className="p-3.5 text-xs text-slate-600 tabular-nums">
                      {item.release_date || formatFirestoreTimestamp(item.updated_at, lang)}
                    </td>
                    <td className="p-3.5 text-xs font-medium">
                      {item.is_published ? (
                        <span className="text-emerald-700">
                          {lang === 'bn' ? 'প্রকাশিত' : 'Published'}
                        </span>
                      ) : (
                        <span className="text-slate-500">
                          {lang === 'bn' ? 'খসড়া' : 'Draft'}
                        </span>
                      )}
                    </td>
                    <td className="p-3.5 text-xs font-mono text-slate-600 tabular-nums">
                      {item.file_size || '—'}
                    </td>
                    <td className="p-3.5 text-right whitespace-nowrap">
                      {isSuperAdmin ? (
                        <div className="inline-flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => openEditRelease(item)}
                            className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                            title={lang === 'bn' ? 'সম্পাদনা' : 'Edit'}
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(item)}
                            className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                            title={lang === 'bn' ? 'মুছে ফেলুন' : 'Delete'}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Release Editor Modal */}
      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন অ্যাপ সংস্করণ রিলিজ করুন'
              : 'Create App Release'
            : lang === 'bn'
            ? 'অ্যাপ সংস্করণ সম্পাদনা'
            : 'Edit App Release'
        }
        maxWidthClass="max-w-3xl"
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Document ID</label>
              <input
                type="text"
                value={form.id}
                disabled={!isNew}
                onChange={(e) => setForm({ ...form, id: e.target.value })}
                className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg disabled:bg-slate-100"
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'সংস্করণ (latest_version) *' : 'Version Name *'}
              </label>
              <input
                type="text"
                value={form.latest_version}
                onChange={(e) => setForm({ ...form, latest_version: e.target.value })}
                placeholder="2.0.0"
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'ভার্সন কোড (version_code) *' : 'Version Code *'}
              </label>
              <input
                type="number"
                min={1}
                value={form.version_code}
                onChange={(e) => setForm({ ...form, version_code: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'ন্যূনতম সমর্থিত কোড' : 'Min Supported Code'}
              </label>
              <input
                type="number"
                min={1}
                value={form.minimum_supported_version}
                onChange={(e) => setForm({ ...form, minimum_supported_version: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'রিলিজ ট্যাগ (release_tag)' : 'GitHub Release Tag'}
              </label>
              <input
                type="text"
                value={form.release_tag}
                onChange={(e) => setForm({ ...form, release_tag: e.target.value })}
                placeholder="v2.0.0"
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'প্রকাশের তারিখ' : 'Release Date'}
              </label>
              <input
                type="date"
                value={form.release_date}
                onChange={(e) => setForm({ ...form, release_date: e.target.value })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          {/* GitHub Permanent URL Helper */}
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 space-y-2.5">
            <p className="text-xs font-semibold text-slate-800">
              {lang === 'bn'
                ? 'GitHub Releases স্থায়ী APK লিংক জেনারেটর'
                : 'Permanent GitHub Releases APK URL Builder'}
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              <input
                type="text"
                value={ghOwner}
                onChange={(e) => setGhOwner(e.target.value)}
                placeholder="GITHUB_OWNER"
                className="px-2.5 py-1.5 text-xs font-mono bg-white border border-slate-300 rounded-lg"
              />
              <input
                type="text"
                value={ghRepo}
                onChange={(e) => setGhRepo(e.target.value)}
                placeholder="GITHUB_REPOSITORY"
                className="px-2.5 py-1.5 text-xs font-mono bg-white border border-slate-300 rounded-lg"
              />
              <button
                type="button"
                onClick={handleApplyGitHubUrl}
                disabled={!ghOwner.trim() || !ghRepo.trim()}
                className="px-3 py-1.5 text-xs font-semibold text-blue-700 bg-blue-50 border border-blue-200 hover:bg-blue-100 rounded-lg disabled:opacity-50 cursor-pointer"
              >
                {lang === 'bn' ? 'স্থায়ী URL বসান' : 'Generate Permanent URL'}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-slate-700">
              {lang === 'bn' ? 'স্থায়ী APK ডাউনলোড URL (GitHub Releases) *' : 'Permanent APK Download URL *'}
            </label>
            <input
              type="url"
              value={form.download_url}
              onChange={(e) => setForm({ ...form, download_url: e.target.value })}
              placeholder="https://github.com/OWNER/REPO/releases/latest/download/AsheqUllahRafiq.apk"
              className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'ফাইলের সাইজ (file_size)' : 'APK File Size'}
              </label>
              <input
                type="text"
                value={form.file_size}
                onChange={(e) => setForm({ ...form, file_size: e.target.value })}
                placeholder="24.8 MB"
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'SHA-256 চেকসাম (ঐচ্ছিক যাচাই)' : 'SHA-256 Checksum (64 hex)'}
              </label>
              <input
                type="text"
                value={form.checksum_sha256}
                onChange={(e) => setForm({ ...form, checksum_sha256: e.target.value })}
                placeholder="64-character SHA-256 hex digest"
                className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'রিলিজ শিরোনাম (বাংলা)' : 'Release Title (Bengali)'}
              </label>
              <input
                type="text"
                value={form.release_title_bn}
                onChange={(e) => setForm({ ...form, release_title_bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'রিলিজ শিরোনাম (ইংরেজি)' : 'Release Title (English)'}
              </label>
              <input
                type="text"
                value={form.release_title_en}
                onChange={(e) => setForm({ ...form, release_title_en: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'রিলিজ নোটস (বাংলা)' : 'Release Notes (Bengali)'}
              </label>
              <textarea
                rows={4}
                value={form.release_notes_bn}
                onChange={(e) => setForm({ ...form, release_notes_bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'রিলিজ নোটস (ইংরেজি)' : 'Release Notes (English)'}
              </label>
              <textarea
                rows={4}
                value={form.release_notes_en}
                onChange={(e) => setForm({ ...form, release_notes_en: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-5 pt-2">
            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={form.is_published}
                onChange={(e) => setForm({ ...form, is_published: e.target.checked })}
                className="w-4 h-4 text-blue-700 border-slate-300 rounded"
              />
              <span>{lang === 'bn' ? 'প্রকাশিত রাখুন (Published)' : 'Publish Release'}</span>
            </label>

            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={form.force_update}
                onChange={(e) => setForm({ ...form, force_update: e.target.checked })}
                className="w-4 h-4 text-red-700 border-slate-300 rounded"
              />
              <span>{lang === 'bn' ? 'বাধ্যতামূলক আপডেট (Force Update)' : 'Force Update'}</span>
            </label>

            {form.id !== 'latest' ? (
              <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={syncToLatest}
                  onChange={(e) => setSyncToLatest(e.target.checked)}
                  className="w-4 h-4 text-blue-700 border-slate-300 rounded"
                />
                <span>
                  {lang === 'bn'
                    ? 'app_release/latest হিসেবে সিঙ্ক করুন'
                    : 'Sync as active app_release/latest'}
                </span>
              </label>
            ) : null}

            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={autoNotify}
                onChange={(e) => setAutoNotify(e.target.checked)}
                className="w-4 h-4 text-blue-700 border-slate-300 rounded"
              />
              <span>
                {lang === 'bn'
                  ? 'স্বয়ংক্রিয়ভাবে অ্যাপ আপডেট নোটিফিকেশন তৈরি করুন'
                  : 'Create In-App Update Notification on publish'}
              </span>
            </label>
          </div>

          <div className="pt-4 border-t border-slate-200 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setEditorOpen(false)}
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer"
            >
              {lang === 'bn' ? 'বাতিল' : 'Cancel'}
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg disabled:opacity-60 cursor-pointer"
            >
              {saving
                ? lang === 'bn'
                  ? 'সংরক্ষণ হচ্ছে...'
                  : 'Saving...'
                : lang === 'bn'
                ? 'রিলিজ সংরক্ষণ করুন'
                : 'Save Release'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Release Workflow Guide Modal */}
      <Modal
        open={workflowOpen}
        onClose={() => setWorkflowOpen(false)}
        title={lang === 'bn' ? 'APK রিলিজ ও GitHub Releases নির্দেশিকা' : 'APK Release & GitHub Releases Workflow'}
        maxWidthClass="max-w-2xl"
      >
        <div className="space-y-4 text-xs text-slate-700 leading-relaxed">
          <ol className="list-decimal pl-4 space-y-1.5">
            <li>Build release APK in Android repo (<code>./gradlew assembleRelease</code>).</li>
            <li>Verify package ID <code>com.ashequllah.portal</code>, <code>versionName</code>, and <code>versionCode</code>.</li>
            <li>Rename output APK strictly to <code>AsheqUllahRafiq.apk</code>.</li>
            <li>Calculate checksum: <code>sha256sum AsheqUllahRafiq.apk</code>.</li>
            <li>Upload <code>AsheqUllahRafiq.apk</code> to GitHub Releases under the latest tag.</li>
            <li>Verify permanent URL resolves: <code>https://github.com/OWNER/REPO/releases/latest/download/AsheqUllahRafiq.apk</code>.</li>
            <li>Save and publish metadata in this App Version Manager to update <code>app_release/latest</code> and alert users automatically.</li>
          </ol>
          <div className="p-3 bg-slate-900 text-slate-100 rounded-lg font-mono text-[11px] overflow-x-auto">
            <pre>{`# Permanent GitHub Releases URL pattern
https://github.com/OWNER/REPO/releases/latest/download/AsheqUllahRafiq.apk`}</pre>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'রিলিজ ইতিহাস মুছে ফেলুন' : 'Delete Release History'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিত যে সংস্করণ v${deleteTarget?.latest_version} (${deleteTarget?.id}) মুছে ফেলতে চান?`
            : `Delete archived release v${deleteTarget?.latest_version} (${deleteTarget?.id})?`
        }
        confirmLabel={lang === 'bn' ? 'মুছে ফেলুন' : 'Delete'}
        cancelLabel={lang === 'bn' ? 'বাতিল' : 'Cancel'}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
}
