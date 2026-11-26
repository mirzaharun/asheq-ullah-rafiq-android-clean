import { Database, Info, ShieldCheck } from 'lucide-react';
import { IntegratedModuleCrudEditor } from '../components/crud/IntegratedModuleCrudEditor';
import { NoticesManager } from '../modules/notices/NoticesManager';
import { getModuleByKey } from '../config/modules';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { canAccessModule } from '../services/authService';
import { UnauthorizedPage } from './UnauthorizedPage';

/** Notices admin uses the shared role-aware Firestore editor, retaining the original specialized manager below. */
export function NoticesIntegrationPage() {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();
  const module = getModuleByKey('notices');

  if (!module || !canAccessModule(adminUser?.role, module)) return <UnauthorizedPage />;

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-emerald-800">
          <Database className="h-4 w-4" />
          {t('Android Notices Firestore Integration', 'Android notices Firestore integration')}
        </p>
        <h2 className="text-2xl font-bold text-slate-900">
          {lang === 'bn' ? module.titleBn : module.titleEn}
        </h2>
        <p className="text-sm text-slate-600">
          {lang === 'bn' ? module.descriptionBn : module.descriptionEn}
        </p>
      </header>

      <div className="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3">
        <div className="flex items-start gap-2.5">
          <Info className="mt-0.5 h-4 w-4 shrink-0 text-sky-700" />
          <div>
            <p className="text-xs font-bold text-sky-950">
              {t('Firestore ও Android প্রকাশনা ক্ষেত্র', 'Firestore and Android publish fields')}
            </p>
            <p className="mt-1 text-xs leading-relaxed text-sky-900">
              {t(
                'নোটিশ /notices/{id}-তে সংরক্ষিত হয়। Draft-এ isPublished=false এবং published=false; Publish-এ দুটিই true হয়। Android-এর notices listener/parser এই publication flags এবং isUrgent/urgent alias পড়ে।',
                'Notices are stored at /notices/{id}. Draft writes isPublished=false and published=false; Publish writes both true. The Android notices listener/parser reads these publish flags and the isUrgent/urgent aliases.'
              )}
            </p>
            <p className="mt-1 flex items-center gap-1 text-[11px] text-sky-800">
              <ShieldCheck className="h-3.5 w-3.5" />
              {t(
                'বিদ্যমান Firestore Rules ও RBAC অপরিবর্তিত; content writes require active Super Admin or Editor.',
                'Existing Firestore Rules and RBAC are unchanged; content writes require an active Super Admin or Editor.'
              )}
            </p>
          </div>
        </div>
      </div>

      <IntegratedModuleCrudEditor module={module} />

      <details className="rounded-xl border border-slate-200 bg-white p-4">
        <summary className="cursor-pointer text-sm font-semibold text-slate-800">
          {t('পুরোনো Notices Manager', 'Existing Notices Manager')}
        </summary>
        <p className="mt-2 text-xs text-amber-800">
          {t(
            'পুরোনো Manager legacy schema ব্যবহার করে এবং publish flag নিয়ন্ত্রণ করে না। Android-এর published-only sync-এর জন্য উপরের integrated editor ব্যবহার করুন।',
            'The legacy manager uses the older schema and does not control publish flags. Use the integrated editor above for Android published-only sync.'
          )}
        </p>
        <div className="mt-4">
          <NoticesManager />
        </div>
      </details>
    </div>
  );
}
