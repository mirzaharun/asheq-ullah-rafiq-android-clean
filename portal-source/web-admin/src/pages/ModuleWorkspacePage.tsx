import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Database, Info, ShieldCheck } from 'lucide-react';
import { ModuleCrudEditor } from '../components/crud/ModuleCrudEditor';
import { StatusBadge } from '../components/crud/CrudPrimitives';
import { ANDROID_SCREEN_MAP, getModuleByKey } from '../config/modules';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { canAccessModule, getRoleLabel } from '../services/authService';
import { UnauthorizedPage } from './UnauthorizedPage';

export const ModuleWorkspacePage: React.FC = () => {
  const { moduleKey = '' } = useParams<{ moduleKey: string }>();
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();
  const module = getModuleByKey(moduleKey);
  const hasAccess = module
    ? canAccessModule(adminUser?.role, module)
    : false;

  if (!module) {
    return (
      <div className="mx-auto mt-8 max-w-lg rounded-2xl border border-slate-200 bg-white p-8 text-center">
        <h2 className="text-lg font-bold text-slate-900">
          {t('মডিউল পাওয়া যায়নি', 'Module Not Found')}
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          {t(
            'অনুরোধকৃত মডিউল রুটটি পোর্টাল রেজিস্ট্রিতে নেই।',
            'The requested module key does not exist in the portal registry.'
          )}
        </p>
        <Link
          to="/dashboard"
          className="mt-5 inline-flex items-center gap-2 rounded-xl bg-emerald-800 px-4 py-2 text-sm font-semibold text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>{t('ড্যাশবোর্ডে ফিরুন', 'Back to Dashboard')}</span>
        </Link>
      </div>
    );
  }

  if (!hasAccess) return <UnauthorizedPage />;

  const androidScreens = ANDROID_SCREEN_MAP[module.key] ?? [];
  const isReadOnly = module.collection === 'audit_logs';

  return (
    <div className="space-y-5">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <div className="mb-1 flex items-center gap-2 text-xs text-slate-500">
            <Link to="/dashboard" className="font-medium hover:text-emerald-700">
              {t('ড্যাশবোর্ড', 'Dashboard')}
            </Link>
            <span>/</span>
            <span className="font-mono text-slate-700">{module.collection}</span>
          </div>
          <h2 className="text-xl font-bold text-slate-900">
            {lang === 'bn' ? module.titleBn : module.titleEn}
          </h2>
          <p className="mt-0.5 text-sm text-slate-600">
            {lang === 'bn' ? module.descriptionBn : module.descriptionEn}
          </p>
        </div>
        <StatusBadge
          status={
            isReadOnly
              ? t('অপরিবর্তনীয় অডিট', 'Immutable audit')
              : module.storageKind === 'singleton'
                ? t('সিঙ্গেলটন ডকুমেন্ট', 'Singleton document')
                : t('CRUD মডিউল', 'CRUD workspace')
          }
        />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <Database className="h-4 w-4 text-emerald-700" />
            <span>{t('Firestore টার্গেট পাথ', 'Firestore target path')}</span>
          </div>
          <p className="mt-2 break-all font-mono text-sm font-bold text-slate-900">
            /{module.collection}
            {module.singletonDocId ? `/${module.singletonDocId}` : ''}
          </p>
          {module.mirrorCollection && (
            <p className="mt-1 font-mono text-xs text-slate-500">
              + /{module.mirrorCollection}
            </p>
          )}
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
            <ShieldCheck className="h-4 w-4 text-emerald-700" />
            <span>{t('অনুমোদিত রোল', 'Authorized roles')}</span>
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {module.allowedRoles.map((role) => (
              <span
                key={role}
                className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-800"
              >
                {getRoleLabel(role, lang)}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-sky-200 bg-sky-50/70 px-4 py-3">
        <div className="flex items-start gap-2.5">
          <Info className="mt-0.5 h-4 w-4 shrink-0 text-sky-700" />
          <div>
            <p className="text-xs font-bold text-sky-950">
              {t('Android স্ক্রিন সংযোগ', 'Android screen integration')}
            </p>
            <p className="mt-1 text-xs leading-relaxed text-sky-900">
              {androidScreens.length
                ? androidScreens.join(' · ')
                : t('কোনো Android স্ক্রিনে ব্যবহৃত নয়', 'Not consumed by an Android screen')}
            </p>
            <p className="mt-1 text-[11px] leading-relaxed text-sky-800">
              {t(
                'সংরক্ষিত পরিবর্তন Firestore Rules দ্বারা যাচাই হবে; প্রকাশিত রেকর্ডই অ্যাপে দেখানো হয়।',
                'Writes are checked by Firestore Rules; published records are what the app displays.'
              )}
            </p>
          </div>
        </div>
      </div>

      <ModuleCrudEditor module={module} />
    </div>
  );
};
