import React from 'react';
import {
  Database,
  Globe2,
  LogOut,
  Menu,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import { getRoleLabel } from '../../services/authService';

interface TopbarProps {
  onOpenMobileMenu: () => void;
}

export const Topbar: React.FC<TopbarProps> = ({ onOpenMobileMenu }) => {
  const { adminUser, logout } = useAuth();
  const { lang, toggleLang, t } = useLanguage();

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-xs border-b border-slate-200 px-4 lg:px-6 py-3 flex items-center justify-between gap-3">
      <div className="flex items-center gap-3 min-w-0">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          aria-label={t('নেভিগেশন মেনু খুলুন', 'Open navigation menu')}
          className="lg:hidden p-2 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
              {t(
                'আশেক উল্লাহ রফিক — উন্নয়ন ও জনসংযোগ পোর্টাল',
                'Asheq Ullah Rafiq — Development & PR Portal'
              )}
            </h1>
          </div>
          <div className="flex items-center gap-2 text-[11px] text-slate-500 mt-0.5">
            <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold">
              <Database className="h-3 w-3" />
              {t('সংযুক্ত লাইভ Firestore', 'Connected Live Firestore')}
            </span>
            <span className="hidden sm:inline text-slate-300">•</span>
            <span className="hidden sm:inline truncate">
              asheq-ullah-rafiq-digital-p
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3 shrink-0">
        {/* Bilingual Toggle */}
        <button
          type="button"
          onClick={toggleLang}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-xs font-semibold text-slate-700 transition-colors"
        >
          <Globe2 className="h-3.5 w-3.5 text-emerald-700" />
          <span>{lang === 'bn' ? 'বাংলা / EN' : 'EN / বাংলা'}</span>
        </button>

        {/* Admin Identity Badge */}
        {adminUser && (
          <div className="hidden md:flex items-center gap-2.5 pl-3 border-l border-slate-200">
            <div className="text-right">
              <p className="text-xs font-semibold text-slate-900 leading-tight max-w-[180px] truncate">
                {adminUser.email || adminUser.displayName}
              </p>
              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700">
                <ShieldCheck className="h-3 w-3" />
                {getRoleLabel(adminUser.role, lang)}
              </span>
            </div>
          </div>
        )}

        {/* Logout */}
        <button
          type="button"
          onClick={() => void logout()}
          title={t('সাইন আউট', 'Sign Out')}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-rose-200 bg-rose-50 hover:bg-rose-100 text-xs font-semibold text-rose-700 transition-colors"
        >
          <LogOut className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">{t('লগআউট', 'Logout')}</span>
        </button>
      </div>
    </header>
  );
};
