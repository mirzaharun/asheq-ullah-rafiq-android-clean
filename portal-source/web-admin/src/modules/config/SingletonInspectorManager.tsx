import { useEffect, useState, type FormEvent } from 'react';
import { AlertCircle, CheckCircle2, Plus, Save, Trash2 } from 'lucide-react';
import { FileUploadField } from '../../components/common/FileUploadField';
import { SchemaNoticeBanner } from '../../components/common/SchemaNoticeBanner';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  saveConfigOrProfileDoc,
  subscribeCollection,
} from '../../services/firebase/crudService';

interface KeyValueEntry {
  key: string;
  value: string;
}

export function SingletonInspectorManager({
  collectionName,
  titleBn,
  titleEn,
  defaultDocId,
  includeProfilePhotoUpload = false,
}: {
  collectionName: 'config' | 'profile_info';
  titleBn: string;
  titleEn: string;
  defaultDocId: string;
  includeProfilePhotoUpload?: boolean;
}) {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [docs, setDocs] = useState<{ id: string; data: Record<string, unknown> }[]>([]);
  const [selectedDocId, setSelectedDocId] = useState(defaultDocId);
  const [hydratedKey, setHydratedKey] = useState<string>('');
  const [entries, setEntries] = useState<KeyValueEntry[]>([]);
  const [photoUrl, setPhotoUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    setSelectedDocId(defaultDocId);
    setHydratedKey('');
    setEntries([]);
    setPhotoUrl('');
    setLoading(true);

    const unsub = subscribeCollection(
      collectionName,
      (id, raw) => ({ id, data: raw }),
      (items) => {
        setDocs(items);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setLoading(false);
      }
    );
    return () => unsub();
  }, [collectionName, defaultDocId]);

  useEffect(() => {
    if (docs.length === 0) return;
    const activeMatch =
      collectionName === 'config' && defaultDocId === 'home' && admin?.role !== 'super_admin'
        ? docs.find((d) => d.id === 'home')
        : docs.find((d) => d.id === selectedDocId) ||
          (hydratedKey === '' ? docs[0] : undefined);
    if (!activeMatch) return;

    const targetKey = `${collectionName}:${activeMatch.id}`;
    if (hydratedKey === targetKey) return;

    setSelectedDocId(activeMatch.id);
    const pairs = Object.entries(activeMatch.data)
      .filter(([k]) => k !== 'photoUrl')
      .map(([k, v]) => ({ key: k, value: String(v ?? '') }));
    setEntries(pairs);
    setPhotoUrl(typeof activeMatch.data.photoUrl === 'string' ? activeMatch.data.photoUrl : '');
    setHydratedKey(targetKey);
  }, [admin?.role, collectionName, defaultDocId, docs, hydratedKey, selectedDocId]);

  const isLockedHomeEditor =
    collectionName === 'config' && defaultDocId === 'home' && admin?.role !== 'super_admin';

  const canWrite =
    collectionName === 'config'
      ? admin?.role === 'super_admin' ||
        (admin?.role === 'editor' && defaultDocId === 'home' && selectedDocId === 'home')
      : admin?.role === 'super_admin' || admin?.role === 'editor';

  const activeDocExists = docs.some((d) => d.id === selectedDocId);

  const handleSelectExistingDoc = (id: string) => {
    if (isLockedHomeEditor && id !== 'home') return;
    setSelectedDocId(id);
    setHydratedKey('');
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canWrite) return;

    const payload: Record<string, string | boolean | number> = {};
    for (const row of entries) {
      const k = row.key.trim();
      if (!k) continue;
      if (row.value === 'true') payload[k] = true;
      else if (row.value === 'false') payload[k] = false;
      else payload[k] = row.value;
    }
    if (includeProfilePhotoUpload && photoUrl.trim()) {
      payload.photoUrl = photoUrl.trim();
    }

    if (Object.keys(payload).length === 0) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'সংরক্ষণের আগে অন্তত একটি বৈধ ফিল্ড যোগ করুন।'
            : 'Add at least one valid field before saving.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveConfigOrProfileDoc({
        collectionName,
        docId: selectedDocId,
        fields: payload,
        actor: admin,
      });
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? `${collectionName}/${selectedDocId} সংরক্ষিত হয়েছে।`
            : `Saved ${collectionName}/${selectedDocId}.`,
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save document.',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 font-display">
          {lang === 'bn' ? titleBn : titleEn}
        </h2>
        <p className="mt-1 text-xs text-slate-500 font-mono">
          {collectionName}/{selectedDocId}
        </p>
      </div>

      {!canWrite ? (
        <SchemaNoticeBanner
          title={
            lang === 'bn'
              ? 'শুধুমাত্র পাঠযোগ্য মোড (Read-Only)'
              : 'Read-Only Access Mode'
          }
          description={
            lang === 'bn'
              ? 'নিরাপত্তা নীতিমালা অনুযায়ী config সংগ্রহ শুধুমাত্র সুপার অ্যাডমিন (এবং home নথি সম্পাদক) পরিবর্তন করতে পারবেন।'
              : 'Per security rules, only a Super Admin can modify config documents (Editors may update config/home).'
          }
        />
      ) : null}

      {!activeDocExists && !loading ? (
        <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-sm text-slate-700">
          <div>
            <p className="font-semibold text-slate-900">
              {lang === 'bn'
                ? `${collectionName}/${defaultDocId} প্রস্তুত করুন`
                : `Initialize ${collectionName}/${defaultDocId}`}
            </p>
            <p className="mt-0.5 text-xs text-slate-600">
              {lang === 'bn'
                ? 'এই নথিতে প্রাথমিক ফিল্ড যুক্ত করে নিচের ফর্ম থেকে সংরক্ষণ করুন।'
                : 'Load starter template fields below or add custom key-value pairs and save.'}
            </p>
          </div>
          {canWrite && entries.length === 0 ? (
            <button
              type="button"
              onClick={() => {
                if (collectionName === 'profile_info') {
                  setEntries([
                    { key: 'nameBn', value: 'আশেক উল্লাহ রফিক' },
                    { key: 'nameEn', value: 'Asheq Ullah Rafiq' },
                    { key: 'constituencyBn', value: 'কক্সবাজার-২ (মহেশখালী-কুতুবদিয়া)' },
                    { key: 'constituencyEn', value: "Cox's Bazar-2 (Maheshkhali-Kutubdia)" },
                    { key: 'bioBn', value: '' },
                    { key: 'bioEn', value: '' },
                  ]);
                } else if (defaultDocId === 'home') {
                  setEntries([
                    { key: 'heroTitleBn', value: 'উন্নয়ন ও জনসেবার ডিজিটাল পোর্টাল' },
                    { key: 'heroTitleEn', value: 'Digital Portal for Development & Public Service' },
                    { key: 'marqueeNoticeBn', value: '' },
                    { key: 'showEmergencyBanner', value: 'true' },
                  ]);
                } else {
                  setEntries([
                    { key: 'portalNameBn', value: 'আশেক উল্লাহ রফিক ডিজিটাল পোর্টাল' },
                    { key: 'portalNameEn', value: 'Asheq Ullah Rafiq Digital Portal' },
                    { key: 'supportEmail', value: 'asheqrafiq@gmail.com' },
                    { key: 'maintenanceMode', value: 'false' },
                  ]);
                }
              }}
              className="px-3.5 py-2 text-xs font-semibold text-blue-700 bg-white border border-blue-200 hover:bg-blue-50 rounded-lg whitespace-nowrap shrink-0 cursor-pointer"
            >
              {lang === 'bn' ? 'টেমপ্লেট ফিল্ড লোড করুন' : 'Load Starter Fields'}
            </button>
          ) : null}
        </div>
      ) : null}

      {docs.length > 1 && !isLockedHomeEditor ? (
        <div className="bg-white border border-slate-200 rounded-xl p-3 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-slate-600">
            {lang === 'bn' ? 'বিদ্যমান নথি:' : 'Existing documents:'}
          </span>
          {docs.map((d) => (
            <button
              key={d.id}
              type="button"
              onClick={() => handleSelectExistingDoc(d.id)}
              className={`px-2.5 py-1 text-xs font-mono rounded-lg border cursor-pointer ${
                d.id === selectedDocId
                  ? 'bg-blue-700 text-white border-blue-700'
                  : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
              }`}
            >
              {d.id}
            </button>
          ))}
        </div>
      ) : null}

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

      <form
        onSubmit={handleSave}
        className="bg-white border border-slate-200 rounded-xl p-6 space-y-5"
      >
        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1">
            {lang === 'bn' ? 'ডকুমেন্ট আইডি' : 'Document ID'}
          </label>
          <input
            type="text"
            value={selectedDocId}
            disabled={!canWrite || isLockedHomeEditor}
            onChange={(e) => setSelectedDocId(e.target.value)}
            className="w-full max-w-xs px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg disabled:bg-slate-100"
          />
        </div>

        {includeProfilePhotoUpload ? (
          <FileUploadField
            label={lang === 'bn' ? 'প্রোফাইল ছবি (photoUrl)' : 'Profile Photograph (photoUrl)'}
            value={photoUrl}
            onChange={setPhotoUrl}
            category="profile"
            accept="image/jpeg,image/png,image/webp"
            helperText="JPG/PNG/WEBP ≤ 5 MB"
          />
        ) : null}

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold text-slate-700">
              {lang === 'bn' ? 'কনফিগারেশন ফিল্ডসমূহ' : 'Document Fields'}
            </h3>
            {canWrite ? (
              <button
                type="button"
                onClick={() =>
                  setEntries((prev) => [...prev, { key: '', value: '' }])
                }
                className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'ফিল্ড যোগ করুন' : 'Add Field'}</span>
              </button>
            ) : null}
          </div>

          {entries.length === 0 ? (
            <p className="text-xs text-slate-500">
              {lang === 'bn'
                ? 'কোনো ফিল্ড যুক্ত করা হয়নি।'
                : 'No fields added yet.'}
            </p>
          ) : (
            <div className="space-y-2">
              {entries.map((row, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <input
                    type="text"
                    placeholder="fieldName"
                    value={row.key}
                    disabled={!canWrite}
                    onChange={(e) => {
                      const next = [...entries];
                      next[idx] = { ...next[idx], key: e.target.value };
                      setEntries(next);
                    }}
                    className="w-1/3 px-3 py-2 text-xs font-mono border border-slate-300 rounded-lg"
                  />
                  <input
                    type="text"
                    placeholder="value"
                    value={row.value}
                    disabled={!canWrite}
                    onChange={(e) => {
                      const next = [...entries];
                      next[idx] = { ...next[idx], value: e.target.value };
                      setEntries(next);
                    }}
                    className="flex-1 px-3 py-2 text-sm border border-slate-300 rounded-lg"
                  />
                  {canWrite ? (
                    <button
                      type="button"
                      onClick={() =>
                        setEntries((prev) => prev.filter((_, i) => i !== idx))
                      }
                      className="p-2 text-slate-500 hover:text-red-700 cursor-pointer"
                      aria-label="Remove field"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </div>

        {canWrite ? (
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
                  ? 'সংরক্ষণ করুন'
                  : 'Save Changes'}
              </span>
            </button>
          </div>
        ) : null}
      </form>
    </div>
  );
}
