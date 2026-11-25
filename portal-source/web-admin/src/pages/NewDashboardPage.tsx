import { Link } from 'react-router-dom';
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  Clock,
  FileText,
  Megaphone,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { StatCard } from '../components/common/StatCard';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { useDashboardData } from '../modules/dashboard/useDashboardData';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../utils/formatters';

export function DashboardPage() {
  const { admin } = useAuth();
  const { lang, t } = useLanguage();
  const {
    loading,
    metrics,
    recentRequests,
    recentLogs,
    recentProjects,
    recentNotices,
    errors,
  } = useDashboardData(Boolean(admin));

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {t.dashboardTitle}
          </h2>
          <p className="mt-1 text-sm text-slate-600">{t.dashboardSubtitle}</p>
        </div>

        {admin ? (
          <div className="text-xs text-slate-500 flex items-center gap-2">
            <span>{admin.displayName}</span>
            <span aria-hidden="true">·</span>
            <span>{t.roleLabels[admin.role]}</span>
          </div>
        ) : null}
      </div>

      {/* Firestore Error Notification (if any rule denies a read) */}
      {errors.length > 0 ? (
        <div
          role="alert"
          className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3 text-xs text-amber-900"
        >
          <AlertCircle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <p className="font-semibold">
              {lang === 'bn'
                ? 'ফায়ারস্টোর ডেটা লোড করতে আংশিক সীমাবদ্ধতা পাওয়া গেছে:'
                : 'Partial Firestore access notice:'}
            </p>
            <ul className="list-disc pl-4 space-y-0.5 font-mono">
              {errors.map((err) => (
                <li key={err}>{err}</li>
              ))}
            </ul>
          </div>
        </div>
      ) : null}

      {/* 6 Primary KPI Cards */}
      <section
        aria-label={lang === 'bn' ? 'প্রধান পরিসংখ্যান' : 'Key statistics'}
        className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4"
      >
        <StatCard
          label={t.kpiTotalCitizens}
          value={metrics.totalCitizens}
          sublabel={lang === 'bn' ? 'সংগ্রহ: users' : 'Collection: users'}
          icon={<Users className="w-4 h-4" />}
          accent="blue"
          loading={loading}
        />
        <StatCard
          label={t.kpiTotalProjects}
          value={metrics.totalProjects}
          sublabel={
            lang === 'bn'
              ? 'সংগ্রহ: development_projects'
              : 'Collection: development_projects'
          }
          icon={<Building2 className="w-4 h-4" />}
          accent="emerald"
          loading={loading}
        />
        <StatCard
          label={t.kpiPublishedNotices}
          value={metrics.publishedNotices}
          sublabel={lang === 'bn' ? 'সংগ্রহ: notices' : 'Collection: notices'}
          icon={<Megaphone className="w-4 h-4" />}
          accent="blue"
          loading={loading}
        />
        <StatCard
          label={t.kpiTotalApplications}
          value={metrics.totalApplications}
          sublabel={lang === 'bn' ? 'সংগ্রহ: applications' : 'Collection: applications'}
          icon={<FileText className="w-4 h-4" />}
          accent="slate"
          loading={loading}
        />
        <StatCard
          label={t.kpiPendingApplications}
          value={metrics.pendingApplications}
          sublabel={
            lang === 'bn'
              ? 'জমা, পর্যালোচনা ও চলমান আবেদন'
              : 'Submitted, Under Review & In Progress'
          }
          icon={<Clock className="w-4 h-4" />}
          accent="amber"
          loading={loading}
        />
        <StatCard
          label={t.kpiResolvedApplications}
          value={metrics.resolvedApplications}
          sublabel={
            lang === 'bn' ? 'সফলভাবে নিষ্পত্তিকৃত' : 'Resolved & Completed'
          }
          icon={<CheckCircle2 className="w-4 h-4" />}
          accent="emerald"
          loading={loading}
        />
      </section>

      {/* Secondary Live Android Collection Summary Strip */}
      <section className="bg-white border border-slate-200 rounded-xl p-5">
        <div className="flex items-center justify-between gap-4 mb-4">
          <h3 className="text-sm font-semibold text-slate-900">
            {t.portalSummaryTitle}
          </h3>
          <span className="text-xs text-slate-500">
            {lang === 'bn' ? 'অ্যান্ড্রয়েড অ্যাপ সিঙ্কড' : 'Synced with Android Portal'}
          </span>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 pt-2 border-t border-slate-100">
          <div>
            <p className="text-xs text-slate-500">
              {lang === 'bn' ? 'গ্যালারি আইটেম' : 'Gallery Items'}
            </p>
            <p className="mt-1 text-xl font-bold text-slate-900 tabular-nums font-display">
              {metrics.galleryItemsCount === null
                ? '—'
                : toLocalizedNumber(metrics.galleryItemsCount, lang)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">
              {lang === 'bn' ? 'নিবন্ধিত রক্তদাতা' : 'Blood Donors'}
            </p>
            <p className="mt-1 text-xl font-bold text-slate-900 tabular-nums font-display">
              {metrics.bloodDonorsCount === null
                ? '—'
                : toLocalizedNumber(metrics.bloodDonorsCount, lang)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">
              {lang === 'bn' ? 'নৌযান রুট সূচি' : 'Watercraft Routes'}
            </p>
            <p className="mt-1 text-xl font-bold text-slate-900 tabular-nums font-display">
              {metrics.watercraftRoutesCount === null
                ? '—'
                : toLocalizedNumber(metrics.watercraftRoutesCount, lang)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">
              {lang === 'bn' ? 'নাগরিক অভিযোগ' : 'Citizen Grievances'}
            </p>
            <p className="mt-1 text-xl font-bold text-slate-900 tabular-nums font-display">
              {metrics.citizenGrievancesCount === null
                ? '—'
                : toLocalizedNumber(metrics.citizenGrievancesCount, lang)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-500">
              {lang === 'bn' ? 'সাক্ষাৎকার অনুরোধ' : 'Appointments'}
            </p>
            <p className="mt-1 text-xl font-bold text-slate-900 tabular-nums font-display">
              {metrics.appointmentsCount === null
                ? '—'
                : toLocalizedNumber(metrics.appointmentsCount, lang)}
            </p>
          </div>
        </div>
      </section>

      {/* Latest Citizen Requests & Recent Administrative Activities */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {/* Latest Citizen Requests */}
        <section className="bg-white border border-slate-200 rounded-xl overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between gap-4">
            <h3 className="text-base font-semibold text-slate-900">
              {t.latestRequestsTitle}
            </h3>
            <Link
              to="/applications"
              className="text-xs font-medium text-blue-700 hover:text-blue-800 whitespace-nowrap"
            >
              {t.viewDetails}
            </Link>
          </div>

          {recentRequests.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500">
              {t.latestRequestsEmpty}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                    <th className="py-2.5 px-4">{t.tableTracking}</th>
                    <th className="py-2.5 px-4">{t.tableApplicant}</th>
                    <th className="py-2.5 px-4">{t.tableCategory}</th>
                    <th className="py-2.5 px-4">{t.tableStatus}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {recentRequests.map((req) => (
                    <tr
                      key={`${req.sourceCollection}-${req.id}`}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-2.5 px-4 font-mono text-xs text-slate-700 tabular-nums whitespace-nowrap">
                        {req.trackingNumber || req.id}
                      </td>
                      <td className="py-2.5 px-4 text-slate-900">
                        <div className="font-medium truncate max-w-[180px]">
                          {req.subject || req.applicantName || req.id}
                        </div>
                      </td>
                      <td className="py-2.5 px-4 text-xs text-slate-600">
                        {[req.category, req.union].filter(Boolean).join(' · ') || '—'}
                      </td>
                      <td className="py-2.5 px-4 text-xs font-medium text-slate-700 whitespace-nowrap">
                        {req.status || 'Submitted'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Recent Administrative Activities */}
        <section className="bg-white border border-slate-200 rounded-xl overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between gap-4">
            <h3 className="text-base font-semibold text-slate-900">
              {t.recentActivitiesTitle}
            </h3>
            <Link
              to="/audit-logs"
              className="text-xs font-medium text-blue-700 hover:text-blue-800 whitespace-nowrap"
            >
              {t.viewDetails}
            </Link>
          </div>

          {recentLogs.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500 flex flex-col items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-slate-400" />
              <p>{t.recentActivitiesEmpty}</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                    <th className="py-2.5 px-4">{t.tableAction}</th>
                    <th className="py-2.5 px-4">{t.tableActor}</th>
                    <th className="py-2.5 px-4">{t.tableTime}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {recentLogs.map((log) => (
                    <tr
                      key={log.id}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-2.5 px-4 text-slate-900">
                        <div className="font-medium">
                          {lang === 'bn'
                            ? log.summaryBn || log.action
                            : log.summaryEn || log.action}
                        </div>
                        {log.targetCollection ? (
                          <div className="text-xs text-slate-500 font-mono">
                            {log.targetCollection}
                            {log.targetDocId ? `/${log.targetDocId}` : ''}
                          </div>
                        ) : null}
                      </td>
                      <td className="py-2.5 px-4 text-xs text-slate-600">
                        {log.actorEmail || log.actorUid || '—'}
                      </td>
                      <td className="py-2.5 px-4 text-xs text-slate-500 tabular-nums whitespace-nowrap">
                        {formatFirestoreTimestamp(log.createdAt, lang)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {/* Live Android Portal Projects & Notices Preview */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <section className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between gap-4">
            <h3 className="text-base font-semibold text-slate-900">
              {t.liveProjectsTitle}
            </h3>
            <Link
              to="/projects"
              className="text-xs font-medium text-blue-700 hover:text-blue-800 whitespace-nowrap"
            >
              {t.viewDetails}
            </Link>
          </div>
          {recentProjects.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500">
              {t.noDataAvailable}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                    <th className="py-2.5 px-4">
                      {lang === 'bn' ? 'প্রকল্পের শিরোনাম' : 'Project Title'}
                    </th>
                    <th className="py-2.5 px-4">
                      {lang === 'bn' ? 'উপজেলা · বিভাগ' : 'Upazila · Category'}
                    </th>
                    <th className="py-2.5 px-4 text-right">
                      {lang === 'bn' ? 'অগ্রগতি' : 'Progress'}
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {recentProjects.map((project) => (
                    <tr
                      key={project.id}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-2.5 px-4 font-medium text-slate-900">
                        {lang === 'bn'
                          ? project.titleBn || project.titleEn
                          : project.titleEn || project.titleBn}
                      </td>
                      <td className="py-2.5 px-4 text-xs text-slate-600">
                        {project.upazila || '—'} ·{' '}
                        {lang === 'bn' ? project.categoryBn : project.categoryEn}
                      </td>
                      <td className="py-2.5 px-4 text-right font-mono text-xs font-semibold text-emerald-700 tabular-nums whitespace-nowrap">
                        {toLocalizedNumber(project.completionPercentage ?? 0, lang)}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between gap-4">
            <h3 className="text-base font-semibold text-slate-900">
              {t.liveNoticesTitle}
            </h3>
            <Link
              to="/notices"
              className="text-xs font-medium text-blue-700 hover:text-blue-800 whitespace-nowrap"
            >
              {t.viewDetails}
            </Link>
          </div>
          {recentNotices.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500">
              {t.noDataAvailable}
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {recentNotices.map((notice) => (
                <div key={notice.id} className="px-5 py-3.5 hover:bg-slate-50/80 transition-colors">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">
                      {lang === 'bn'
                        ? notice.titleBn || notice.titleEn
                        : notice.titleEn || notice.titleBn}
                    </p>
                    {notice.urgent ? (
                      <span className="text-xs font-semibold text-red-700 whitespace-nowrap">
                        {lang === 'bn' ? 'জরুরি' : 'Urgent'}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    {(lang === 'bn' ? notice.categoryBn : notice.categoryEn) || '—'} ·{' '}
                    {(lang === 'bn' ? notice.dateBn : notice.dateEn) || '—'}
                  </p>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
