import { useEffect, useState, type FormEvent } from 'react';
import { AlertCircle, CheckCircle2, Edit3, Plus, Trash2 } from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  deleteWatercraftSchedule,
  generateDocId,
  saveWatercraftSchedule,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { WatercraftScheduleDoc } from '../../types/firestore';
import { toLocalizedNumber } from '../../utils/formatters';

function mapSchedule(id: string, raw: Record<string, unknown>): WatercraftScheduleDoc {
  return {
    id: typeof raw.id === 'string' ? raw.id : id,
    routeGroup: typeof raw.routeGroup === 'string' ? raw.routeGroup : '',
    routeTitleBn: typeof raw.routeTitleBn === 'string' ? raw.routeTitleBn : '',
    routeTitleEn: typeof raw.routeTitleEn === 'string' ? raw.routeTitleEn : '',
    vesselTypeBn: typeof raw.vesselTypeBn === 'string' ? raw.vesselTypeBn : '',
    vesselTypeEn: typeof raw.vesselTypeEn === 'string' ? raw.vesselTypeEn : '',
    timeSlotsBn: Array.isArray(raw.timeSlotsBn)
      ? raw.timeSlotsBn.filter((s): s is string => typeof s === 'string')
      : [],
    timeSlotsEn: Array.isArray(raw.timeSlotsEn)
      ? raw.timeSlotsEn.filter((s): s is string => typeof s === 'string')
      : [],
    officialNoticeTextBn:
      typeof raw.officialNoticeTextBn === 'string' ? raw.officialNoticeTextBn : '',
    officialNoticeTextEn:
      typeof raw.officialNoticeTextEn === 'string' ? raw.officialNoticeTextEn : '',
    originDeparture: Boolean(raw.originDeparture),
    order: typeof raw.order === 'number' ? raw.order : 1,
  };
}

