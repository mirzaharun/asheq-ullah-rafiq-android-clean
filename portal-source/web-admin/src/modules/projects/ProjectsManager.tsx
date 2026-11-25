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
import { FileUploadField } from '../../components/common/FileUploadField';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteDevelopmentProject,
  generateDocId,
  saveDevelopmentProject,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { DevelopmentProjectDoc } from '../../types/firestore';
import { toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 8;

const EMPTY_PROJECT: DevelopmentProjectDoc = {
  id: '',
  titleBn: '',
  titleEn: '',
  descriptionBn: '',
  descriptionEn: '',
  categoryBn: '',
  categoryEn: '',
  budgetBn: '',
  budgetEn: '',
  timelineBn: '',
  timelineEn: '',
  impactBn: '',
  impactEn: '',
  union: 'All',
  upazila: 'Maheshkhali',
  year: new Date().getFullYear(),
  status: 'Ongoing',
  completionPercentage: 0,
  imageUrl: '',
  beforeImageUrl: '',
  afterImageUrl: '',
  pdfUrl: '',
};

function mapProject(id: string, raw: Record<string, unknown>): DevelopmentProjectDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    titleBn: typeof raw.titleBn === 'string' ? raw.titleBn : '',
    titleEn: typeof raw.titleEn === 'string' ? raw.titleEn : '',
    descriptionBn: typeof raw.descriptionBn === 'string' ? raw.descriptionBn : '',
    descriptionEn: typeof raw.descriptionEn === 'string' ? raw.descriptionEn : '',
    categoryBn: typeof raw.categoryBn === 'string' ? raw.categoryBn : '',
    categoryEn: typeof raw.categoryEn === 'string' ? raw.categoryEn : '',
    budgetBn: typeof raw.budgetBn === 'string' ? raw.budgetBn : '',
    budgetEn: typeof raw.budgetEn === 'string' ? raw.budgetEn : '',
    timelineBn: typeof raw.timelineBn === 'string' ? raw.timelineBn : '',
    timelineEn: typeof raw.timelineEn === 'string' ? raw.timelineEn : '',
    impactBn: typeof raw.impactBn === 'string' ? raw.impactBn : '',
    impactEn: typeof raw.impactEn === 'string' ? raw.impactEn : '',
    union: typeof raw.union === 'string' ? raw.union : '',
    upazila: typeof raw.upazila === 'string' ? raw.upazila : '',
    year: typeof raw.year === 'number' ? raw.year : 2026,
    status: typeof raw.status === 'string' ? raw.status : 'Ongoing',
    completionPercentage:
      typeof raw.completionPercentage === 'number' ? raw.completionPercentage : 0,
    imageUrl: typeof raw.imageUrl === 'string' ? raw.imageUrl : '',
    beforeImageUrl: typeof raw.beforeImageUrl === 'string' ? raw.beforeImageUrl : '',
    afterImageUrl: typeof raw.afterImageUrl === 'string' ? raw.afterImageUrl : '',
    pdfUrl: typeof raw.pdfUrl === 'string' ? raw.pdfUrl : '',
  };
}

