import { useEffect, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Droplets,
  Save,
  Shield,
  UserX,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  saveGenericEntity,
  subscribeCollection,
  subscribeDoc,
} from '../../services/firebase/crudService';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

interface PrivacyPolicyConfig {
  maskDonorPhonesByDefault: boolean;
  donorConsentExpiryDays: number;
  allowPublicDonorSearch: boolean;
  citizenDataRetentionMonths: number;
  requireAdminConsentForRemoval: boolean;
  maskCitizenNidAfterResolution: boolean;
  anonymizeGrievanceComplainants: boolean;
  lastUpdated: string;
}

interface DataRemovalRequest {
  id: string;
  applicantName: string;
  phone: string;
  collectionTarget: string;
  targetDocId: string;
  reasonBn: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestedAtLabel: string;
}

const DEFAULT_PRIVACY_CONFIG: PrivacyPolicyConfig = {
  maskDonorPhonesByDefault: true,
  donorConsentExpiryDays: 365,
  allowPublicDonorSearch: true,
  citizenDataRetentionMonths: 24,
  requireAdminConsentForRemoval: true,
  maskCitizenNidAfterResolution: true,
  anonymizeGrievanceComplainants: false,
  lastUpdated: '',
};

function mapRemovalRequestDoc(id: string, raw: Record<string, unknown>): DataRemovalRequest {
  const statusRaw = typeof raw.status === 'string' ? raw.status.toUpperCase() : 'PENDING';
  const status: 'PENDING' | 'APPROVED' | 'REJECTED' =
    statusRaw === 'APPROVED' || statusRaw === 'REJECTED' ? statusRaw : 'PENDING';

  const dateSource = raw.createdAt ?? raw.requestedAt;
  const requestedAtLabel =
    typeof dateSource === 'string'
      ? dateSource
      : formatFirestoreTimestamp(dateSource as any, 'en') || '—';

  return {
    id,
    applicantName:
      typeof raw.applicantName === 'string'
        ? raw.applicantName
        : typeof raw.name === 'string'
        ? raw.name
        : '—',
    phone: typeof raw.phone === 'string' ? raw.phone : '—',
    collectionTarget:
      typeof raw.collectionTarget === 'string' ? raw.collectionTarget : '—',
    targetDocId: typeof raw.targetDocId === 'string' ? raw.targetDocId : '—',
    reasonBn:
      typeof raw.reasonBn === 'string'
        ? raw.reasonBn
        : typeof raw.reason === 'string'
        ? raw.reason
        : '—',
    status,
    requestedAtLabel,
  };
}

