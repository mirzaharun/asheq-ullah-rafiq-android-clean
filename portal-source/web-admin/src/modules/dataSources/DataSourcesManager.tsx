import { useEffect, useState, type FormEvent } from 'react';
import {
  Activity,
  AlertCircle,
  CheckCircle2,
  CloudLightning,
  Radio,
  RefreshCw,
  Save,
  Server,
  Ship,
  Waves,
  type LucideIcon,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  fetchCollectionCountSafe,
  saveGenericEntity,
  subscribeCollection,
} from '../../services/firebase/crudService';

type FeedId = 'weather' | 'tide' | 'sea_truck' | 'watercraft';
type OperationalStatus = 'operational' | 'delayed' | 'maintenance' | 'suspended';
type FeedDisplayStatus = 'ONLINE' | 'DEGRADED' | 'FALLBACK' | 'OFFLINE';

interface DataSourceDoc {
  id: FeedId;
  sourceNameBn: string;
  sourceNameEn: string;
  category: string;
  provider: string;
  enabled: boolean;
  operationalStatus: OperationalStatus;
  status: FeedDisplayStatus;
  verificationStatus: 'VERIFIED' | 'PENDING' | 'MANUAL_OVERRIDE';
  autoSyncIntervalMins: number;
  lastSyncAt: string;
  lastFailedUpdate: string;
  fallbackMessageBn: string;
  fallbackMessageEn: string;
  manualNotes: string;
}

const BACKING_COLLECTION_MAP: Record<FeedId, string> = {
  weather: 'weather_data',
  tide: 'tide_schedules',
  sea_truck: 'sea_truck_updates',
  watercraft: 'watercraft_schedules',
};

const FEED_ICONS: Record<FeedId, LucideIcon> = {
  weather: CloudLightning,
  tide: Waves,
  sea_truck: Ship,
  watercraft: Radio,
};

function toOperationalStatus(status: FeedDisplayStatus): OperationalStatus {
  switch (status) {
    case 'ONLINE':
      return 'operational';
    case 'DEGRADED':
      return 'delayed';
    case 'FALLBACK':
      return 'maintenance';
    case 'OFFLINE':
      return 'suspended';
  }
}

function toDisplayStatus(op: string, fallbackStatus?: string): FeedDisplayStatus {
  if (
    fallbackStatus === 'ONLINE' ||
    fallbackStatus === 'DEGRADED' ||
    fallbackStatus === 'FALLBACK' ||
    fallbackStatus === 'OFFLINE'
  ) {
    return fallbackStatus;
  }
  switch (op.toLowerCase()) {
    case 'operational':
      return 'ONLINE';
    case 'delayed':
      return 'DEGRADED';
    case 'maintenance':
      return 'FALLBACK';
    case 'suspended':
      return 'OFFLINE';
    default:
      return 'ONLINE';
  }
}

