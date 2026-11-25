import { useEffect, useMemo, useState } from 'react';
import { Search, ShieldAlert, ShieldCheck } from 'lucide-react';
import { Pagination } from '../../components/common/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import { subscribeAuditLogFeed } from '../../services/firebase/auditService';
import type { AuditLogRecord } from '../../types/firestore';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

const PAGE_SIZE = 15;

export function AuditLogsViewer() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [logs, setLogs] = useState<AuditLogRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [collectionFilter, setCollectionFilter] = useState('ALL');
  const [page, setPage] = useState(1);

  useEffect(() => {
    if (admin?.role !== 'super_admin') {
      setLoading(false);
      return;
    }

    const unsub = subscribeAuditLogFeed(
      200,
      (items) => {
        setLogs(items);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setLoading(false);
      }
    );
    return () => unsub();
  }, [admin?.role]);

  const collections = useMemo(
    () =>
      Array.from(
        new Set(
          logs
            .map((l) => l.targetCollection)
            .filter((v): v is string => Boolean(v))
        )
      ),
    [logs]
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return logs.filter((l) => {
      const matchesCol =
        collectionFilter === 'ALL' || l.targetCollection === collectionFilter;
      const matchesQuery =
        !q ||
        l.action.toLowerCase().includes(q) ||
        (l.actorEmail ?? '').toLowerCase().includes(q) ||
        (l.summaryBn ?? '').toLowerCase().includes(q) ||
        (l.summaryEn ?? '').toLowerCase().includes(q) ||
        (l.targetDocId ?? '').toLowerCase().includes(q);
      return matchesCol && matchesQuery;
    });
  }, [logs, search, collectionFilter]);

  const paginated = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filtered.slice(start, start + PAGE_SIZE);
  }, [filtered, page]);

  useEffect(() => {
    setPage(1);
  }, [search, collectionFilter]);

  if (admin?.role !== 'super_admin') {
    return (
      <div className="max-w-xl bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex items-center gap-2.5 text-amber-700 font-semibold">
          <ShieldAlert className="w-5 h-5" />
          <span>
            {lang === 'bn'
              ? 'অডিট লগ শুধুমাত্র সুপার অ্যাডমিনের জন্য সংরক্ষিত'
              : 'Audit logs are restricted to Super Admins'}
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 font-display">
          {lang === 'bn' ? 'অপরিবর্তনীয় অডিট লগ' : 'Immutable Audit Logs'}
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          audit_logs · {toLocalizedNumber(logs.length, lang)}{' '}
          {lang === 'bn' ? 'টি ইভেন্ট' : 'events recorded'}
        </p>
      </div>

      {loadError ? (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800">
          {loadError}
        </div>
      ) : null}

      <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col md:flex-row gap-3 justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={
              lang === 'bn'
                ? 'অ্যাকশন, অ্যাডমিন ইমেইল বা বিবরণ খুঁজুন...'
                : 'Search actions, actors, or summaries...'
            }
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg"
          />
        </div>

        <select
          value={collectionFilter}
          onChange={(e) => setCollectionFilter(e.target.value)}
          className="px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white"
        >
          <option value="ALL">
            {lang === 'bn' ? 'সকল সংগ্রহ' : 'All Collections'}
          </option>
          {collections.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading audit logs...'}
          </div>
        ) : paginated.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500 flex flex-col items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-slate-400" />
            <p>
              {lang === 'bn'
                ? 'এখনো কোনো অডিট লগ রেকর্ড করা হয়নি।'
                : 'No audit log entries recorded yet.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-3 px-4">{lang === 'bn' ? 'কার্যক্রম ও বিবরণ' : 'Action & Summary'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'লক্ষ্যবস্তু' : 'Target'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'প্রশাসক' : 'Actor'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'সময়' : 'Timestamp'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {paginated.map((l) => (
                  <tr key={l.id} className="hover:bg-slate-50/80">
                    <td className="py-3 px-4">
                      <div className="font-mono text-xs font-semibold text-blue-700">
                        {l.action}
                      </div>
                      <div className="text-slate-900 mt-0.5">
                        {lang === 'bn'
                          ? l.summaryBn || l.summaryEn || '—'
                          : l.summaryEn || l.summaryBn || '—'}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-mono text-xs text-slate-600">
                      {[l.targetCollection, l.targetDocId].filter(Boolean).join('/') || '—'}
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-700">
                      <div>{l.actorEmail || l.actorUid || '—'}</div>
                      <div className="text-slate-500 font-mono">{l.actorRole || '—'}</div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500 tabular-nums whitespace-nowrap">
                      {formatFirestoreTimestamp(l.createdAt, lang)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Pagination
          currentPage={page}
          totalItems={filtered.length}
          pageSize={PAGE_SIZE}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
