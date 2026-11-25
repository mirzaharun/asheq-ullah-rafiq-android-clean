import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Download,
  ExternalLink,
  Eye,
  FileText,
  Image as ImageIcon,
  Search,
  ShieldAlert,
  X,
} from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  mapCitizenRequestDoc,
  subscribeCollection,
  updateCitizenRequestStatus,
} from '../../services/firebase/crudService';
import { isUnsafeLocalUrl } from '../../services/firebase/storageService';
import { exportToCsv } from '../../utils/exportUtils';
import {
  APPLICATION_STATUSES,
  type ApplicationStatus,
  type CitizenRequestCollection,
  type CitizenRequestRecord,
} from '../../types/firestore';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 10;

const COLLECTION_TABS: {
  id: CitizenRequestCollection;
  labelBn: string;
  labelEn: string;
}[] = [
  { id: 'applications', labelBn: 'নাগরিক আবেদন', labelEn: 'Applications' },
  { id: 'citizen_grievances', labelBn: 'অভিযোগ (Grievances)', labelEn: 'Grievances' },
  { id: 'appointments', labelBn: 'সাক্ষাৎকার (Appointments)', labelEn: 'Appointments' },
  { id: 'feedback', labelBn: 'মতামত (Feedback)', labelEn: 'Feedback' },
  { id: 'general_feedback', labelBn: 'সাধারণ মতামত (General Feedback)', labelEn: 'General Feedback' },
];

const STATUS_LABELS_BN: Record<ApplicationStatus, string> = {
  Submitted: 'জমা দেওয়া হয়েছে',
  'Under Review': 'পর্যালোচনাধীন',
  'Additional Information Required': 'অতিরিক্ত তথ্য প্রয়োজন',
  'Forwarded to Relevant Office': 'সংশ্লিষ্ট দপ্তরে প্রেরিত',
  'In Progress': 'চলমান',
  Resolved: 'নিষ্পত্তিকৃত',
  Rejected: 'প্রত্যাখ্যাত (ব্যাখ্যাসহ)',
};

