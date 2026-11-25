import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Check,
  FileUp,
  Loader2,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { DEFAULT_MIGRATION_BUNDLES } from '../../data/defaultMigrationData';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import { recordAuditLog } from '../../services/auditService';
import {
  createDoc,
  fetchCollection,
  fetchSingletonDoc,
  formatFirestoreError,
  removeDoc,
  upsertDoc,
} from '../../services/firestoreCrud';
import {
  buildBlankTemplate,
  formatRecordForEditor,
  parseEditorJson,
  validateModuleRecord,
} from '../../services/moduleValidation';
import {
  formatFileSizeMb,
  isAllowedCmsMimeType,
  uploadCmsAsset,
} from '../../services/storageService';
import { AdminModuleDefinition } from '../../types/portal';

type ModuleRecord = Record<string, unknown> & { id: string };

function getRecordTitle(record: ModuleRecord): string {
  for (const key of [
    'titleBn',
    'titleEn',
    'nameBn',
    'name',
    'routeTitleBn',
    'questionBn',
    'platformNameBn',
    'unionNameBn',
  ]) {
    if (typeof record[key] === 'string' && record[key]) return record[key] as string;
  }
  return record.id;
}

function getRecordSearchText(record: ModuleRecord): string {
  return Object.values(record)
    .filter((value) => typeof value === 'string' || typeof value === 'number')
    .join(' ')
    .toLocaleLowerCase();
}

function getBlankModuleTemplate(module: AdminModuleDefinition): string {
  const bundle = DEFAULT_MIGRATION_BUNDLES.find(
    (item) => item.moduleKey === module.key
  );
  const sample = bundle?.records[0]?.data;
  if (sample) return buildBlankTemplate(sample);

  if (module.collection === 'applications' || module.collection === 'appointments') {
    return JSON.stringify(
      { userId: '', name: '', phone: '', message: '', status: 'Pending' },
      null,
      2
    );
  }
  if (module.collection === 'citizen_grievances') {
    return JSON.stringify(
      {
        userId: '',
        fullName: '',
        phone: '',
        subject: '',
        details: '',
        status: 'Pending',
      },
      null,
      2
    );
  }
  if (module.collection === 'general_feedback') {
    return JSON.stringify({ userId: '', name: '', phone: '', message: '' }, null, 2);
  }
  if (module.collection === 'admins') {
    return JSON.stringify(
      { uid: '', role: 'editor', active: true, email: '', displayName: '' },
      null,
      2
    );
  }
  return '{}';
}

