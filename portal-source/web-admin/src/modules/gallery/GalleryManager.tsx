import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Eye,
  EyeOff,
  Image as ImageIcon,
  Plus,
  Search,
  Trash2,
  Video,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { FileUploadField } from '../../components/common/FileUploadField';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteGalleryItem,
  generateDocId,
  saveGalleryItem,
  subscribeCollection,
  toggleGalleryPublished,
} from '../../services/firebase/crudService';
import type { GalleryItemDoc } from '../../types/firestore';
import { toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 9;

function mapGalleryItem(id: string, raw: Record<string, unknown>): GalleryItemDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    titleBn: typeof raw.titleBn === 'string' ? raw.titleBn : '',
    titleEn: typeof raw.titleEn === 'string' ? raw.titleEn : '',
    descriptionBn: typeof raw.descriptionBn === 'string' ? raw.descriptionBn : '',
    captionBn: typeof raw.captionBn === 'string' ? raw.captionBn : '',
    categoryBn: typeof raw.categoryBn === 'string' ? raw.categoryBn : '',
    categoryEn: typeof raw.categoryEn === 'string' ? raw.categoryEn : '',
    dateBn: typeof raw.dateBn === 'string' ? raw.dateBn : '',
    dateEn: typeof raw.dateEn === 'string' ? raw.dateEn : '',
    year: typeof raw.year === 'string' ? raw.year : '',
    locationBn: typeof raw.locationBn === 'string' ? raw.locationBn : '',
    unionBn: typeof raw.unionBn === 'string' ? raw.unionBn : '',
    upazila: typeof raw.upazila === 'string' ? raw.upazila : '',
    mediaType: raw.mediaType === 'VIDEO' ? 'VIDEO' : 'PHOTO',
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : '',
    videoEmbedUrl: typeof raw.videoEmbedUrl === 'string' ? raw.videoEmbedUrl : '',
    verifiedSource: typeof raw.verifiedSource === 'string' ? raw.verifiedSource : '',
    published: raw.published !== false,
  };
}

