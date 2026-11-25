import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Plus,
  Search,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteNotice,
  generateDocId,
  saveNotice,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { NoticeDoc } from '../../types/firestore';
import { toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 8;

const EMPTY_NOTICE: NoticeDoc = {
  id: '',
  titleBn: '',
  titleEn: '',
  detailsBn: '',
  detailsEn: '',
  categoryBn: '',
  categoryEn: '',
  dateBn: '',
  dateEn: '',
  urgent: false,
};

function mapNotice(id: string, raw: Record<string, unknown>): NoticeDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    titleBn: typeof raw.titleBn === 'string' ? raw.titleBn : '',
    titleEn: typeof raw.titleEn === 'string' ? raw.titleEn : '',
    detailsBn: typeof raw.detailsBn === 'string' ? raw.detailsBn : '',
    detailsEn: typeof raw.detailsEn === 'string' ? raw.detailsEn : '',
    categoryBn: typeof raw.categoryBn === 'string' ? raw.categoryBn : '',
    categoryEn: typeof raw.categoryEn === 'string' ? raw.categoryEn : '',
    dateBn: typeof raw.dateBn === 'string' ? raw.dateBn : '',
    dateEn: typeof raw.dateEn === 'string' ? raw.dateEn : '',
    urgent: Boolean(raw.urgent),
  };
}

