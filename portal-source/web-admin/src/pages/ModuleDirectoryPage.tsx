import { ArrowRight, Database, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PORTAL_MODULES } from '../config/modules';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { canAccessModule } from '../services/authService';
import type { ModuleGroup } from '../types/portal';

const GROUPS: readonly ModuleGroup[] = ['cms', 'citizen', 'governance'];

export function ModuleDirectoryPage() {
  const { admin } = useAuth();
  const { lang, t } = useLanguage();
  const modules = PORTAL_MODULES.filter((module) => canAccessModule(admin?.role, module));

  return (
    <div className="space-y-6">
      <header>
        <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-emerald-800">
          <Database className="h-4 w-4" />
          {t('ডাইনামিক Firestore মডিউল', 'Dynamic Firestore modules')}
        </p>
        <h2 className="mt-2 text-2xl font-bold text-slate-900">
          {t('মডিউল ডিরেক্টরি', 'Module directory')}
        </h2>
        <p className="mt-1 max-w-3xl text-sm text-slate-600">
          {t(
            'আপনার রোল অনুযায়ী অনুমোদিত মডিউল নির্বাচন করুন। প্রতিটি মডিউল বিদ্যমান Firestore collection বা singleton document ব্যবহার করে।',
            'Choose a module permitted for your role. Each workspace uses its registered Firestore collection or singleton document.'
          )}
        </p>
      </header>

      {GROUPS.map((group) => {
        const groupModules = modules.filter((module) => module.group === group);
        if (groupModules.length === 0) return null;
        const title = group === 'cms'
          ? t('পোর্টাল কনটেন্ট', 'Portal content')
          : group === 'citizen'
            ? t('নাগরিক সেবা', 'Citizen services')
            : t('প্রশাসন ও অডিট', 'Administration & governance');

        return (
          <section key={group} aria-labelledby={`module-group-${group}`} className="space-y-3">
            <h3 id={`module-group-${group}`} className="text-sm font-bold text-slate-700">{title}</h3>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {groupModules.map((module) => (
                <Link
                  key={module.key}
                  to={`/modules/${module.key}`}
                  className="group rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-emerald-300 hover:shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-600"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h4 className="font-semibold text-slate-900">
                        {lang === 'bn' ? module.titleBn : module.titleEn}
                      </h4>
                      <p className="mt-1 line-clamp-2 text-xs leading-relaxed text-slate-600">
                        {lang === 'bn' ? module.descriptionBn : module.descriptionEn}
                      </p>
                    </div>
                    <ArrowRight className="h-4 w-4 shrink-0 text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-emerald-700" />
                  </div>
                  <div className="mt-4 flex items-center justify-between gap-2 border-t border-slate-100 pt-3">
                    <span className="font-mono text-[11px] text-slate-500">
                      /{module.collection}{module.singletonDocId ? `/${module.singletonDocId}` : ''}
                    </span>
                    <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-slate-500">
                      <ShieldCheck className="h-3.5 w-3.5" />
                      {module.allowedRoles.map((role) => role.replace('_', ' ')).join(', ')}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}
