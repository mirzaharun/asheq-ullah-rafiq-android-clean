import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Check, Loader2, Pencil, Plus, RefreshCw, Save, Search, Trash2, X } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import { recordAuditLog } from '../../services/auditService';
import { createDoc, fetchCollection, fetchSingletonDoc, formatFirestoreError, removeDoc, upsertDoc } from '../../services/firestoreCrud';
import { formatRecordForEditor, parseEditorJson, validateModuleRecord } from '../../services/moduleValidation';
import type { AdminModuleDefinition } from '../../types/portal';

type CmsRecord = Record<string, unknown> & { id: string };
type SaveMode = 'draft' | 'publish';

function isPublished(record: CmsRecord): boolean {
  return record.isPublished !== false && record.published !== false;
}

function recordTitle(record: CmsRecord): string {
  for (const key of ['titleBn', 'titleEn', 'nameBn', 'name', 'questionBn', 'routeTitleBn', 'unionNameBn']) {
    if (typeof record[key] === 'string' && record[key]) return record[key] as string;
  }
  return record.id;
}

/** Generic CRUD editor for the existing Firestore module registry. Firestore Rules remain authoritative. */
export function IntegratedModuleCrudEditor({ module }: { module: AdminModuleDefinition }) {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();
  const [records, setRecords] = useState<CmsRecord[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [original, setOriginal] = useState<CmsRecord | null>(null);
  const [json, setJson] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const isAuditLog = module.collection === 'audit_logs';
  const isCitizen = module.group === 'citizen';
  const isSuperAdmin = adminUser?.role === 'super_admin';
  const canWrite = Boolean(adminUser) && !isAuditLog && (
    module.group === 'cms' ||
    (module.collection === 'admins' && isSuperAdmin) ||
    (isCitizen && (module.collection !== 'general_feedback' || isSuperAdmin))
  );
  const canCreate = canWrite && !isCitizen && module.collection !== 'admins';
  const canDelete = canWrite && (isSuperAdmin || (module.group === 'cms' && !['config', 'profile_info'].includes(module.collection)));
  const canPublish = module.group === 'cms' && module.storageKind === 'collection';

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (module.storageKind === 'singleton' && module.singletonDocId) {
        const record = await fetchSingletonDoc<Record<string, unknown>>(module.collection, module.singletonDocId);
        setRecords(record ? [record as CmsRecord] : []);
      } else {
        setRecords(await fetchCollection<CmsRecord>(module.collection));
      }
    } catch (reason) {
      setError(formatFirestoreError(reason, lang));
    } finally {
      setLoading(false);
    }
  }, [lang, module]);

  useEffect(() => {
    setSelectedId('');
    setOriginal(null);
    setJson('');
    setMessage(null);
    void refresh();
  }, [refresh]);

  const visibleRecords = useMemo(() => {
    const needle = search.trim().toLocaleLowerCase();
    return needle ? records.filter((record) => JSON.stringify(record).toLocaleLowerCase().includes(needle)) : records;
  }, [records, search]);

  const selectRecord = (record: CmsRecord) => {
    setSelectedId(record.id);
    setOriginal(record);
    setJson(formatRecordForEditor(record));
    setError(null);
    setMessage(null);
  };

  const startCreate = () => {
    setSelectedId(module.singletonDocId ?? '');
    setOriginal(null);
    setJson(JSON.stringify({}, null, 2));
    setError(null);
    setMessage(null);
  };

  const save = async (event: React.SyntheticEvent, mode: SaveMode) => {
    event.preventDefault();
    if (!adminUser || !canWrite) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    let saved = false;
    try {
      const id = module.storageKind === 'singleton' ? (module.singletonDocId ?? selectedId) : selectedId.trim();
      const payload = validateModuleRecord(module, id, json, original);
      if (module.collection === 'admins') payload.uid = id;
      if (canPublish) {
        payload.isPublished = mode === 'publish';
        payload.published = mode === 'publish';
      }
      if (isCitizen && !isSuperAdmin && original) {
        const editable = module.collection === 'applications' || module.collection === 'appointments'
          ? new Set(['status', 'adminNote', 'scheduledDate'])
          : new Set(['status', 'adminNote']);
        for (const key of new Set([...Object.keys(original), ...Object.keys(payload)])) {
          if (!editable.has(key) && JSON.stringify(original[key]) !== JSON.stringify(payload[key])) {
            throw new Error(`Application managers may only update ${Array.from(editable).join(', ')}.`);
          }
        }
      }
      if (original) await upsertDoc(module.collection, id, payload, true);
      else await createDoc(module.collection, id, payload);
      saved = true;
      if (module.mirrorCollection) {
        const mirror = await fetchSingletonDoc(module.mirrorCollection, id);
        if (mirror) await upsertDoc(module.mirrorCollection, id, payload, true);
      }
      await recordAuditLog({ actorUid: adminUser.uid, actorEmail: adminUser.email, action: original ? (mode === 'publish' ? 'PUBLISH' : 'UPDATE') : (mode === 'publish' ? 'CREATE_AND_PUBLISH' : 'CREATE_DRAFT'), targetCollection: module.collection, targetId: id });
      await refresh();
      const persisted = await fetchSingletonDoc<Record<string, unknown>>(module.collection, id);
      if (persisted) selectRecord(persisted as CmsRecord);
      setMessage(mode === 'publish' ? t('রেকর্ড সংরক্ষণ ও প্রকাশিত হয়েছে।', 'Record saved and published.') : t('রেকর্ড খসড়া হিসেবে সংরক্ষিত হয়েছে।', 'Record saved as a draft.'));
    } catch (reason) {
      const detail = formatFirestoreError(reason, lang);
      setError(saved
        ? t(`রেকর্ড সংরক্ষিত হয়েছে, কিন্তু পরবর্তী কাজ ব্যর্থ: ${detail}`, `Record was saved, but a follow-up action failed: ${detail}`)
        : detail);
      if (saved) await refresh();
    } finally {
      setSaving(false);
    }
  };

  const deleteSelected = async () => {
    if (!adminUser || !original || !canDelete) return;
    if (!window.confirm(t(`/${module.collection}/${original.id} মুছে ফেলবেন?`, `Delete /${module.collection}/${original.id}?`))) return;
    setSaving(true);
    setError(null);
    try {
      await removeDoc(module.collection, original.id);
      if (module.mirrorCollection) {
        const mirror = await fetchSingletonDoc(module.mirrorCollection, original.id);
        if (mirror) await removeDoc(module.mirrorCollection, original.id);
      }
      await recordAuditLog({ actorUid: adminUser.uid, actorEmail: adminUser.email, action: 'DELETE', targetCollection: module.collection, targetId: original.id });
      setOriginal(null);
      setSelectedId('');
      setJson('');
      await refresh();
      setMessage(t('রেকর্ড মুছে ফেলা হয়েছে।', 'Record deleted.'));
    } catch (reason) {
      setError(formatFirestoreError(reason, lang));
      await refresh();
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="space-y-4" aria-label={lang === 'bn' ? 'মডিউল CRUD' : 'Module CRUD'}>
      {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error}</div>}
      {message && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{message}</div>}
      <div className="grid gap-4 xl:grid-cols-2">
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div className="flex items-center justify-between gap-2 border-b border-slate-100 p-4">
            <div><h3 className="font-bold text-slate-900">{t('রেকর্ড', 'Records')} ({records.length})</h3><p className="font-mono text-xs text-slate-500">/{module.collection}</p></div>
            <div className="flex gap-2">
              <button type="button" onClick={() => void refresh()} disabled={loading} className="rounded-lg border p-2" aria-label={t('রিফ্রেশ', 'Refresh')}>{loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}</button>
              {canCreate && module.storageKind === 'collection' && <button type="button" onClick={startCreate} className="inline-flex items-center gap-1 rounded-lg bg-emerald-800 px-3 py-2 text-xs font-semibold text-white"><Plus className="h-4 w-4" />{t('নতুন', 'Create')}</button>}
            </div>
          </div>
          {module.storageKind === 'collection' && <label className="relative block p-3"><Search className="absolute left-6 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('রেকর্ড খুঁজুন…', 'Search records…')} className="w-full rounded-lg border py-2 pl-9 pr-3 text-sm" /></label>}
          <div className="max-h-[32rem] divide-y divide-slate-100 overflow-auto">
            {visibleRecords.map((record) => <button key={record.id} type="button" onClick={() => selectRecord(record)} className={`flex w-full items-center justify-between gap-2 p-3 text-left hover:bg-slate-50 ${selectedId === record.id ? 'bg-emerald-50' : ''}`}><span className="min-w-0"><span className="block truncate text-sm font-medium">{recordTitle(record)}</span><span className="font-mono text-[11px] text-slate-500">{record.id}</span></span><span className="flex items-center gap-2">{canPublish && <span className={`rounded-full px-2 py-0.5 text-[10px] ${isPublished(record) ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{isPublished(record) ? t('প্রকাশিত', 'Published') : t('খসড়া', 'Draft')}</span>}<Pencil className="h-4 w-4 text-slate-400" /></span></button>)}
            {!loading && visibleRecords.length === 0 && <p className="p-6 text-center text-sm text-slate-500">{t('কোনো রেকর্ড নেই।', 'No records found.')}</p>}
          </div>
        </section>
        <section className="rounded-2xl border border-slate-200 bg-white p-4">
          {!json ? <div className="flex min-h-48 flex-col items-center justify-center text-center text-sm text-slate-500"><Pencil className="mb-2 h-6 w-6" />{t('সম্পাদনা করতে একটি রেকর্ড নির্বাচন করুন।', 'Select a record to edit.')}{canCreate && module.storageKind === 'singleton' && records.length === 0 && <button type="button" onClick={startCreate} className="mt-3 rounded-lg bg-emerald-800 px-3 py-2 text-xs font-semibold text-white">{t('সিঙ্গেলটন তৈরি', 'Create singleton')}</button>}</div> : <form onSubmit={(event) => void save(event, 'draft')} className="space-y-3"><div className="flex items-center justify-between"><h3 className="font-bold">{original ? t('রেকর্ড সম্পাদনা', 'Edit record') : t('নতুন রেকর্ড', 'New record')}</h3><button type="button" onClick={() => { setJson(''); setSelectedId(''); setOriginal(null); }} aria-label={t('বন্ধ', 'Close')}><X className="h-4 w-4" /></button></div>{module.storageKind === 'collection' && <label className="block text-xs font-semibold">Document ID<input required disabled={Boolean(original)} pattern="[A-Za-z0-9_-]{1,128}" maxLength={128} value={selectedId} onChange={(event) => setSelectedId(event.target.value)} className="mt-1 w-full rounded-lg border px-3 py-2 font-mono text-sm" /></label>}<label className="block text-xs font-semibold">{t('ডকুমেন্ট ডেটা (JSON)', 'Document data (JSON)')}<textarea required rows={16} spellCheck={false} value={json} onChange={(event) => setJson(event.target.value)} className="mt-1 w-full rounded-xl border border-slate-300 bg-slate-950 p-3 font-mono text-xs leading-relaxed text-emerald-50" /></label>{canWrite && <div className="flex flex-wrap justify-between gap-2 border-t pt-3">{original && canDelete ? <button type="button" onClick={() => void deleteSelected()} disabled={saving} className="inline-flex items-center gap-1 rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-700"><Trash2 className="h-4 w-4" />{t('মুছুন', 'Delete')}</button> : <span /> }<div className="flex flex-wrap gap-2"><button type="submit" disabled={saving || !parseEditorJson(json) || !selectedId.trim()} className="inline-flex items-center gap-1 rounded-lg border px-3 py-2 text-xs font-semibold disabled:opacity-50">{saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}{t('খসড়া সংরক্ষণ', 'Save draft')}</button>{canPublish && <button type="button" disabled={saving || !parseEditorJson(json) || !selectedId.trim()} onClick={(event) => void save(event, 'publish')} className="inline-flex items-center gap-1 rounded-lg bg-emerald-800 px-3 py-2 text-xs font-semibold text-white disabled:opacity-50"><Check className="h-4 w-4" />{t('প্রকাশ করুন', 'Publish')}</button>}</div></div>}</form>}
        </section>
      </div>
    </section>
  );
}