export const ModuleCrudEditor: React.FC<{
  module: AdminModuleDefinition;
}> = ({ module }) => {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();
  const [records, setRecords] = useState<ModuleRecord[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [original, setOriginal] = useState<ModuleRecord | null>(null);
  const [draftJson, setDraftJson] = useState('');
  const [published, setPublished] = useState(false);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [assetField, setAssetField] = useState('imageUrl');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const isAuditLog = module.collection === 'audit_logs';
  const isCitizenIntake = module.group === 'citizen';
  const isSuperAdmin = adminUser?.role === 'super_admin';
  const canWrite =
    !isAuditLog &&
    (module.group === 'cms' ||
      (module.collection === 'admins' && isSuperAdmin) ||
      (isCitizenIntake &&
        (module.collection !== 'general_feedback' || isSuperAdmin)));
  const canCreate =
    canWrite &&
    !isCitizenIntake &&
    (module.collection !== 'admins' || isSuperAdmin);
  const canDelete =
    canWrite &&
    (isSuperAdmin ||
      (module.group === 'cms' &&
        module.collection !== 'config' &&
        module.collection !== 'profile_info'));
  const canPublish = module.group === 'cms' && module.storageKind === 'collection';
  const canUpload = module.group === 'cms';

  const refreshRecords = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (module.storageKind === 'singleton' && module.singletonDocId) {
        const record = await fetchSingletonDoc<Record<string, unknown>>(
          module.collection,
          module.singletonDocId
        );
        setRecords(record ? [record as ModuleRecord] : []);
      } else {
        const items = await fetchCollection<ModuleRecord>(module.collection);
        setRecords(items);
      }
    } catch (err) {
      setError(formatFirestoreError(err, lang));
    } finally {
      setLoading(false);
    }
  }, [lang, module]);

  useEffect(() => {
    setSelectedId('');
    setOriginal(null);
    setDraftJson('');
    setMessage(null);
    void refreshRecords();
  }, [module, refreshRecords]);

  const visibleRecords = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase();
    return needle
      ? records.filter((record) => getRecordSearchText(record).includes(needle))
      : records;
  }, [records, search]);

  const openRecord = (record: ModuleRecord) => {
    setSelectedId(record.id);
    setOriginal(record);
    setDraftJson(formatRecordForEditor(record));
    setPublished(record.isPublished !== false);
    setError(null);
    setMessage(null);
  };

  const startCreate = () => {
    const id = module.singletonDocId ?? '';
    setSelectedId(id);
    setOriginal(null);
    setDraftJson(getBlankModuleTemplate(module));
    setPublished(false);
    setError(null);
    setMessage(null);
  };

  const saveRecord = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!adminUser || !canWrite) return;
    setSaving(true);
    setError(null);
    setMessage(null);

    try {
      const docId = module.storageKind === 'singleton'
        ? module.singletonDocId ?? selectedId
        : selectedId.trim();
      const creating = original === null;
      let rawJson = draftJson;
      if (creating) {
        const draft = parseEditorJson(rawJson);
        if (draft) {
          if ('id' in draft) draft.id = docId;
          if (module.collection === 'admins') draft.uid = docId;
          rawJson = JSON.stringify(draft);
        }
      }
      const payload = validateModuleRecord(module, docId, rawJson, original);
      if (module.collection === 'admins' && docId === adminUser.uid) {
        const currentActive = original?.active;
        if (payload.active === false && currentActive !== false) {
          throw new Error('You cannot deactivate your own administrator account.');
        }
      }
      if (isCitizenIntake && !isSuperAdmin) {
        if (!original) {
          throw new Error('Citizen submissions cannot be created from the admin panel.');
        }
        const editableFields =
          module.collection === 'applications' || module.collection === 'appointments'
            ? new Set(['status', 'adminNote', 'scheduledDate'])
            : new Set(['status', 'adminNote']);
        const fields = new Set([
          ...Object.keys(original),
          ...Object.keys(payload),
        ]);
        for (const field of fields) {
          if (
            !editableFields.has(field) &&
            JSON.stringify(original[field]) !== JSON.stringify(payload[field])
          ) {
            throw new Error(
              `Application managers may only update ${Array.from(editableFields).join(', ')}.`
            );
            }
        }
      }
      if (canPublish) payload.isPublished = published;
      if (module.collection === 'admins') payload.uid = docId;

      if (creating) {
        await createDoc(module.collection, docId, payload);
      } else {
        await upsertDoc(module.collection, docId, payload, true);
      }
      if (!creating && module.mirrorCollection) {
        const mirror = await fetchSingletonDoc(module.mirrorCollection, docId);
        if (mirror) {
          await upsertDoc(module.mirrorCollection, docId, payload, true);
        }
      }

      let auditWarning: string | null = null;
      try {
        await recordAuditLog({
          actorUid: adminUser.uid,
          actorEmail: adminUser.email,
          action: creating ? 'CREATE' : 'UPDATE',
          targetCollection: module.collection,
          targetId: docId,
        });
      } catch (auditError) {
        auditWarning = formatFirestoreError(auditError, lang);
      }

      await refreshRecords();
      const saved = await fetchSingletonDoc<Record<string, unknown>>(
        module.collection,
        docId
      );
      if (saved) openRecord(saved as ModuleRecord);
      setMessage(
        auditWarning
          ? t(
              `তথ্য সংরক্ষিত হয়েছে, তবে অডিট লগ লেখা যায়নি: ${auditWarning}`,
              `Record saved, but the audit log could not be written: ${auditWarning}`
            )
          : t('তথ্য সংরক্ষণ ও অডিট লগ সম্পন্ন হয়েছে।', 'Record saved and audit logged.')
      );
    } catch (err) {
      setError(formatFirestoreError(err, lang));
    } finally {
      setSaving(false);
    }
  };

  const deleteRecord = async () => {
    if (!adminUser || !original || !canDelete) return;
    const confirmed = window.confirm(
      t(
        `/${module.collection}/${original.id} স্থায়ীভাবে মুছে ফেলবেন?`,
        `Permanently delete /${module.collection}/${original.id}?`
      )
    );
    if (!confirmed) return;

    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await removeDoc(module.collection, original.id);
      if (module.mirrorCollection) {
        const mirror = await fetchSingletonDoc(module.mirrorCollection, original.id);
        if (mirror) await removeDoc(module.mirrorCollection, original.id);
      }

      let auditWarning: string | null = null;
      try {
        await recordAuditLog({
          actorUid: adminUser.uid,
          actorEmail: adminUser.email,
          action: 'DELETE',
          targetCollection: module.collection,
          targetId: original.id,
        });
      } catch (auditError) {
        auditWarning = formatFirestoreError(auditError, lang);
      }
      setOriginal(null);
      setSelectedId('');
      setDraftJson('');
      await refreshRecords();
      setMessage(
        auditWarning
          ? t(
              `রেকর্ড মুছে ফেলা হয়েছে, তবে অডিট লগ লেখা যায়নি: ${auditWarning}`,
              `Record deleted, but the audit log could not be written: ${auditWarning}`
            )
          : t('রেকর্ড মুছে ফেলা ও অডিট লগ সম্পন্ন হয়েছে।', 'Record deleted and audit logged.')
      );
    } catch (err) {
      setError(formatFirestoreError(err, lang));
    } finally {
      setSaving(false);
    }
  };

  const uploadAsset = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !canUpload) return;
    if (!isAllowedCmsMimeType(file.type, module.key, assetField)) {
      setError(
        t(
          'এই ফাইলের ধরনটি এই ফিল্ডে অনুমোদিত নয়।',
          'This file type is not allowed for the selected field.'
        )
      );
      return;
    }
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(assetField.trim())) {
      setError(t('ফিল্ডের নাম বৈধ নয়।', 'Enter a valid field name.'));
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setError(null);
    try {
      const result = await uploadCmsAsset(
        module.key,
        file,
        setUploadProgress,
        assetField.trim()
      );
      const current = parseEditorJson(draftJson) ?? {};
      current[assetField.trim()] = result.downloadUrl;
      setDraftJson(JSON.stringify(current, null, 2));
      setMessage(
        t(
          `${file.name} (${formatFileSizeMb(file.size)}) আপলোড হয়েছে; সংরক্ষণ করতে রেকর্ডটি সেভ করুন।`,
          `${file.name} (${formatFileSizeMb(file.size)}) uploaded. Save the record to publish its URL.`
        )
      );
      if (adminUser) {
        try {
          await recordAuditLog({
            actorUid: adminUser.uid,
            actorEmail: adminUser.email,
            action: 'UPLOAD_ASSET',
            targetCollection: module.collection,
            targetId: selectedId || result.storagePath,
          });
        } catch (auditError) {
          setError(
            t(
              `ফাইল আপলোড হয়েছে, তবে অডিট লগ লেখা যায়নি: ${formatFirestoreError(auditError, lang)}`,
              `File uploaded, but the audit log could not be written: ${formatFirestoreError(auditError, lang)}`
            )
          );
        }
      }
    } catch (err) {
      setError(formatFirestoreError(err, lang));
    } finally {
      setUploading(false);
    }
  };

  const editorJson = parseEditorJson(draftJson);

  return (
    <div className="space-y-4">
      {error && (
        <div
          role="alert"
          className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
        >
          {error}
        </div>
      )}
      {message && (
        <div
          role="status"
          className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          {message}
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)] gap-4">
        <section className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
          <div className="p-4 border-b border-slate-100 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h3 className="font-bold text-slate-900">
                  {t('রেকর্ড', 'Records')} ({records.length})
                </h3>
                <p className="text-xs text-slate-500">
                  /{module.collection}
                  {module.singletonDocId ? `/${module.singletonDocId}` : ''}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => void refreshRecords()}
                  disabled={loading}
                  aria-label={t('রিফ্রেশ', 'Refresh')}
                  className="rounded-lg border border-slate-200 p-2 text-slate-600 hover:bg-slate-50 disabled:opacity-50"
                >
                  {loading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <RefreshCw className="h-4 w-4" />
                  )}
                </button>
                {canCreate && module.storageKind === 'collection' && (
                  <button
                    type="button"
                    onClick={startCreate}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-800 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700"
                  >
                    <Plus className="h-4 w-4" />
                    {t('নতুন', 'Create')}
                  </button>
                )}
              </div>
            </div>
            {module.storageKind === 'collection' && (
              <label className="relative block">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder={t('রেকর্ড খুঁজুন…', 'Search records…')}
                  className="w-full rounded-xl border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none focus:border-emerald-600"
                />
              </label>
            )}
          </div>
          <div className="max-h-[34rem] overflow-auto divide-y divide-slate-100">
            {loading ? (
              <div className="p-8 flex justify-center">
                <Loader2 className="h-5 w-5 animate-spin text-emerald-700" />
              </div>
            ) : visibleRecords.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500">
                {t('কোনো রেকর্ড নেই।', 'No records found.')}
              </div>
            ) : (
              visibleRecords.map((record) => (
                <button
                  key={record.id}
                  type="button"
                  onClick={() => openRecord(record)}
                  className={`flex w-full items-start justify-between gap-3 p-4 text-left hover:bg-slate-50 ${
                    selectedId === record.id ? 'bg-emerald-50/70' : ''
                  }`}
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-semibold text-slate-900">
                      {getRecordTitle(record)}
                    </span>
                    <span className="mt-1 block truncate font-mono text-[11px] text-slate-500">
                      {record.id}
                    </span>
                  </span>
                  <span className="flex shrink-0 items-center gap-2">
                    {record.isPublished === false && (
                      <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-800">
                        {t('খসড়া', 'Draft')}
                      </span>
                    )}
                    <Pencil className="h-4 w-4 text-slate-400" />
                  </span>
                </button>
              ))
            )}
          </div>
        </section>

        <section className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5">
          {!draftJson ? (
            <div className="flex min-h-56 flex-col items-center justify-center text-center">
              <Pencil className="h-7 w-7 text-slate-300" />
              <p className="mt-3 text-sm font-semibold text-slate-700">
                {t('একটি রেকর্ড নির্বাচন করুন', 'Select a record to edit')}
              </p>
              <p className="mt-1 max-w-sm text-xs text-slate-500">
                {t(
                  'নতুন CMS রেকর্ড তৈরি করতে বাম পাশের Create বোতাম ব্যবহার করুন।',
                  'Use Create to add a new CMS record.'
                )}
              </p>
              {canCreate && module.storageKind === 'singleton' && records.length === 0 && (
                <button
                  type="button"
                  onClick={startCreate}
                  className="mt-4 inline-flex items-center gap-2 rounded-lg bg-emerald-800 px-3 py-2 text-xs font-semibold text-white"
                >
                  <Plus className="h-4 w-4" />
                  {t('সিঙ্গেলটন তৈরি করুন', 'Create singleton')}
                </button>
              )}
            </div>
          ) : (
            <form onSubmit={saveRecord} className="space-y-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="font-bold text-slate-900">
                    {original
                      ? t('রেকর্ড সম্পাদনা', 'Edit record')
                      : t('নতুন রেকর্ড', 'Create record')}
                  </h3>
                  <p className="text-xs text-slate-500">
                    {module.storageKind === 'singleton'
                      ? `${module.collection}/${module.singletonDocId}`
                      : t('Firestore document ID', 'Firestore document ID')}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setDraftJson('');
                    setOriginal(null);
                    setSelectedId('');
                  }}
                  aria-label={t('সম্পাদনা বন্ধ করুন', 'Close editor')}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {module.storageKind === 'collection' && (
                <label className="block space-y-1">
                  <span className="text-xs font-semibold text-slate-700">
                    Document ID
                  </span>
                  <input
                    value={selectedId}
                    onChange={(event) => setSelectedId(event.target.value)}
                    disabled={Boolean(original)}
                    required
                    maxLength={128}
                    pattern="[A-Za-z0-9_-]{1,128}"
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-sm outline-none focus:border-emerald-600 disabled:bg-slate-50"
                  />
                </label>
              )}

              {canPublish && (
                <label className="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm">
                  <input
                    type="checkbox"
                    checked={published}
                    onChange={(event) => setPublished(event.target.checked)}
                    className="h-4 w-4 accent-emerald-700"
                  />
                  <span className="font-medium text-slate-800">
                    {published
                      ? t('প্রকাশিত', 'Published')
                      : t('খসড়া — অ্যাপে দেখানো হবে না', 'Draft — hidden from the app')}
                  </span>
                </label>
              )}

              {canUpload && (
                <div className="rounded-xl border border-sky-200 bg-sky-50/60 p-3 space-y-2.5">
                  <div className="flex items-center gap-2 text-xs font-semibold text-sky-900">
                    <FileUp className="h-4 w-4" />
                    {t('Firebase Storage ফাইল আপলোড', 'Firebase Storage upload')}
                  </div>
                  <div className="flex flex-col sm:flex-row gap-2">
                    <input
                      value={assetField}
                      onChange={(event) => setAssetField(event.target.value)}
                      aria-label={t('ফাইল URL ফিল্ড', 'File URL field')}
                      placeholder="imageUrl"
                      className="min-w-0 flex-1 rounded-lg border border-sky-200 bg-white px-3 py-2 text-xs"
                    />
                    <label className="inline-flex cursor-pointer items-center justify-center gap-2 rounded-lg border border-sky-300 bg-white px-3 py-2 text-xs font-semibold text-sky-900 hover:bg-sky-100">
                      {uploading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <FileUp className="h-4 w-4" />
                      )}
                      {uploading ? `${uploadProgress}%` : t('ফাইল বাছুন', 'Choose file')}
                      <input
                        type="file"
                        disabled={uploading}
                        accept="image/*,video/*,application/pdf,.doc,.docx,.odt,.txt,.apk"
                        onChange={(event) => void uploadAsset(event)}
                        className="sr-only"
                      />
                    </label>
                  </div>
                  <p className="text-[11px] text-sky-800">
                    {t(
                      'ছবি, ভিডিও, PDF ও ডকুমেন্ট; সর্বোচ্চ ৫০ MB। APK কেবল config-এর apkUrl ফিল্ডে।',
                      'Images, video, PDF and documents; max 50 MB. APKs are restricted to config/apkUrl.'
                    )}
                  </p>
                </div>
              )}

              <label className="block space-y-1">
                <span className="text-xs font-semibold text-slate-700">
                  {t('ডকুমেন্ট ডেটা (JSON)', 'Document data (JSON)')}
                </span>
                <textarea
                  value={draftJson}
                  onChange={(event) => setDraftJson(event.target.value)}
                  required
                  spellCheck={false}
                  rows={18}
                  className="w-full resize-y rounded-xl border border-slate-300 bg-slate-950 p-3 font-mono text-xs leading-relaxed text-emerald-50 outline-none focus:ring-2 focus:ring-emerald-600/30"
                />
                {!editorJson && (
                  <span className="text-[11px] text-amber-700">
                    {t('JSON অবজেক্ট বৈধ না হওয়া পর্যন্ত সংরক্ষণ করা যাবে না।', 'Enter a valid JSON object before saving.')}
                  </span>
                )}
              </label>

              {canWrite && (
                <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3">
                  <div>
                    {original && canDelete && (
                      <button
                        type="button"
                        onClick={() => void deleteRecord()}
                        disabled={saving}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50"
                      >
                        <Trash2 className="h-4 w-4" />
                        {t('মুছুন', 'Delete')}
                      </button>
                    )}
                  </div>
                  <button
                    type="submit"
                    disabled={saving || uploading || !editorJson || !selectedId.trim()}
                    className="inline-flex items-center gap-2 rounded-lg bg-emerald-800 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {saving ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Save className="h-4 w-4" />
                    )}
                    {t('সংরক্ষণ', 'Save')}
                  </button>
                </div>
              )}
              {!canWrite && (
                <p className="flex items-center gap-2 border-t border-slate-100 pt-3 text-xs text-slate-500">
                  <Check className="h-4 w-4 text-emerald-700" />
                  {t('এই মডিউলটি কেবল পাঠযোগ্য।', 'This module is read-only.')}
                </p>
              )}
            </form>
          )}
        </section>
      </div>
    </div>
  );
};
