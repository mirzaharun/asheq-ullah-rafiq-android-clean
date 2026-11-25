import { useEffect, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  MessageSquare,
  Save,
  Send,
  ShieldAlert,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  generateDocId,
  saveGenericEntity,
  subscribeCollection,
  subscribeDoc,
} from '../../services/firebase/crudService';
import { formatFirestoreTimestamp } from '../../utils/formatters';

interface SmsTemplate {
  key: string;
  labelBn: string;
  templateBn: string;
  templateEn: string;
}

interface SmsGatewayConfig {
  enabled: boolean;
  provider: 'TELETALK' | 'GRAMEENPHONE' | 'SSL_WIRELESS' | 'CUSTOM_WEBHOOK';
  senderId: string;
  apiUrl: string;
  dailySmsLimit: number;
  monthlyQuota: number;
  smsSentToday: number;
  templates: SmsTemplate[];
  lastTestedAt: string;
}

interface SmsLogItem {
  id: string;
  recipient: string;
  templateKey: string;
  content: string;
  status: 'DELIVERED' | 'FAILED' | 'QUEUED' | 'QUEUED_TEST';
  timestampLabel: string;
  sortTime: number;
}

const DEFAULT_SMS_CONFIG: SmsGatewayConfig = {
  enabled: false, // Must remain disabled by default until configured
  provider: 'SSL_WIRELESS',
  senderId: 'AR_PORTAL',
  apiUrl: 'https://smsplus.sslwireless.com/api/v3/send-sms',
  dailySmsLimit: 500,
  monthlyQuota: 10000,
  smsSentToday: 0,
  lastTestedAt: '—',
  templates: [
    {
      key: 'application_resolved',
      labelBn: 'আবেদন নিষ্পত্তি নোটিশ',
      templateBn:
        'শ্রদ্ধেয় নাগরিক, আপনার আবেদনপত্র {TRACKING_ID} সফলভাবে নিষ্পত্তিকৃত হয়েছে। বিস্তারিত জানতে মোবাইল অ্যাপের আবেদন মেনু দেখুন। - আশেক উল্লাহ রফিক ডিজিটাল পোর্টাল',
      templateEn:
        'Dear Citizen, your request {TRACKING_ID} has been successfully resolved. Please check the portal app for details. - Asheq Ullah Rafiq Digital Portal',
    },
    {
      key: 'appointment_confirmed',
      labelBn: 'সাক্ষাৎকার নিশ্চিতকরণ',
      templateBn:
        'আপনার সাক্ষাৎকারের অনুরোধ অনুমোদিত হয়েছে। তারিখ: {DATE}, সময়: {TIME}, স্থান: সংসদীয় কার্যালয়, মহেশখালী। - আশেক উল্লাহ রফিক সংসদীয় কার্যালয়',
      templateEn:
        'Your appointment is confirmed for {DATE} at {TIME} at Maheshkhali Parliamentary Office.',
    },
    {
      key: 'emergency_alert',
      labelBn: 'জরুরি উপকূলীয় দুর্যোগ সতর্কবার্তা',
      templateBn:
        'মহেশখালী-কুতুবদিয়া জরুরি সতর্কবার্তা: সাগরে {SIGNAL_NO} নম্বর সতর্ক সংকেত জারি। সকল নৌযানকে নিরাপদে থাকার নির্দেশ দেওয়া হচ্ছে। - দুর্যোগ কন্ট্রোল রুম',
      templateEn:
        'Coastal Emergency Alert: Signal {SIGNAL_NO} in effect. All marine watercraft are advised to take shelter.',
    },
  ],
};

function mapSmsLogDoc(id: string, raw: Record<string, unknown>): SmsLogItem {
  const statusRaw = typeof raw.status === 'string' ? raw.status.toUpperCase() : 'QUEUED';
  const status: SmsLogItem['status'] =
    statusRaw === 'DELIVERED' ||
    statusRaw === 'FAILED' ||
    statusRaw === 'QUEUED' ||
    statusRaw === 'QUEUED_TEST'
      ? statusRaw
      : 'QUEUED';

  const tsRaw = raw.createdAt ?? raw.timestamp;
  const sortTime =
    typeof tsRaw === 'string'
      ? Date.parse(tsRaw) || 0
      : tsRaw && typeof tsRaw === 'object' && 'toMillis' in tsRaw
      ? Number((tsRaw as { toMillis: () => number }).toMillis())
      : 0;
  const timestampLabel =
    typeof raw.timestamp === 'string'
      ? raw.timestamp
      : formatFirestoreTimestamp(tsRaw as any, 'en') || '—';

  return {
    id,
    recipient: typeof raw.recipient === 'string' ? raw.recipient : '—',
    templateKey: typeof raw.templateKey === 'string' ? raw.templateKey : 'manual_test',
    content: typeof raw.content === 'string' ? raw.content : '',
    status,
    timestampLabel,
    sortTime,
  };
}

