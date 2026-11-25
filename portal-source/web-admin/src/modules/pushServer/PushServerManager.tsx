import { useEffect, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  Bell,
  CheckCircle2,
  Copy,
  ExternalLink,
  Flame,
  Radio,
  RefreshCw,
  Save,
  Send,
  ShieldAlert,
  Smartphone,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  recordAuditEvent,
  saveGenericEntity,
  subscribeDoc,
} from '../../services/firebase/crudService';

interface PushServerConfig {
  fcmEnabled: boolean;
  serverAuthMethod: 'FIREBASE_CONSOLE_OR_BLAZE' | 'HTTP_V1_API';
  defaultTopics: string[];
  activeChannelUrgent: string;
  activeChannelGeneral: string;
  lastBroadcastAt: string;
  totalPushesDelivered: number;
}

const DEFAULT_PUSH_CONFIG: PushServerConfig = {
  fcmEnabled: true,
  serverAuthMethod: 'FIREBASE_CONSOLE_OR_BLAZE',
  defaultTopics: [
    'all_users',
    'urgent',
    'general',
    'app_update',
    'development',
    'public_service',
    'events',
    'other',
  ],
  activeChannelUrgent: 'portal_urgent_notices',
  activeChannelGeneral: 'portal_general_notices',
  lastBroadcastAt: '',
  totalPushesDelivered: 0,
};

