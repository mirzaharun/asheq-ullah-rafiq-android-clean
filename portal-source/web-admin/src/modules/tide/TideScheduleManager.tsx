import { useEffect, useState, type FormEvent } from 'react';
import { AlertCircle, CheckCircle2, Save } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  saveTideSchedule,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { TideScheduleDoc } from '../../types/firestore';

function mapTide(id: string, raw: Record<string, unknown>): TideScheduleDoc {
  return {
    id,
    highTide1Bn: typeof raw.highTide1Bn === 'string' ? raw.highTide1Bn : '',
    highTide1En: typeof raw.highTide1En === 'string' ? raw.highTide1En : '',
    highTide2Bn: typeof raw.highTide2Bn === 'string' ? raw.highTide2Bn : '',
    highTide2En: typeof raw.highTide2En === 'string' ? raw.highTide2En : '',
    lowTide1Bn: typeof raw.lowTide1Bn === 'string' ? raw.lowTide1Bn : '',
    lowTide1En: typeof raw.lowTide1En === 'string' ? raw.lowTide1En : '',
    lowTide2Bn: typeof raw.lowTide2Bn === 'string' ? raw.lowTide2Bn : '',
    lowTide2En: typeof raw.lowTide2En === 'string' ? raw.lowTide2En : '',
    statusBn: typeof raw.statusBn === 'string' ? raw.statusBn : '',
    statusEn: typeof raw.statusEn === 'string' ? raw.statusEn : '',
    safetyMarqueeBn:
      typeof raw.safetyMarqueeBn === 'string' ? raw.safetyMarqueeBn : '',
    safetyMarqueeEn:
      typeof raw.safetyMarqueeEn === 'string' ? raw.safetyMarqueeEn : '',
    lastUpdated: typeof raw.lastUpdated === 'string' ? raw.lastUpdated : '',
  };
}

export function TideScheduleManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [docId, setDocId] = useState('current');
  const [form, setForm] = useState<Omit<TideScheduleDoc, 'id'>>({
    highTide1Bn: '',
    highTide1En: '',
    highTide2Bn: '',
    highTide2En: '',
    lowTide1Bn: '',
    lowTide1En: '',
    lowTide2Bn: '',
    lowTide2En: '',
    statusBn: '',
    statusEn: '',
    safetyMarqueeBn: '',
    safetyMarqueeEn: '',
    lastUpdated: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    const unsub = subscribeCollection(
      'tide_schedules',
      mapTide,
      (docs) => {
        const target = docs.find((d) => d.id === 'current') || docs[0];
        if (target) {
          setDocId(target.id);
          const { id: _id, ...rest } = target;
          setForm(rest);
        }
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

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.statusBn.trim()) {
      setFeedback({
        type: 'error',
        text: lang === 'bn' ? 'সাগরের অবস্থা (বাংলা) আবশ্যক।' : 'Status (Bn) is required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveTideSchedule(form, docId, admin);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'জোয়ার-ভাটার সময়সূচি সফলভাবে হালনাগাদ হয়েছে।'
            : 'Tide schedule updated.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update tide schedule.',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 font-display">
          {lang === 'bn' ? 'জোয়ার-ভাটার সময়সূচি' : 'Tide Schedules'}
        </h2>
        <p className="mt-1 text-xs text-slate-500 font-mono">
          tide_schedules/{docId}
        </p>
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

      {loading ? (
        <div className="bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
          {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading tide schedule...'}
        </div>
      ) : (
        <form
          onSubmit={handleSave}
          className="bg-white border border-slate-200 rounded-xl p-6 space-y-5"
        >
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সাগরের অবস্থা (বাংলা) *' : 'Sea Status (Bn) *'}
              </label>
              <input
                type="text"
                required
                disabled={!canEdit}
                value={form.statusBn}
                onChange={(e) => setForm({ ...form, statusBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg disabled:bg-slate-50"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সাগরের অবস্থা (ইংরেজি)' : 'Sea Status (En)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.statusEn}
                onChange={(e) => setForm({ ...form, statusEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg disabled:bg-slate-50"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সর্বশেষ হালনাগাদ' : 'Last Updated'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.lastUpdated}
                onChange={(e) => setForm({ ...form, lastUpdated: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg disabled:bg-slate-50"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2 border-t border-slate-100">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '১ম জোয়ার (বাংলা)' : 'High Tide 1 (Bn)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.highTide1Bn}
                onChange={(e) => setForm({ ...form, highTide1Bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '১ম জোয়ার (ইংরেজি)' : 'High Tide 1 (En)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.highTide1En}
                onChange={(e) => setForm({ ...form, highTide1En: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '২য় জোয়ার (বাংলা)' : 'High Tide 2 (Bn)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.highTide2Bn}
                onChange={(e) => setForm({ ...form, highTide2Bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '২য় জোয়ার (ইংরেজি)' : 'High Tide 2 (En)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.highTide2En}
                onChange={(e) => setForm({ ...form, highTide2En: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '১ম ভাটা (বাংলা)' : 'Low Tide 1 (Bn)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.lowTide1Bn}
                onChange={(e) => setForm({ ...form, lowTide1Bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '১ম ভাটা (ইংরেজি)' : 'Low Tide 1 (En)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.lowTide1En}
                onChange={(e) => setForm({ ...form, lowTide1En: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '২য় ভাটা (বাংলা)' : 'Low Tide 2 (Bn)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.lowTide2Bn}
                onChange={(e) => setForm({ ...form, lowTide2Bn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? '২য় ভাটা (ইংরেজি)' : 'Low Tide 2 (En)'}
              </label>
              <input
                type="text"
                disabled={!canEdit}
                value={form.lowTide2En}
                onChange={(e) => setForm({ ...form, lowTide2En: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-slate-100">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সতর্কতা বার্তা (বাংলা)' : 'Safety Marquee (Bn)'}
              </label>
              <textarea
                rows={3}
                disabled={!canEdit}
                value={form.safetyMarqueeBn}
                onChange={(e) => setForm({ ...form, safetyMarqueeBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সতর্কতা বার্তা (ইংরেজি)' : 'Safety Marquee (En)'}
              </label>
              <textarea
                rows={3}
                disabled={!canEdit}
                value={form.safetyMarqueeEn}
                onChange={(e) => setForm({ ...form, safetyMarqueeEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          {canEdit ? (
            <div className="pt-4 border-t border-slate-200 flex justify-end">
              <button
                type="submit"
                disabled={saving}
                className="inline-flex items-center gap-2 px-5 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg disabled:opacity-60 cursor-pointer"
              >
                <Save className="w-4 h-4" />
                <span>
                  {saving
                    ? lang === 'bn'
                      ? 'সংরক্ষণ হচ্ছে...'
                      : 'Saving...'
                    : lang === 'bn'
                    ? 'সময়সূচি সংরক্ষণ করুন'
                    : 'Save Tide Schedule'}
                </span>
              </button>
            </div>
          ) : null}
        </form>
      )}
    </div>
  );
}
