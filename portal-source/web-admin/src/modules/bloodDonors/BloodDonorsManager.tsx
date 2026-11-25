import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Download,
  Edit3,
  Eye,
  EyeOff,
  Heart,
  Plus,
  Search,
  ShieldAlert,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteBloodDonor,
  generateDocId,
  saveBloodDonor,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { BloodDonorDoc } from '../../types/firestore';
import { toLocalizedNumber } from '../../utils/formatters';
import { exportToCsv } from '../../utils/exportUtils';

const PAGE_SIZE = 10;
const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function calculateDonationEligibility(lastDonatedStr?: string): {
  isEligible: boolean;
  daysRemaining: number;
  labelBn: string;
  labelEn: string;
} {
  if (!lastDonatedStr || !lastDonatedStr.trim()) {
    return {
      isEligible: true,
      daysRemaining: 0,
      labelBn: 'রক্তদানে উপযুক্ত',
      labelEn: 'Eligible (Ready)',
    };
  }

  // Check for ISO/standard date pattern YYYY-MM-DD or DD/MM/YYYY
  const isoMatch = lastDonatedStr.match(/\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b/);
  let timestamp: number | null = null;

  if (isoMatch) {
    timestamp = new Date(Number(isoMatch[1]), Number(isoMatch[2]) - 1, Number(isoMatch[3])).getTime();
  } else {
    const parsed = Date.parse(lastDonatedStr);
    if (!isNaN(parsed)) {
      timestamp = parsed;
    }
  }

  if (!timestamp) {
    return {
      isEligible: true,
      daysRemaining: 0,
      labelBn: 'রক্তদানে প্রস্তুত',
      labelEn: 'Ready',
    };
  }

  const diffMs = Date.now() - timestamp;
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays >= 90) {
    return {
      isEligible: true,
      daysRemaining: 0,
      labelBn: 'রক্তদানে উপযুক্ত (> ৯০ দিন)',
      labelEn: 'Eligible (> 90 days)',
    };
  } else {
    const remaining = Math.max(1, 90 - diffDays);
    return {
      isEligible: false,
      daysRemaining: remaining,
      labelBn: `বিশ্রামকাল (${remaining} দিন বাকি)`,
      labelEn: `Cooling Period (${remaining}d left)`,
    };
  }
}

const EMPTY_DONOR: BloodDonorDoc = {
  id: '',
  nameBn: '',
  nameEn: '',
  bloodGroup: 'O+',
  phone: '',
  areaBn: '',
  areaEn: '',
  upazila: 'Maheshkhali',
  available: true,
  lastDonatedBn: '',
  lastDonatedEn: '',
};

function mapDonor(id: string, raw: Record<string, unknown>): BloodDonorDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    nameBn: typeof raw.nameBn === 'string' ? raw.nameBn : '',
    nameEn: typeof raw.nameEn === 'string' ? raw.nameEn : '',
    bloodGroup: typeof raw.bloodGroup === 'string' ? raw.bloodGroup : 'O+',
    phone: typeof raw.phone === 'string' ? raw.phone : '',
    areaBn: typeof raw.areaBn === 'string' ? raw.areaBn : '',
    areaEn: typeof raw.areaEn === 'string' ? raw.areaEn : '',
    upazila: typeof raw.upazila === 'string' ? raw.upazila : '',
    available: raw.available !== false,
    lastDonatedBn: typeof raw.lastDonatedBn === 'string' ? raw.lastDonatedBn : '',
    lastDonatedEn: typeof raw.lastDonatedEn === 'string' ? raw.lastDonatedEn : '',
  };
}

function maskPhone(phone: string): string {
  if (!phone) return '—';
  if (phone.length <= 4) return '****';
  return `${phone.slice(0, 3)}****${phone.slice(-3)}`;
}