export function GalleryManager({
  defaultMediaType,
}: {
  defaultMediaType: 'PHOTO' | 'VIDEO';
}) {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [items, setItems] = useState<GalleryItemDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [mediaFilter, setMediaFilter] = useState<'PHOTO' | 'VIDEO' | 'ALL'>(defaultMediaType);
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<GalleryItemDoc>(() => ({
    id: '',
    titleBn: '',
    titleEn: '',
    descriptionBn: '',
    captionBn: '',
    categoryBn: '',
    categoryEn: '',
    dateBn: '',
    dateEn: '',
    year: '২০২৬',
    locationBn: '',
    unionBn: '',
    upazila: '',
    mediaType: defaultMediaType,
    imageUrl: '',
    videoEmbedUrl: '',
    verifiedSource: '',
    published: true,
  }));
  const [saving, setSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<GalleryItemDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    setMediaFilter(defaultMediaType);
  }, [defaultMediaType]);

  useEffect(() => {
    const unsub = subscribeCollection(
      'gallery_items',
      mapGalleryItem,
      (docs) => {
        setItems(docs);
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
    return items.filter((item) => {
      const matchesType = mediaFilter === 'ALL' || item.mediaType === mediaFilter;
      const matchesQuery =
        !q ||
        item.titleBn.toLowerCase().includes(q) ||
        item.titleEn.toLowerCase().includes(q) ||
        item.locationBn.toLowerCase().includes(q);
      return matchesType && matchesQuery;
    });
  }, [items, mediaFilter, search]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, mediaFilter]);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  const openCreate = () => {
    setIsNew(true);
    setForm({
      id: generateDocId('g'),
      titleBn: '',
      titleEn: '',
      descriptionBn: '',
      captionBn: '',
      categoryBn: '',
      categoryEn: '',
      dateBn: '',
      dateEn: '',
      year: '২০২৬',
      locationBn: '',
      unionBn: '',
      upazila: '',
      mediaType: defaultMediaType,
      imageUrl: '',
      videoEmbedUrl: '',
      verifiedSource: '',
      published: true,
    });
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: GalleryItemDoc) => {
    setIsNew(false);
    setForm({ ...item });
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.titleBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'বাংলা শিরোনাম আবশ্যক।' : 'Bengali title is required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveGalleryItem(form, isNew, admin);
      setEditorOpen(false);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'গ্যালারি আইটেম সংরক্ষিত হয়েছে।' : 'Gallery item saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save gallery item.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleTogglePublished = async (item: GalleryItemDoc) => {
    if (!admin || !canEdit) return;
    try {
      await toggleGalleryPublished(
        item.id,
        !item.published,
        item.titleBn || item.titleEn,
        admin
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `আইটেমটি ${!item.published ? 'প্রকাশিত' : 'অপ্রকাশিত'} করা হয়েছে।`
            : `Item ${!item.published ? 'published' : 'unpublished'}.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to toggle visibility.',
      });
    }
  };

  const handleDelete = async () => {
    if (!admin || !deleteTarget || !canDelete) return;
    setDeleting(true);
    try {
      await deleteGalleryItem(
        deleteTarget.id,
        deleteTarget.titleBn || deleteTarget.titleEn,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'গ্যালারি আইটেম মুছে ফেলা হয়েছে।' : 'Gallery item deleted.',
      });
    } catch (err) {
      setDeleteTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete gallery item.',
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
            {defaultMediaType === 'VIDEO'
              ? lang === 'bn'
                ? 'ভিডিও গ্যালারি'
                : 'Video Gallery'
              : lang === 'bn'
              ? 'ফটো গ্যালারি'
              : 'Photo Gallery'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            gallery_items · {toLocalizedNumber(filtered.length, lang)}{' '}
            {lang === 'bn' ? 'টি আইটেম' : 'items'}
          </p>
        </div>

        {canEdit ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>
              {defaultMediaType === 'VIDEO'
                ? lang === 'bn'
                  ? 'নতুন ভিডিও যুক্ত করুন'
                  : 'Add Video'
                : lang === 'bn'
                ? 'নতুন ছবি যুক্ত করুন'
                : 'Add Photo'}
            </span>
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
              lang === 'bn' ? 'শিরোনাম বা স্থান দিয়ে খুঁজুন...' : 'Search gallery...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>

        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg">
          {(['PHOTO', 'VIDEO', 'ALL'] as const).map((mt) => (
            <button
              key={mt}
              type="button"
              onClick={() => setMediaFilter(mt)}
              className={`px-3 py-1.5 text-xs font-medium rounded-md cursor-pointer ${
                mediaFilter === mt
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {mt === 'ALL' ? (lang === 'bn' ? 'সকল' : 'All') : mt}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
          {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading gallery...'}
        </div>
      ) : paginated.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
          {lang === 'bn' ? 'কোনো গ্যালারি আইটেম পাওয়া যায়নি।' : 'No gallery items found.'}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {paginated.map((item) => {
            const isHttpImg = /^https?:\/\//i.test(item.imageUrl);
            return (
              <div
                key={item.id}
                className="bg-white border border-slate-200 rounded-xl overflow-hidden flex flex-col justify-between"
              >
                <div>
                  <div className="h-44 bg-slate-100 border-b border-slate-200 flex items-center justify-center overflow-hidden">
                    {isHttpImg ? (
                      <img
                        src={item.imageUrl}
                        alt={item.titleBn || item.titleEn}
                        referrerPolicy="no-referrer"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="flex flex-col items-center gap-1.5 text-slate-400 px-4 text-center">
                        {item.mediaType === 'VIDEO' ? (
                          <Video className="w-7 h-7" />
                        ) : (
                          <ImageIcon className="w-7 h-7" />
                        )}
                        <span className="text-[11px] font-mono truncate max-w-full">
                          {item.imageUrl || item.videoEmbedUrl || item.id}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="p-4">
                    <div className="text-xs text-slate-500 flex items-center justify-between gap-2">
                      <span>
                        {item.mediaType} ·{' '}
                        {(lang === 'bn' ? item.categoryBn : item.categoryEn) || '—'}
                      </span>
                      <span
                        className={
                          item.published ? 'text-emerald-700 font-medium' : 'text-amber-700 font-medium'
                        }
                      >
                        {item.published
                          ? lang === 'bn'
                            ? 'প্রকাশিত'
                            : 'Published'
                          : lang === 'bn'
                          ? 'অপ্রকাশিত'
                          : 'Unpublished'}
                      </span>
                    </div>

                    <h3 className="mt-1.5 text-sm font-semibold text-slate-900">
                      {lang === 'bn' ? item.titleBn : item.titleEn || item.titleBn}
                    </h3>
                    <p className="mt-1 text-xs text-slate-600 line-clamp-2">
                      {item.descriptionBn || item.captionBn || '—'}
                    </p>
                  </div>
                </div>

                <div className="px-4 py-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <span className="text-xs text-slate-500 truncate">
                    {item.locationBn || item.upazila || '—'}
                  </span>
                  <div className="flex items-center gap-1 shrink-0">
                    {canEdit ? (
                      <>
                        <button
                          type="button"
                          onClick={() => handleTogglePublished(item)}
                          className="p-1.5 text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg cursor-pointer"
                          title={item.published ? 'Unpublish' : 'Publish'}
                        >
                          {item.published ? (
                            <EyeOff className="w-4 h-4" />
                          ) : (
                            <Eye className="w-4 h-4" />
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => openEdit(item)}
                          className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                          aria-label="Edit gallery item"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                      </>
                    ) : null}
                    {canDelete ? (
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(item)}
                        className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                        aria-label="Delete gallery item"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : null}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
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
        maxWidthClass="max-w-2xl"
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন গ্যালারি আইটেম যুক্ত করুন'
              : 'Add Gallery Item'
            : lang === 'bn'
            ? 'গ্যালারি আইটেম সম্পাদনা'
            : 'Edit Gallery Item'
        }
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'আইডি *' : 'ID *'}
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
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'মিডিয়ার ধরন' : 'Media Type'}
              </label>
              <select
                value={form.mediaType}
                onChange={(e) => setForm({ ...form, mediaType: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
              >
                <option value="PHOTO">PHOTO</option>
                <option value="VIDEO">VIDEO</option>
              </select>
            </div>
            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-800 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.published}
                  onChange={(e) => setForm({ ...form, published: e.target.checked })}
                  className="w-4 h-4 rounded border-slate-300 text-emerald-700"
                />
                <span>{lang === 'bn' ? 'প্রকাশিত (Published)' : 'Published'}</span>
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

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              {lang === 'bn' ? 'বিবরণ (বাংলা)' : 'Description (Bengali)'}
            </label>
            <textarea
              rows={2}
              value={form.descriptionBn}
              onChange={(e) => setForm({ ...form, descriptionBn: e.target.value })}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
            />
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'উপজেলা' : 'Upazila'}
              </label>
              <input
                type="text"
                value={form.upazila}
                onChange={(e) => setForm({ ...form, upazila: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ইউনিয়ন (বাংলা)' : 'Union (Bn)'}
              </label>
              <input
                type="text"
                value={form.unionBn}
                onChange={(e) => setForm({ ...form, unionBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'স্থান (বাংলা)' : 'Location (Bn)'}
              </label>
              <input
                type="text"
                value={form.locationBn}
                onChange={(e) => setForm({ ...form, locationBn: e.target.value })}
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
                {lang === 'bn' ? 'সাল' : 'Year'}
              </label>
              <input
                type="text"
                value={form.year}
                onChange={(e) => setForm({ ...form, year: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'যাচাইকৃত সূত্র' : 'Verified Source'}
              </label>
              <input
                type="text"
                value={form.verifiedSource}
                onChange={(e) => setForm({ ...form, verifiedSource: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <FileUploadField
            label={lang === 'bn' ? 'ছবি আপলোড / URL (imageUrl)' : 'Photo Upload / URL (imageUrl)'}
            value={form.imageUrl}
            onChange={(url) => setForm({ ...form, imageUrl: url })}
            category="gallery"
            accept="image/jpeg,image/png,image/webp"
            helperText="JPG/PNG/WEBP ≤ 5 MB"
          />

          <FileUploadField
            label={
              lang === 'bn'
                ? 'ভিডিও আপলোড / এম্বেড URL (videoEmbedUrl)'
                : 'Video Upload / Embed URL (videoEmbedUrl)'
            }
            value={form.videoEmbedUrl}
            onChange={(url) => setForm({ ...form, videoEmbedUrl: url })}
            category="video"
            accept="video/mp4,video/webm"
            helperText="MP4/WEBM ≤ 50 MB"
          />

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
                : 'Save Item'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'গ্যালারি আইটেম মুছুন' : 'Confirm Deletion'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${deleteTarget?.titleBn || deleteTarget?.titleEn}" মুছে ফেলতে চান?`
            : `Delete "${deleteTarget?.titleEn || deleteTarget?.titleBn}"?`
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