export function CitizenRequestsManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [activeTab, setActiveTab] = useState<CitizenRequestCollection>('applications');
  const [records, setRecords] = useState<CitizenRequestRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [searchTracking, setSearchTracking] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [unionFilter, setUnionFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [dateFilter, setDateFilter] = useState('');
  const [page, setPage] = useState(1);

  const [selected, setSelected] = useState<CitizenRequestRecord | null>(null);
  const [nextStatus, setNextStatus] = useState<ApplicationStatus>('Under Review');
  const [officialResponse, setOfficialResponse] = useState('');
  const [updating, setUpdating] = useState(false);
  const [previewAttachment, setPreviewAttachment] = useState<{ url: string; title: string; isImage: boolean } | null>(null);

  const handleExportCsv = () => {
    const headers = [
      'ID',
      'Tracking Number',
      'Collection',
      'Applicant Name',
      'Subject',
      'Category',
      'Union',
      'Status',
      'Submitted Date',
      'Official Response',
    ];
    const rows = filtered.map((r) => [
      r.id,
      r.trackingNumber || '',
      r.sourceCollection,
      r.applicantName || '',
      r.subject || r.title || '',
      r.category || '',
      r.union || '',
      r.status || '',
      formatFirestoreTimestamp(r.createdAt, 'en'),
      r.officialResponse || '',
    ]);
    exportToCsv({
      filename: `${activeTab}_export_${new Date().toISOString().slice(0, 10)}.csv`,
      headers,
      rows,
    });
  };

  useEffect(() => {
    setLoading(true);
    setLoadError(null);
    const unsub = subscribeCollection(
      activeTab,
      (id, raw) => mapCitizenRequestDoc(activeTab, id, raw),
      (items) => {
        setRecords(items);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setRecords([]);
        setLoading(false);
      }
    );
    return () => unsub();
  }, [activeTab]);

  const uniqueUnions = useMemo(
    () => Array.from(new Set(records.map((r) => r.union).filter((v): v is string => Boolean(v)))),
    [records]
  );

  const uniqueCategories = useMemo(
    () =>
      Array.from(
        new Set(records.map((r) => r.category).filter((v): v is string => Boolean(v)))
      ),
    [records]
  );

  const filtered = useMemo(() => {
    const q = searchTracking.trim().toLowerCase();
    return records.filter((r) => {
      const matchesQuery =
        !q ||
        (r.trackingNumber ?? '').toLowerCase().includes(q) ||
        r.id.toLowerCase().includes(q) ||
        (r.applicantName ?? '').toLowerCase().includes(q) ||
        (r.subject ?? '').toLowerCase().includes(q);
      const matchesStatus = statusFilter === 'ALL' || r.status === statusFilter;
      const matchesUnion = unionFilter === 'ALL' || r.union === unionFilter;
      const matchesCategory = categoryFilter === 'ALL' || r.category === categoryFilter;
      const matchesDate =
        !dateFilter ||
        formatFirestoreTimestamp(r.createdAt, 'en').toLowerCase().includes(dateFilter.toLowerCase());
      return (
        matchesQuery &&
        matchesStatus &&
        matchesUnion &&
        matchesCategory &&
        matchesDate
      );
    });
  }, [records, searchTracking, statusFilter, unionFilter, categoryFilter, dateFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [activeTab, searchTracking, statusFilter, unionFilter, categoryFilter, dateFilter]);

  const canReview =
    admin?.role === 'super_admin' || admin?.role === 'application_manager';

  const openDetail = (record: CitizenRequestRecord) => {
    setSelected(record);
    const currentStatus = APPLICATION_STATUSES.includes(
      record.status as ApplicationStatus
    )
      ? (record.status as ApplicationStatus)
      : 'Submitted';
    setNextStatus(currentStatus);
    setOfficialResponse(record.officialResponse ?? '');
    setFeedback(null);
  };

  const isTerminalLocked =
    selected &&
    (selected.status === 'Resolved' || selected.status === 'Rejected') &&
    admin?.role !== 'super_admin';

  const handleUpdateStatus = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !selected || !canReview) return;

    if (nextStatus === 'Rejected' && !officialResponse.trim()) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'আবেদন প্রত্যাখ্যান করার জন্য দাপ্তরিক ব্যাখ্যা প্রদান আবশ্যক।'
            : 'An official explanation is required when rejecting a request.',
      });
      return;
    }

    setUpdating(true);
    setFeedback(null);
    try {
      await updateCitizenRequestStatus({
        sourceCollection: selected.sourceCollection,
        docId: selected.id,
        nextStatus,
        officialResponse,
        existingHistory: selected.statusHistory ?? [],
        actor: admin,
      });
      setSelected(null);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'আবেদনের অবস্থা ও দাপ্তরিক উত্তর সংরক্ষিত হয়েছে।'
            : 'Request status and official response updated.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update status.',
      });
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn'
              ? 'নাগরিক আবেদন, অভিযোগ ও সাক্ষাৎকার'
              : 'Citizen Applications, Grievances & Appointments'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {activeTab} · {toLocalizedNumber(records.length, lang)}{' '}
            {lang === 'bn' ? 'টি রেকর্ড' : 'records'}
          </p>
        </div>

        <button
          type="button"
          onClick={handleExportCsv}
          disabled={filtered.length === 0}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shadow-sm transition-colors whitespace-nowrap disabled:opacity-50 cursor-pointer self-start sm:self-auto"
        >
          <Download className="w-4 h-4 text-emerald-700 shrink-0" />
          <span>{lang === 'bn' ? 'এক্সপোর্ট (CSV)' : 'Export CSV'}</span>
        </button>
      </div>

      {/* Source Collection Tabs */}
      <div className="flex flex-wrap items-center gap-1 p-1 bg-slate-100 rounded-xl w-fit">
        {COLLECTION_TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id)}
            className={`px-3.5 py-2 text-xs font-medium rounded-lg transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === tab.id
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            {lang === 'bn' ? tab.labelBn : tab.labelEn}
          </button>
        ))}
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

      {/* Search & Filter Toolbar */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <div className="relative lg:col-span-2">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="search"
            value={searchTracking}
            onChange={(e) => setSearchTracking(e.target.value)}
            placeholder={
              lang === 'bn'
                ? 'ট্র্যাকিং নম্বর বা আবেদনকারীর নাম...'
                : 'Search tracking number or applicant...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
        >
          <option value="ALL">{lang === 'bn' ? 'সকল অবস্থা' : 'All Statuses'}</option>
          {APPLICATION_STATUSES.map((st) => (
            <option key={st} value={st}>
              {lang === 'bn' ? STATUS_LABELS_BN[st] : st}
            </option>
          ))}
        </select>

        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
        >
          <option value="ALL">{lang === 'bn' ? 'সকল বিভাগ' : 'All Categories'}</option>
          {uniqueCategories.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>

        <select
          value={unionFilter}
          onChange={(e) => setUnionFilter(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
        >
          <option value="ALL">{lang === 'bn' ? 'সকল ইউনিয়ন' : 'All Unions'}</option>
          {uniqueUnions.map((u) => (
            <option key={u} value={u}>
              {u}
            </option>
          ))}
        </select>

        <div className="sm:col-span-2 lg:col-span-5 flex items-center justify-end gap-2">
          <label className="text-xs text-slate-500">
            {lang === 'bn' ? 'তারিখ ফিল্টার:' : 'Date filter:'}
          </label>
          <input
            type="text"
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
            placeholder="Nov 2026"
            className="px-2.5 py-1 text-xs border border-slate-300 rounded-lg w-36"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading citizen requests...'}
          </div>
        ) : paginated.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn'
              ? 'এই ফিল্টারের আওতায় কোনো আবেদন পাওয়া যায়নি।'
              : 'No matching requests found.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-3 px-4">{lang === 'bn' ? 'ট্র্যাকিং নম্বর' : 'Tracking No.'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'আবেদনকারী ও বিষয়' : 'Applicant & Subject'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'বিভাগ · ইউনিয়ন' : 'Category · Union'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'অবস্থা' : 'Status'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'তারিখ' : 'Submitted'}</th>
                  <th className="py-3 px-4 text-right">{lang === 'bn' ? 'বিস্তারিত' : 'Details'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((rec) => (
                  <tr key={rec.id} className="hover:bg-slate-50/80">
                    <td className="py-3 px-4 font-mono text-xs text-slate-700 tabular-nums">
                      {rec.trackingNumber || rec.id}
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900">
                        {rec.subject || rec.title || rec.id}
                      </div>
                      <div className="text-xs text-slate-500">
                        {rec.applicantName || '—'}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">
                      {[rec.category, rec.union].filter(Boolean).join(' · ') || '—'}
                    </td>
                    <td className="py-3 px-4 text-xs font-medium text-slate-800">
                      {lang === 'bn' &&
                      rec.status &&
                      rec.status in STATUS_LABELS_BN
                        ? STATUS_LABELS_BN[rec.status as ApplicationStatus]
                        : rec.status || 'Submitted'}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500 tabular-nums">
                      {formatFirestoreTimestamp(rec.createdAt, lang)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => openDetail(rec)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>{lang === 'bn' ? 'পর্যালোচনা' : 'Review'}</span>
                      </button>
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

      {/* Application Detail & Status Transition Modal */}
      <Modal
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        maxWidthClass="max-w-3xl"
        title={selected?.subject || selected?.trackingNumber || 'Request Details'}
        subtitle={`${selected?.sourceCollection} · ID: ${selected?.id}`}
      >
        {selected ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm bg-slate-50 border border-slate-200 rounded-xl p-4">
              <div>
                <p className="text-xs text-slate-500">
                  {lang === 'bn' ? 'ট্র্যাকিং নম্বর' : 'Tracking Number'}
                </p>
                <p className="font-mono font-semibold text-slate-900">
                  {selected.trackingNumber || selected.id}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">
                  {lang === 'bn' ? 'আবেদনকারীর নাম' : 'Applicant Name'}
                </p>
                <p className="font-medium text-slate-900">
                  {selected.applicantName || '—'}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">
                  {lang === 'bn' ? 'বিভাগ ও ইউনিয়ন' : 'Category & Union'}
                </p>
                <p className="text-slate-800">
                  {[selected.category, selected.union].filter(Boolean).join(' · ') || '—'}
                </p>
              </div>
              <div>
                <p className="text-xs text-slate-500">
                  {lang === 'bn' ? 'বর্তমান অবস্থা' : 'Current Status'}
                </p>
                <p className="font-semibold text-blue-700">
                  {selected.status || 'Submitted'}
                </p>
              </div>
            </div>

            {selected.description ? (
              <div>
                <h4 className="text-xs font-semibold text-slate-700 mb-1.5">
                  {lang === 'bn' ? 'আবেদনের বিবরণ' : 'Description'}
                </h4>
                <p className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed bg-white border border-slate-200 rounded-lg p-3.5">
                  {selected.description}
                </p>
              </div>
            ) : null}

            {/* Authorized Attachments */}
            {selected.attachmentUrls && selected.attachmentUrls.length > 0 ? (
              <div>
                <h4 className="text-xs font-semibold text-slate-700 mb-2">
                  {lang === 'bn' ? 'সংযুক্ত নথিপত্র ও প্রমাণাদি' : 'Authorized Attachments & Evidences'}
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {selected.attachmentUrls.map((url, idx) => {
                    const unsafe = isUnsafeLocalUrl(url);
                    const isImg = /\.(jpeg|jpg|png|webp|gif)($|\?)/i.test(url);
                    const isPdf = /\.pdf($|\?)/i.test(url);

                    return (
                      <div
                        key={`${url}-${idx}`}
                        className="flex items-center gap-3 p-2.5 bg-slate-50 border border-slate-200 rounded-xl"
                      >
                        {isImg ? (
                          <div
                            onClick={() => !unsafe && setPreviewAttachment({ url, title: `সংযুক্তি #${idx + 1}`, isImage: true })}
                            className="w-12 h-12 rounded-lg bg-slate-200 overflow-hidden shrink-0 cursor-pointer border border-slate-300 relative group"
                          >
                            <img src={url} alt={`attach-${idx}`} className="w-full h-full object-cover" />
                            <div className="absolute inset-0 bg-slate-900/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white">
                              <Eye className="w-4 h-4" />
                            </div>
                          </div>
                        ) : (
                          <div className="w-12 h-12 rounded-lg bg-red-50 border border-red-200 flex items-center justify-center text-red-600 shrink-0">
                            <FileText className="w-6 h-6" />
                          </div>
                        )}

                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-semibold text-slate-800 truncate">
                            {isImg
                              ? lang === 'bn'
                                ? `ছবি সংযুক্তি #${idx + 1}`
                                : `Image Attachment #${idx + 1}`
                              : isPdf
                              ? lang === 'bn'
                                ? `পিডিএফ নথি #${idx + 1}`
                                : `PDF Document #${idx + 1}`
                              : lang === 'bn'
                              ? `নথি #${idx + 1}`
                              : `Document #${idx + 1}`}
                          </p>
                          <p className="font-mono text-[11px] text-slate-500 truncate">{url}</p>
                          <div className="flex items-center gap-2 mt-1">
                            {unsafe ? (
                              <span className="text-red-700 font-medium text-[11px]">Unsafe URL</span>
                            ) : (
                              <>
                                <button
                                  type="button"
                                  onClick={() => setPreviewAttachment({
                                    url,
                                    title: isImg ? `ছবি #${idx + 1}` : `নথি #${idx + 1}`,
                                    isImage: isImg,
                                  })}
                                  className="text-[11px] font-semibold text-blue-700 hover:underline cursor-pointer"
                                >
                                  {lang === 'bn' ? 'প্রিভিউ দেখুন' : 'Preview'}
                                </button>
                                <span className="text-slate-300">·</span>
                                <a
                                  href={url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="inline-flex items-center gap-0.5 text-[11px] text-slate-600 hover:text-slate-900"
                                >
                                  <span>{lang === 'bn' ? 'সরাসরি খুলুন' : 'Open'}</span>
                                  <ExternalLink className="w-3 h-3" />
                                </a>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {/* Status Update Form */}
            {canReview ? (
              <form
                onSubmit={handleUpdateStatus}
                className="pt-4 border-t border-slate-200 space-y-4"
              >
                {isTerminalLocked ? (
                  <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 flex items-start gap-2 text-xs text-amber-900">
                    <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                    <span>
                      {lang === 'bn'
                        ? 'এই আবেদনটি চূড়ান্ত অবস্থায় (Resolved/Rejected) রয়েছে। কেবল সুপার অ্যাডমিন এটি পরিবর্তন করতে পারবেন।'
                        : 'This request is in a terminal state. Only a Super Admin can modify it further.'}
                    </span>
                  </div>
                ) : null}

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {lang === 'bn' ? 'নতুন অবস্থা নির্বাচন করুন' : 'Update Status'}
                  </label>
                  <select
                    disabled={Boolean(isTerminalLocked)}
                    value={nextStatus}
                    onChange={(e) => setNextStatus(e.target.value as ApplicationStatus)}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white disabled:bg-slate-100"
                  >
                    {APPLICATION_STATUSES.map((st) => (
                      <option key={st} value={st}>
                        {lang === 'bn' ? `${STATUS_LABELS_BN[st]} (${st})` : st}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {lang === 'bn'
                      ? 'দাপ্তরিক উত্তর / ব্যাখ্যা (প্রত্যাখ্যানের ক্ষেত্রে আবশ্যক)'
                      : 'Official Response / Explanation (Required for Rejection)'}
                  </label>
                  <textarea
                    rows={3}
                    disabled={Boolean(isTerminalLocked)}
                    value={officialResponse}
                    onChange={(e) => setOfficialResponse(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg disabled:bg-slate-100"
                  />
                </div>

                {!isTerminalLocked ? (
                  <div className="flex justify-end gap-3">
                    <button
                      type="button"
                      onClick={() => setSelected(null)}
                      className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer"
                    >
                      {lang === 'bn' ? 'বন্ধ করুন' : 'Close'}
                    </button>
                    <button
                      type="submit"
                      disabled={updating}
                      className="px-5 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg disabled:opacity-60 cursor-pointer"
                    >
                      {updating
                        ? lang === 'bn'
                          ? 'হালনাগাদ হচ্ছে...'
                          : 'Updating...'
                        : lang === 'bn'
                        ? 'অবস্থা হালনাগাদ করুন'
                        : 'Update Status'}
                    </button>
                  </div>
                ) : null}
              </form>
            ) : null}

            {/* Status History */}
            {selected.statusHistory && selected.statusHistory.length > 0 ? (
              <div className="pt-4 border-t border-slate-200">
                <h4 className="text-xs font-semibold text-slate-700 mb-2">
                  {lang === 'bn' ? 'অবস্থা পরিবর্তনের ইতিহাস' : 'Status History'}
                </h4>
                <div className="space-y-2">
                  {selected.statusHistory.map((h, i) => (
                    <div
                      key={`${h.timestamp}-${i}`}
                      className="text-xs bg-slate-50 border border-slate-200 rounded-lg p-3"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-semibold text-slate-900">{h.status}</span>
                        <span className="font-mono text-slate-500 tabular-nums">
                          {formatFirestoreTimestamp(h.timestamp, lang)}
                        </span>
                      </div>
                      {h.note ? (
                        <p className="mt-1 text-slate-700">{h.note}</p>
                      ) : null}
                      <p className="mt-1 text-[11px] text-slate-500">
                        {h.updatedByEmail || h.updatedByUid}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
      </Modal>

      {/* Attachment Preview Modal */}
      <Modal
        open={Boolean(previewAttachment)}
        onClose={() => setPreviewAttachment(null)}
        title={previewAttachment?.title || (lang === 'bn' ? 'নথি প্রিভিউ' : 'Attachment Preview')}
        maxWidthClass="max-w-4xl"
      >
        {previewAttachment ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 text-xs">
              <span className="font-mono text-slate-500 truncate max-w-md">{previewAttachment.url}</span>
              <a
                href={previewAttachment.url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 font-semibold shrink-0"
              >
                <span>{lang === 'bn' ? 'মূল ফাইল ডাউনলোড' : 'Download / Open Original'}</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>

            <div className="flex items-center justify-center p-2 bg-slate-100 rounded-xl max-h-[70vh] overflow-auto">
              {previewAttachment.isImage ? (
                <img
                  src={previewAttachment.url}
                  alt={previewAttachment.title}
                  className="max-h-[65vh] w-auto object-contain rounded-lg shadow-sm"
                />
              ) : (
                <iframe
                  src={previewAttachment.url}
                  title="PDF Document"
                  className="w-full h-[65vh] rounded-lg border border-slate-300 bg-white"
                />
              )}
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