export function WatercraftSchedulesManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [schedules, setSchedules] = useState<WatercraftScheduleDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [editorOpen, setEditorOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [form, setForm] = useState<WatercraftScheduleDoc>({
    id: '',
    routeGroup: 'MAHESHKHALI_COXSBAZAR',
    routeTitleBn: '',
    routeTitleEn: '',
    vesselTypeBn: 'স্পিডবোট',
    vesselTypeEn: 'Speedboat',
    timeSlotsBn: [],
    timeSlotsEn: [],
    officialNoticeTextBn: '',
    officialNoticeTextEn: '',
    originDeparture: false,
    order: 1,
  });
  const [slotsBnText, setSlotsBnText] = useState('');
  const [slotsEnText, setSlotsEnText] = useState('');
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<WatercraftScheduleDoc | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const unsub = subscribeCollection(
      'watercraft_schedules',
      mapSchedule,
      (items) => {
        setSchedules([...items].sort((a, b) => a.order - b.order));
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
  const canDelete = admin?.role === 'super_admin';

  const openCreate = () => {
    setIsNew(true);
    setForm({
      id: generateDocId('ROUTE'),
      routeGroup: 'MAHESHKHALI_COXSBAZAR',
      routeTitleBn: '',
      routeTitleEn: '',
      vesselTypeBn: 'স্পিডবোট',
      vesselTypeEn: 'Speedboat',
      timeSlotsBn: [],
      timeSlotsEn: [],
      officialNoticeTextBn: '',
      officialNoticeTextEn: '',
      originDeparture: false,
      order: schedules.length + 1,
    });
    setSlotsBnText('');
    setSlotsEnText('');
    setFeedback(null);
    setEditorOpen(true);
  };

  const openEdit = (item: WatercraftScheduleDoc) => {
    setIsNew(false);
    setForm({ ...item });
    setSlotsBnText(item.timeSlotsBn.join('\n'));
    setSlotsEnText(item.timeSlotsEn.join('\n'));
    setFeedback(null);
    setEditorOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;
    if (!form.routeTitleBn.trim() || !form.vesselTypeBn.trim()) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'রুটের নাম এবং নৌযানের ধরন আবশ্যক।'
            : 'Route title and vessel type are required.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      const payload: WatercraftScheduleDoc = {
        ...form,
        timeSlotsBn: slotsBnText
          .split(/\r?\n|,/)
          .map((s) => s.trim())
          .filter(Boolean),
        timeSlotsEn: slotsEnText
          .split(/\r?\n|,/)
          .map((s) => s.trim())
          .filter(Boolean),
      };
      await saveWatercraftSchedule(payload, isNew, admin);
      setEditorOpen(false);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'নৌযান সূচি সংরক্ষিত হয়েছে।' : 'Schedule saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save schedule.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!admin || !deleteTarget || !canDelete) return;
    setDeleting(true);
    try {
      await deleteWatercraftSchedule(
        deleteTarget.id,
        deleteTarget.routeTitleBn || deleteTarget.routeTitleEn,
        admin
      );
      setDeleteTarget(null);
      setFeedback({
        type: 'success',
        text: lang === 'bn' ? 'নৌযান সূচি মুছে ফেলা হয়েছে।' : 'Schedule deleted.',
      });
    } catch (err) {
      setDeleteTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to delete schedule.',
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
            {lang === 'bn' ? 'সী-ট্রাক ও নৌযান সূচি' : 'Sea-Truck & Watercraft Schedules'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            watercraft_schedules · {toLocalizedNumber(schedules.length, lang)}{' '}
            {lang === 'bn' ? 'টি রুট' : 'routes'}
          </p>
        </div>

        {canEdit ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন রুট যুক্ত করুন' : 'Add Route'}</span>
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

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {loading ? (
          <div className="col-span-full bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading schedules...'}
          </div>
        ) : schedules.length === 0 ? (
          <div className="col-span-full bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'কোনো সময়সূচি পাওয়া যায়নি।' : 'No schedules found.'}
          </div>
        ) : (
          schedules.map((s) => (
            <div
              key={s.id}
              className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col justify-between gap-4"
            >
              <div>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-mono text-slate-500">
                      {s.id} · #{toLocalizedNumber(s.order, lang)}
                    </p>
                    <h3 className="mt-0.5 text-base font-bold text-slate-900">
                      {lang === 'bn' ? s.routeTitleBn : s.routeTitleEn || s.routeTitleBn}
                    </h3>
                    <p className="text-xs text-emerald-700 font-medium mt-0.5">
                      {lang === 'bn' ? s.vesselTypeBn : s.vesselTypeEn || s.vesselTypeBn} ·{' '}
                      {s.routeGroup}
                    </p>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {canEdit ? (
                      <button
                        type="button"
                        onClick={() => openEdit(s)}
                        className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                        aria-label="Edit schedule"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                    ) : null}
                    {canDelete ? (
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(s)}
                        className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                        aria-label="Delete schedule"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : null}
                  </div>
                </div>

                <div className="mt-3 pt-3 border-t border-slate-100">
                  <p className="text-xs font-semibold text-slate-700 mb-1.5">
                    {lang === 'bn' ? 'নির্ধারিত সময়সূচি:' : 'Departure Slots:'}
                  </p>
                  <p className="text-xs text-slate-700 leading-relaxed tabular-nums">
                    {(lang === 'bn' ? s.timeSlotsBn : s.timeSlotsEn).join(' · ') || '—'}
                  </p>
                </div>

                {s.officialNoticeTextBn || s.officialNoticeTextEn ? (
                  <p className="mt-3 text-xs text-slate-500 leading-relaxed">
                    {lang === 'bn' ? s.officialNoticeTextBn : s.officialNoticeTextEn}
                  </p>
                ) : null}
              </div>
            </div>
          ))
        )}
      </div>

      <Modal
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        maxWidthClass="max-w-2xl"
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন নৌযান সূচি যুক্ত করুন'
              : 'Add Watercraft Schedule'
            : lang === 'bn'
            ? 'নৌযান সূচি সম্পাদনা'
            : 'Edit Watercraft Schedule'
        }
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'রুট আইডি *' : 'Route ID *'}
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
                {lang === 'bn' ? 'রুট গ্রুপ' : 'Route Group'}
              </label>
              <input
                type="text"
                value={form.routeGroup}
                onChange={(e) => setForm({ ...form, routeGroup: e.target.value })}
                className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ক্রম (Order)' : 'Order'}
              </label>
              <input
                type="number"
                min={0}
                value={form.order}
                onChange={(e) => setForm({ ...form, order: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'রুটের নাম (বাংলা) *' : 'Route Title (Bn) *'}
              </label>
              <input
                type="text"
                required
                value={form.routeTitleBn}
                onChange={(e) => setForm({ ...form, routeTitleBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'রুটের নাম (ইংরেজি)' : 'Route Title (En)'}
              </label>
              <input
                type="text"
                value={form.routeTitleEn}
                onChange={(e) => setForm({ ...form, routeTitleEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'নৌযানের ধরন (বাংলা) *' : 'Vessel Type (Bn) *'}
              </label>
              <input
                type="text"
                required
                value={form.vesselTypeBn}
                onChange={(e) => setForm({ ...form, vesselTypeBn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'নৌযানের ধরন (ইংরেজি)' : 'Vessel Type (En)'}
              </label>
              <input
                type="text"
                value={form.vesselTypeEn}
                onChange={(e) => setForm({ ...form, vesselTypeEn: e.target.value })}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2 text-xs font-medium text-slate-800 cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.originDeparture}
                  onChange={(e) =>
                    setForm({ ...form, originDeparture: e.target.checked })
                  }
                  className="w-4 h-4 rounded border-slate-300 text-blue-700"
                />
                <span>{lang === 'bn' ? 'মূল ঘাট হতে যাত্রা' : 'Origin Departure'}</span>
              </label>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn'
                  ? 'সময়সূচি (বাংলা — প্রতি লাইনে একটি)'
                  : 'Time Slots (Bn — one per line)'}
              </label>
              <textarea
                rows={4}
                value={slotsBnText}
                onChange={(e) => setSlotsBnText(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn'
                  ? 'সময়সূচি (ইংরেজি — প্রতি লাইনে একটি)'
                  : 'Time Slots (En — one per line)'}
              </label>
              <textarea
                rows={4}
                value={slotsEnText}
                onChange={(e) => setSlotsEnText(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'দাপ্তরিক বিজ্ঞপ্তি (বাংলা)' : 'Official Notice (Bn)'}
              </label>
              <textarea
                rows={2}
                value={form.officialNoticeTextBn}
                onChange={(e) =>
                  setForm({ ...form, officialNoticeTextBn: e.target.value })
                }
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'দাপ্তরিক বিজ্ঞপ্তি (ইংরেজি)' : 'Official Notice (En)'}
              </label>
              <textarea
                rows={2}
                value={form.officialNoticeTextEn}
                onChange={(e) =>
                  setForm({ ...form, officialNoticeTextEn: e.target.value })
                }
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
                : 'Save Schedule'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={lang === 'bn' ? 'নৌযান সূচি মুছুন' : 'Confirm Schedule Deletion'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${deleteTarget?.routeTitleBn || deleteTarget?.routeTitleEn}" মুছে ফেলতে চান?`
            : `Delete route "${deleteTarget?.routeTitleEn || deleteTarget?.routeTitleBn}"?`
        }
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