export function PrivacyConsentManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [config, setConfig] = useState<PrivacyPolicyConfig>(DEFAULT_PRIVACY_CONFIG);
  const [requests, setRequests] = useState<DataRemovalRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [requestsError, setRequestsError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    let active = true;
    const unsubConfig = subscribeDoc(
      'config',
      'privacy_policy',
      (data) => {
        if (!active) return;
        if (data) {
          setConfig((prev) => ({ ...prev, ...data }));
        }
        setLoading(false);
      },
      () => {
        if (active) setLoading(false);
      }
    );

    const unsubRequests = subscribeCollection(
      'data_removal_requests',
      mapRemovalRequestDoc,
      (items) => {
        if (!active) return;
        setRequests(items);
        setRequestsLoading(false);
        setRequestsError(null);
      },
      (msg) => {
        if (!active) return;
        setRequests([]);
        setRequestsError(msg);
        setRequestsLoading(false);
      }
    );

    return () => {
      active = false;
      unsubConfig();
      unsubRequests();
    };
  }, []);

  const canEdit = admin?.role === 'super_admin';

  const handleAction = async (id: string, newStatus: 'APPROVED' | 'REJECTED') => {
    if (!admin || !canEdit) return;
    setProcessingId(id);
    setFeedback(null);
    try {
      await saveGenericEntity(
        'data_removal_requests',
        id,
        {
          status: newStatus,
          reviewedByUid: admin.uid,
          reviewedAt: new Date().toISOString(),
        },
        false,
        admin,
        `তথ্য প্রত্যাহার অনুরোধ (${newStatus})`
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `অনুরোধটি সফলভাবে ${newStatus === 'APPROVED' ? 'অনুমোদিত' : 'প্রত্যাখ্যাত'} হিসেবে ফায়ারস্টোরে সংরক্ষিত হয়েছে।`
            : `Request status persisted as ${newStatus}.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update removal request.',
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;

    setSaving(true);
    setFeedback(null);
    try {
      await saveGenericEntity(
        'config',
        'privacy_policy',
        {
          ...config,
          lastUpdated: new Date().toISOString(),
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'নাগরিক ডাটা প্রাইভেসি ও কনসেন্ট সেটিংস'
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'প্রাইভেসি ও কনসেন্ট পলিসি সফলভাবে সংরক্ষিত হয়েছে।'
            : 'Privacy and consent policy saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save privacy settings.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'প্রাইভেসি সেটিংস লোড হচ্ছে...' : 'Loading privacy settings...'}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Shield className="w-6 h-6 text-[#006A4E]" />
            <span>
              {lang === 'bn'
                ? 'নাগরিক ডাটা প্রাইভেসি ও কনসেন্ট ব্যবস্থাপনা'
                : 'Citizen Privacy & Consent Management'}
            </span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'রক্তদাতা ও সাধারণ নাগরিকদের ব্যক্তিগত তথ্যের নিরাপত্তা, ফোন মাস্কিং, তথ্য প্রত্যাহার (data_removal_requests) ও আবেদনপত্রের গোপনীয়তা নিয়ন্ত্রণ করুন।'
              : 'Enforce citizen data protection, blood donor consent visibility, data retention limits, and real-time removal requests.'}
          </p>
        </div>
      </div>

      {feedback && (
        <div
          role="alert"
          className={`p-4 rounded-xl border flex items-start gap-3 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
          )}
          <span>{feedback.text}</span>
        </div>
      )}

      {/* Configuration Form */}
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <Droplets className="w-4 h-4 text-red-600" />
            <span>
              {lang === 'bn'
                ? 'রক্তদাতা ও সংবেদনশীল তথ্য গোপনীয়তা'
                : 'Blood Donor & Sensitive Data Controls'}
            </span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 cursor-pointer">
              <input
                type="checkbox"
                checked={config.maskDonorPhonesByDefault}
                onChange={(e) =>
                  setConfig({ ...config, maskDonorPhonesByDefault: e.target.checked })
                }
                className="w-4 h-4 rounded text-[#006A4E] focus:ring-[#006A4E] mt-0.5"
              />
              <div className="text-xs">
                <span className="font-semibold text-slate-800 block">
                  {lang === 'bn'
                    ? 'ডিফল্টভাবে রক্তদাতার ফোন মাস্ক রাখুন'
                    : 'Mask Donor Phones by Default'}
                </span>
                <span className="text-slate-500">
                  {lang === 'bn'
                    ? 'অ্যাডমিন প্যানেলে নম্বর 01819****123 আকারে আংশিক গোপন থাকবে।'
                    : 'Hides middle digits in list views unless revealed.'}
                </span>
              </div>
            </label>

            <label className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50/50 cursor-pointer">
              <input
                type="checkbox"
                checked={config.allowPublicDonorSearch}
                onChange={(e) =>
                  setConfig({ ...config, allowPublicDonorSearch: e.target.checked })
                }
                className="w-4 h-4 rounded text-[#006A4E] focus:ring-[#006A4E] mt-0.5"
              />
              <div className="text-xs">
                <span className="font-semibold text-slate-800 block">
                  {lang === 'bn'
                    ? 'স্বেচ্ছাসেবী রক্তদাতা অনুসন্ধান উন্মুক্ত রাখুন'
                    : 'Allow Public Donor Directory Search'}
                </span>
                <span className="text-slate-500">
                  {lang === 'bn'
                    ? 'শুধুমাত্র "Available" রক্তদাতারা মোবাইল অ্যাপে দৃশ্যমান হবেন।'
                    : 'Only voluntary available donors show in mobile app.'}
                </span>
              </div>
            </label>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn'
                  ? 'নাগরিক আবেদন ডাটা সংরক্ষণের সময়সীমা (মাস)'
                  : 'Data Retention Period (Months)'}
              </label>
              <input
                type="number"
                min={6}
                max={60}
                value={config.citizenDataRetentionMonths}
                onChange={(e) =>
                  setConfig({
                    ...config,
                    citizenDataRetentionMonths: Number(e.target.value) || 24,
                  })
                }
                className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'রক্তদাতা সম্মতির মেয়াদ (দিন)' : 'Donor Consent Validity (Days)'}
              </label>
              <input
                type="number"
                min={90}
                max={730}
                value={config.donorConsentExpiryDays}
                onChange={(e) =>
                  setConfig({ ...config, donorConsentExpiryDays: Number(e.target.value) || 365 })
                }
                className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>
          </div>
        </div>

        {canEdit && (
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 text-xs font-semibold text-white bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-60 rounded-lg transition-colors flex items-center gap-1.5 shadow-sm cursor-pointer"
            >
              <Save className="w-3.5 h-3.5" />
              <span>
                {saving
                  ? lang === 'bn'
                    ? 'সংরক্ষণ হচ্ছে...'
                    : 'Saving...'
                  : lang === 'bn'
                  ? 'পলিসি সংরক্ষণ করুন'
                  : 'Save Policy'}
              </span>
            </button>
          </div>
        )}
      </form>

      {/* Citizen Data Removal Requests Queue */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <h3 className="text-base font-semibold text-slate-900 flex items-center gap-2">
            <UserX className="w-4 h-4 text-amber-700" />
            <span>
              {lang === 'bn'
                ? 'ব্যক্তিগত তথ্য অপসারণ ও প্রত্যাহারের অনুরোধ'
                : 'Citizen Data Removal Requests'}
            </span>
          </h3>
          <span className="text-xs font-medium text-slate-500">
            {toLocalizedNumber(requests.length, lang)} {lang === 'bn' ? 'টি অনুরোধ' : 'requests'}
          </span>
        </div>

        {requestsError ? (
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
            {requestsError}
          </div>
        ) : requestsLoading ? (
          <div className="py-8 text-center text-xs text-slate-500">
            {lang === 'bn' ? 'অনুরোধ তালিকা লোড হচ্ছে...' : 'Loading removal requests...'}
          </div>
        ) : requests.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-500">
            {lang === 'bn'
              ? 'বর্তমানে কোনো ব্যক্তিগত তথ্য অপসারণের অনুরোধ নেই।'
              : 'No citizen data removal requests submitted yet.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-semibold">
                  <th className="py-2.5 px-3">নাগরিক নাম / ফোন</th>
                  <th className="py-2.5 px-3">টার্গেট কালেকশন</th>
                  <th className="py-2.5 px-3">প্রত্যাহারের কারণ</th>
                  <th className="py-2.5 px-3">তারিখ</th>
                  <th className="py-2.5 px-3">স্ট্যাটাস</th>
                  <th className="py-2.5 px-3 text-right">পদক্ষেপ</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50">
                    <td className="py-2.5 px-3 font-medium text-slate-900">
                      <div>{r.applicantName}</div>
                      <div className="text-[11px] text-slate-500 font-mono">{r.phone}</div>
                    </td>
                    <td className="py-2.5 px-3 font-mono text-[11px] text-slate-600">
                      {r.collectionTarget}/{r.targetDocId}
                    </td>
                    <td className="py-2.5 px-3 text-slate-700 max-w-xs">{r.reasonBn}</td>
                    <td className="py-2.5 px-3 text-slate-500 font-mono">{r.requestedAtLabel}</td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold ${
                          r.status === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : r.status === 'REJECTED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {r.status}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-right space-x-1.5 whitespace-nowrap">
                      {r.status === 'PENDING' && canEdit && (
                        <>
                          <button
                            type="button"
                            disabled={processingId === r.id}
                            onClick={() => handleAction(r.id, 'APPROVED')}
                            className="px-2 py-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded border border-emerald-200 cursor-pointer disabled:opacity-50"
                          >
                            অনুমোদন
                          </button>
                          <button
                            type="button"
                            disabled={processingId === r.id}
                            onClick={() => handleAction(r.id, 'REJECTED')}
                            className="px-2 py-1 text-[11px] font-medium text-red-700 bg-red-50 hover:bg-red-100 rounded border border-red-200 cursor-pointer disabled:opacity-50"
                          >
                            প্রত্যাখ্যান
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
