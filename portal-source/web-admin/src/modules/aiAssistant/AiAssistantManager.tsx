import { useEffect, useState, type FormEvent } from 'react';
import {
  Activity,
  AlertCircle,
  Bot,
  CheckCircle2,
  Cpu,
  HelpCircle,
  MessageSquare,
  Plus,
  RefreshCw,
  Save,
  Send,
  ShieldCheck,
  Sparkles,
  Trash2,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  recordAuditEvent,
  saveGenericEntity,
  subscribeDoc,
} from '../../services/firebase/crudService';

interface KnowledgeBaseItem {
  id: string;
  topicBn: string;
  questionBn: string;
  answerBn: string;
}

interface AiAssistantConfig {
  enabled: boolean;
  model: string;
  welcomeMessageBn: string;
  welcomeMessageEn: string;
  systemInstruction: string;
  suggestedQuestionsBn: string[];
  maxTokensPerResponse: number;
  dailyUserLimit: number;
  emergencyNoticeBn: string;
  knowledgeBase: KnowledgeBaseItem[];
  totalQueriesHandled: number;
  lastTestedAt: string;
}

const DEFAULT_AI_CONFIG: AiAssistantConfig = {
  enabled: true,
  model: 'gemini-4-hydrogen',
  welcomeMessageBn: 'আসসালামু আলাইকুম! আমি জননেতা আশেক উল্লাহ রফিক ডিজিটাল পোর্টালের ভার্চুয়াল অ্যাসিস্ট্যান্ট। মহেশখালী-কুতুবদিয়ার উন্নয়ন প্রকল্প, নাগরিক সেবা বা নোটিশ সংক্রান্ত যেকোনো তথ্যের জন্য আমাকে প্রশ্ন করতে পারেন।',
  welcomeMessageEn: 'Hello! I am the AI Virtual Assistant for the Asheq Ullah Rafiq Digital Portal. How may I assist you with development updates or public services today?',
  systemInstruction:
    'You are the official, polite, and well-informed AI Virtual Assistant for the Asheq Ullah Rafiq Digital Portal (Cox\'s Bazar-2: Maheshkhali and Kutubdia constituencies). Answer in fluent, respectful Bengali (or English if requested). Focus on development projects, public notices, digital citizen services, sea-truck schedules, emergency helplines (333, 999, 16122), and coastal advisories. Never invent facts or endorse unofficial statements.',
  suggestedQuestionsBn: [
    'মাতারবাড়ি গভীর সমুদ্র বন্দর ও বিদ্যুৎ প্রকল্পের বর্তমান অগ্রগতি কতটুকু?',
    'কুতুবদিয়া সী-ট্রাক ও স্পিডবোট ছাড়ার সময়সূচি কি?',
    'অনলাইনে জন্ম নিবন্ধন ও ই-নামজারি করার নিয়ম কি?',
    'মহেশখালী উপজেলা স্বাস্থ্য কমপ্লেক্স ও জরুরি অ্যাম্বুলেন্স নম্বর কি?',
  ],
  maxTokensPerResponse: 800,
  dailyUserLimit: 25,
  emergencyNoticeBn: 'জরুরি পরিস্থিতিতে (পুলিশ ৯৯৯, সরকারি সেবা ৩৩৩, ভূমি সেবা ১৬১২২) সরাসরি ডায়াল করুন।',
  knowledgeBase: [
    {
      id: 'kb_1',
      topicBn: 'উন্নয়ন',
      questionBn: 'মাতারবাড়ি গভীর সমুদ্রবন্দর প্রকল্পের সুবিধা কি?',
      answerBn: 'মাতারবাড়ি গভীর সমুদ্রবন্দর দেশের প্রথম গভীর সমুদ্রবন্দর যা বড় জাহাজ সরাসরি ভেড়ার সুযোগ তৈরি করেছে এবং স্থানীয় অর্থনীতিতে হাজার হাজার কর্মসংস্থান সৃষ্টি করেছে।',
    },
    {
      id: 'kb_2',
      topicBn: 'নাগরিক সেবা',
      questionBn: 'আবেদনপত্রের ট্র্যাকিং নম্বর দিয়ে স্ট্যাটাস কিভাবে দেখব?',
      answerBn: 'মোবাইল অ্যাপের "নাগরিক আবেদন" ট্যাবে আপনার ৮ ডিজিটের ট্র্যাকিং নম্বরটি লিখে সার্চ করলে তাৎক্ষণিক অগ্রগতি ও অ্যাডমিন কমেন্ট দেখা যাবে।',
    },
  ],
  totalQueriesHandled: 0,
  lastTestedAt: '—',
};