export function NoticesManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [notices, setNotices] = useState<NoticeDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [urgentFilter, setUrgentFilter] = useState<'ALL' | 'URGENT' | 'REGULAR'>('ALL');
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<NoticeDoc>(EMPTY_NOTICE);
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<NoticeDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'notices',
      mapNotice,
      (items) => {
        setNotices(items);
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

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return notices.filter((n) => {
      const matchesUrgent =
        urgentFilter === 'ALL' ||
        (urgentFilter === 'URGENT' && n.urgent) ||
        (urgentFilter === 'REGULAR' && !n.urgent);
      const matchesQuery =
        !q ||
        n.titleBn.toLowerCase().includes(q) ||
        n.titleEn.toLowerCase().includes(q) ||
        n.categoryBn.toLowerCase().includes(q);
      return matchesUrgent && matchesQuery;
    });
  }, [notices, search, urgentFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, urgentFilter]);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  const openCreate = () => {
    setIsNew(true);
    setForm({ ...EMPTY_NOTICE, id: generateDocId('N') });
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: NoticeDoc) => {
    setIsNew(false);
    setForm({ ...item });
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.titleBn.trim() || !form.detailsBn.trim()) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'বাংলা শিরোনাম ও বিস্তারিত তথ্য আবশ্যক।'
            : 'Bengali title and details are required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveNotice(form, isNew, admin);
      setEditorOpen(false);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'নোটিশ সফলভাবে সংরক্ষিত হয়েছে।'
            : 'Notice saved successfully.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save notice.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!admin || !deleteTarget || !canDelete) return;
    setDeleting(true);
    try {
      await deleteNotice(
        deleteTarget.id,
        deleteTarget.titleBn || deleteTarget.titleEn,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'নোটিশ মুছে ফেলা হয়েছে।' : 'Notice deleted.',
      });
    } catch (err) {
      setDeleteTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete notice.',
      });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? 'নোটিশ ও ঘোষণা' : 'Notices and Announcements'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            notices · {toLocalizedNumber(notices.length, lang)}{' '}
            {lang === 'bn' ? 'টি নোটিশ' : 'notices'}
          </p>
        </div>

        {canEdit ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন নোটিশ প্রকাশ' : 'Publish Notice'}</span>
          </button>
        ) : null}
      </div>

      {feedback ? (
        <div
          role="alert"
          className={`p-3.5 rounded-xl border flex items-start gap-2.5 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-800'
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

      <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col md:flex-row gap-3 justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={
              lang === 'bn' ? 'নোটিশ শিরোনাম বা বিভাগ খুঁজুন...' : 'Search notices...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
          />
        </div>

        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg">
          {(['ALL', 'URGENT', 'REGULAR'] as const).map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setUrgentFilter(f)}
              className={`px-3 py-1.5 text-xs font-medium rounded-md cursor-pointer ${
                urgentFilter === f
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {f === 'ALL'
                ? lang === 'bn'
                  ? 'সকল'
                  : 'All'
                : f === 'URGENT'
                ? lang === 'bn'
                  ? 'জরুরি'
                  : 'Urgent'
                : lang === 'bn'
                ? 'সাধারণ'
                : 'Regular'}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading notices...'}
          </div>
        ) : paginated.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'কোনো নোটিশ পাওয়া যায়নি।' : 'No notices found.'}
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {paginated.map((n) => (
              <div
                key={n.id}
                className="p-4 sm:px-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/70 transition-colors"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2 text-xs text-slate-500 mb-1">
                    <span className="font-mono">{n.id}</span>
                    <span aria-hidden="true">·</span>
                    <span>{(lang === 'bn' ? n.categoryBn : n.categoryEn) || '—'}</span>
                    <span aria-hidden="true">·</span>
                    <span>{(lang === 'bn' ? n.dateBn : n.dateEn) || '—'}</span>
                    {n.urgent ? (
                      <>
                        <span aria-hidden="true">·</span>
                        <span className="font-semibold text-red-700">
                          {lang === 'bn' ? 'জরুরি ঘোষণা' : 'Urgent'}
                        </span>
                      </>
                    ) : null}
                  </div>
                  <h3 className="text-sm font-semibold text-slate-900">
                    {lang === 'bn' ? n.titleBn : n.titleEn || n.titleBn}
                  </h3>
                  <p className="mt-1 text-xs text-slate-600 line-clamp-2">
                    {lang === 'bn' ? n.detailsBn : n.detailsEn || n.detailsBn}
                  </p>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {canEdit ? (
                    <button
                      type="button"
                      onClick={() => openEdit(n)}
                      className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                      aria-label="Edit notice"
                    >
                      <Edit3 className="w-4 h-4" />
                    </button>
                  ) : null}
                  {canDelete ? (
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(n)}
                      className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                      aria-label="Delete notice"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}

        <Pagination
          currentPage={page}
          totalItems={filtered.length}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      </div>

      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন নোটিশ প্রকাশ'
              : 'Publish Notice'
            : lang === 'bn'
            ? 'নোটিশ সম্পাদনা'
            : 'Edit Notice'
        }
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ডকুমেন্ট আইডি *' : 'Document ID *'}
              </label>
              <input
                type="text"
                required
                disabled={!isNew}
                value={form.id}
                onChange={(e) => setForm({ ...form, id: e.target.value })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg disabled:bg-slate-100"
              />
            </div>

            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-800 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.urgent}
                  onChange={(e) => setForm({ ...form, urgent: e.target.checked })}
                  className="w-4 h-4 rounded border-slate-300 text-red-700"
                />
                <span>{lang === 'bn' ? 'জরুরি নোটিশ হিসেবে চিহ্নিত করুন' : 'Mark as Urgent Notice'}</span>
              </label>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'শিরোনাম (বাংলা) *' : 'Title (Bengali) *'}
              </label>
              <input
                type="text"
                required
                value={form.titleBn}
                onChange={(e) => setForm({ ...form, titleBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
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
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিস্তারিত (বাংলা) *' : 'Details (Bengali) *'}
              </label>
              <textarea
                rows={4}
                required
                value={form.detailsBn}
                onChange={(e) => setForm({ ...form, detailsBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিস্তারিত (ইংরেজি)' : 'Details (English)'}
              </label>
              <textarea
                rows={4}
                value={form.detailsEn}
                onChange={(e) => setForm({ ...form, detailsEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিভাগ (বাংলা)' : 'Category (Bn)'}
              </label>
              <input
                type="text"
                value={form.categoryBn}
                onChange={(e) => setForm({ ...form, categoryBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিভাগ (ইংরেজি)' : 'Category (En)'}
              </label>
              <input
                type="text"
                value={form.categoryEn}
                onChange={(e) => setForm({ ...form, categoryEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'তারিখ (বাংলা)' : 'Date (Bn)'}
              </label>
              <input
                type="text"
                value={form.dateBn}
                onChange={(e) => setForm({ ...form, dateBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'তারিখ (ইংরেজি)' : 'Date (En)'}
              </label>
              <input
                type="text"
                value={form.dateEn}
                onChange={(e) => setForm({ ...form, dateEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
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
                ? 'সংরক্ষণ করুন'
                : 'Save Notice'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'নোটিশ মুছে ফেলার নিশ্চিতকরণ' : 'Confirm Notice Deletion'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${deleteTarget?.titleBn || deleteTarget?.titleEn}" নোটিশটি মুছে ফেলতে চান?`
            : `Delete notice "${deleteTarget?.titleEn || deleteTarget?.titleBn}"?`
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