export function SmsGatewayManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [config, setConfig] = useState<SmsGatewayConfig>(DEFAULT_SMS_CONFIG);
  const [logs, setLogs] = useState<SmsLogItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [logsLoading, setLogsLoading] = useState(true);
  const [logsError, setLogsError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [testNumber, setTestNumber] = useState('');
  const [testMessage, setTestMessage] = useState('');
  const [testing, setTesting] = useState(false);

  useEffect(() => {
    let active = true;
    const unsubConfig = subscribeDoc(
      'config',
      'sms_gateway',
      (data) => {
        if (!active) return;
        if (data) {
          setConfig((prev) => ({
            ...prev,
            ...data,
            templates: Array.isArray(data.templates) ? data.templates : prev.templates,
          }));
        }
        setLoading(false);
      },
      () => {
        if (active) setLoading(false);
      }
    );

    const unsubLogs = subscribeCollection(
      'sms_logs',
      mapSmsLogDoc,
      (items) => {
        if (!active) return;
        setLogs([...items].sort((a, b) => b.sortTime - a.sortTime));
        setLogsLoading(false);
        setLogsError(null);
      },
      (msg) => {
        if (!active) return;
        setLogs([]);
        setLogsError(msg);
        setLogsLoading(false);
      }
    );

    return () => {
      active = false;
      unsubConfig();
      unsubLogs();
    };
  }, []);

  const canEdit = admin?.role === 'super_admin';

  const handleTemplateChange = (key: string, field: 'templateBn' | 'templateEn', value: string) => {
    setConfig((prev) => ({
      ...prev,
      templates: prev.templates.map((t) => (t.key === key ? { ...t, [field]: value } : t)),
    }));
  };

  const handleTestSend = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit || !testNumber.trim() || !testMessage.trim()) return;

    if (!config.enabled) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'এসএমএস গেটওয়ে বর্তমানে নিষ্ক্রিয় রয়েছে। অনুগ্রহ করে আগে গেটওয়ে সক্রিয় করুন।'
            : 'SMS Gateway is currently disabled. Please enable it first.',
      });
      return;
    }

    setTesting(true);
    setFeedback(null);
    const nowIso = new Date().toISOString();
    const logId = generateDocId('sms');

    try {
      await saveGenericEntity(
        'sms_logs',
        logId,
        {
          id: logId,
          recipient: testNumber.trim().slice(0, 32),
          templateKey: 'admin_test_dispatch',
          content: testMessage.trim().slice(0, 1000),
          provider: config.provider,
          status: 'QUEUED_TEST',
          timestamp: nowIso,
          createdByUid: admin.uid,
        },
        true,
        admin,
        `টেস্ট এসএমএস রেকর্ড (${testNumber.trim()})`
      );

      await saveGenericEntity(
        'config',
        'sms_gateway',
        {
          ...config,
          smsSentToday: (Number(config.smsSentToday) || 0) + 1,
          lastTestedAt: nowIso,
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'এসএমএস টেস্ট রেকর্ড হালনাগাদ'
      );

      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `টেস্ট এসএমএস রেকর্ডটি sms_logs-এ সংরক্ষিত হয়েছে (${testNumber.trim()})। Spark প্ল্যানে সরাসরি টেলিকম ডেলিভারির জন্য আপনার সার্ভার/গেটওয়ে ওয়েবহুক প্রসেসিং প্রয়োজন হবে।`
            : `Test SMS payload logged to sms_logs (${testNumber.trim()}) as QUEUED_TEST.`,
      });
      setTestNumber('');
      setTestMessage('');
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to log test SMS.',
      });
    } finally {
      setTesting(false);
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
        'sms_gateway',
        {
          ...config,
          lastUpdated: new Date().toISOString(),
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'এসএমএস গেটওয়ে কনফিগারেশন'
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'এসএমএস গেটওয়ে সেটিংস সফলভাবে সংরক্ষিত হয়েছে।'
            : 'SMS Gateway configuration saved successfully.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save SMS gateway settings.',
      });
    } finally {
      setSaving(false);
    }
  };

  const calculateUnicodeSmsParts = (text: string) => {
    const len = text.length;
    if (len === 0) return { parts: 0, chars: 0 };
    if (len <= 70) return { parts: 1, chars: len };
    return { parts: Math.ceil(len / 67), chars: len };
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'এসএমএস গেটওয়ে লোড হচ্ছে...' : 'Loading SMS gateway...'}
      </div>
    );
  }

  const { parts, chars } = calculateUnicodeSmsParts(testMessage);

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <MessageSquare className="w-6 h-6 text-[#006A4E]" />
            <span>
              {lang === 'bn' ? 'এসএমএস গেটওয়ে ইন্টিগ্রেশন (ঐচ্ছিক)' : 'SMS Gateway Integration'}
            </span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'নাগরিক আবেদন নিষ্পত্তি, সাক্ষাৎকার অনুমোদন ও জরুরি দুর্যোগ সতর্কতা এসএমএস টেমপ্লেট, কোটা ও ফায়ারস্টোর ডেলিভারি লগ (sms_logs) পরিচালনা করুন।'
              : 'Configure national SMS gateway providers, templates, character limits, and real Firestore sms_logs records.'}
          </p>
        </div>

        <span
          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
            config.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900'
          }`}
        >
          {config.enabled ? (
            <>
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700" />
              <span>{lang === 'bn' ? 'গেটওয়ে সক্রিয়' : 'Gateway Enabled'}</span>
            </>
          ) : (
            <>
              <ShieldAlert className="w-3.5 h-3.5 text-amber-700" />
              <span>
                {lang === 'bn' ? 'গেটওয়ে নিষ্ক্রিয় (নিরাপদ মোড)' : 'Disabled (Safety Default)'}
              </span>
            </>
          )}
        </span>
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

      {/* Gateway Configuration */}
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center justify-between">
            <span>
              {lang === 'bn' ? 'গেটওয়ে সংযোগ ও প্রোভাইডার সেটিংস' : 'Provider Settings'}
            </span>
            <label className="flex items-center gap-2 text-xs font-semibold text-slate-800 cursor-pointer">
              <input
                type="checkbox"
                checked={config.enabled}
                onChange={(e) => setConfig({ ...config, enabled: e.target.checked })}
                className="w-4 h-4 rounded text-[#006A4E] focus:ring-[#006A4E]"
              />
              <span>{lang === 'bn' ? 'এসএমএস সার্ভিস চালু রাখুন' : 'Enable SMS Gateway'}</span>
            </label>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'এসএমএস প্রোভাইডার' : 'SMS Provider'}
              </label>
              <select
                value={config.provider}
                onChange={(e) => setConfig({ ...config, provider: e.target.value as any })}
                className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              >
                <option value="SSL_WIRELESS">SSL Wireless (Enterprise Gateway)</option>
                <option value="TELETALK">Teletalk (Government Portal Gateway)</option>
                <option value="GRAMEENPHONE">Grameenphone Bulk SMS</option>
                <option value="CUSTOM_WEBHOOK">Custom HTTPS Webhook</option>
              </select>
            </div>
            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রেরক আইডি (Sender ID / Masking)' : 'Sender ID (Masking)'}
              </label>
              <input
                type="text"
                value={config.senderId}
                onChange={(e) => setConfig({ ...config, senderId: e.target.value })}
                className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900 font-mono"
              />
            </div>
            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'দৈনিক সর্বোচ্চ লিমিট (ডেইলি থ্রটল)' : 'Daily Quota Limit'}
              </label>
              <input
                type="number"
                value={config.dailySmsLimit}
                onChange={(e) =>
                  setConfig({ ...config, dailySmsLimit: Number(e.target.value) || 100 })
                }
                className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              {lang === 'bn' ? 'এপিআই এন্ডপয়েন্ট ইউআরএল (API Endpoint URL)' : 'Gateway API Endpoint'}
            </label>
            <input
              type="text"
              value={config.apiUrl}
              onChange={(e) => setConfig({ ...config, apiUrl: e.target.value })}
              className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900 font-mono"
            />
          </div>
        </div>

        {/* Templates */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3">
            {lang === 'bn' ? 'স্বয়ংক্রিয় এসএমএস টেমপ্লেটসমূহ' : 'Automated SMS Templates'}
          </h3>

          <div className="space-y-4">
            {config.templates.map((tpl) => (
              <div key={tpl.key} className="p-4 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-900">{tpl.labelBn}</span>
                  <span className="text-[10px] font-mono bg-slate-200 text-slate-700 px-2 py-0.5 rounded">
                    {tpl.key}
                  </span>
                </div>
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    {lang === 'bn' ? 'টেমপ্লেট টেক্সট (বাংলা - ইউনিকোড)' : 'Template Text (Bengali)'}
                  </label>
                  <textarea
                    rows={2}
                    value={tpl.templateBn}
                    onChange={(e) => handleTemplateChange(tpl.key, 'templateBn', e.target.value)}
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {canEdit && (
          <div className="flex justify-end">
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
                  ? 'এসএমএস কনফিগ সংরক্ষণ করুন'
                  : 'Save SMS Gateway Config'}
              </span>
            </button>
          </div>
        )}
      </form>

      {/* Queue Test Entry & Real Firestore Delivery Logs */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        <div className="md:col-span-5 bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 mb-3 flex items-center gap-2">
            <Send className="w-4 h-4 text-[#006A4E]" />
            <span>
              {lang === 'bn' ? 'টেস্ট এসএমএস কিউ ও লগ রেকর্ডার' : 'Test SMS Queue & Logger'}
            </span>
          </h3>

          <form onSubmit={handleTestSend} className="space-y-3 text-xs">
            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রাপকের মোবাইল নম্বর' : 'Recipient Phone Number'}
              </label>
              <input
                type="text"
                value={testNumber}
                onChange={(e) => setTestNumber(e.target.value)}
                placeholder="01819000000"
                className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded text-slate-900"
              />
            </div>

            <div>
              <label className="block font-medium text-slate-700 mb-1">
                {lang === 'bn' ? 'এসএমএস বডি' : 'Message Body'}
              </label>
              <textarea
                rows={3}
                value={testMessage}
                onChange={(e) => setTestMessage(e.target.value)}
                placeholder="টেস্ট বার্তা..."
                className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded text-slate-900"
              />
              <div className="flex justify-between items-center text-[10px] text-slate-500 mt-1">
                <span>
                  {chars} অক্ষর · {parts}টি এসএমএস (ইউনিকোড)
                </span>
                <span>৭০ অক্ষরে ১টি এসএমএস</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={!canEdit || testing || !testNumber.trim() || !testMessage.trim()}
              className="w-full py-2 bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-50 text-white font-semibold rounded transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Send className="w-3.5 h-3.5" />
              <span>
                {testing
                  ? lang === 'bn'
                    ? 'সংরক্ষণ হচ্ছে...'
                    : 'Logging...'
                  : lang === 'bn'
                  ? 'টেস্ট এসএমএস লগ করুন'
                  : 'Queue Test SMS Record'}
              </span>
            </button>
          </form>
        </div>

        {/* Delivery Logs */}
        <div className="md:col-span-7 bg-white border border-slate-200 rounded-xl p-5 shadow-xs flex flex-col">
          <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 mb-3 flex items-center justify-between">
            <span>{lang === 'bn' ? 'এসএমএস ডেলিভারি লগ (sms_logs)' : 'SMS Delivery Logs'}</span>
            <span className="text-[11px] font-mono text-slate-500">
              {logs.length} {lang === 'bn' ? 'টি রেকর্ড' : 'records'}
            </span>
          </h3>

          {logsError ? (
            <div className="p-4 bg-red-50 border border-red-200 rounded text-xs text-red-700">
              {logsError}
            </div>
          ) : logsLoading ? (
            <div className="py-8 text-center text-xs text-slate-500">
              {lang === 'bn' ? 'লগ লোড হচ্ছে...' : 'Loading logs...'}
            </div>
          ) : logs.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-500">
              {lang === 'bn'
                ? 'এখনো কোনো এসএমএস ডেলিভারি লগ সংরক্ষিত হয়নি।'
                : 'No SMS delivery logs recorded yet.'}
            </div>
          ) : (
            <div className="flex-1 overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-slate-600">
                    <th className="py-2 px-2.5">প্রাপক</th>
                    <th className="py-2 px-2.5">বার্তা</th>
                    <th className="py-2 px-2.5">স্ট্যাটাস</th>
                    <th className="py-2 px-2.5">সময়</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {logs.map((log) => (
                    <tr key={log.id}>
                      <td className="py-2 px-2.5 font-mono text-slate-800">{log.recipient}</td>
                      <td className="py-2 px-2.5 text-slate-600 truncate max-w-[180px]">
                        {log.content}
                      </td>
                      <td className="py-2 px-2.5">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                            log.status === 'DELIVERED'
                              ? 'bg-emerald-100 text-emerald-800'
                              : log.status === 'FAILED'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {log.status}
                        </span>
                      </td>
                      <td className="py-2 px-2.5 font-mono text-slate-500 whitespace-nowrap">
                        {log.timestampLabel}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