export function ProjectsManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [projects, setProjects] = useState<DevelopmentProjectDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<DevelopmentProjectDoc>(EMPTY_PROJECT);
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<DevelopmentProjectDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'development_projects',
      mapProject,
      (items) => {
        setProjects(items);
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
    return projects.filter((p) => {
      const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
      const matchesQuery =
        !q ||
        p.titleBn.toLowerCase().includes(q) ||
        p.titleEn.toLowerCase().includes(q) ||
        p.upazila.toLowerCase().includes(q) ||
        p.union.toLowerCase().includes(q) ||
        p.categoryBn.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  }, [projects, search, statusFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, statusFilter]);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  const openCreate = () => {
    setIsNew(true);
    setForm({ ...EMPTY_PROJECT, id: generateDocId('proj') });
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: DevelopmentProjectDoc) => {
    setIsNew(false);
    setForm({ ...item });
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.titleBn.trim() || !form.titleEn.trim()) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'বাংলা এবং ইংরেজি উভয় শিরোনাম আবশ্যক।'
            : 'Both Bengali and English titles are required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveDevelopmentProject(form, isNew, admin);
      setEditorOpen(false);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'উন্নয়ন প্রকল্প সফলভাবে ফায়ারস্টোরে সংরক্ষিত হয়েছে।'
            : 'Development project saved to Firestore.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save project.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!admin || !deleteTarget || !canDelete) return;
    setDeleting(true);
    try {
      await deleteDevelopmentProject(
        deleteTarget.id,
        deleteTarget.titleBn || deleteTarget.titleEn,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'প্রকল্পটি স্থায়ীভাবে মুছে ফেলা হয়েছে।'
            : 'Development project deleted.',
      });
    } catch (err) {
      setDeleteTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete project.',
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
            {lang === 'bn' ? 'উন্নয়ন প্রকল্প ব্যবস্থাপনা' : 'Development Projects'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            development_projects · {toLocalizedNumber(projects.length, lang)}{' '}
            {lang === 'bn' ? 'টি রেকর্ড' : 'records'}
          </p>
        </div>

        {canEdit ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন প্রকল্প যুক্ত করুন' : 'Add Project'}</span>
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

      {/* Search & Status Filters */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col md:flex-row gap-3 justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={
              lang === 'bn'
                ? 'শিরোনাম, উপজেলা বা বিভাগ দিয়ে খুঁজুন...'
                : 'Search by title, upazila, or category...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
          />
        </div>

        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg overflow-x-auto">
          {['ALL', 'Ongoing', 'Completed', 'Planned'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 text-xs font-medium rounded-md whitespace-nowrap transition-colors cursor-pointer ${
                statusFilter === st
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {st === 'ALL' ? (lang === 'bn' ? 'সকল' : 'All') : st}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading projects...'}
          </div>
        ) : paginated.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'কোনো প্রকল্প পাওয়া যায়নি।' : 'No projects found.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-3 px-4">{lang === 'bn' ? 'শিরোনাম' : 'Title'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'এলাকা · বিভাগ' : 'Area · Category'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'বাজেট · বছর' : 'Budget · Year'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'অবস্থা' : 'Status'}</th>
                  <th className="py-3 px-4 text-right">{lang === 'bn' ? 'অগ্রগতি' : 'Progress'}</th>
                  <th className="py-3 px-4 text-right">{lang === 'bn' ? 'কার্যক্রম' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((proj) => (
                  <tr key={proj.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">
                        {lang === 'bn' ? proj.titleBn : proj.titleEn || proj.titleBn}
                      </div>
                      <div className="text-xs text-slate-500 font-mono">{proj.id}</div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">
                      <div>{[proj.upazila, proj.union].filter(Boolean).join(' · ') || '—'}</div>
                      <div className="text-slate-500">
                        {lang === 'bn' ? proj.categoryBn : proj.categoryEn}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600 tabular-nums">
                      <div>{lang === 'bn' ? proj.budgetBn || '—' : proj.budgetEn || '—'}</div>
                      <div className="font-mono text-slate-500">{proj.year}</div>
                    </td>
                    <td className="py-3 px-4 text-xs font-medium text-slate-700 whitespace-nowrap">
                      {proj.status}
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-xs font-semibold text-emerald-700 tabular-nums">
                      {toLocalizedNumber(proj.completionPercentage, lang)}%
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center gap-1.5">
                        {canEdit ? (
                          <button
                            type="button"
                            onClick={() => openEdit(proj)}
                            className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                            aria-label="Edit project"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                        ) : null}
                        {canDelete ? (
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(proj)}
                            className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            aria-label="Delete project"
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

        <Pagination
          currentPage={page}
          totalItems={filtered.length}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      </div>

      {/* Add / Edit Project Modal */}
      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        maxWidthClass="max-w-3xl"
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন উন্নয়ন প্রকল্প যুক্ত করুন'
              : 'Add Development Project'
            : lang === 'bn'
            ? 'উন্নয়ন প্রকল্প সম্পাদনা'
            : 'Edit Development Project'
        }
        subtitle={`Document ID: ${form.id}`}
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ডকুমেন্ট আইডি (ID) *' : 'Document ID *'}
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

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {lang === 'bn' ? 'অবস্থা (Status)' : 'Status'}
                </label>
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
                >
                  <option value="Ongoing">Ongoing</option>
                  <option value="Completed">Completed</option>
                  <option value="Planned">Planned</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {lang === 'bn' ? 'অগ্রগতি (%)' : 'Completion (%)'}
                </label>
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={form.completionPercentage}
                  onChange={(e) =>
                    setForm({ ...form, completionPercentage: Number(e.target.value) })
                  }
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
                {lang === 'bn' ? 'শিরোনাম (ইংরেজি) *' : 'Title (English) *'}
              </label>
              <input
                type="text"
                required
                value={form.titleEn}
                onChange={(e) => setForm({ ...form, titleEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিবরণ (বাংলা)' : 'Description (Bengali)'}
              </label>
              <textarea
                rows={3}
                value={form.descriptionBn}
                onChange={(e) => setForm({ ...form, descriptionBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিবরণ (ইংরেজি)' : 'Description (English)'}
              </label>
              <textarea
                rows={3}
                value={form.descriptionEn}
                onChange={(e) => setForm({ ...form, descriptionEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
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
                {lang === 'bn' ? 'ইউনিয়ন' : 'Union'}
              </label>
              <input
                type="text"
                value={form.union}
                onChange={(e) => setForm({ ...form, union: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বাজেট (বাংলা)' : 'Budget (Bn)'}
              </label>
              <input
                type="text"
                value={form.budgetBn}
                onChange={(e) => setForm({ ...form, budgetBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বাজেট (ইংরেজি)' : 'Budget (En)'}
              </label>
              <input
                type="text"
                value={form.budgetEn}
                onChange={(e) => setForm({ ...form, budgetEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সময়কাল (বাংলা)' : 'Timeline (Bn)'}
              </label>
              <input
                type="text"
                value={form.timelineBn}
                onChange={(e) => setForm({ ...form, timelineBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সময়কাল (ইংরেজি)' : 'Timeline (En)'}
              </label>
              <input
                type="text"
                value={form.timelineEn}
                onChange={(e) => setForm({ ...form, timelineEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সাল' : 'Year'}
              </label>
              <input
                type="number"
                min={1970}
                max={2100}
                value={form.year}
                onChange={(e) => setForm({ ...form, year: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রভাব (বাংলা)' : 'Impact (Bengali)'}
              </label>
              <input
                type="text"
                value={form.impactBn}
                onChange={(e) => setForm({ ...form, impactBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রভাব (ইংরেজি)' : 'Impact (English)'}
              </label>
              <input
                type="text"
                value={form.impactEn}
                onChange={(e) => setForm({ ...form, impactEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="pt-2 border-t border-slate-200 space-y-4">
            <FileUploadField
              label={lang === 'bn' ? 'মূল প্রকল্প ছবি (imageUrl)' : 'Main Project Photo (imageUrl)'}
              value={form.imageUrl}
              onChange={(url) => setForm({ ...form, imageUrl: url })}
              category="project"
              accept="image/jpeg,image/png,image/webp"
              helperText="JPG/PNG/WEBP ≤ 5 MB"
            />
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FileUploadField
                label={lang === 'bn' ? 'পূর্বের ছবি (beforeImageUrl)' : 'Before Photo'}
                value={form.beforeImageUrl}
                onChange={(url) => setForm({ ...form, beforeImageUrl: url })}
                category="project"
                accept="image/jpeg,image/png,image/webp"
              />
              <FileUploadField
                label={lang === 'bn' ? 'পরবর্তী ছবি (afterImageUrl)' : 'After Photo'}
                value={form.afterImageUrl}
                onChange={(url) => setForm({ ...form, afterImageUrl: url })}
                category="project"
                accept="image/jpeg,image/png,image/webp"
              />
            </div>
            <FileUploadField
              label={lang === 'bn' ? 'প্রকল্প নথি PDF (pdfUrl)' : 'Project Report PDF (pdfUrl)'}
              value={form.pdfUrl}
              onChange={(url) => setForm({ ...form, pdfUrl: url })}
              category="document"
              accept="application/pdf"
              helperText="PDF ≤ 10 MB (Firebase Storage বা সরাসরি লিংক)"
            />
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
                : 'Save Project'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'প্রকল্প মুছে ফেলার নিশ্চিতকরণ' : 'Confirm Project Deletion'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${deleteTarget?.titleBn || deleteTarget?.titleEn}" প্রকল্পটি মুছে ফেলতে চান?`
            : `Are you sure you want to permanently delete "${deleteTarget?.titleEn || deleteTarget?.titleBn}"?`
        }
        loading={deleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
