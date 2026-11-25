import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Database, Layers, ShieldAlert } from 'lucide-react';
import { useLanguage } from '../hooks/useLanguage';
import { fetchCollectionCountSafe } from '../services/firebase/crudService';
import type { SidebarNavItem } from '../types/navigation';
import { toLocalizedNumber } from '../utils/formatters';

interface ModulePlaceholderPageProps {
  module: SidebarNavItem;
}

export function ModulePlaceholderPage({ module }: ModulePlaceholderPageProps) {
  const { lang, t } = useLanguage();
  const [docCount, setDocCount] = useState<number | null>(null);
  const [checking, setChecking] = useState(Boolean(module.collectionName));

  useEffect(() => {
    let active = true;
    if (!module.collectionName) {
      setDocCount(null);
      setChecking(false);
      return;
    }

    setChecking(true);
    fetchCollectionCountSafe(module.collectionName).then((count) => {
      if (!active) return;
      setDocCount(count);
      setChecking(false);
    });

    return () => {
      active = false;
    };
  }, [module.collectionName]);

  return (
    <div className="max-w-2xl bg-white border border-slate-200 rounded-xl p-6 sm:p-8 space-y-5">
      <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-blue-700">
        <Layers className="w-4 h-4" />
        <span>
          {lang === 'bn' ? `ধাপ (Phase ${module.phase})` : `Phase ${module.phase}`}
        </span>
        {module.collectionName ? (
          <>
            <span className="text-slate-300" aria-hidden="true">
              ·
            </span>
            <span className="font-mono text-slate-500 inline-flex items-center gap-1">
              <Database className="w-3.5 h-3.5" />
              {module.collectionName}
            </span>
            <span className="text-slate-300" aria-hidden="true">
              ·
            </span>
            <span className="font-mono text-slate-600 tabular-nums">
              {checking
                ? lang === 'bn'
                  ? 'যাচাই হচ্ছে...'
                  : 'Checking...'
                : docCount === null
                ? lang === 'bn'
                  ? 'অনুমতি/সংখ্যা অনুপলব্ধ'
                  : 'Count unavailable'
                : lang === 'bn'
                ? `${toLocalizedNumber(docCount, lang)} টি ডকুমেন্ট`
                : `${docCount} documents`}
            </span>
          </>
        ) : null}
      </div>

      <h2 className="text-2xl font-bold text-slate-900 font-display">
        {lang === 'bn' ? module.labelBn : module.labelEn}
      </h2>

      <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex items-start gap-3">
        <ShieldAlert className="w-5 h-5 text-slate-600 shrink-0 mt-0.5" />
        <div className="text-sm text-slate-800 space-y-1.5 leading-relaxed">
          <p className="font-semibold">
            {lang === 'bn'
              ? 'কাস্টম মডিউল রুট নির্ধারিত হয়নি'
              : 'Dedicated Route Not Assigned'}
          </p>
          <p className="text-xs text-slate-600">
            {lang === 'bn'
              ? `"${module.collectionName ?? module.id}" সংগ্রহের জন্য কোনো নির্ধারিত অ্যাডমিন ভিউ কনফিগার করা হয়নি।`
              : `No custom admin view is currently registered for "${module.collectionName ?? module.id}".`}
          </p>
        </div>
      </div>

      <div className="pt-2 border-t border-slate-100">
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-700 hover:bg-blue-800 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>{t.backToDashboard}</span>
        </Link>
      </div>
    </div>
  );
}

