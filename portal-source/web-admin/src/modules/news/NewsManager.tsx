import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Plus,
  Search,
  Trash2,
  Newspaper,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteNewsItem,
  generateDocId,
  mapNewsItemDoc,
  saveNewsItem,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { NewsItemDoc } from '../../types/firestore';

const PAGE_SIZE = 8;

const EMPTY_NEWS: NewsItemDoc = {
  id: '',
  titleBn: '',
  titleEn: '',
  summaryBn: '',
  summaryEn: '',
  contentBn: '',
  contentEn: '',
  category: 'General',
  dateBn: '',
  dateEn: '',
  imageUrl: '',
  published: true,
};

export function NewsManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [newsList, setNewsList] = useState<NewsItemDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL');
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<NewsItemDoc>(EMPTY_NEWS);
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<NewsItemDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  useEffect(() => {
    const unsub = subscribeCollection(
      'news',
      mapNewsItemDoc,
      (items) => {
        setNewsList(items);
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
    return newsList.filter((n) => {
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'PUBLISHED' && n.published) ||
        (statusFilter === 'DRAFT' && !n.published);
      const matchesQuery =
        !q ||
        n.titleBn.toLowerCase().includes(q) ||
        n.titleEn.toLowerCase().includes(q) ||
        n.category.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  }, [newsList, search, statusFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const handleOpenCreate = () => {
    setIsNew(true);
    setForm({
      ...EMPTY_NEWS,
      id: generateDocId('news'),
      dateBn: new Date().toLocaleDateString('bn-BD'),
      dateEn: new Date().toISOString().slice(0, 10),
    });
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleOpenEdit = (item: NewsItemDoc) => {
    setIsNew(false);
    setForm(item);
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin) return;

    if (!form.id.trim() || !form.titleBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'আইডি এবং বাংলা শিরোনাম আবশ্যক।' : 'ID and Bengali title are required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);

    try {
      await saveNewsItem(form, isNew, admin);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'সংবাদ সফলভাবে সংরক্ষণ করা হয়েছে।' : 'News item saved successfully.',
      });
      setEditorOpen(false);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save news item.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!admin || !deleteTarget) return;
    setDeleting(true);
    try {
      await deleteNewsItem(deleteTarget.id, deleteTarget.titleBn || deleteTarget.id, admin);
      setDeleteTarget(null);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete news item.',
      });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Newspaper className="w-6 h-6 text-blue-700" />
            <span>{lang === 'bn' ? 'সংবাদ ও প্রেস বিজ্ঞপ্তি ব্যবস্থাপনা' : 'News & Press Releases'}</span>
          </h1>
          <p className="text-sm text-slate-600">
            {lang === 'bn'
              ? 'অফিসিয়াল সংবাদ, ঘোষণা এবং প্রেস রিলিজ পরিচালনা করুন।'
              : 'Manage official press releases, news items, and announcements.'}
          </p>
        </div>
        {canEdit ? (
          <button
            type="button"
            onClick={handleOpenCreate}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg shadow-sm transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন সংবাদ যুক্ত করুন' : 'Add News Item'}</span>
          </button>
        ) : null}
      </div>

      {feedback ? (
        <div
          className={`p-4 rounded-xl border flex items-start gap-3 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-900'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
          )}
          <span>{feedback.text}</span>
        </div>
      ) : null}

      <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row gap-3 justify-between items-center bg-slate-50/50">
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder={lang === 'bn' ? 'সংবাদ খুঁজুন...' : 'Search news...'}
              className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as any);
                setPage(1);
              }}
              className="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-700/20"
            >
              <option value="ALL">{lang === 'bn' ? 'সকল স্ট্যাটাস' : 'All Status'}</option>
              <option value="PUBLISHED">{lang === 'bn' ? 'প্রকাশিত' : 'Published'}</option>
              <option value="DRAFT">{lang === 'bn' ? 'খসড়া' : 'Draft'}</option>
            </select>
          </div>
        </div>

        {loadError ? (
          <div className="p-6 bg-red-50 text-sm text-red-700">{loadError}</div>
        ) : loading ? (
          <div className="py-12 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading news...'}
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-12 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'কোনো সংবাদ পাওয়া যায়নি।' : 'No news items found.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  <th className="p-3">ID / Date</th>
                  <th className="p-3">Title (BN)</th>
                  <th className="p-3">Category</th>
                  <th className="p-3">Status</th>
                  <th className="p-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="p-3 whitespace-nowrap font-mono text-xs text-slate-500">
                      <div>{item.id}</div>
                      <div className="text-[11px] text-slate-400">{item.dateBn || item.dateEn}</div>
                    </td>
                    <td className="p-3">
                      <div className="font-semibold text-slate-900 line-clamp-1">{item.titleBn}</div>
                      <div className="text-xs text-slate-500 line-clamp-1">{item.titleEn}</div>
                    </td>
                    <td className="p-3 whitespace-nowrap">
                      <span className="px-2.5 py-1 text-xs font-medium bg-slate-100 text-slate-700 rounded-full">
                        {item.category}
                      </span>
                    </td>
                    <td className="p-3 whitespace-nowrap">
                      <span
                        className={`px-2.5 py-1 text-xs font-medium rounded-full ${
                          item.published
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {item.published
                          ? lang === 'bn' ? 'প্রকাশিত' : 'Published'
                          : lang === 'bn' ? 'খসড়া' : 'Draft'}
                      </span>
                    </td>
                    <td className="p-3 whitespace-nowrap text-right space-x-2">
                      {canEdit ? (
                        <button
                          type="button"
                          onClick={() => handleOpenEdit(item)}
                          className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                          title={lang === 'bn' ? 'সম্পাদনা' : 'Edit'}
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                      ) : null}
                      {canDelete ? (
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(item)}
                          className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title={lang === 'bn' ? 'মুছে ফেলুন' : 'Delete'}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      ) : null}
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

      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={isNew ? (lang === 'bn' ? 'নতুন সংবাদ যুক্ত করুন' : 'Add News Item') : (lang === 'bn' ? 'সংবাদ সম্পাদনা' : 'Edit News Item')}
        maxWidthClass="max-w-3xl"
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Document ID (slug)</label>
              <input
                type="text"
                value={form.id}
                onChange={(e) => setForm({ ...form, id: e.target.value })}
                disabled={!isNew}
                placeholder="e.g. news_01"
                className="w-full px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-lg disabled:bg-slate-100"
                required
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Category</label>
              <input
                type="text"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                placeholder="e.g. Press Release"
                className="w-full px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Title (Bengali)</label>
              <input
                type="text"
                value={form.titleBn}
                onChange={(e) => setForm({ ...form, titleBn: e.target.value })}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
                required
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Title (English)</label>
              <input
                type="text"
                value={form.titleEn}
                onChange={(e) => setForm({ ...form, titleEn: e.target.value })}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Summary (Bengali)</label>
              <textarea
                value={form.summaryBn}
                onChange={(e) => setForm({ ...form, summaryBn: e.target.value })}
                rows={2}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Summary (English)</label>
              <textarea
                value={form.summaryEn}
                onChange={(e) => setForm({ ...form, summaryEn: e.target.value })}
                rows={2}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Content (Bengali)</label>
              <textarea
                value={form.contentBn}
                onChange={(e) => setForm({ ...form, contentBn: e.target.value })}
                rows={4}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Content (English)</label>
              <textarea
                value={form.contentEn}
                onChange={(e) => setForm({ ...form, contentEn: e.target.value })}
                rows={4}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Date (Bengali / English)</label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="text"
                  value={form.dateBn}
                  onChange={(e) => setForm({ ...form, dateBn: e.target.value })}
                  placeholder="তারিখ"
                  className="px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg"
                />
                <input
                  type="text"
                  value={form.dateEn}
                  onChange={(e) => setForm({ ...form, dateEn: e.target.value })}
                  placeholder="Date"
                  className="px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg"
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-slate-700">Image URL</label>
              <input
                type="text"
                value={form.imageUrl}
                onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                placeholder="https://..."
                className="w-full px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="newsPublished"
              checked={form.published}
              onChange={(e) => setForm({ ...form, published: e.target.checked })}
              className="w-4 h-4 text-blue-700 border-slate-300 rounded focus:ring-blue-700"
            />
            <label htmlFor="newsPublished" className="text-xs font-medium text-slate-700">
              {lang === 'bn' ? 'প্রকাশিত (Published)' : 'Published'}
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
              {saving ? (lang === 'bn' ? 'সংরক্ষণ হচ্ছে...' : 'Saving...') : (lang === 'bn' ? 'সংরক্ষণ করুন' : 'Save News')}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'সংবাদ মুছে ফেলুন' : 'Delete News Item'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিত যে "${deleteTarget?.titleBn}" সংবাদটি মুছে ফেলতে চান?`
            : `Are you sure you want to delete "${deleteTarget?.titleEn || deleteTarget?.titleBn}"?`
        }
        confirmLabel={lang === 'bn' ? 'মুছে ফেলুন' : 'Delete'}
        cancelLabel={lang === 'bn' ? 'বাতিল' : 'Cancel'}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
}