export function BloodDonorsManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [donors, setDonors] = useState<BloodDonorDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [search, setSearch] = useState('');
  const [groupFilter, setGroupFilter] = useState('ALL');
  const [showPhones, setShowPhones] = useState(false);
  const [page, setPage] = useState(1);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<BloodDonorDoc>(EMPTY_DONOR);
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<BloodDonorDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'blood_donors',
      mapDonor,
      (items) => {
        setDonors(items);
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
    return donors.filter((d) => {
      const matchesGroup = groupFilter === 'ALL' || d.bloodGroup === groupFilter;
      const matchesQuery =
        !q ||
        d.nameBn.toLowerCase().includes(q) ||
        d.nameEn.toLowerCase().includes(q) ||
        d.areaBn.toLowerCase().includes(q) ||
        d.upazila.toLowerCase().includes(q);
      return matchesGroup && matchesQuery;
    });
  }, [donors, groupFilter, search]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, groupFilter]);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';
  const canDelete = admin?.role === 'super_admin';

  const openCreate = () => {
    setIsNew(true);
    setForm({ ...EMPTY_DONOR, id: generateDocId('B') });
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: BloodDonorDoc) => {
    setIsNew(false);
    setForm({ ...item });
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.nameBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'রক্তদাতার নাম আবশ্যক।' : 'Donor name is required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveBloodDonor(form, isNew, admin);
      setEditorOpen(false);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'রক্তদাতার তথ্য সংরক্ষিত হয়েছে।' : 'Blood donor saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save blood donor.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!admin || !deleteTarget || !canDelete) return;
    setDeleting(true);
    try {
      await deleteBloodDonor(
        deleteTarget.id,
        deleteTarget.nameBn || deleteTarget.nameEn,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'রক্তদাতা রেকর্ড মুছে ফেলা হয়েছে।' : 'Donor record deleted.',
      });
    } catch (err) {
      setDeleteTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete donor.',
      });
    } finally {
      setDeleting(false);
    }
  };

  const handleExportCsv = () => {
    const headers = [
      'ID',
      'Name (Bengali)',
      'Name (English)',
      'Blood Group',
      'Phone',
      'Upazila',
      'Area',
      'Last Donated',
      '90-Day Eligibility',
      'Availability',
    ];
    const rows = filtered.map((d) => {
      const eligibility = calculateDonationEligibility(d.lastDonatedEn || d.lastDonatedBn);
      return [
        d.id,
        d.nameBn,
        d.nameEn || '',
        d.bloodGroup,
        showPhones || admin?.role === 'super_admin' ? d.phone : maskPhone(d.phone),
        d.upazila,
        d.areaBn || d.areaEn,
        d.lastDonatedBn || d.lastDonatedEn || '',
        eligibility.labelEn,
        d.available ? 'Available' : 'Unavailable',
      ];
    });
    exportToCsv({
      filename: `blood_donors_${new Date().toISOString().slice(0, 10)}.csv`,
      headers,
      rows,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? 'রক্তদাতা ডিরেক্টরি' : 'Blood Donor Directory'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            blood_donors · {toLocalizedNumber(donors.length, lang)}{' '}
            {lang === 'bn' ? 'জন রক্তদাতা' : 'donors'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={handleExportCsv}
            disabled={filtered.length === 0}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5 text-emerald-700" />
            <span>{lang === 'bn' ? 'এক্সপোর্ট (CSV)' : 'Export CSV'}</span>
          </button>

          <button
            type="button"
            onClick={() => setShowPhones((prev) => !prev)}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer"
          >
            {showPhones ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
            <span>
              {showPhones
                ? lang === 'bn'
                  ? 'ফোন নম্বর লুকান'
                  : 'Mask Phones'
                : lang === 'bn'
                ? 'ফোন নম্বর দেখুন'
                : 'Reveal Phones'}
            </span>
          </button>

          {canEdit ? (
            <button
              type="button"
              onClick={openCreate}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>{lang === 'bn' ? 'রক্তদাতা যুক্ত করুন' : 'Add Donor'}</span>
            </button>
          ) : null}
        </div>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-xl p-3.5 flex items-start gap-2.5 text-xs text-amber-900">
        <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
        <span>
          {lang === 'bn'
            ? 'গোপনীয়তা সুরক্ষা: রক্তদাতাদের ব্যক্তিগত ফোন নম্বর ডিফল্টভাবে মাস্ক করা থাকে এবং অনুমোদিত অ্যাডমিন ব্যতীত উন্মুক্ত করা হয় না।'
            : 'PII Protection: Donor phone numbers are masked by default and restricted to authorized administrators.'}
        </span>
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
              lang === 'bn' ? 'নাম বা এলাকা দিয়ে খুঁজুন...' : 'Search by name or area...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>

        <select
          value={groupFilter}
          onChange={(e) => setGroupFilter(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
        >
          <option value="ALL">
            {lang === 'bn' ? 'সকল রক্তের গ্রুপ' : 'All Blood Groups'}
          </option>
          {BLOOD_GROUPS.map((bg) => (
            <option key={bg} value={bg}>
              {bg}
            </option>
          ))}
        </select>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading donors...'}
          </div>
        ) : paginated.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'কোনো রক্তদাতা পাওয়া যায়নি।' : 'No donors found.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-3 px-4">{lang === 'bn' ? 'নাম' : 'Name'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'গ্রুপ' : 'Group'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'ফোন' : 'Phone'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'এলাকা · উপজেলা' : 'Area · Upazila'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'সর্বশেষ রক্তদান' : 'Last Donated'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'যোগ্যতা (৯০ দিন)' : 'Eligibility (90d)'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'অবস্থা' : 'Availability'}</th>
                  <th className="py-3 px-4 text-right">{lang === 'bn' ? 'কার্যক্রম' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((d) => {
                  const eligibility = calculateDonationEligibility(d.lastDonatedEn || d.lastDonatedBn);

                  return (
                    <tr key={d.id} className="hover:bg-slate-50/80">
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900">
                          {lang === 'bn' ? d.nameBn : d.nameEn || d.nameBn}
                        </div>
                        <div className="text-xs text-slate-500 font-mono">{d.id}</div>
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-red-700">
                        {d.bloodGroup}
                      </td>
                      <td className="py-3 px-4 font-mono text-xs text-slate-700 tabular-nums">
                        {showPhones ? d.phone || '—' : maskPhone(d.phone)}
                      </td>
                      <td className="py-3 px-4 text-xs text-slate-600">
                        {[(lang === 'bn' ? d.areaBn : d.areaEn), d.upazila]
                          .filter(Boolean)
                          .join(' · ') || '—'}
                      </td>
                      <td className="py-3 px-4 text-xs text-slate-600">
                        {(lang === 'bn' ? d.lastDonatedBn : d.lastDonatedEn) || '—'}
                      </td>
                      <td className="py-3 px-4 text-xs">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                            eligibility.isEligible
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {eligibility.isEligible ? (
                            <Heart className="w-3 h-3 text-emerald-600 shrink-0" />
                          ) : (
                            <Clock className="w-3 h-3 text-amber-600 shrink-0" />
                          )}
                          <span>{lang === 'bn' ? eligibility.labelBn : eligibility.labelEn}</span>
                        </span>
                      </td>
                      <td className="py-3 px-4 text-xs font-medium">
                        <span className={d.available ? 'text-emerald-700' : 'text-slate-500'}>
                          {d.available
                            ? lang === 'bn'
                              ? 'উপলব্ধ'
                              : 'Available'
                            : lang === 'bn'
                            ? 'অনুপলব্ধ'
                            : 'Unavailable'}
                        </span>
                      </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="inline-flex items-center gap-1.5">
                        {canEdit ? (
                          <button
                            type="button"
                            onClick={() => openEdit(d)}
                            className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                            aria-label="Edit donor"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                        ) : null}
                        {canDelete ? (
                          <button
                            type="button"
                            onClick={() => setDeleteTarget(d)}
                            className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                            aria-label="Delete donor"
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
              ? 'নতুন রক্তদাতা যুক্ত করুন'
              : 'Add Blood Donor'
            : lang === 'bn'
            ? 'রক্তদাতা সম্পাদনা'
            : 'Edit Blood Donor'
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
                {lang === 'bn' ? 'রক্তের গ্রুপ *' : 'Blood Group *'}
              </label>
              <select
                value={form.bloodGroup}
                onChange={(e) => setForm({ ...form, bloodGroup: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
              >
                {BLOOD_GROUPS.map((bg) => (
                  <option key={bg} value={bg}>
                    {bg}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-800 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.available}
                  onChange={(e) => setForm({ ...form, available: e.target.checked })}
                  className="w-4 h-4 rounded border-slate-300 text-emerald-700"
                />
                <span>{lang === 'bn' ? 'রক্তদানে প্রস্তুত' : 'Available'}</span>
              </label>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'নাম (বাংলা) *' : 'Name (Bengali) *'}
              </label>
              <input
                type="text"
                required
                value={form.nameBn}
                onChange={(e) => setForm({ ...form, nameBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'নাম (ইংরেজি)' : 'Name (English)'}
              </label>
              <input
                type="text"
                value={form.nameEn}
                onChange={(e) => setForm({ ...form, nameEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ফোন নম্বর' : 'Phone Number'}
              </label>
              <input
                type="tel"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
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
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'এলাকা (বাংলা)' : 'Area (Bengali)'}
              </label>
              <input
                type="text"
                value={form.areaBn}
                onChange={(e) => setForm({ ...form, areaBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'এলাকা (ইংরেজি)' : 'Area (English)'}
              </label>
              <input
                type="text"
                value={form.areaEn}
                onChange={(e) => setForm({ ...form, areaEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সর্বশেষ রক্তদান (বাংলা)' : 'Last Donated (Bn)'}
              </label>
              <input
                type="text"
                value={form.lastDonatedBn}
                onChange={(e) => setForm({ ...form, lastDonatedBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সর্বশেষ রক্তদান (ইংরেজি)' : 'Last Donated (En)'}
              </label>
              <input
                type="text"
                value={form.lastDonatedEn}
                onChange={(e) => setForm({ ...form, lastDonatedEn: e.target.value })}
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
                : 'Save Donor'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'রক্তদাতা তথ্য মুছুন' : 'Confirm Donor Deletion'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${deleteTarget?.nameBn || deleteTarget?.nameEn}" মুছে ফেলতে চান?`
            : `Delete donor "${deleteTarget?.nameEn || deleteTarget?.nameBn}"?`
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
