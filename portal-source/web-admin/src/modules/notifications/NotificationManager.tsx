import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  Bell,
  CheckCircle2,
  Edit3,
  ExternalLink,
  Eye,
  EyeOff,
  Plus,
  Search,
  Send,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { FileUploadField } from '../../components/common/FileUploadField';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteNotification,
  generateDocId,
  mapNotificationDoc,
  saveNotification,
  subscribeCollection,
  toggleNotificationPublish,
} from '../../services/firebase/crudService';
import { isUnsafeLocalUrl } from '../../services/firebase/storageService';
import {
  NOTIFICATION_CATEGORIES,
  type NotificationDoc,
} from '../../types/firestore';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 8;

const DESTINATION_OPTIONS = [
  { value: 'home', labelBn: 'হোম পেজ (Home)', labelEn: 'Home Page' },
  { value: 'notices', labelBn: 'নোটিশ বোর্ড (Notices)', labelEn: 'Notices Board' },
  { value: 'development', labelBn: 'উন্নয়ন প্রকল্প (Development)', labelEn: 'Development Projects' },
  { value: 'digital_services', labelBn: 'ডিজিটাল নাগরিক সেবা', labelEn: 'Digital Citizen Services' },
  { value: 'education', labelBn: 'শিক্ষা ও বৃত্তি (Education)', labelEn: 'Education & Support' },
  { value: 'employment', labelBn: 'কর্মসংস্থান ও অর্থনীতি', labelEn: 'Employment & Economy' },
  { value: 'social_welfare', labelBn: 'সামাজিক কল্যাণ (Welfare)', labelEn: 'Social Welfare' },
  { value: 'gallery', labelBn: 'মিডিয়া গ্যালারি (Media Gallery)', labelEn: 'Media Gallery' },
  { value: 'profile', labelBn: 'প্রোফাইল (Profile)', labelEn: 'Profile Info' },
  { value: 'contact', labelBn: 'যোগাযোগ ও আবেদন (Contact)', labelEn: 'Contact & Applications' },
  { value: 'app_update', labelBn: 'অ্যাপ আপডেট স্ক্রিন (App Update)', labelEn: 'App Update Screen' },
];

function createEmptyNotification(): NotificationDoc {
  return {
    id: generateDocId('notif'),
    titleBn: '',
    titleEn: '',
    bodyBn: '',
    bodyEn: '',
    categoryId: 'general',
    categoryBn: 'সাধারণ বিজ্ঞপ্তি',
    categoryEn: 'General Notice',
    imageUrl: '',
    destinationPage: 'home',
    externalLink: '',
    targetAudience: 'ALL',
    priority: 'NORMAL',
    sendPush: false,
    pushStatus: 'not_requested',
    isPublished: true,
    scheduledAt: '',
  };
}