const DEFAULT_FEEDS: DataSourceDoc[] = [
  {
    id: 'weather',
    sourceNameBn: 'আবহাওয়া ও সামুদ্রিক সংকেত ফিড',
    sourceNameEn: 'Weather & Marine Danger Signals',
    category: 'weather',
    provider: 'বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD) ও উপকূলীয় রাডার',
    enabled: true,
    operationalStatus: 'operational',
    status: 'ONLINE',
    verificationStatus: 'PENDING',
    autoSyncIntervalMins: 60,
    lastSyncAt: '—',
    lastFailedUpdate: '—',
    fallbackMessageBn:
      'আবহাওয়া তথ্য সাময়িকভাবে অনুপলব্ধ। উপকূলীয় কন্ট্রোল রুমে (৩৩৩) যোগাযোগ করুন।',
    fallbackMessageEn:
      'Weather feed temporarily unavailable. Please contact the national helpline (333) or coastal radio advisories.',
    manualNotes: 'কুতুবদিয়া বাতিঘর ও কক্সবাজার রাডার স্টেশনের সতর্কবার্তার সাথে সমন্বিত।',
  },
  {
    id: 'tide',
    sourceNameBn: 'জোয়ার-ভাটা গণক ও সারণী',
    sourceNameEn: 'Tide Schedules & Jetty Water Levels',
    category: 'tide',
    provider: 'বিআইডব্লিউটিএ হাইড্রোগ্রাফিক ডিপার্টমেন্ট ও কোস্টাল টেবিল',
    enabled: true,
    operationalStatus: 'operational',
    status: 'ONLINE',
    verificationStatus: 'PENDING',
    autoSyncIntervalMins: 360,
    lastSyncAt: '—',
    lastFailedUpdate: '—',
    fallbackMessageBn: 'ভাটার সময় ট্রলার ওঠা-নামায় বিশেষ সতর্কতা অবলম্বন করুন।',
    fallbackMessageEn: 'Exercise caution during low tide passenger boarding at sea truck jetties.',
    manualNotes: 'দৈনিক সকাল ও সন্ধ্যার জোয়ার-ভাটার উচ্চতা সারণী হালনাগাদ।',
  },
  {
    id: 'sea_truck',
    sourceNameBn: 'কুতুবদিয়া-মগনামা সী-ট্রাক ট্র্যাকার',
    sourceNameEn: 'Kutubdia - Magnama Sea-Truck Dispatch',
    category: 'sea_truck',
    provider: 'বিআইডব্লিউটিসি (BIWTC) কুতুবদিয়া ও দরবার ঘাট কন্ট্রোল',
    enabled: true,
    operationalStatus: 'operational',
    status: 'ONLINE',
    verificationStatus: 'PENDING',
    autoSyncIntervalMins: 30,
    lastSyncAt: '—',
    lastFailedUpdate: '—',
    fallbackMessageBn:
      'ভাটা বা কুয়াশার কারণে সী-ট্রাক চলাচলে বিলম্ব হতে পারে। ঘাটে যোগাযোগ করুন।',
    fallbackMessageEn: 'Sea-truck departures may be delayed due to low tide or coastal fog.',
    manualNotes: 'নৌ-পরিবহন মন্ত্রণালয়ের সমন্বয়ে পরিচালিত।',
  },
  {
    id: 'watercraft',
    sourceNameBn: 'মহেশখালী-কক্সবাজার স্পিডবোট ও ট্রলার সূচি',
    sourceNameEn: "Maheshkhali - Cox's Bazar Speedboat Schedules",
    category: 'watercraft',
    provider: 'গোরকঘাটা ঘাট ও ৬নং ঘাট চালক সমবায় সমিতি',
    enabled: true,
    operationalStatus: 'operational',
    status: 'ONLINE',
    verificationStatus: 'PENDING',
    autoSyncIntervalMins: 120,
    lastSyncAt: '—',
    lastFailedUpdate: '—',
    fallbackMessageBn: 'সন্ধ্যা ৬টার পর স্পিডবোট চলাচল সরকারি নির্দেশনায় বন্ধ থাকবে।',
    fallbackMessageEn:
      'Speedboat navigation suspended after 6:00 PM per coastal safety regulations.',
    manualNotes: 'মহেশখালী চ্যানেল নৌ-পুলিশের অনুমোদিত সময়সূচি।',
  },
];