export function PushServerManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [config, setConfig] = useState<PushServerConfig>(DEFAULT_PUSH_CONFIG);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Payload Generator State
  const [selectedTopic, setSelectedTopic] = useState('all_users');
  const [categoryId, setCategoryId] = useState('development');
  const [title, setTitle] = useState('জরুরী উন্নয়ন বিজ্ঞপ্তি');
  const [body, setBody] = useState('মহেশখালী-কুতুবদিয়া আসনের উন্নয়ন প্রকল্পের নতুন অগ্রগতি হালনাগাদ করা হয়েছে।');
  const [priority, setPriority] = useState<'HIGH' | 'NORMAL'>('HIGH');
  const [destinationPage, setDestinationPage] = useState('development');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let active = true;
    const unsub = subscribeDoc(
      'config',
      'push_server',
      (data) => {
        if (!active) return;
        if (data) {
          setConfig((prev) => ({
            ...prev,
            ...data,
            defaultTopics: Array.isArray(data.defaultTopics) ? data.defaultTopics : prev.defaultTopics,
          }));
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

  const canEdit = admin?.role === 'super_admin';

  const generatedPayload = JSON.stringify(
    {
      message: {
        topic: selectedTopic,
        notification: {
          title,
          body,
        },
        data: {
          categoryId,
          titleBn: title,
          bodyBn: body,
          priority,
          destinationPage,
          externalLink: '',
        },
        android: {
          priority: priority.toLowerCase(),
          notification: {
            channel_id: priority === 'HIGH' ? config.activeChannelUrgent : config.activeChannelGeneral,
            sound: 'default',
            default_sound: true,
            default_vibrate_timings: true,
          },
        },
      },
    },
    null,
    2
  );

  const handleCopyPayload = () => {
    navigator.clipboard.writeText(generatedPayload);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;

    setSaving(true);
    setFeedback(null);
    try {
      await saveGenericEntity(
        'config',
        'push_server',
        {
          ...config,
          lastUpdated: new Date().toISOString(),
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'পুশ নোটিফিকেশন সার্ভার কনফিগারেশন'
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'পুশ নোটিফিকেশন সার্ভার কনফিগারেশন সংরক্ষিত হয়েছে।'
            : 'Push Notification Server configuration saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save push config.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'পুশ কনফিগারেশন লোড হচ্ছে...' : 'Loading push configuration...'}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Radio className="w-6 h-6 text-[#006A4E]" />
            <span>{lang === 'bn' ? 'পুশ নোটিফিকেশন সার্ভার কনফিগারেশন (FCM)' : 'Push Notification Server Config (FCM)'}</span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'ফায়ারবেস ক্লাউড মেসেজিং (FCM) টপিক, নোটিফিকেশন চ্যানেল ও অ্যান্ড্রয়েড ব্যাকগ্রাউন্ড পুশ ব্রডকাস্ট সমন্বয় করুন।'
              : 'Configure FCM push notification topics, channel IDs, deep links, and HTTP v1 payload templates.'}
          </p>
        </div>
      </div>

      {/* Blaze Plan Notice Callout */}
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3 text-xs text-amber-950">
        <Flame className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <p className="font-bold text-amber-900">
            {lang === 'bn'
              ? 'ফায়ারবেস প্ল্যান তথ্য ও সার্ভার-সাইড ক্লাউড ফাংশন প্রয়োজনীয়তা:'
              : 'Firebase Plan Note & Automated Cloud Functions Requirement:'}
          </p>
          <p className="leading-relaxed">
            {lang === 'bn'
              ? 'বর্তমানে প্রকল্পটি ফায়ারবেসের ফ্রি Spark প্ল্যানে চলমান। সরাসরি অ্যাডমিন ব্রাউজার থেকে ক্লাউড সার্ভার কল ছাড়াই নোটিফিকেশন ফায়ারস্টোরে লাইভ সিঙ্ক হয় এবং অ্যান্ড্রয়েড অ্যাপের নোটিফিকেশন সেন্টারে পৌঁছে যায়। তবে ব্যাকগ্রাউন্ডে স্বয়ংক্রিয় পুশ অ্যালার্ট ট্র্রিগারের জন্য ফায়ারবেস কনসোলের "Cloud Messaging" ড্যাশবোর্ড ব্যবহার করা যাবে অথবা পরবর্তীতে Blaze প্ল্যানে ক্লাউড ফাংশন ডিপ্লয় করা যাবে।'
              : 'The project is hosted on the Firebase Spark (free) plan. Live sync to the Android Notification Center occurs in real time via Firestore. Background push broadcasts can be dispatched via the Firebase Console Cloud Messaging dashboard using the payload generator below.'}
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

      {/* Two Column Layout: Left FCM Topic Settings, Right Payload Generator */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-6 space-y-6">
          <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4">
            <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 flex items-center gap-2">
              <Bell className="w-4 h-4 text-[#006A4E]" />
              <span>{lang === 'bn' ? 'অ্যান্ড্রয়েড চ্যানেল ও টপিক ম্যাপিং' : 'Android Channels & FCM Topics'}</span>
            </h3>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">
                  {lang === 'bn' ? 'জরুরি নোটিফিকেশন চ্যানেল আইডি' : 'Urgent Notification Channel ID'}
                </label>
                <input
                  type="text"
                  value={config.activeChannelUrgent}
                  onChange={(e) => setConfig({ ...config, activeChannelUrgent: e.target.value })}
                  className="w-full px-3 py-1.5 font-mono text-xs bg-white border border-slate-300 rounded text-slate-900"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">
                  {lang === 'bn' ? 'সাধারণ নোটিফিকেশন চ্যানেল আইডি' : 'General Notification Channel ID'}
                </label>
                <input
                  type="text"
                  value={config.activeChannelGeneral}
                  onChange={(e) => setConfig({ ...config, activeChannelGeneral: e.target.value })}
                  className="w-full px-3 py-1.5 font-mono text-xs bg-white border border-slate-300 rounded text-slate-900"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">
                  {lang === 'bn' ? 'সাবস্ক্রাইবকৃত সক্রিয় টপিকসমূহ' : 'Active Subscribed Topics'}
                </label>
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {config.defaultTopics.map((topic) => (
                    <span
                      key={topic}
                      className="px-2 py-0.5 rounded bg-emerald-50 text-[#006A4E] border border-emerald-200 font-mono text-[11px]"
                    >
                      {topic}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {canEdit && (
              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 text-xs font-semibold text-white bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-60 rounded-lg transition-colors flex items-center gap-1.5 shadow-sm cursor-pointer"
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>{saving ? (lang === 'bn' ? 'সংরক্ষণ হচ্ছে...' : 'Saving...') : (lang === 'bn' ? 'চ্যানেল কনফিগ সংরক্ষণ' : 'Save Channel Config')}</span>
                </button>
              </div>
            )}
          </form>
        </div>

        {/* Right Column: Live FCM Payload Generator */}
        <div className="lg:col-span-6 bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4 flex flex-col">
          <div className="border-b border-slate-100 pb-2.5 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-[#006A4E]" />
              <span>{lang === 'bn' ? 'FCM পে-লোড জেনারেটর (Firebase Console Test)' : 'Live FCM Payload Generator'}</span>
            </h3>
            <button
              type="button"
              onClick={handleCopyPayload}
              className="px-2.5 py-1 text-xs font-semibold text-[#006A4E] bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded flex items-center gap-1 cursor-pointer transition-colors"
            >
              <Copy className="w-3 h-3" />
              <span>{copied ? (lang === 'bn' ? 'কপি হয়েছে!' : 'Copied!') : (lang === 'bn' ? 'কপি JSON' : 'Copy JSON')}</span>
            </button>
          </div>

          <div className="space-y-3 text-xs">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block font-medium text-slate-700 mb-1">
                  {lang === 'bn' ? 'উদ্দিষ্ট টপিক' : 'Target Topic'}
                </label>
                <select
                  value={selectedTopic}
                  onChange={(e) => setSelectedTopic(e.target.value)}
                  className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                >
                  {config.defaultTopics.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">
                  {lang === 'bn' ? 'ডিপ-লিংক স্ক্রিন' : 'Destination Screen'}
                </label>
                <select
                  value={destinationPage}
                  onChange={(e) => setDestinationPage(e.target.value)}
                  className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                >
                  <option value="home">হোম পেজ (home)</option>
                  <option value="development">উন্নয়ন প্রকল্প (development)</option>
                  <option value="notices">নোটিশ বোর্ড (notices)</option>
                  <option value="digital_services">ডিজিটাল সেবা (digital_services)</option>
                  <option value="education">শিক্ষা ও বৃত্তি (education)</option>
                  <option value="employment">কর্মসংস্থান (employment)</option>
                  <option value="social_welfare">সামাজিক কল্যাণ (social_welfare)</option>
                  <option value="contact">যোগাযোগ (contact)</option>
                  <option value="app_update">অ্যাপ আপডেট স্ক্রিন (app_update)</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'নোটিফিকেশন শিরোনাম' : 'Title'}
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>

            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'বার্তা বিবরণ' : 'Body Text'}
              </label>
              <textarea
                rows={2}
                value={body}
                onChange={(e) => setBody(e.target.value)}
                className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>
          </div>

          <div className="flex-1 bg-slate-950 text-emerald-400 p-3 rounded-lg font-mono text-[11px] overflow-x-auto max-h-56">
            <pre>{generatedPayload}</pre>
          </div>
        </div>
      </div>
    </div>
  );
}
