import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  BarChart2,
  CheckCircle2,
  DatabaseZap,
  Download,
  Edit3,
  ExternalLink,
  Eye,
  EyeOff,
  PieChart,
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
  deleteGenericEntity,
  generateDocId,
  saveGenericEntity,
  seedCollectionIdempotent,
  subscribeCollection,
} from '../../services/firebase/crudService';
import { isUnsafeLocalUrl } from '../../services/firebase/storageService';
import type { EntityConfig } from './entityConfigs';
import { CURATED_SEED_RECORDS } from './seedData';
import { toLocalizedNumber } from '../../utils/formatters';
import { exportToCsv } from '../../utils/exportUtils';

const PAGE_SIZE = 10;

interface EntityManagerProps {
  config: EntityConfig;
}

function isValidHttpUrl(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed) return true;
  if (isUnsafeLocalUrl(trimmed)) return false;
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === 'https:' || parsed.protocol === 'http:';
  } catch {
    return false;
  }
}

export function EntityManager({ config }: EntityManagerProps) {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [items, setItems] = useState<Record<string, any>[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<Record<string, any>>({});
  const [saving, setSaving] = useState(false);

  const [seedOpen, setSeedOpen] = useState(false);
  const [seedJson, setSeedJson] = useState('');
  const [seeding, setSeeding] = useState(false);

  const [reorderingId, setReorderingId] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<Record<string, any> | null>(null);
  const [deleting, setDeleting] = useState(false);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  const hasOrderField = useMemo(
    () => config.fields.some((f) => f.key === 'order'),
    [config.fields]
  );

  const curatedSeeds = CURATED_SEED_RECORDS[config.collectionName] ?? [];

  useEffect(() => {
    setLoading(true);
    setLoadError(null);
    setFeedback(null);
    setSearch('');
    setCategoryFilter('ALL');
    setPage(1);

    const unsub = subscribeCollection(
      config.collectionName,
      (id, raw) => ({ id, ...raw }),
      (list) => {
        setItems(list);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setLoading(false);
      }
    );

    return () => unsub();
  }, [config.collectionName]);

  const categoryOptions = useMemo(() => {
    if (!config.categoryField) return [];
    const fieldDef = config.fields.find((f) => f.key === config.categoryField);
    const map = new Map<string, { value: string; labelBn: string; labelEn: string }>();

    if (fieldDef?.options) {
      for (const opt of fieldDef.options) {
        map.set(opt.value, opt);
      }
    }

    for (const item of items) {
      const val = item[config.categoryField];
      if (typeof val === 'string' && val.trim() && !map.has(val.trim())) {
        map.set(val.trim(), { value: val.trim(), labelBn: val.trim(), labelEn: val.trim() });
      }
    }

    return Array.from(map.values());
  }, [config.categoryField, config.fields, items]);

  const sortedItems = useMemo(() => {
    if (!hasOrderField) return items;
    return [...items].sort((a, b) => {
      const oa = typeof a.order === 'number' ? a.order : Number(a.order ?? 9999);
      const ob = typeof b.order === 'number' ? b.order : Number(b.order ?? 9999);
      return oa - ob;
    });
  }, [items, hasOrderField]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return sortedItems.filter((item) => {
      if (
        config.categoryField &&
        categoryFilter !== 'ALL' &&
        String(item[config.categoryField] ?? '') !== categoryFilter
      ) {
        return false;
      }

      if (!q) return true;
      return Object.values(item).some((val) => {
        if (typeof val === 'string') return val.toLowerCase().includes(q);
        if (typeof val === 'number') return String(val).includes(q);
        return false;
      });
    });
  }, [sortedItems, search, config.categoryField, categoryFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  const handleOpenCreate = () => {
    const nextOrder =
      sortedItems.length > 0
        ? Math.max(
            0,
            ...sortedItems.map((i) => (typeof i.order === 'number' ? i.order : 0))
          ) + 1
        : 1;

    const initial: Record<string, any> = {
      id: generateDocId(config.idPrefix),
    };
    for (const f of config.fields) {
      if (f.key === 'order') {
        initial[f.key] = nextOrder;
      } else {
        initial[f.key] = f.defaultValue !== undefined ? f.defaultValue : '';
      }
    }
    setForm(initial);
    setIsNew(true);
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleOpenEdit = (item: Record<string, any>) => {
    const initial: Record<string, any> = { ...item };
    for (const f of config.fields) {
      if (initial[f.key] === undefined) {
        initial[f.key] = f.defaultValue !== undefined ? f.defaultValue : '';
      }
    }
    setForm(initial);
    setIsNew(false);
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;

    if (!form.id || !String(form.id).trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'ডকুমেন্ট আইডি আবশ্যক।' : 'Document ID is required.',
      });
      return;
    }

    for (const f of config.fields) {
      const val = form[f.key];
      if (f.required && (val === undefined || val === '')) {
        setFeedback({
          type: 'error',
          text: lang === 'bn' ? `"${f.labelBn}" ফিল্ডটি আবশ্যক।` : `"${f.labelEn}" is required.`,
        });
        return;
      }
      if (f.isUrl && typeof val === 'string' && val.trim() && !isValidHttpUrl(val)) {
        setFeedback({
          type: 'error',
          text:
            lang === 'bn'
              ? `"${f.labelBn}"-এ একটি বৈধ HTTPS/HTTP লিংক দিন।`
              : `"${f.labelEn}" must be a valid HTTPS or HTTP URL.`,
        });
        return;
      }
    }

    setSaving(true);
    setFeedback(null);

    const summaryTitle =
      form.titleBn ||
      form.nameBn ||
      form.serviceNameBn ||
      form.jobTitleBn ||
      form.eventNameBn ||
      form.spotNameBn ||
      form.questionBn ||
      form.id;

    try {
      await saveGenericEntity(
        config.collectionName,
        String(form.id).trim(),
        form,
        isNew,
        admin,
        summaryTitle
      );
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'সফলভাবে সংরক্ষণ করা হয়েছে।' : 'Saved successfully.',
      });
      setEditorOpen(false);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save record.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleQuickTogglePublish = async (item: Record<string, any>) => {
    if (!admin || !canEdit || !config.publishField) return;
    const field = config.publishField;
    const nextVal = !Boolean(item[field]);
    setTogglingId(item.id);
    try {
      const summaryTitle =
        item.titleBn ||
        item.serviceNameBn ||
        item.jobTitleBn ||
        item.platformNameBn ||
        item.spotNameBn ||
        item.questionBn ||
        item.chipNameBn ||
        item.centerNameBn ||
        item.id;
      await saveGenericEntity(
        config.collectionName,
        item.id,
        { ...item, [field]: nextVal },
        false,
        admin,
        summaryTitle
      );
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update status.',
      });
    } finally {
      setTogglingId(null);
    }
  };

  const handleMoveOrder = async (item: Record<string, any>, direction: 'up' | 'down') => {
    if (!admin || !canEdit) return;
    const idx = sortedItems.findIndex((x) => x.id === item.id);
    if (idx === -1) return;
    const swapIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= sortedItems.length) return;

    const neighbor = sortedItems[swapIdx];
    const currentOrder = typeof item.order === 'number' ? item.order : idx + 1;
    const neighborOrder = typeof neighbor.order === 'number' ? neighbor.order : swapIdx + 1;
    const newItemOrder = neighborOrder === currentOrder ? (direction === 'up' ? currentOrder - 1 : currentOrder + 1) : neighborOrder;
    const newNeighborOrder = neighborOrder === currentOrder ? currentOrder : currentOrder;

    setReorderingId(item.id);
    try {
      await Promise.all([
        saveGenericEntity(
          config.collectionName,
          item.id,
          { ...item, order: newItemOrder },
          false,
          admin,
          item.titleBn || item.serviceNameBn || item.jobTitleBn || item.id
        ),
        saveGenericEntity(
          config.collectionName,
          neighbor.id,
          { ...neighbor, order: newNeighborOrder },
          false,
          admin,
          neighbor.titleBn || neighbor.serviceNameBn || neighbor.jobTitleBn || neighbor.id
        ),
      ]);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to reorder items.',
      });
    } finally {
      setReorderingId(null);
    }
  };

  const handleCuratedSeed = async () => {
    if (!admin || !canEdit || curatedSeeds.length === 0) return;
    setSeeding(true);
    setFeedback(null);
    try {
      const result = await seedCollectionIdempotent({
        collectionName: config.collectionName,
        records: curatedSeeds.map((s) => ({ id: s.id, ...s.data })),
        actor: admin,
      });
      setSeedOpen(false);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `সিড সম্পন্ন: ${toLocalizedNumber(result.inserted, lang)}টি নতুন যুক্ত, ${toLocalizedNumber(result.skipped, lang)}টি পূর্ব-বিদ্যমান স্কিপ করা হয়েছে।`
            : `Seed completed: ${result.inserted} inserted, ${result.skipped} existing skipped.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to seed collection.',
      });
    } finally {
      setSeeding(false);
    }
  };

  const handleJsonImport = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(seedJson);
    } catch {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'সঠিক JSON অ্যারে প্রদান করুন।' : 'Please enter a valid JSON array.',
      });
      return;
    }

    if (!Array.isArray(parsed) || parsed.length === 0) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'JSON অবশ্যই কমপক্ষে ১টি অবজেক্টসহ একটি অ্যারে হতে হবে।'
            : 'JSON must be a non-empty array of record objects.',
      });
      return;
    }

    const validRecords = parsed.filter(
      (r): r is Record<string, unknown> => Boolean(r) && typeof r === 'object' && !Array.isArray(r)
    );

    setSeeding(true);
    setFeedback(null);
    try {
      const result = await seedCollectionIdempotent({
        collectionName: config.collectionName,
        records: validRecords,
        actor: admin,
      });
      setSeedOpen(false);
      setSeedJson('');
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `JSON ইমপোর্ট সম্পন্ন: ${toLocalizedNumber(result.inserted, lang)}টি নতুন যুক্ত, ${toLocalizedNumber(result.skipped, lang)}টি স্কিপ করা হয়েছে।`
            : `JSON import completed: ${result.inserted} inserted, ${result.skipped} existing skipped.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to import JSON.',
      });
    } finally {
      setSeeding(false);
    }
  };

  const handleDelete = async () => {
    if (!admin || !canDelete || !deleteTarget) return;
    setDeleting(true);
    const summaryTitle =
      deleteTarget.titleBn ||
      deleteTarget.nameBn ||
      deleteTarget.serviceNameBn ||
      deleteTarget.jobTitleBn ||
      deleteTarget.eventNameBn ||
      deleteTarget.spotNameBn ||
      deleteTarget.id;
    try {
      await deleteGenericEntity(
        config.collectionName,
        deleteTarget.id,
        summaryTitle,
        admin
      );
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'তথ্য মুছে ফেলা হয়েছে।' : 'Record deleted successfully.',
      });
      setDeleteTarget(null);
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete record.',
      });
    } finally {
      setDeleting(false);
    }
  };

  const handleExportCsv = () => {
    const headers = ['ID', ...config.columns.map((c) => (lang === 'bn' ? c.headerBn : c.headerEn))];
    const rows = filtered.map((item) => [
      item.id,
      ...config.columns.map((c) => {
        const val = item[c.key];
        if (c.type === 'boolean') return val ? 'Yes' : 'No';
        return val !== undefined && val !== null ? String(val) : '';
      }),
    ]);
    exportToCsv({
      filename: `${config.collectionName}_${new Date().toISOString().slice(0, 10)}.csv`,
      headers,
      rows,
    });
  };

  const renderFieldValue = (col: (typeof config.columns)[0], item: Record<string, any>) => {
    const val = item[col.key];

    if (col.type === 'boolean') {
      const truthy = Boolean(val);
      return (
        <span
          className={`inline-flex items-center gap-1.5 text-xs font-medium whitespace-nowrap ${
            truthy ? 'text-emerald-700' : 'text-slate-500'
          }`}
        >
          <span
            className={`w-1.5 h-1.5 rounded-full ${
              truthy ? 'bg-emerald-600' : 'bg-slate-400'
            }`}
            aria-hidden="true"
          />
          {truthy ? (lang === 'bn' ? 'সক্রিয় / প্রকাশিত' : 'Published') : (lang === 'bn' ? 'খসড়া / বন্ধ' : 'Unpublished')}
        </span>
      );
    }

    if (col.type === 'badge') {
      return (
        <span className="text-xs font-semibold text-slate-800 whitespace-nowrap">
          {val ? String(val) : '—'}
        </span>
      );
    }

    if (col.type === 'link' && val) {
      return (
        <a
          href={String(val)}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 text-xs text-blue-700 hover:underline max-w-[200px] truncate"
        >
          <span className="truncate">{String(val)}</span>
          <ExternalLink className="w-3 h-3 shrink-0" />
        </a>
      );
    }

    if (col.type === 'image' && val) {
      return (
        <img
          src={String(val)}
          alt="thumb"
          className="w-10 h-10 object-cover rounded border border-slate-200 bg-white"
        />
      );
    }

    if (col.key === 'order') {
      return (
        <span className="font-mono text-xs text-slate-600 tabular-nums">
          {val !== undefined && val !== null ? toLocalizedNumber(Number(val), lang) : '—'}
        </span>
      );
    }

    return <span className="line-clamp-1">{val !== undefined && val !== null && val !== '' ? String(val) : '—'}</span>;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? config.titleBn : config.titleEn}
          </h1>
          <p className="text-sm text-slate-600">
            {lang === 'bn' ? config.descriptionBn : config.descriptionEn}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={handleExportCsv}
            disabled={filtered.length === 0}
            className="inline-flex items-center justify-center gap-2 px-3.5 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer disabled:opacity-50"
          >
            <Download className="w-4 h-4 text-emerald-700 shrink-0" />
            <span>{lang === 'bn' ? 'এক্সপোর্ট (CSV)' : 'Export CSV'}</span>
          </button>

          {canEdit ? (
            <>
              <button
                type="button"
                onClick={() => {
                  setFeedback(null);
                  setSeedOpen(true);
                }}
                className="inline-flex items-center justify-center gap-2 px-3.5 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <DatabaseZap className="w-4 h-4 text-emerald-700 shrink-0" />
                <span>{lang === 'bn' ? 'সিড / JSON ইমপোর্ট' : 'Seed / JSON Import'}</span>
              </button>

              <button
                type="button"
                onClick={handleOpenCreate}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg shadow-sm transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <Plus className="w-4 h-4 shrink-0" />
                <span>{lang === 'bn' ? 'নতুন যোগ করুন' : 'Add New'}</span>
              </button>
            </>
          ) : null}
        </div>
      </div>

      {feedback ? (
        <div
          role="alert"
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

      {/* Citizen Polls Real-time Analytics Visualizer */}
      {config.collectionName === 'citizen_polls' && items.length > 0 ? (
        <div className="bg-gradient-to-br from-slate-900 to-[#004d38] text-white p-5 rounded-xl border border-emerald-500/20 shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-white/10">
            <div className="flex items-center gap-2">
              <BarChart2 className="w-5 h-5 text-emerald-400" />
              <h3 className="font-bold text-sm">
                {lang === 'bn' ? 'নাগরিক মতামত জরিপ বিশ্লেষণ (Live Poll Analytics)' : 'Live Poll Analytics & Vote Trends'}
              </h3>
            </div>
            <span className="text-xs text-emerald-300 font-mono">
              {toLocalizedNumber(items.length, lang)} {lang === 'bn' ? 'টি জরিপ' : 'polls'}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {items.map((poll) => {
              const opts = typeof poll.options === 'string'
                ? poll.options.split(',').map((s: string) => s.trim()).filter(Boolean)
                : Array.isArray(poll.options) ? poll.options : ['হ্যাঁ', 'না'];
              const voteCounts: Record<string, number> = poll.voteCounts || {};
              const totalVotes = Object.values(voteCounts).reduce((a, b) => a + Number(b || 0), 0);

              return (
                <div key={poll.id} className="bg-white/10 rounded-lg p-3.5 space-y-2.5 backdrop-blur-sm border border-white/10">
                  <div className="flex items-start justify-between gap-2">
                    <p className="font-semibold text-xs leading-snug">{poll.questionBn || poll.questionEn}</p>
                    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full shrink-0 ${poll.active ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/30' : 'bg-slate-700 text-slate-300'}`}>
                      {poll.active ? (lang === 'bn' ? 'চলমান' : 'Live') : (lang === 'bn' ? 'সমাপ্ত' : 'Closed')}
                    </span>
                  </div>

                  <div className="space-y-1.5">
                    {opts.map((opt: string, idx: number) => {
                      const count = Number(voteCounts[opt] ?? 0);
                      const pct = totalVotes > 0 ? Math.round((count / totalVotes) * 100) : 0;
                      const colors = ['bg-emerald-500', 'bg-blue-500', 'bg-amber-500', 'bg-purple-500'];
                      const barColor = colors[idx % colors.length];

                      return (
                        <div key={opt} className="space-y-0.5 text-[11px]">
                          <div className="flex justify-between text-slate-200">
                            <span>{opt}</span>
                            <span className="font-mono">{toLocalizedNumber(pct, lang)}% ({toLocalizedNumber(count, lang)})</span>
                          </div>
                          <div className="w-full bg-black/30 rounded-full h-1.5 overflow-hidden">
                            <div className={`${barColor} h-1.5 rounded-full transition-all duration-500`} style={{ width: `${pct}%` }} />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : null}

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex flex-col md:flex-row gap-3 justify-between items-stretch md:items-center bg-slate-50/50">
          <div className="flex flex-col sm:flex-row gap-3 flex-1">
            <div className="relative w-full sm:w-80">
              <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder={lang === 'bn' ? 'অনুসন্ধান করুন...' : 'Search...'}
                className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700"
              />
            </div>

            {categoryOptions.length > 0 ? (
              <select
                value={categoryFilter}
                onChange={(e) => {
                  setCategoryFilter(e.target.value);
                  setPage(1);
                }}
                className="px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-800"
              >
                <option value="ALL">
                  {lang === 'bn' ? 'সকল বিভাগ / ক্যাটাগরি' : 'All Categories'}
                </option>
                {categoryOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {lang === 'bn' ? opt.labelBn : opt.labelEn}
                  </option>
                ))}
              </select>
            ) : null}
          </div>

          <div className="text-xs font-mono text-slate-500 whitespace-nowrap tabular-nums">
            {config.collectionName} · {toLocalizedNumber(filtered.length, lang)}{' '}
            {lang === 'bn' ? 'টি রেকর্ড' : 'records'}
          </div>
        </div>

        {loadError ? (
          <div className="p-6 bg-red-50 text-sm text-red-700">{loadError}</div>
        ) : loading ? (
          <div className="py-12 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading...'}
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-12 text-center text-sm text-slate-500 space-y-2">
            <p>{lang === 'bn' ? 'বর্তমানে কোনো তথ্য নেই।' : 'No records found.'}</p>
            <p className="text-xs text-slate-400">
              {lang === 'bn'
                ? 'নতুন তথ্য যোগ করতে "নতুন যোগ করুন" অথবা "সিড / JSON ইমপোর্ট" ব্যবহার করুন।'
                : 'Use "Add New" or "Seed / JSON Import" above to populate live Android data.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  <th className="p-3">ID</th>
                  {config.columns.map((c) => (
                    <th key={c.key} className="p-3 whitespace-nowrap">
                      {lang === 'bn' ? c.headerBn : c.headerEn}
                    </th>
                  ))}
                  <th className="p-3 text-right whitespace-nowrap">
                    {lang === 'bn' ? 'কার্যক্রম' : 'Actions'}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((item) => {
                  const globalIdx = sortedItems.findIndex((x) => x.id === item.id);
                  const isFirst = globalIdx <= 0;
                  const isLast = globalIdx === -1 || globalIdx >= sortedItems.length - 1;
                  const isPublished = config.publishField
                    ? Boolean(item[config.publishField])
                    : false;

                  return (
                    <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="p-3 whitespace-nowrap font-mono text-xs text-slate-500">
                        {item.id}
                      </td>
                      {config.columns.map((c) => (
                        <td key={c.key} className="p-3">
                          {renderFieldValue(c, item)}
                        </td>
                      ))}
                      <td className="p-3 whitespace-nowrap text-right">
                        <div className="inline-flex items-center justify-end gap-1">
                          {canEdit && hasOrderField && !search && categoryFilter === 'ALL' ? (
                            <>
                              <button
                                type="button"
                                disabled={isFirst || reorderingId === item.id}
                                onClick={() => handleMoveOrder(item, 'up')}
                                className="p-1.5 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg disabled:opacity-30 cursor-pointer"
                                title={lang === 'bn' ? 'উপরে নিন' : 'Move Up'}
                              >
                                <ArrowUp className="w-4 h-4" />
                              </button>
                              <button
                                type="button"
                                disabled={isLast || reorderingId === item.id}
                                onClick={() => handleMoveOrder(item, 'down')}
                                className="p-1.5 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg disabled:opacity-30 cursor-pointer"
                                title={lang === 'bn' ? 'নিচে নিন' : 'Move Down'}
                              >
                                <ArrowDown className="w-4 h-4" />
                              </button>
                            </>
                          ) : null}

                          {canEdit && config.publishField ? (
                            <button
                              type="button"
                              disabled={togglingId === item.id}
                              onClick={() => handleQuickTogglePublish(item)}
                              className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                                isPublished
                                  ? 'text-emerald-700 hover:bg-emerald-50'
                                  : 'text-slate-500 hover:bg-slate-100'
                              }`}
                              title={
                                isPublished
                                  ? lang === 'bn'
                                    ? 'অপ্রকাশিত করুন'
                                    : 'Unpublish'
                                  : lang === 'bn'
                                  ? 'প্রকাশ করুন'
                                  : 'Publish'
                              }
                            >
                              {isPublished ? (
                                <Eye className="w-4 h-4" />
                              ) : (
                                <EyeOff className="w-4 h-4" />
                              )}
                            </button>
                          ) : null}

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
                        </div>
                      </td>
                    </tr>
                  );
                })}
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

      {/* Create / Edit Modal */}
      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        title={
          isNew
            ? lang === 'bn'
              ? `নতুন ${config.titleBn} যোগ করুন`
              : `Add New ${config.titleEn}`
            : lang === 'bn'
            ? `${config.titleBn} সম্পাদনা`
            : `Edit ${config.titleEn}`
        }
        maxWidthClass="max-w-3xl"
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold text-slate-700">Document ID</label>
            <input
              type="text"
              value={form.id || ''}
              onChange={(e) => setForm({ ...form, id: e.target.value })}
              disabled={!isNew}
              className="w-full px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-lg disabled:bg-slate-100"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {config.fields.map((f) => {
              if (f.type === 'textarea') {
                return (
                  <div key={f.key} className="sm:col-span-2 space-y-1.5">
                    <label className="block text-xs font-semibold text-slate-700">
                      {lang === 'bn' ? f.labelBn : f.labelEn} {f.required ? '*' : ''}
                    </label>
                    <textarea
                      value={form[f.key] || ''}
                      onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                      rows={3}
                      className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
                      required={f.required}
                    />
                  </div>
                );
              }

              if (f.type === 'image' || f.type === 'video' || f.type === 'pdf') {
                const acceptTypes =
                  f.type === 'image'
                    ? 'image/jpeg,image/png,image/webp'
                    : f.type === 'video'
                    ? 'video/mp4,video/webm'
                    : 'application/pdf';
                return (
                  <div key={f.key} className="sm:col-span-2 space-y-1.5">
                    <FileUploadField
                      label={lang === 'bn' ? f.labelBn : f.labelEn}
                      value={form[f.key] || ''}
                      onChange={(url) => setForm({ ...form, [f.key]: url })}
                      category={f.category || 'gallery'}
                      accept={acceptTypes}
                      helperText={f.helperText || (f.type === 'pdf' ? 'PDF ≤ 10 MB' : '≤ 5 MB')}
                    />
                  </div>
                );
              }

              if (f.type === 'select') {
                return (
                  <div key={f.key} className="space-y-1.5">
                    <label className="block text-xs font-semibold text-slate-700">
                      {lang === 'bn' ? f.labelBn : f.labelEn} {f.required ? '*' : ''}
                    </label>
                    <select
                      value={form[f.key] !== undefined ? form[f.key] : ''}
                      onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                      className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-800"
                    >
                      {f.options?.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {lang === 'bn' ? opt.labelBn : opt.labelEn}
                        </option>
                      ))}
                    </select>
                  </div>
                );
              }

              if (f.type === 'checkbox') {
                return (
                  <div key={f.key} className="flex items-center gap-2 pt-6">
                    <input
                      type="checkbox"
                      id={`field_${f.key}`}
                      checked={Boolean(form[f.key])}
                      onChange={(e) => setForm({ ...form, [f.key]: e.target.checked })}
                      className="w-4 h-4 text-blue-700 border-slate-300 rounded focus:ring-blue-700"
                    />
                    <label htmlFor={`field_${f.key}`} className="text-xs font-medium text-slate-700 cursor-pointer">
                      {lang === 'bn' ? f.labelBn : f.labelEn}
                    </label>
                  </div>
                );
              }

              return (
                <div key={f.key} className="space-y-1.5">
                  <label className="block text-xs font-semibold text-slate-700">
                    {lang === 'bn' ? f.labelBn : f.labelEn} {f.required ? '*' : ''}
                  </label>
                  <input
                    type={f.type === 'number' ? 'number' : f.isUrl ? 'url' : 'text'}
                    value={form[f.key] !== undefined ? form[f.key] : ''}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        [f.key]: f.type === 'number' ? Number(e.target.value) : e.target.value,
                      })
                    }
                    placeholder={lang === 'bn' ? f.placeholderBn : f.placeholderEn}
                    className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg"
                    required={f.required}
                  />
                </div>
              );
            })}
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
              {saving ? (lang === 'bn' ? 'সংরক্ষণ হচ্ছে...' : 'Saving...') : (lang === 'bn' ? 'সংরক্ষণ করুন' : 'Save')}
            </button>
          </div>
        </form>
      </Modal>

      {/* Safe Idempotent Seed / JSON Import Modal */}
      <Modal
        open={seedOpen}
        onClose={() => setSeedOpen(false)}
        title={lang === 'bn' ? 'নিরাপদ ডেটা সিড ও JSON ইমপোর্ট' : 'Idempotent Data Seed & JSON Import'}
        subtitle={
          lang === 'bn'
            ? `সংগ্রহ: ${config.collectionName} — বিদ্যমান ডকুমেন্ট আইডি অক্ষত থাকবে (কোনো ডুপ্লিকেট তৈরি হবে না)`
            : `Collection: ${config.collectionName} — existing document IDs are checked and preserved without duplication`
        }
        maxWidthClass="max-w-2xl"
      >
        <div className="space-y-5">
          {curatedSeeds.length > 0 ? (
            <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50/70 space-y-3">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h4 className="text-sm font-semibold text-slate-900">
                    {lang === 'bn'
                      ? 'প্রাথমিক অফিশিয়াল ও সরকারি পোর্টাল তালিকা সিড করুন'
                      : 'Seed Curated Official Starter Records'}
                  </h4>
                  <p className="text-xs text-slate-600 mt-1">
                    {lang === 'bn'
                      ? `যাচাইকৃত ${toLocalizedNumber(curatedSeeds.length, lang)}টি সরকারি/স্থানীয় তথ্য একবারে যুক্ত করবে। আগে থেকে থাকা ডকুমেন্ট আইডি স্কিপ করা হবে।`
                      : `Inserts ${curatedSeeds.length} verified official entries safely, skipping any ID that already exists in Firestore.`}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={seeding}
                  onClick={handleCuratedSeed}
                  className="px-4 py-2 text-xs font-semibold text-white bg-emerald-700 hover:bg-emerald-800 rounded-lg whitespace-nowrap shrink-0 disabled:opacity-60 cursor-pointer"
                >
                  {seeding
                    ? lang === 'bn'
                      ? 'সিড হচ্ছে...'
                      : 'Seeding...'
                    : lang === 'bn'
                    ? 'এক-ক্লিক সিড চালান'
                    : 'Run Safe Seed'}
                </button>
              </div>
            </div>
          ) : null}

          <form onSubmit={handleJsonImport} className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn'
                  ? 'কাস্টম JSON অ্যারে পেস্ট করুন (ঐচ্ছিক, প্রতিটি আইটেমে id থাকা উত্তম)'
                  : 'Paste Custom JSON Array (Optional — include unique id keys)'}
              </label>
              <textarea
                rows={6}
                value={seedJson}
                onChange={(e) => setSeedJson(e.target.value)}
                placeholder={'[\n  {\n    "id": "custom_1",\n    "titleBn": "নমুনা শিরোনাম",\n    "published": true\n  }\n]'}
                className="w-full px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg bg-slate-50"
              />
            </div>

            <div className="flex justify-end gap-3 pt-2 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setSeedOpen(false)}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer"
              >
                {lang === 'bn' ? 'বন্ধ করুন' : 'Close'}
              </button>
              <button
                type="submit"
                disabled={seeding || !seedJson.trim()}
                className="px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg disabled:opacity-50 cursor-pointer"
              >
                {seeding
                  ? lang === 'bn'
                    ? 'ইমপোর্ট হচ্ছে...'
                    : 'Importing...'
                  : lang === 'bn'
                  ? 'JSON ইমপোর্ট করুন'
                  : 'Import JSON'}
              </button>
            </div>
          </form>
        </div>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'তথ্য মুছে ফেলুন' : 'Delete Record'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিত যে এই রেকর্ডটি (${deleteTarget?.id}) মুছে ফেলতে চান?`
            : `Are you sure you want to delete this record (${deleteTarget?.id})?`
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