export function AiAssistantManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [form, setForm] = useState<AiAssistantConfig>(DEFAULT_AI_CONFIG);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Playground Chat State
  const [testPrompt, setTestPrompt] = useState('');
  const [chatMessages, setChatMessages] = useState<Array<{ sender: 'user' | 'ai'; text: string }>>([
    {
      sender: 'ai',
      text: DEFAULT_AI_CONFIG.welcomeMessageBn,
    },
  ]);
  const [chatLoading, setChatLoading] = useState(false);

  useEffect(() => {
    let active = true;
    const unsub = subscribeDoc(
      'config',
      'ai_assistant',
      (data) => {
        if (!active) return;
        if (data) {
          const nextWelcome =
            typeof data.welcomeMessageBn === 'string' && data.welcomeMessageBn.trim()
              ? data.welcomeMessageBn
              : undefined;
          setForm((prev) => ({
            ...prev,
            ...data,
            knowledgeBase: Array.isArray(data.knowledgeBase) ? data.knowledgeBase : prev.knowledgeBase,
            suggestedQuestionsBn: Array.isArray(data.suggestedQuestionsBn) ? data.suggestedQuestionsBn : prev.suggestedQuestionsBn,
          }));
          if (nextWelcome) {
            setChatMessages((prev) =>
              prev.length === 1 && prev[0].sender === 'ai'
                ? [{ sender: 'ai', text: nextWelcome }]
                : prev
            );
          }
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

  const handleAddKb = () => {
    setForm((prev) => ({
      ...prev,
      knowledgeBase: [
        ...prev.knowledgeBase,
        {
          id: `kb_${Date.now()}`,
          topicBn: 'সাধারণ',
          questionBn: '',
          answerBn: '',
        },
      ],
    }));
  };

  const handleRemoveKb = (id: string) => {
    setForm((prev) => ({
      ...prev,
      knowledgeBase: prev.knowledgeBase.filter((k) => k.id !== id),
    }));
  };

  const handleKbChange = (id: string, field: keyof KnowledgeBaseItem, value: string) => {
    setForm((prev) => ({
      ...prev,
      knowledgeBase: prev.knowledgeBase.map((k) => (k.id === id ? { ...k, [field]: value } : k)),
    }));
  };

  const handleTestChat = async () => {
    const q = testPrompt.trim();
    if (!q) return;

    setChatMessages((prev) => [...prev, { sender: 'user', text: q }]);
    setTestPrompt('');
    setChatLoading(true);

    try {
      const res = await fetch('/api/assistant/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt: q,
          systemInstruction: form.systemInstruction,
        }),
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.error || 'Failed to communicate with AI server');
      }

      setChatMessages((prev) => [...prev, { sender: 'ai', text: json.reply || 'কোনো উত্তর পাওয়া যায়নি।' }]);
      setForm((prev) => ({
        ...prev,
        totalQueriesHandled: prev.totalQueriesHandled + 1,
        lastTestedAt: new Date().toLocaleTimeString(lang === 'bn' ? 'bn-BD' : 'en-US', {
          hour: '2-digit',
          minute: '2-digit',
          hour12: true,
        }),
      }));
    } catch (err) {
      setChatMessages((prev) => [
        ...prev,
        {
          sender: 'ai',
          text: `[সার্ভার ত্রুটি: ${err instanceof Error ? err.message : 'AI রেসপন্স ব্যর্থ'}]`,
        },
      ]);
    } finally {
      setChatLoading(false);
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
        'ai_assistant',
        {
          ...form,
          updatedAt: new Date().toISOString(),
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'এআই স্মার্ট অ্যাসিস্ট্যান্ট কনফিগারেশন'
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'এআই স্মার্ট অ্যাসিস্ট্যান্ট সেটিংস সফলভাবে সংরক্ষিত হয়েছে।'
            : 'AI Smart Assistant settings saved and updated for Android app.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save AI config.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'এআই কনফিগারেশন লোড হচ্ছে...' : 'Loading AI configuration...'}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Bot className="w-6 h-6 text-[#006A4E]" />
            <span>{lang === 'bn' ? 'এআই স্মার্ট অ্যাসিস্ট্যান্ট ব্যবস্থাপনা' : 'AI Smart Assistant Management'}</span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'মোবাইল অ্যাপের ভার্চুয়াল অ্যাসিস্ট্যান্টের পারসোনা, নলেজ বেস, বাংলা স্বাগতম বার্তা ও সার্ভার-সাইড সিক্রেট কনফিগার করুন।'
              : 'Configure Gemini 4 Hydrogen AI Assistant persona, local knowledge-base, welcome messages, and secure server-side limits.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
              form.enabled ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-700'
            }`}
          >
            <Activity className="w-3.5 h-3.5" />
            <span>{form.enabled ? (lang === 'bn' ? 'এআই সক্রিয়' : 'AI Enabled') : (lang === 'bn' ? 'এআই নিষ্ক্রিয়' : 'AI Disabled')}</span>
          </span>
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

      {/* Two-Column Layout: Left Settings, Right Live Interactive Test Playground */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column (8 cols): Config Form */}
        <div className="lg:col-span-7 space-y-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* General Control */}
            <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4">
              <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 flex items-center gap-2">
                <Cpu className="w-4 h-4 text-[#006A4E]" />
                <span>{lang === 'bn' ? 'মডেল ও অ্যাক্টিভেশন' : 'Model & Service Control'}</span>
              </h3>

              <div className="flex items-center justify-between p-3 rounded-lg border border-slate-200 bg-slate-50/50">
                <div>
                  <p className="text-xs font-semibold text-slate-900">
                    {lang === 'bn' ? 'মোবাইল অ্যাপে এআই চালু রাখুন' : 'Enable AI Assistant in App'}
                  </p>
                  <p className="text-[11px] text-slate-500">
                    {lang === 'bn' ? 'বন্ধ করলে অ্যাপে চ্যাটবটের পরিবর্তে সাধারণ সাহায্য দেখাবে।' : 'If disabled, fallback static help will display.'}
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
                  className="w-5 h-5 rounded text-[#006A4E] focus:ring-[#006A4E] cursor-pointer"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn' ? 'নির্ধারিত এআই মডেল' : 'AI Model'}
                  </label>
                  <input
                    type="text"
                    disabled
                    value={form.model}
                    className="w-full px-3 py-1.5 text-xs bg-slate-100 border border-slate-200 rounded text-slate-700 font-mono"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">
                    {lang === 'bn' ? 'ব্যবহারকারী দৈনিক লিমিট' : 'Daily User Limit'}
                  </label>
                  <input
                    type="number"
                    value={form.dailyUserLimit}
                    onChange={(e) => setForm({ ...form, dailyUserLimit: Number(e.target.value) || 20 })}
                    className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
              </div>
            </div>

            {/* Welcome Message & Persona */}
            <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4">
              <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-[#006A4E]" />
                <span>{lang === 'bn' ? 'স্বাগতম বার্তা ও পারসোনা' : 'Welcome Message & Persona'}</span>
              </h3>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {lang === 'bn' ? 'স্বাগতম বার্তা (বাংলা)' : 'Welcome Message (Bengali)'}
                </label>
                <textarea
                  rows={3}
                  value={form.welcomeMessageBn}
                  onChange={(e) => setForm({ ...form, welcomeMessageBn: e.target.value })}
                  className="w-full px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg text-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {lang === 'bn' ? 'সিস্টেম প্রম্পট নির্দেশনা (System Instructions)' : 'System Instructions'}
                </label>
                <textarea
                  rows={4}
                  value={form.systemInstruction}
                  onChange={(e) => setForm({ ...form, systemInstruction: e.target.value })}
                  className="w-full px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg text-slate-900 font-mono text-[11px]"
                />
              </div>
            </div>

            {/* Knowledge Base */}
            <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                <h3 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                  <HelpCircle className="w-4 h-4 text-[#006A4E]" />
                  <span>{lang === 'bn' ? 'লোকাল নলেজ বেস ও প্রশ্নোত্তর' : 'Local Knowledge Base & FAQs'}</span>
                </h3>
                <button
                  type="button"
                  onClick={handleAddKb}
                  className="px-2.5 py-1 text-xs font-semibold text-[#006A4E] bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded transition-colors flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3 h-3" />
                  <span>{lang === 'bn' ? 'প্রশ্নোত্তর যোগ' : 'Add QA'}</span>
                </button>
              </div>

              <div className="space-y-3">
                {form.knowledgeBase.map((item) => (
                  <div key={item.id} className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <input
                        type="text"
                        value={item.topicBn}
                        onChange={(e) => handleKbChange(item.id, 'topicBn', e.target.value)}
                        placeholder="ক্যাটাগরি"
                        className="w-24 px-2 py-1 text-xs bg-white border border-slate-300 rounded font-semibold text-slate-700"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemoveKb(item.id)}
                        className="p-1 text-red-600 hover:bg-red-50 rounded cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    <input
                      type="text"
                      value={item.questionBn}
                      onChange={(e) => handleKbChange(item.id, 'questionBn', e.target.value)}
                      placeholder="প্রশ্ন (বাংলা)"
                      className="w-full px-2.5 py-1 text-xs bg-white border border-slate-300 rounded text-slate-900"
                    />
                    <textarea
                      rows={2}
                      value={item.answerBn}
                      onChange={(e) => handleKbChange(item.id, 'answerBn', e.target.value)}
                      placeholder="সঠিক উত্তর (বাংলা)"
                      className="w-full px-2.5 py-1 text-xs bg-white border border-slate-300 rounded text-slate-900"
                    />
                  </div>
                ))}
              </div>
            </div>

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
                      ? 'এআই কনফিগারেশন সংরক্ষণ করুন'
                      : 'Save AI Config'}
                  </span>
                </button>
              </div>
            )}
          </form>
        </div>

        {/* Right Column (5 cols): Live Interactive Test Playground */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs flex flex-col h-[600px]">
            <div className="border-b border-slate-100 pb-3 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-emerald-700" />
                <h3 className="text-sm font-bold text-slate-900 font-display">
                  {lang === 'bn' ? 'এআই টেস্ট প্লে-গ্রাউন্ড' : 'Live Test Playground'}
                </h3>
              </div>
              <span className="text-[10px] font-mono bg-emerald-50 text-[#006A4E] px-2 py-0.5 rounded border border-emerald-200">
                Server-Proxy
              </span>
            </div>

            {/* Chat message box */}
            <div className="flex-1 overflow-y-auto p-3 space-y-3 bg-slate-50/60 rounded-lg my-3 text-xs border border-slate-100">
              {chatMessages.map((msg, idx) => (
                <div
                  key={idx}
                  className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[85%] rounded-xl px-3.5 py-2.5 leading-relaxed ${
                      msg.sender === 'user'
                        ? 'bg-[#006A4E] text-white rounded-br-none shadow-xs'
                        : 'bg-white text-slate-800 border border-slate-200 rounded-bl-none shadow-xs'
                    }`}
                  >
                    {msg.text}
                  </div>
                </div>
              ))}
              {chatLoading && (
                <div className="flex justify-start">
                  <div className="bg-white text-slate-500 border border-slate-200 rounded-xl px-3 py-2 flex items-center gap-2 text-xs">
                    <span className="w-3.5 h-3.5 border-2 border-[#006A4E] border-t-transparent rounded-full animate-spin" />
                    <span>{lang === 'bn' ? 'এআই উত্তর তৈরি করছে...' : 'Thinking...'}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Suggested quick question chips */}
            <div className="flex flex-wrap gap-1.5 mb-3 max-h-20 overflow-y-auto">
              {form.suggestedQuestionsBn.slice(0, 3).map((sq, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => setTestPrompt(sq)}
                  className="text-[11px] text-slate-600 bg-white hover:bg-emerald-50 border border-slate-200 hover:border-emerald-300 rounded px-2 py-1 text-left truncate max-w-full transition-colors cursor-pointer"
                >
                  {sq}
                </button>
              ))}
            </div>

            {/* Input form */}
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={testPrompt}
                onChange={(e) => setTestPrompt(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleTestChat()}
                placeholder={lang === 'bn' ? 'প্রশ্ন টাইপ করুন...' : 'Type a test prompt...'}
                className="flex-1 px-3 py-2 text-xs bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-1 focus:ring-[#006A4E]"
              />
              <button
                type="button"
                onClick={handleTestChat}
                disabled={chatLoading || !testPrompt.trim()}
                className="p-2 bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-50 text-white rounded-lg transition-colors cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
