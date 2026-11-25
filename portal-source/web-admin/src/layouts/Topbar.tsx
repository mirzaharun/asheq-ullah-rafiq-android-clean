import { LogOut, Menu } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { LanguageToggle } from '../components/common/LanguageToggle';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { SIDEBAR_MODULES } from '../utils/navigation';

interface TopbarProps {
  onOpenMobileMenu: () => void;
}

export function Topbar({ onOpenMobileMenu }: TopbarProps) {
  const { admin, logout } = useAuth();
  const { lang, t } = useLanguage();
  const location = useLocation();

  const currentModule = SIDEBAR_MODULES.find((m) => m.path === location.pathname);
  const currentTitle = currentModule
    ? lang === 'bn'
      ? currentModule.labelBn
      : currentModule.labelEn
    : t.dashboardTitle;

  const roleLabel = admin ? t.roleLabels[admin.role] : '';

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-slate-200 px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          className="lg:hidden p-2 -ml-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors"
          aria-label="Open navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2 text-sm min-w-0">
          <span className="hidden sm:inline text-slate-400 whitespace-nowrap shrink-0">
            {t.adminConsoleLabel}
          </span>
          <span className="hidden sm:inline text-slate-300" aria-hidden="true">
            /
          </span>
          <h1 className="font-semibold text-slate-900 truncate">{currentTitle}</h1>
        </div>
      </div>

      <div className="flex items-center gap-3 shrink-0">
        <LanguageToggle />

        {admin ? (
          <div className="hidden md:flex items-center gap-2 text-xs text-slate-600 pl-2 border-l border-slate-200">
            <span className="font-medium text-slate-900 truncate max-w-[160px]">
              {admin.email}
            </span>
            <span className="text-slate-300" aria-hidden="true">
              ·
            </span>
            <span className="text-emerald-700 font-medium whitespace-nowrap">
              {roleLabel}
            </span>
          </div>
        ) : null}

        <button
          type="button"
          onClick={logout}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-700 bg-slate-100 hover:bg-red-50 hover:text-red-700 rounded-lg transition-colors whitespace-nowrap shrink-0 cursor-pointer"
        >
          <LogOut className="w-3.5 h-3.5" />
          <span>{t.signOutButton}</span>
        </button>
      </div>
    </header>
  );
}
