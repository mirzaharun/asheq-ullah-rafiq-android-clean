import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  BadgeCheck,
  Building2,
  ClipboardList,
  CloudSun,
  Droplets,
  History,
  Lock,
  Megaphone,
  MessageSquareWarning,
  RefreshCw,
  ShieldCheck,
  Ship,
  Waves,
} from 'lucide-react';
import {
  DataColumn,
  DataTable,
  PermissionErrorBanner,
  StatCard,
  StatusBadge,
} from '../components/crud/CrudPrimitives';
import { InlineRoleNotice } from '../components/ProtectedRoute';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { getRoleLabel } from '../services/authService';
import {
  createInitialDashboardStats,
  subscribeDashboardStats,
} from '../services/dashboardService';
import {
  AuditLogEntry,
  CitizenApplication,
  DashboardLiveStats,
} from '../types/portal';

function formatTimestamp(val: unknown, lang: 'bn' | 'en'): string {
  if (!val) return '—';
  try {
    if (
      typeof val === 'object' &&
      val !== null &&
      'toDate' in val &&
      typeof (val as { toDate: unknown }).toDate === 'function'
    ) {
      const date = (val as { toDate: () => Date }).toDate();
      return date.toLocaleString(lang === 'bn' ? 'bn-BD' : 'en-US', {
        dateStyle: 'medium',
        timeStyle: 'short',
      });
    }
    if (typeof val === 'number') {
      return new Date(val).toLocaleString(lang === 'bn' ? 'bn-BD' : 'en-US', {
        dateStyle: 'medium',
        timeStyle: 'short',
      });
    }
  } catch {
    // Fallback
  }
  return '—';
}

