import { useEffect, useMemo, useState } from 'react';
import { useLanguage } from '../../hooks/useLanguage';
import { subscribeCollection } from '../../services/firebase/crudService';
import { toLocalizedNumber } from '../../utils/formatters';

export function ReportsAnalyticsView() {
  const { lang } = useLanguage();

  const [projects, setProjects] = useState<{ status: string; upazila: string; completion: number }[]>([]);
  const [donors, setDonors] = useState<{ bloodGroup: string; available: boolean }[]>([]);
  const [gallery, setGallery] = useState<{ mediaType: string; published: boolean }[]>([]);
  const [applications, setApplications] = useState<{ status: string }[]>([]);

  useEffect(() => {
    const u1 = subscribeCollection(
      'development_projects',
      (_id, r) => ({
        status: typeof r.status === 'string' ? r.status : 'Unknown',
        upazila: typeof r.upazila === 'string' ? r.upazila : 'Unspecified',
        completion: typeof r.completionPercentage === 'number' ? r.completionPercentage : 0,
      }),
      setProjects,
      () => {}
    );
    const u2 = subscribeCollection(
      'blood_donors',
      (_id, r) => ({
        bloodGroup: typeof r.bloodGroup === 'string' ? r.bloodGroup : 'Unknown',
        available: r.available !== false,
      }),
      setDonors,
      () => {}
    );
    const u3 = subscribeCollection(
      'gallery_items',
      (_id, r) => ({
        mediaType: typeof r.mediaType === 'string' ? r.mediaType : 'PHOTO',
        published: r.published !== false,
      }),
      setGallery,
      () => {}
    );
    const u4 = subscribeCollection(
      'applications',
      (_id, r) => ({
        status: typeof r.status === 'string' ? r.status : 'Submitted',
      }),
      setApplications,
      () => {}
    );
    return () => {
      u1();
      u2();
      u3();
      u4();
    };
  }, []);

  const projectByStatus = useMemo(() => {
    const map: Record<string, number> = {};
    projects.forEach((p) => {
      map[p.status] = (map[p.status] || 0) + 1;
    });
    return Object.entries(map);
  }, [projects]);

  const donorByGroup = useMemo(() => {
    const map: Record<string, number> = {};
    donors.forEach((d) => {
      map[d.bloodGroup] = (map[d.bloodGroup] || 0) + 1;
    });
    return Object.entries(map);
  }, [donors]);

  const avgCompletion = useMemo(() => {
    if (projects.length === 0) return 0;
    const sum = projects.reduce((acc, p) => acc + p.completion, 0);
    return Math.round(sum / projects.length);
  }, [projects]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 font-display">
          {lang === 'bn' ? 'রিপোর্ট ও লাইভ বিশ্লেষণ' : 'Reports & Live Analytics'}
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          {lang === 'bn'
            ? 'সরাসরি লাইভ ফায়ারস্টোর সংগ্রহ থেকে হিসাবকৃত পরিসংখ্যান'
            : 'Real-time aggregations computed directly from live Firestore collections'}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-900">
              {lang === 'bn' ? 'উন্নয়ন প্রকল্পের অবস্থা' : 'Development Projects by Status'}
            </h3>
            <span className="font-mono text-xs font-semibold text-emerald-700 tabular-nums">
              {lang === 'bn'
                ? `গড় অগ্রগতি: ${toLocalizedNumber(avgCompletion, lang)}%`
                : `Avg Completion: ${avgCompletion}%`}
            </span>
          </div>

          <div className="space-y-2.5">
            {projectByStatus.map(([status, count]) => (
              <div key={status} className="space-y-1">
                <div className="flex justify-between text-xs text-slate-700">
                  <span>{status}</span>
                  <span className="font-mono tabular-nums">
                    {toLocalizedNumber(count, lang)}
                  </span>
                </div>
                <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-700"
                    style={{
                      width: `${projects.length > 0 ? Math.round((count / projects.length) * 100) : 0}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
          <h3 className="text-sm font-semibold text-slate-900">
            {lang === 'bn' ? 'রক্তের গ্রুপ অনুযায়ী দাতা' : 'Blood Donors by Group'}
          </h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {donorByGroup.map(([group, count]) => (
              <div
                key={group}
                className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-center"
              >
                <div className="font-mono font-bold text-red-700">{group}</div>
                <div className="mt-1 text-lg font-bold text-slate-900 tabular-nums">
                  {toLocalizedNumber(count, lang)}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-3">
          <h3 className="text-sm font-semibold text-slate-900">
            {lang === 'bn' ? 'গ্যালারি প্রকাশনা সারসংক্ষেপ' : 'Gallery Publication Summary'}
          </h3>
          <div className="grid grid-cols-3 gap-4 text-sm pt-2 border-t border-slate-100">
            <div>
              <p className="text-xs text-slate-500">{lang === 'bn' ? 'মোট আইটেম' : 'Total'}</p>
              <p className="text-xl font-bold tabular-nums">
                {toLocalizedNumber(gallery.length, lang)}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500">{lang === 'bn' ? 'প্রকাশিত' : 'Published'}</p>
              <p className="text-xl font-bold text-emerald-700 tabular-nums">
                {toLocalizedNumber(gallery.filter((g) => g.published).length, lang)}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500">{lang === 'bn' ? 'অপ্রকাশিত' : 'Draft'}</p>
              <p className="text-xl font-bold text-amber-700 tabular-nums">
                {toLocalizedNumber(gallery.filter((g) => !g.published).length, lang)}
              </p>
            </div>
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-3">
          <h3 className="text-sm font-semibold text-slate-900">
            {lang === 'bn' ? 'নাগরিক আবেদন সারসংক্ষেপ' : 'Citizen Applications Summary'}
          </h3>
          <p className="text-sm text-slate-600 tabular-nums">
            {lang === 'bn'
              ? `মোট আবেদন: ${toLocalizedNumber(applications.length, lang)}`
              : `Total applications: ${applications.length}`}
          </p>
        </section>
      </div>
    </div>
  );
}