export function NotificationManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [notifications, setNotifications] = useState<NotificationDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL');
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<NotificationDoc>(createEmptyNotification);
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<NotificationDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'notifications',
      mapNotificationDoc,
      (list) => {
        const sorted = [...list].sort((a, b) => {
          const ta = typeof a.createdAt === 'object' && a.createdAt && 'toMillis' in a.createdAt ? a.createdAt.toMillis() : 0;
          const tb = typeof b.createdAt === 'object' && b.createdAt && 'toMillis' in b.createdAt ? b.createdAt.toMillis() : 0;
          return tb - ta;
        });
        setNotifications(sorted);
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

  const canWrite =
    admin?.role === 'super_admin' ||
    admin?.role === 'editor' ||
    admin?.role === 'application_manager';
  const canDelete = admin?.role === 'super_admin';

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return notifications.filter((item) => {
      const catMatch =
        selectedCategory === 'ALL' ||
        item.categoryId === selectedCategory ||
        item.categoryBn === selectedCategory;
      const statusMatch =
        statusFilter === 'ALL' ||
        (statusFilter === 'PUBLISHED' && item.isPublished) ||
        (statusFilter === 'DRAFT' && !item.isPublished);
      const queryMatch =
        !q ||
        item.titleBn.toLowerCase().includes(q) ||
        item.titleEn.toLowerCase().includes(q) ||
        item.bodyBn.toLowerCase().includes(q) ||
        item.bodyEn.toLowerCase().includes(q) ||
        item.categoryBn.toLowerCase().includes(q);
      return catMatch && statusMatch && queryMatch;
    });
  }, [notifications, search, selectedCategory, statusFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, selectedCategory, statusFilter]);

  const openCreate = () => {
    setForm(createEmptyNotification());
    setIsNew(true);
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: NotificationDoc) => {
    setForm({ ...item });
    setIsNew(false);
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleCategoryChange = (categoryId: string) => {
    const found = NOTIFICATION_CATEGORIES.find((c) => c.id === categoryId);
    if (!found) return;
    setForm((prev) => ({
      ...prev,
      categoryId: found.id,
      categoryBn: found.labelBn,
      categoryEn: found.labelEn,
    }));
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canWrite) return;

    if (!form.titleBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'বাংলা শিরোনাম অবশ্যই দিতে হবে।' : 'Bengali title is required.',
      });
      return;
    }

    if (!form.bodyBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'বাংলা বিবরণ অবশ্যই দিতে হবে।' : 'Bengali description is required.',
      });
      return;
    }

    if (form.externalLink.trim()) {
      const link = form.externalLink.trim();
      if (isUnsafeLocalUrl(link) || !/^https?:\/\//i.test(link)) {
        setFeedback({
          type: 'error',
          text:
            lang === 'bn'
              ? 'বাহ্যিক লিংক অবশ্যই বৈধ http:// বা https:// URL হতে হবে।'
              : 'External link must be a valid http:// or https:// URL.',
        });
        return;
      }
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveNotification(form, isNew, admin);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? form.isPublished
              ? 'বিজ্ঞপ্তি প্রকাশিত হয়েছে এবং ফায়ারস্টোর লাইভ সিঙ্কে যুক্ত হয়েছে।'
              : 'বিজ্ঞপ্তি খসড়া হিসেবে সংরক্ষিত হয়েছে।'
            : form.isPublished
            ? 'Notification published and synced live to Firestore.'
            : 'Notification saved as draft.',
      });
      setEditorOpen(false);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save notification.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleTogglePublish = async (item: NotificationDoc) => {
    if (!admin || !canWrite) return;
    try {
      await toggleNotificationPublish(
        item.id,
        !item.isPublished,
        item.titleBn || item.titleEn || item.id,
        admin
      );
      setFeedback({
        type: 'success',
        text: !item.isPublished
          ? lang === 'bn'
            ? 'বিজ্ঞপ্তি প্রকাশিত হয়েছে।'
            : 'Notification published.'
          : lang === 'bn'
          ? 'বিজ্ঞপ্তি অপ্রকাশিত করা হয়েছে।'
          : 'Notification unpublished.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update publish status.',
      });
    }
  };

  const handleConfirmDelete = async () => {
    if (!admin || !canDelete || !deleteTarget) return;
    setDeleting(true);
    try {
      await deleteNotification(
        deleteTarget.id,
        deleteTarget.titleBn || deleteTarget.titleEn || deleteTarget.id,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'বিজ্ঞপ্তি মুছে ফেলা হয়েছে।' : 'Notification deleted.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete notification.',
      });
    } finally {
      setDeleting(false);
    }
  };

  const publishedCount = notifications.filter((n) => n.isPublished).length;
  const urgentCount = notifications.filter((n) => n.categoryId === 'urgent' || n.priority === 'HIGH').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? 'নোটিফিকেশন ব্যবস্থাপনা' : 'Notification Manager'}
          </h2>
          <p className="mt-1 text-sm text-slate-600">
            {lang === 'bn'
              ? 'হোম পেজ, ইন-অ্যাপ নোটিফিকেশন সেন্টার ও অ্যাপ আপডেট বার্তা লাইভ ফায়ারস্টোর সিঙ্কের মাধ্যমে নিয়ন্ত্রণ করুন।'
              : 'Control homepage alerts, the in-app Notification Center, and push metadata synced live via Firestore.'}
          </p>
        </div>

        {canWrite ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন বিজ্ঞপ্তি তৈরি করুন' : 'Create Notification'}</span>
          </button>
        ) : null}
      </div>

      {/* Spark Plan FCM / Live Sync Clarity Banner */}
      <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-xs text-slate-700 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-start gap-2.5">
          <Bell className="w-4 h-4 text-blue-700 shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <p className="font-semibold text-slate-900">
              {lang === 'bn'
                ? 'Firebase Spark (ফ্রি) প্ল্যান সিঙ্ক ও FCM পুশ অবস্থা'
                : 'Firebase Spark Plan Live Sync & Push Notification Status'}
            </p>
            <p className="text-slate-600 leading-relaxed">
              {lang === 'bn'
                ? 'প্রকাশিত প্রতিটি বিজ্ঞপ্তি সাথে সাথেই অ্যান্ড্রয়েড অ্যাপের নোটিফিকেশন বেল, ব্যাজ ও নোটিফিকেশন সেন্টারে লাইভ সিঙ্ক হয়। স্বয়ংক্রিয় সার্ভার-সাইড FCM পুশ ট্রিগারের জন্য Blaze প্ল্যানে Cloud Functions অথবা একটি অনুমোদিত ব্যাকএন্ড/Firebase Console ক্যাম্পেইন প্রয়োজন।'
                : 'Published notifications sync immediately to the Android homepage bell badge and Notification Center on the Spark plan. Automated server-side FCM fan-out requires Blaze Cloud Functions or an authorized backend dispatcher.'}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-4 text-xs font-mono tabular-nums whitespace-nowrap shrink-0 border-t sm:border-t-0 sm:border-l border-slate-200 pt-2 sm:pt-0 sm:pl-4">
          <span>
            {lang === 'bn' ? 'প্রকাশিত:' : 'Published:'}{' '}
            <strong className="text-emerald-700">{toLocalizedNumber(publishedCount, lang)}</strong>
          </span>
          <span aria-hidden="true">·</span>
          <span>
            {lang === 'bn' ? 'জরুরি:' : 'Urgent:'}{' '}
            <strong className="text-red-700">{toLocalizedNumber(urgentCount, lang)}</strong>
          </span>
        </div>
      </div>

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

      {/* Filter & Search Bar */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="p-4 border-b border-slate-200 space-y-3 bg-slate-50/50">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
            <div className="relative w-full lg:w-80">
              <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder={lang === 'bn' ? 'শিরোনাম বা বিবরণ খুঁজুন...' : 'Search title or description...'}
                className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
              />
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="px-3 py-2 text-xs font-medium bg-white border border-slate-300 rounded-lg text-slate-800"
                aria-label={lang === 'bn' ? 'ক্যাটাগরি ফিল্টার' : 'Filter by category'}
              >
                <option value="ALL">
                  {lang === 'bn' ? 'সকল ক্যাটাগরি' : 'All Categories'}
                </option>
                {NOTIFICATION_CATEGORIES.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {lang === 'bn' ? cat.labelBn : cat.labelEn}
                  </option>
                ))}
              </select>

              <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg">
                {(['ALL', 'PUBLISHED', 'DRAFT'] as const).map((status) => (
                  <button
                    key={status}
                    type="button"
                    onClick={() => setStatusFilter(status)}
                    className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap cursor-pointer ${
                      statusFilter === status
                        ? 'bg-white text-slate-900 shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {status === 'ALL'
                      ? lang === 'bn'
                        ? 'সবগুলো'
                        : 'All'
                      : status === 'PUBLISHED'
                      ? lang === 'bn'
                        ? 'প্রকাশিত'
                        : 'Published'
                      : lang === 'bn'
                      ? 'খসড়া'
                      : 'Draft'}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {loadError ? (
          <div className="p-6 text-sm text-red-700 bg-red-50">{loadError}</div>
        ) : loading ? (
          <div className="py-12 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'বিজ্ঞপ্তি লোড হচ্ছে...' : 'Loading notifications...'}
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-12 text-center text-sm text-slate-500 space-y-2">
            <p>{lang === 'bn' ? 'কোনো বিজ্ঞপ্তি পাওয়া যায়নি।' : 'No notifications found.'}</p>
            {canWrite ? (
              <button
                type="button"
                onClick={openCreate}
                className="text-xs font-semibold text-blue-700 hover:underline cursor-pointer"
              >
                {lang === 'bn' ? '+ প্রথম বিজ্ঞপ্তি তৈরি করুন' : '+ Create first notification'}
              </button>
            ) : null}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="p-3.5">{lang === 'bn' ? 'বিজ্ঞপ্তির শিরোনাম ও বিভাগ' : 'Notification & Category'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'গন্তব্য ও অডিয়েন্স' : 'Target & Destination'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'অবস্থা' : 'Status'}</th>
                  <th className="p-3.5">{lang === 'bn' ? 'সময়' : 'Timestamp'}</th>
                  <th className="p-3.5 text-right">{lang === 'bn' ? 'কার্যক্রম' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="p-3.5 max-w-md">
                      <div className="flex items-start gap-3">
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.titleBn || item.titleEn}
                            referrerPolicy="no-referrer"
                            className="w-11 h-11 rounded object-cover border border-slate-200 shrink-0"
                          />
                        ) : null}
                        <div className="min-w-0">
                          <p className="font-semibold text-slate-900 line-clamp-1">
                            {lang === 'bn'
                              ? item.titleBn || item.titleEn
                              : item.titleEn || item.titleBn}
                          </p>
                          <p className="text-xs text-slate-600 line-clamp-1 mt-0.5">
                            {lang === 'bn'
                              ? item.bodyBn || item.bodyEn
                              : item.bodyEn || item.bodyBn}
                          </p>
                          <div className="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                            <span>{lang === 'bn' ? item.categoryBn : item.categoryEn}</span>
                            {item.priority === 'HIGH' ? (
                              <>
                                <span aria-hidden="true">·</span>
                                <span className="font-semibold text-red-700">
                                  {lang === 'bn' ? 'উচ্চ অগ্রাধিকার' : 'High Priority'}
                                </span>
                              </>
                            ) : null}
                            {item.externalLink ? (
                              <>
                                <span aria-hidden="true">·</span>
                                <a
                                  href={item.externalLink}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="inline-flex items-center gap-0.5 text-blue-700 hover:underline"
                                >
                                  <span>{lang === 'bn' ? 'লিংক' : 'Link'}</span>
                                  <ExternalLink className="w-3 h-3" />
                                </a>
                              </>
                            ) : null}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="p-3.5 text-xs text-slate-600 whitespace-nowrap">
                      <div className="font-mono text-slate-800">{item.destinationPage || 'home'}</div>
                      <div className="text-slate-500 mt-0.5">
                        {item.targetAudience}
                        {item.sendPush ? ' · FCM Flagged' : ''}
                      </div>
                    </td>

                    <td className="p-3.5 whitespace-nowrap">
                      {item.isPublished ? (
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>{lang === 'bn' ? 'প্রকাশিত' : 'Published'}</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs font-medium text-slate-500">
                          <EyeOff className="w-3.5 h-3.5" />
                          <span>{lang === 'bn' ? 'অপ্রকাশিত' : 'Draft'}</span>
                        </span>
                      )}
                    </td>

                    <td className="p-3.5 text-xs text-slate-500 tabular-nums whitespace-nowrap">
                      {item.publishedAt
                        ? formatFirestoreTimestamp(item.publishedAt, lang)
                        : item.scheduledAt || formatFirestoreTimestamp(item.createdAt, lang)}
                    </td>

                    <td className="p-3.5 whitespace-nowrap text-right">
                      <div className="inline-flex items-center gap-1">
                        {canWrite ? (
                          <>
                            <button
                              type="button"
                              onClick={() => handleTogglePublish(item)}
                              title={
                                item.isPublished
                                  ? lang === 'bn'
                                    ? 'অপ্রকাশিত করুন'
                                    : 'Unpublish'
                                  : lang === 'bn'
                                  ? 'প্রকাশ করুন'
                                  : 'Publish'
                              }
                              className="p-1.5 text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition-colors cursor-pointer"
                            >
                              {item.isPublished ? (
                                <EyeOff className="w-4 h-4" />
                              ) : (
                                <Eye className="w-4 h-4" />
                              )}
                            </button>
                            <button
                              type="button"
                              onClick={() => openEdit(item)}
                              title={lang === 'bn' ? 'সম্পাদনা' : 'Edit'}
                              className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                            >
                              <Edit3 className="w-4 h-4" />
                            </button>
                          </>
                        ) : null}

                        {canDelete ? (
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(item)}
                            title={lang === 'bn' ? 'মুছে ফেলুন' : 'Delete'}
                            className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && filtered.length > 0 ? (
          <Pagination
            currentPage={page}
            totalItems={filtered.length}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
          />
        ) : null}
      </div>

      {/* Editor Modal */}
      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন বিজ্ঞপ্তি তৈরি করুন'
              : 'Create Notification'
            : lang === 'bn'
            ? 'বিজ্ঞপ্তি সম্পাদনা করুন'
            : 'Edit Notification'
        }
        maxWidthClass="max-w-3xl"
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'ডকুমেন্ট আইডি' : 'Document ID'}
              </label>
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
                {lang === 'bn' ? 'বিজ্ঞপ্তির ক্যাটাগরি *' : 'Notification Category *'}
              </label>
              <select
                value={form.categoryId}
                onChange={(e) => handleCategoryChange(e.target.value)}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              >
                {NOTIFICATION_CATEGORIES.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.labelBn} ({cat.labelEn})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'শিরোনাম (বাংলা) *' : 'Title (Bengali) *'}
              </label>
              <input
                type="text"
                value={form.titleBn}
                onChange={(e) => setForm({ ...form, titleBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
                required
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'শিরোনাম (ইংরেজি)' : 'Title (English)'}
              </label>
              <input
                type="text"
                value={form.titleEn}
                onChange={(e) => setForm({ ...form, titleEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'বিস্তারিত বার্তা (বাংলা) *' : 'Description (Bengali) *'}
              </label>
              <textarea
                rows={3}
                value={form.bodyBn}
                onChange={(e) => setForm({ ...form, bodyBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
                required
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'বিস্তারিত বার্তা (ইংরেজি)' : 'Description (English)'}
              </label>
              <textarea
                rows={3}
                value={form.bodyEn}
                onChange={(e) => setForm({ ...form, bodyEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <FileUploadField
            label={lang === 'bn' ? 'ঐচ্ছিক ছবি সংযুক্ত করুন (Image)' : 'Optional Notification Image'}
            value={form.imageUrl}
            onChange={(url) => setForm({ ...form, imageUrl: url })}
            category="gallery"
            accept="image/jpeg,image/png,image/webp"
            helperText="JPG/PNG/WEBP ≤ 5 MB"
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'অ্যাপের গন্তব্য পেজ (Destination)' : 'In-App Destination Page'}
              </label>
              <select
                value={form.destinationPage}
                onChange={(e) => setForm({ ...form, destinationPage: e.target.value })}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              >
                {DESTINATION_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {lang === 'bn' ? opt.labelBn : opt.labelEn}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'ঐচ্ছিক বাহ্যিক লিংক (External URL)' : 'Optional External Link'}
              </label>
              <input
                type="url"
                value={form.externalLink}
                onChange={(e) => setForm({ ...form, externalLink: e.target.value })}
                placeholder="https://..."
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'টার্গেট অডিয়েন্স' : 'Target Audience'}
              </label>
              <select
                value={form.targetAudience}
                onChange={(e) => setForm({ ...form, targetAudience: e.target.value })}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              >
                <option value="ALL">
                  {lang === 'bn' ? 'সকল ব্যবহারকারী (ALL)' : 'All Users'}
                </option>
                <option value="MAHESHKHALI">
                  {lang === 'bn' ? 'মহেশখালী অঞ্চল' : 'Maheshkhali'}
                </option>
                <option value="KUTUBDIA">
                  {lang === 'bn' ? 'কুতুবদিয়া অঞ্চল' : 'Kutubdia'}
                </option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'অগ্রাধিকার' : 'Priority'}
              </label>
              <select
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value })}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              >
                <option value="NORMAL">{lang === 'bn' ? 'সাধারণ (Normal)' : 'Normal'}</option>
                <option value="HIGH">{lang === 'bn' ? 'জরুরি (High)' : 'High / Urgent'}</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">
                {lang === 'bn' ? 'নির্ধারিত সময় (ঐচ্ছিক)' : 'Schedule Time (Optional)'}
              </label>
              <input
                type="datetime-local"
                value={form.scheduledAt}
                onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
                className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-6 pt-2">
            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={form.isPublished}
                onChange={(e) => setForm({ ...form, isPublished: e.target.checked })}
                className="w-4 h-4 text-blue-700 border-slate-300 rounded"
              />
              <span>
                {lang === 'bn' ? 'অবিলম্বে প্রকাশ করুন (Publish Live)' : 'Publish immediately'}
              </span>
            </label>

            <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={form.sendPush}
                onChange={(e) => setForm({ ...form, sendPush: e.target.checked })}
                className="w-4 h-4 text-blue-700 border-slate-300 rounded"
              />
              <span className="inline-flex items-center gap-1">
                <Send className="w-3.5 h-3.5 text-slate-500" />
                <span>
                  {lang === 'bn'
                    ? 'FCM পুশ নোটিফিকেশন ফ্ল্যাগ যুক্ত করুন'
                    : 'Flag for FCM Push Delivery'}
                </span>
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
                ? 'সংরক্ষণ ও প্রকাশ করুন'
                : 'Save Notification'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'বিজ্ঞপ্তি মুছে ফেলুন' : 'Delete Notification'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিত যে "${deleteTarget?.titleBn || deleteTarget?.id}" বিজ্ঞপ্তিটি মুছে ফেলতে চান?`
            : `Are you sure you want to delete notification "${deleteTarget?.titleEn || deleteTarget?.id}"?`
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