export const DashboardPage: React.FC = () => {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();

  const [stats, setStats] = useState<DashboardLiveStats>(() =>
    createInitialDashboardStats(adminUser?.role ?? 'editor')
  );
  const [refreshNonce, setRefreshNonce] = useState(0);

  useEffect(() => {
    if (!adminUser) return;
    setStats(createInitialDashboardStats(adminUser.role));
    const unsub = subscribeDashboardStats(adminUser.role, lang, setStats);
    return () => unsub();
  }, [adminUser, lang, refreshNonce]);

  const applicationSummary = useMemo(() => {
    const counts = {
      total: stats.applications.length,
      pending: 0,
      inProgress: 0,
      resolved: 0,
      rejected: 0,
    };
    for (const app of stats.applications) {
      const s = (app.status || '').toLowerCase();
      if (s === 'pending' || s === 'submitted' || s === 'under review') {
        counts.pending += 1;
      } else if (s === 'resolved' || s === 'completed') {
        counts.resolved += 1;
      } else if (s.includes('reject') || s === 'cancelled') {
        counts.rejected += 1;
      } else {
        counts.inProgress += 1;
      }
    }
    return counts;
  }, [stats.applications]);

  const recentApplications = useMemo(
    () =>
      [...stats.applications]
        .sort((a, b) => (b.createdAt || b.timestamp || 0) - (a.createdAt || a.timestamp || 0))
        .slice(0, 6),
    [stats.applications]
  );

  const openGrievancesCount = useMemo(
    () =>
      stats.grievances.filter((g) => {
        const s = (g.status || '').toLowerCase();
        return s !== 'resolved' && !s.includes('reject');
      }).length,
    [stats.grievances]
  );

  const availableDonorsCount = useMemo(
    () => stats.bloodDonors.filter((d) => d.isAvailable !== false).length,
    [stats.bloodDonors]
  );

  const appColumns: DataColumn<CitizenApplication>[] = [
    {
      key: 'tracking',
      header: t('ট্র্যাকিং ও আবেদনকারী', 'Tracking & Applicant'),
      render: (row) => (
        <div>
          <p className="font-semibold text-slate-900">{row.name || '—'}</p>
          <p className="text-xs font-mono text-slate-500">
            {row.trackingId || row.trackingNumber || row.id}
          </p>
        </div>
      ),
    },
    {
      key: 'area',
      header: t('এলাকা ও ধরন', 'Area & Category'),
      render: (row) => (
        <div className="text-xs">
          <p className="font-medium text-slate-800">
            {row.category || row.type || t('সাধারণ আবেদন', 'General')}
          </p>
          <p className="text-slate-500">
            {[row.upazila, row.union].filter(Boolean).join(', ') || '—'}
          </p>
        </div>
      ),
    },
    {
      key: 'status',
      header: t('অবস্থা', 'Status'),
      render: (row) => <StatusBadge status={row.status || 'Submitted'} />,
    },
  ];

  const auditColumns: DataColumn<AuditLogEntry>[] = [
    {
      key: 'action',
      header: t('কার্যক্রম', 'Action'),
      render: (row) => (
        <div>
          <span className="font-semibold text-slate-900">{row.action}</span>
          <p className="text-xs font-mono text-slate-500">
            /{row.targetCollection}
            {row.targetId ? `/${row.targetId}` : ''}
          </p>
        </div>
      ),
    },
    {
      key: 'actor',
      header: t('প্রশাসক', 'Actor'),
      render: (row) => (
        <div className="text-xs">
          <p className="text-slate-800 font-medium">
            {row.actorEmail || row.actorUid}
          </p>
        </div>
      ),
    },
    {
      key: 'time',
      header: t('সময়', 'Timestamp'),
      render: (row) => (
        <span className="text-xs text-slate-500">
          {formatTimestamp(row.createdAt, lang)}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top Welcome & Verification Banner */}
      <div className="bg-gradient-to-r from-emerald-900 via-emerald-800 to-teal-800 rounded-3xl p-6 text-white shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/15 border border-white/20 text-[11px] font-semibold">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-200" />
              <span>
                {adminUser ? getRoleLabel(adminUser.role, lang) : ''} •{' '}
                {t('লাইভ Firestore পরিসংখ্যান', 'Live Firestore Telemetry')}
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-bold mt-2.5">
              {t('কেন্দ্রীয় প্রশাসনিক ড্যাশবোর্ড', 'Central Administrative Dashboard')}
            </h2>
            <p className="text-xs sm:text-sm text-emerald-100/90 mt-1">
              {t(
                'মহেশখালী ও কুতুবদিয়ার নাগরিক সেবা, উন্নয়ন প্রকল্প ও পাবলিক পোর্টালের রিয়েল-টাইম অবস্থা।',
                'Real-time citizen services, development tracking, and portal collection telemetry.'
              )}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            {stats.lastSyncedAt && (
              <span className="text-xs text-emerald-100/90 bg-black/20 px-3 py-1.5 rounded-xl">
                {t('সর্বশেষ সিঙ্ক:', 'Synced:')}{' '}
                {stats.lastSyncedAt.toLocaleTimeString(
                  lang === 'bn' ? 'bn-BD' : 'en-US'
                )}
              </span>
            )}
            <button
              type="button"
              onClick={() => setRefreshNonce((n) => n + 1)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white text-emerald-950 hover:bg-emerald-50 text-xs font-bold transition-colors"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>{t('রিফ্রেশ', 'Refresh')}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Live Coastal Weather & Sea-Truck Bar */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="h-11 w-11 rounded-xl bg-sky-50 text-sky-700 border border-sky-200 flex items-center justify-center shrink-0">
              <CloudSun className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500">
                {t('আবহাওয়া ও সমুদ্র সংকেত (weather_data/current)', 'Coastal Weather & Signal (weather_data/current)')}
              </p>
              {stats.weather ? (
                <p className="text-sm font-bold text-slate-900 mt-0.5 truncate">
                  {stats.weather.temperature !== undefined
                    ? `${stats.weather.temperature}°C • `
                    : ''}
                  {(lang === 'bn'
                    ? stats.weather.conditionBn
                    : stats.weather.conditionEn) ||
                    stats.weather.conditionBn ||
                    t('তথ্য সংযুক্ত', 'Synced')}
                  {stats.weather.maritimeSignal !== undefined
                    ? ` • ${t('সংকেত', 'Signal')} #${stats.weather.maritimeSignal}`
                    : ''}
                </p>
              ) : (
                <p className="text-xs text-slate-500 mt-0.5">
                  {t(
                    'ডকুমেন্ট এখনো সেট করা হয়নি অথবা লোড হচ্ছে',
                    'Document not yet populated in Firestore or loading'
                  )}
                </p>
              )}
            </div>
          </div>
          <Link
            to="/modules/weather_data"
            className="text-xs font-semibold text-emerald-700 hover:underline shrink-0"
          >
            {t('মডিউল দেখুন', 'Open')}
          </Link>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="h-11 w-11 rounded-xl bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center justify-center shrink-0">
              <Ship className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500">
                {t('সী-ট্রাক চলাচল অবস্থা (sea_truck_updates/current)', 'Sea-Truck Status (sea_truck_updates/current)')}
              </p>
              {stats.seaTruck ? (
                <div className="flex items-center gap-2 mt-1">
                  <StatusBadge
                    status={
                      stats.seaTruck.isNormal === false
                        ? t('স্থগিত / সতর্কতা', 'Suspended / Alert')
                        : t('চলাচল স্বাভাবিক', 'Normal Operation')
                    }
                  />
                  <span className="text-xs text-slate-600 truncate">
                    {(lang === 'bn'
                      ? stats.seaTruck.noticeBn
                      : stats.seaTruck.noticeEn) ||
                      stats.seaTruck.noticeBn ||
                      ''}
                  </span>
                </div>
              ) : (
                <p className="text-xs text-slate-500 mt-0.5">
                  {t(
                    'ডকুমেন্ট এখনো সেট করা হয়নি অথবা লোড হচ্ছে',
                    'Document not yet populated in Firestore or loading'
                  )}
                </p>
              )}
            </div>
          </div>
          <Link
            to="/modules/sea_truck_updates"
            className="text-xs font-semibold text-emerald-700 hover:underline shrink-0"
          >
            {t('মডিউল দেখুন', 'Open')}
          </Link>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <StatCard
          title={t('উন্নয়ন প্রকল্প', 'Development Projects')}
          value={stats.projects.length}
          subtitle={t('সকল তালিকাভুক্ত প্রকল্প', 'Total tracked projects')}
          icon={<Building2 className="h-5 w-5" />}
          accent="emerald"
          error={stats.projectsError}
        />

        <StatCard
          title={t('বিজ্ঞপ্তি ও নোটিশ', 'Notices & Bulletins')}
          value={stats.noticesCount ?? '—'}
          subtitle={
            stats.urgentNoticesCount !== null
              ? `${stats.urgentNoticesCount} ${t('টি জরুরি বিজ্ঞপ্তি', 'urgent notices')}`
              : t('সক্রিয় বিজ্ঞপ্তি', 'Active bulletins')
          }
          icon={<Megaphone className="h-5 w-5" />}
          accent="amber"
          error={stats.noticesError}
        />

        <StatCard
          title={t('নাগরিক আবেদন ও সাক্ষাৎ', 'Citizen Applications')}
          value={stats.applicationsRestricted ? '—' : applicationSummary.total}
          restricted={stats.applicationsRestricted}
          subtitle={
            stats.applicationsRestricted
              ? t('আপনার রোলের জন্য সংরক্ষিত', 'Restricted by RBAC rules')
              : `${applicationSummary.pending} ${t('টি অপেক্ষমাণ', 'pending review')}`
          }
          icon={<ClipboardList className="h-5 w-5" />}
          accent="blue"
          error={stats.applicationsError}
        />

        <StatCard
          title={t('নাগরিক অভিযোগ', 'Citizen Grievances')}
          value={stats.grievancesRestricted ? '—' : stats.grievances.length}
          restricted={stats.grievancesRestricted}
          subtitle={
            stats.grievancesRestricted
              ? t('আপনার রোলের জন্য সংরক্ষিত', 'Restricted by RBAC rules')
              : `${openGrievancesCount} ${t('টি নিষ্পত্তির অপেক্ষায়', 'open grievances')}`
          }
          icon={<MessageSquareWarning className="h-5 w-5" />}
          accent="rose"
          error={stats.grievancesError}
        />

        <StatCard
          title={t('রক্তদাতা ডিরেক্টরি', 'Blood Donors')}
          value={stats.bloodDonors.length}
          subtitle={`${availableDonorsCount} ${t('জন রক্তদানে প্রস্তুত', 'currently available')}`}
          icon={<Droplets className="h-5 w-5" />}
          accent="rose"
          error={stats.bloodDonorsError}
        />

        <StatCard
          title={t('মতামত ও পরামর্শ', 'General Feedback')}
          value={stats.feedbackCount ?? '—'}
          restricted={stats.feedbackRestricted}
          subtitle={t('নাগরিক মতামত ও পরামর্শ', 'Submitted citizen feedback')}
          icon={<BadgeCheck className="h-5 w-5" />}
          accent="indigo"
          error={stats.feedbackError}
        />
      </div>

      {/* Citizen Intake Queue + Top Development Projects */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-6">
        {/* Applications & Grievances Queue */}
        <div className="xl:col-span-7 bg-white rounded-2xl border border-slate-200 overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {t('সাম্প্রতিক নাগরিক আবেদন কিউ', 'Recent Citizen Applications Queue')}
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                {t(
                  'সরাসরি Firestore `applications` কালেকশন থেকে প্রাপ্ত',
                  'Real-time snapshot from Firestore `applications` collection'
                )}
              </p>
            </div>
            {!stats.applicationsRestricted && (
              <Link
                to="/modules/applications"
                className="text-xs font-semibold text-emerald-700 hover:underline"
              >
                {t('সকল আবেদন', 'View Module')}
              </Link>
            )}
          </div>

          <div className="p-4 flex-1">
            {stats.applicationsRestricted ? (
              <InlineRoleNotice
                title={t(
                  'ভূমিকা-ভিত্তিক গোপনীয়তা সংরক্ষিত',
                  'Role-Based Privacy Protection Active'
                )}
                description={t(
                  'নিরাপত্তা নীতিমালা অনুযায়ী শুধুমাত্র সুপার এডমিন ও আবেদন ব্যবস্থাপক (Application Manager) ব্যক্তিগত নাগরিক আবেদন, অভিযোগ এবং মতামত দেখতে পারেন।',
                  'Per Firestore zero-trust security rules, private citizen applications, grievances, and feedback are restricted to Super Admin and Application Manager roles.'
                )}
              />
            ) : stats.applicationsError ? (
              <PermissionErrorBanner message={stats.applicationsError} />
            ) : (
              <>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mb-4">
                  <div className="rounded-xl bg-amber-50 border border-amber-200 px-3 py-2">
                    <p className="text-[11px] font-semibold text-amber-800">
                      {t('অপেক্ষমাণ', 'Pending')}
                    </p>
                    <p className="text-lg font-bold text-amber-950">
                      {applicationSummary.pending}
                    </p>
                  </div>
                  <div className="rounded-xl bg-sky-50 border border-sky-200 px-3 py-2">
                    <p className="text-[11px] font-semibold text-sky-800">
                      {t('প্রক্রিয়াধীন', 'In Progress')}
                    </p>
                    <p className="text-lg font-bold text-sky-950">
                      {applicationSummary.inProgress}
                    </p>
                  </div>
                  <div className="rounded-xl bg-emerald-50 border border-emerald-200 px-3 py-2">
                    <p className="text-[11px] font-semibold text-emerald-800">
                      {t('নিষ্পত্তিকৃত', 'Resolved')}
                    </p>
                    <p className="text-lg font-bold text-emerald-950">
                      {applicationSummary.resolved}
                    </p>
                  </div>
                  <div className="rounded-xl bg-rose-50 border border-rose-200 px-3 py-2">
                    <p className="text-[11px] font-semibold text-rose-800">
                      {t('বাতিল / প্রত্যাখ্যাত', 'Rejected')}
                    </p>
                    <p className="text-lg font-bold text-rose-950">
                      {applicationSummary.rejected}
                    </p>
                  </div>
                </div>

                <DataTable
                  columns={appColumns}
                  rows={recentApplications}
                  rowKey={(r) => r.id}
                  emptyTitle={t('কোনো নাগরিক আবেদন পাওয়া যায়নি', 'No Citizen Applications Yet')}
                  emptyDescription={t(
                    'অ্যান্ড্রয়েড অ্যাপ থেকে নাগরিকরা আবেদন জমা দিলে তা রিয়েল-টাইমে এখানে প্রদর্শিত হবে।',
                    'Citizen submissions from the Android app will appear here in real time.'
                  )}
                />
              </>
            )}
          </div>
        </div>

        {/* Development Projects Snapshot */}
        <div className="xl:col-span-5 bg-white rounded-2xl border border-slate-200 overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {t('উন্নয়ন প্রকল্পের অগ্রগতি', 'Development Progress')}
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                {t('মহেশখালী ও কুতুবদিয়া প্রকল্পসমূহ', 'Moheshkhali & Kutubdia initiatives')}
              </p>
            </div>
            <Link
              to="/modules/development_projects"
              className="text-xs font-semibold text-emerald-700 hover:underline"
            >
              {t('সকল প্রকল্প', 'View Module')}
            </Link>
          </div>

          <div className="p-4 space-y-3 flex-1">
            {stats.projectsError && (
              <PermissionErrorBanner message={stats.projectsError} />
            )}

            {stats.projects.length === 0 ? (
              <div className="py-10 text-center text-xs text-slate-500">
                {t(
                  'কোনো উন্নয়ন প্রকল্প পাওয়া যায়নি।',
                  'No development projects found in Firestore.'
                )}
              </div>
            ) : (
              stats.projects.slice(0, 5).map((proj) => {
                const pct = Math.max(
                  0,
                  Math.min(100, Number(proj.completionPercentage ?? 0))
                );
                return (
                  <div
                    key={proj.id}
                    className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/70"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="text-sm font-bold text-slate-900 truncate">
                          {(lang === 'bn' ? proj.titleBn : proj.titleEn) ||
                            proj.titleBn}
                        </p>
                        <p className="text-xs text-slate-500 mt-0.5">
                          {[proj.upazila, proj.categoryBn || proj.categoryEn]
                            .filter(Boolean)
                            .join(' • ') || '—'}
                        </p>
                      </div>
                      <span className="text-xs font-bold text-emerald-700 shrink-0">
                        {pct}%
                      </span>
                    </div>
                    <div className="mt-2.5 h-2 w-full rounded-full bg-slate-200 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-emerald-600 transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Live Firestore Collection Inventory (All 26 + 2 Governance Modules) */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <Activity className="h-5 w-5 text-emerald-700" />
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {t(
                  'লাইভ Firestore কালেকশন ইনভেন্টরি (সকল মডিউল)',
                  'Live Firestore Collection Inventory (All Modules)'
                )}
              </h3>
              <p className="text-xs text-slate-500">
                {t(
                  'সার্ভার-সাইড getCountFromServer() দ্বারা প্রাপ্ত প্রকৃত ডকুমেন্ট সংখ্যা',
                  'Verified server-side document counts via getCountFromServer()'
                )}
              </p>
            </div>
          </div>
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600">
            <Waves className="h-4 w-4 text-emerald-700" />
            {stats.collectionInventory.length}{' '}
            {t('টি মডিউল সংযুক্ত', 'modules wired')}
          </span>
        </div>

        <div className="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
          {stats.collectionInventory.map((item) => {
            const cardBody = (
              <div
                className={`p-3.5 rounded-xl border transition-all h-full flex flex-col justify-between ${
                  item.restricted
                    ? 'bg-slate-50 border-slate-200/80 opacity-80'
                    : 'bg-white border-slate-200 hover:border-emerald-400 hover:shadow-xs'
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-900 truncate">
                      {lang === 'bn' ? item.labelBn : item.labelEn}
                    </p>
                    <p className="text-[11px] font-mono text-slate-500 truncate mt-0.5">
                      /{item.collection}
                    </p>
                  </div>
                  {item.restricted ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-200 text-slate-600 shrink-0">
                      <Lock className="h-2.5 w-2.5" />
                      {t('সংরক্ষিত', 'RBAC')}
                    </span>
                  ) : (
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 shrink-0">
                      {item.count !== null ? item.count : '…'}
                    </span>
                  )}
                </div>

                {item.error && !item.restricted && (
                  <p className="mt-2 text-[10px] text-rose-600 truncate">
                    {item.error}
                  </p>
                )}
              </div>
            );

            return item.restricted ? (
              <div key={item.key}>{cardBody}</div>
            ) : (
              <Link key={item.key} to={`/modules/${item.key}`}>
                {cardBody}
              </Link>
            );
          })}
        </div>
      </div>

      {/* Recent Audit Logs */}
      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <History className="h-5 w-5 text-emerald-700" />
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {t('সাম্প্রতিক নিরাপত্তা ও অডিট লগ', 'Recent Security & Audit Logs')}
              </h3>
              <p className="text-xs text-slate-500">
                {t(
                  'সকল প্রশাসনিক কার্যক্রম Firestore `audit_logs` থেকে সরাসরি প্রদর্শিত',
                  'Immutable administrative trail streamed directly from Firestore `audit_logs`'
                )}
              </p>
            </div>
          </div>
          <Link
            to="/modules/audit_logs"
            className="text-xs font-semibold text-emerald-700 hover:underline"
          >
            {t('বিস্তারিত', 'View All')}
          </Link>
        </div>

        <div className="p-4">
          {stats.auditLogsError ? (
            <PermissionErrorBanner message={stats.auditLogsError} />
          ) : (
            <DataTable
              columns={auditColumns}
              rows={stats.auditLogs}
              rowKey={(r) => r.id}
              emptyTitle={t('এখনো কোনো অডিট লগ নেই', 'No Audit Logs Recorded Yet')}
              emptyDescription={t(
                'ফেজ ৩সি-তে প্রশাসনিক CRUD কার্যক্রম শুরু হলে এখানে রিয়েল-টাইম অডিট রেকর্ড প্রদর্শিত হবে।',
                'Administrative CRUD events will be logged here in real time once write actions occur.'
              )}
            />
          )}
        </div>
      </div>
    </div>
  );
};