function mapDataSourceDoc(id: string, raw: Record<string, unknown>): DataSourceDoc | null {
  const normalizedId: FeedId | null =
    id === 'weather' || id === 'tide' || id === 'sea_truck' || id === 'watercraft'
      ? id
      : id === 'seaTruck'
      ? 'sea_truck'
      : null;
  if (!normalizedId) return null;

  const base = DEFAULT_FEEDS.find((f) => f.id === normalizedId)!;
  const opRaw =
    typeof raw.operationalStatus === 'string' ? raw.operationalStatus : base.operationalStatus;
  const displayStatus = toDisplayStatus(
    opRaw,
    typeof raw.status === 'string' ? raw.status : undefined
  );
  const operationalStatus: OperationalStatus =
    opRaw === 'operational' ||
    opRaw === 'delayed' ||
    opRaw === 'maintenance' ||
    opRaw === 'suspended'
      ? opRaw
      : toOperationalStatus(displayStatus);

  return {
    id: normalizedId,
    sourceNameBn:
      typeof raw.sourceNameBn === 'string'
        ? raw.sourceNameBn
        : typeof raw.nameBn === 'string'
        ? raw.nameBn
        : base.sourceNameBn,
    sourceNameEn:
      typeof raw.sourceNameEn === 'string'
        ? raw.sourceNameEn
        : typeof raw.nameEn === 'string'
        ? raw.nameEn
        : base.sourceNameEn,
    category: typeof raw.category === 'string' ? raw.category : base.category,
    provider: typeof raw.provider === 'string' ? raw.provider : base.provider,
    enabled: typeof raw.enabled === 'boolean' ? raw.enabled : base.enabled,
    operationalStatus,
    status: displayStatus,
    verificationStatus:
      raw.verificationStatus === 'VERIFIED' ||
      raw.verificationStatus === 'PENDING' ||
      raw.verificationStatus === 'MANUAL_OVERRIDE'
        ? raw.verificationStatus
        : base.verificationStatus,
    autoSyncIntervalMins:
      typeof raw.autoSyncIntervalMins === 'number'
        ? raw.autoSyncIntervalMins
        : base.autoSyncIntervalMins,
    lastSyncAt:
      typeof raw.lastSyncAt === 'string'
        ? raw.lastSyncAt
        : typeof raw.lastSuccessfulUpdate === 'string'
        ? raw.lastSuccessfulUpdate
        : base.lastSyncAt,
    lastFailedUpdate:
      typeof raw.lastFailedUpdate === 'string' ? raw.lastFailedUpdate : base.lastFailedUpdate,
    fallbackMessageBn:
      typeof raw.fallbackMessageBn === 'string' ? raw.fallbackMessageBn : base.fallbackMessageBn,
    fallbackMessageEn:
      typeof raw.fallbackMessageEn === 'string' ? raw.fallbackMessageEn : base.fallbackMessageEn,
    manualNotes: typeof raw.manualNotes === 'string' ? raw.manualNotes : base.manualNotes,
  };
}

