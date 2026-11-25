import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Briefcase,
  Building2,
  Calendar,
  ClipboardList,
  Clock,
  CloudSun,
  Compass,
  Droplets,
  FileText,
  Film,
  Globe,
  GraduationCap,
  HeartHandshake,
  HelpCircle,
  History,
  Images,
  Landmark,
  Layers,
  LayoutDashboard,
  LayoutGrid,
  Link2,
  Megaphone,
  MessageSquareHeart,
  MessageSquareWarning,
  Newspaper,
  PhoneCall,
  Radio,
  Settings,
  Share2,
  ShieldCheck,
  Ship,
  Smartphone,
  Tags,
  UserCheck,
  Users,
  Vote,
  Waves,
  X,
} from 'lucide-react';
import { getModulesForRole } from '../../config/modules';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import { AdminModuleDefinition, ModuleGroup } from '../../types/portal';

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  Settings,
  UserCheck,
  LayoutGrid,
  Building2,
  Layers,
  Megaphone,
  Images,
  Newspaper,
  Film,
  FileText,
  Link2,
  CalendarStar: Calendar,
  Tags,
  CloudSun,
  Ship,
  Waves,
  Radio,
  PhoneCall,
  Droplets,
  Globe,
  GraduationCap,
  Briefcase,
  HeartHandshake,
  Landmark,
  Compass,
  Vote,
  Clock,
  HelpCircle,
  Users,
  Share2,
  ClipboardList,
  MessageSquareWarning,
  MessageSquareHeart,
  ShieldCheck,
  History,
};

interface SidebarProps {
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ mobileOpen, onCloseMobile }) => {
  const { adminUser } = useAuth();
  const { lang, t } = useLanguage();

  const modules = getModulesForRole(adminUser?.role ?? null);

  const grouped: Record<ModuleGroup, AdminModuleDefinition[]> = {
    cms: modules.filter((m) => m.group === 'cms'),
    citizen: modules.filter((m) => m.group === 'citizen'),
    governance: modules.filter((m) => m.group === 'governance'),
  };

  const renderGroup = (
    labelBn: string,
    labelEn: string,
    items: AdminModuleDefinition[]
  ) => {
    if (items.length === 0) return null;
    return (
      <div className="mt-5">
        <p className="px-3 mb-1.5 text-[11px] font-bold uppercase tracking-wider text-emerald-300/80">
          {t(labelBn, labelEn)} ({items.length})
        </p>
        <div className="space-y-0.5">
          {items.map((mod) => {
            const Icon = ICON_MAP[mod.iconName] || FileText;
            return (
              <NavLink
                key={mod.key}
                to={`/modules/${mod.key}`}
                onClick={onCloseMobile}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium transition-colors ${
                    isActive
                      ? 'bg-white text-emerald-950 font-semibold shadow-xs'
                      : 'text-emerald-50/85 hover:bg-emerald-800/70 hover:text-white'
                  }`
                }
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span className="truncate">
                  {lang === 'bn' ? mod.titleBn : mod.titleEn}
                </span>
              </NavLink>
            );
          })}
        </div>
      </div>
    );
  };

  const sidebarBody = (
    <div className="flex flex-col h-full bg-emerald-950 text-white w-72 select-none">
      {/* Brand Header */}
      <div className="px-4 py-4 border-b border-emerald-800/70 flex items-center justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-bold text-white leading-snug truncate">
            {t('আশেক উল্লাহ রফিক', 'Asheq Ullah Rafiq')}
          </p>
          <p className="text-[11px] text-emerald-200/90 truncate">
            {t('উন্নয়ন ও জনসংযোগ এডমিন প্যানেল', 'Development & PR Admin Panel')}
          </p>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          aria-label={t('মেনু বন্ধ করুন', 'Close menu')}
          className="lg:hidden p-1.5 rounded-lg text-emerald-200 hover:bg-emerald-800"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 custom-scrollbar">
        <NavLink
          to="/dashboard"
          onClick={onCloseMobile}
          className={({ isActive }) =>
            `flex items-center gap-2.5 px-3 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
              isActive
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-emerald-50 hover:bg-emerald-800/70'
            }`
          }
        >
          <LayoutDashboard className="h-4 w-4 shrink-0" />
          <span>{t('লাইভ ড্যাশবোর্ড', 'Live Dashboard')}</span>
        </NavLink>

        {adminUser?.role === 'super_admin' && (
          <NavLink
            to="/app-releases"
            onClick={onCloseMobile}
            className={({ isActive }) =>
              `mt-1 flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors ${
                isActive
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'text-emerald-50 hover:bg-emerald-800/70'
              }`
            }
          >
            <Smartphone className="h-4 w-4 shrink-0" />
            <span>{t('অ্যান্ড্রয়েড APK ব্যবস্থাপনা', 'Android APK Management')}</span>
          </NavLink>
        )}

        {renderGroup('নাগরিক সেবা ও আবেদন', 'Citizen Services & Intake', grouped.citizen)}
        {renderGroup('পোর্টাল কন্টেন্ট (CMS)', 'Portal Content (CMS)', grouped.cms)}
        {renderGroup('নিরাপত্তা ও প্রশাসন', 'Security & Governance', grouped.governance)}
      </nav>

      {/* Footer Info */}
      <div className="px-4 py-3 border-t border-emerald-800/70 text-[11px] text-emerald-200/80">
        <p className="font-medium text-emerald-100">
          {t('মহেশখালী • কুতুবদিয়া পোর্টাল', 'Moheshkhali • Kutubdia Portal')}
        </p>
        <p className="mt-0.5 truncate">
          {t('সম্পূর্ণ লাইভ CMS ও রোল-ভিত্তিক নিয়ন্ত্রণ', 'Production CMS • RBAC Enforced')}
        </p>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Static Sidebar */}
      <aside className="hidden lg:block shrink-0 h-screen sticky top-0">
        {sidebarBody}
      </aside>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden flex">
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs"
            onClick={onCloseMobile}
          />
          <div className="relative z-50 h-full">{sidebarBody}</div>
        </div>
      )}
    </>
  );
};