export function DataSourcesManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [feeds, setFeeds] = useState<DataSourceDoc[]>(DEFAULT_FEEDS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testingId, setTestingId] = useState<FeedId | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    let active = true;
    const unsub = subscribeCollection(
      'data_sources',
      (id, raw) => mapDataSourceDoc(id, raw),
      (docs) => {
        if (!active) return;
        const validDocs = docs.filter((d): d is DataSourceDoc => d !== null);
        if (validDocs.length > 0) {
          setFeeds(
            DEFAULT_FEEDS.map((base) => validDocs.find((d) => d.id === base.id) ?? base)
          );
        }
        setLoading(false);
      },
      () => {
        if (active) setLoading(false);
      }
    );

    return () => {
      active = false;
      unsub();
    };
  }, []);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';

  const handleUpdateFeed = (id: FeedId, patch: Partial<DataSourceDoc>) => {
    setFeeds((prev) =>
      prev.map((feed) => {
        if (feed.id !== id) return feed;
        const merged = { ...feed, ...patch };
        if (patch.status && !patch.operationalStatus) {
          merged.operationalStatus = toOperationalStatus(patch.status);
        }
        return merged;
      })
    );
  };

  const handleManualTestSync = async (id: FeedId) => {
    const targetFeed = feeds.find((f) => f.id === id);
    if (!targetFeed) return;

    setTestingId(id);
    setFeedback(null);
    const backingCollection = BACKING_COLLECTION_MAP[id];

    try {
      const count = await fetchCollectionCountSafe(backingCollection);
      const nowIso = new Date().toISOString();

      if (count === null) {
        handleUpdateFeed(id, {
          lastFailedUpdate: nowIso,
          verificationStatus: 'PENDING',
        });
        setFeedback({
          type: 'error',
          text:
            lang === 'bn'
              ? `${targetFeed.sourceNameBn}: ${backingCollection} সংগ্রহ যাচাই করা যায়নি।`
              : `${targetFeed.sourceNameEn}: Could not verify ${backingCollection}.`,
        });
        return;
      }

      const nextFeed: DataSourceDoc = {
        ...targetFeed,
        lastSyncAt: nowIso,
        status: count > 0 ? 'ONLINE' : 'FALLBACK',
        operationalStatus: count > 0 ? 'operational' : 'maintenance',
        verificationStatus: count > 0 ? 'VERIFIED' : 'PENDING',
      };
      handleUpdateFeed(id, nextFeed);

      if (admin && canEdit) {
        await saveGenericEntity(
          'data_sources',
          id,
          {
            ...nextFeed,
            updatedByUid: admin.uid,
            updatedAt: nowIso,
          } as unknown as Record<string, unknown>,
          false,
          admin,
          `${targetFeed.sourceNameBn} (${count} রেকর্ড যাচাইকৃত)`
        );
      }

      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `${targetFeed.sourceNameBn}: ${backingCollection} সংগ্রহে ${count}টি নথি পাওয়া গেছে ও সিঙ্ক সময় হালনাগাদ হয়েছে।`
            : `${targetFeed.sourceNameEn}: Verified ${count} documents in ${backingCollection} and updated sync timestamp.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Verification failed.',
      });
    } finally {
      setTestingId(null);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;

    setSaving(true);
    setFeedback(null);
    const nowIso = new Date().toISOString();
    try {
      for (const feed of feeds) {
        await saveGenericEntity(
          'data_sources',
          feed.id,
          {
            ...feed,
            updatedAt: nowIso,
            updatedByUid: admin.uid,
          } as unknown as Record<string, unknown>,
          false,
          admin,
          `ডাটা সোর্স (${feed.id})`
        );
      }
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'ডাটা সোর্স সেটিংস (data_sources) সংগ্রহে সংরক্ষিত হয়েছে এবং মোবাইল অ্যাপে লাইভ সিঙ্ক হয়েছে।'
            : 'Data source documents saved to data_sources collection and synced to Android.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save data source config.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'ডাটা সোর্স লোড হচ্ছে...' : 'Loading data sources...'}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Server className="w-6 h-6 text-[#006A4E]" />
            <span>
              {lang === 'bn'
                ? 'লাইভ ডাটা সোর্স ও এপিআই ব্যবস্থাপনা'
                : 'Live Data Source & API Management'}
            </span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'উপকূলীয় আবহাওয়া, জোয়ার-ভাটা, সী-ট্রাক ও স্পিডবোট লাইভ ফিডের (data_sources) স্ট্যাটাস, ফলব্যাক বার্তা ও সিঙ্ক অবস্থা নিয়ন্ত্রণ করুন।'
              : 'Manage live data_sources documents (weather, tide, sea_truck, watercraft) for real-time Android status & fallback notices.'}
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

      <form onSubmit={handleSubmit} className="space-y-6">
        {feeds.map((feed) => {
          const Icon = FEED_ICONS[feed.id];
          const isTesting = testingId === feed.id;

          return (
            <div key={feed.id} className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 bg-emerald-50 text-[#006A4E] rounded-lg">
                    <Icon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-base font-semibold text-slate-900">
                      {lang === 'bn' ? feed.sourceNameBn : feed.sourceNameEn}
                    </h3>
                    <p className="text-xs text-slate-500">{feed.provider}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                      feed.status === 'ONLINE'
                        ? 'bg-emerald-100 text-emerald-800'
                        : feed.status === 'DEGRADED'
                        ? 'bg-amber-100 text-amber-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    <Activity className="w-3 h-3" />
                    <span>
                      {feed.status} ({feed.operationalStatus})
                    </span>
                  </span>

                  <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={feed.enabled}
                      onChange={(e) => handleUpdateFeed(feed.id, { enabled: e.target.checked })}
                      className="w-4 h-4 rounded text-[#006A4E] focus:ring-[#006A4E]"
                    />
                    <span>{lang === 'bn' ? 'ফিড সক্রিয়' : 'Enabled'}</span>
                  </label>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn' ? 'ডাটা প্রোভাইডার / সোর্স' : 'Data Provider'}
                  </label>
                  <input
                    type="text"
                    value={feed.provider}
                    onChange={(e) => handleUpdateFeed(feed.id, { provider: e.target.value })}
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn' ? 'অটো-সিঙ্ক বিরতি (মিনিট)' : 'Sync Interval (mins)'}
                  </label>
                  <input
                    type="number"
                    min={5}
                    max={1440}
                    value={feed.autoSyncIntervalMins}
                    onChange={(e) =>
                      handleUpdateFeed(feed.id, {
                        autoSyncIntervalMins: Number(e.target.value) || 60,
                      })
                    }
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn' ? 'স্ট্যাটাস মোড' : 'Operational Status'}
                  </label>
                  <select
                    value={feed.status}
                    onChange={(e) =>
                      handleUpdateFeed(feed.id, {
                        status: e.target.value as FeedDisplayStatus,
                      })
                    }
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  >
                    <option value="ONLINE">ONLINE (operational)</option>
                    <option value="DEGRADED">DEGRADED (delayed)</option>
                    <option value="FALLBACK">FALLBACK (maintenance)</option>
                    <option value="OFFLINE">OFFLINE (suspended)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn'
                      ? 'ফলব্যাক সতর্কতা বার্তা (বাংলা)'
                      : 'Fallback Warning Message (Bengali)'}
                  </label>
                  <input
                    type="text"
                    value={feed.fallbackMessageBn}
                    onChange={(e) =>
                      handleUpdateFeed(feed.id, { fallbackMessageBn: e.target.value })
                    }
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn'
                      ? 'ফলব্যাক সতর্কতা বার্তা (ইংরেজি)'
                      : 'Fallback Warning Message (English)'}
                  </label>
                  <input
                    type="text"
                    value={feed.fallbackMessageEn}
                    onChange={(e) =>
                      handleUpdateFeed(feed.id, { fallbackMessageEn: e.target.value })
                    }
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 text-[11px] text-slate-500 border-t border-slate-100">
                <div className="flex flex-wrap items-center gap-4">
                  <span>
                    {lang === 'bn' ? 'সর্বশেষ সিঙ্ক:' : 'Last Sync:'}{' '}
                    <strong className="text-slate-700 font-mono">{feed.lastSyncAt || '—'}</strong>
                  </span>
                  <span>
                    {lang === 'bn' ? 'ভেরিফিকেশন:' : 'Verification:'}{' '}
                    <strong className="text-emerald-700 font-mono">{feed.verificationStatus}</strong>
                  </span>
                </div>

                <button
                  type="button"
                  onClick={() => handleManualTestSync(feed.id)}
                  disabled={isTesting}
                  className="px-3 py-1 font-semibold text-[#006A4E] bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded transition-colors flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                >
                  <RefreshCw className={`w-3 h-3 ${isTesting ? 'animate-spin' : ''}`} />
                  <span>
                    {isTesting
                      ? lang === 'bn'
                        ? 'যাচাই হচ্ছে...'
                        : 'Checking...'
                      : lang === 'bn'
                      ? 'সোর্স যাচাই ও সিঙ্ক'
                      : 'Verify Collection & Sync'}
                  </span>
                </button>
              </div>
            </div>
          );
        })}

        {canEdit && (
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={saving}
              className="px-6 py-2.5 text-sm font-semibold text-white bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-60 rounded-lg transition-colors flex items-center gap-2 shadow-sm cursor-pointer"
            >
              <Save className="w-4 h-4" />
              <span>
                {saving
                  ? lang === 'bn'
                    ? 'সংরক্ষণ হচ্ছে...'
                    : 'Saving...'
                  : lang === 'bn'
                  ? 'ডাটা সোর্স সেটিংস সংরক্ষণ করুন'
                  : 'Save Data Source Settings'}
              </span>
            </button>
          </div>
        )}
      </form>
    </div>
  );
}
