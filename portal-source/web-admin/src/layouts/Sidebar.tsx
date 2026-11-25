import { NavLink } from 'react-router-dom';
import {
  BarChart3,
  Bell,
  Briefcase,
  Building,
  Building2,
  CalendarDays,
  Clapperboard,
  CloudLightning,
  Compass,
  Cpu,
  Droplets,
  ExternalLink,
  FileArchive,
  FileText,
  Globe,
  GraduationCap,
  HeartHandshake,
  HelpCircle,
  Home,
  Image,
  ImagePlus,
  Landmark,
  LayoutDashboard,
  MapPin,
  Megaphone,
  MessageSquare,
  Newspaper,
  PhoneCall,
  PieChart,
  Radio,
  Server,
  Settings,
  Share2,
  Shield,
  ShieldCheck,
  Ship,
  Smartphone,
  Sparkles,
  Tags,
  UserCheck,
  Users,
  Video,
  Waves,
  X,
} from 'lucide-react';
import type { ComponentType } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { NAV_GROUP_LABELS, SIDEBAR_MODULES } from '../utils/navigation';
import type { NavGroupId } from '../types/navigation';

const ICON_MAP: Record<string, ComponentType<{ className?: string }>> = {
  LayoutDashboard,
  UserCheck,
  Home,
  Building,
  Building2,
  Landmark,
  FileText,
  Megaphone,
  CalendarDays,
  MapPin,
  PhoneCall,
  Droplets,
  GraduationCap,
  Briefcase,
  Compass,
  Ship,
  Waves,
  CloudLightning,
  Radio,
  Image,
  Video,
  Newspaper,
  Share2,
  BarChart3,
  Bell,
  Users,
  PieChart,
  ShieldCheck,
  Shield,
  Settings,
  Sparkles,
  Globe,
  FileArchive,
  ImagePlus,
  HelpCircle,
  ExternalLink,
  HeartHandshake,
  Smartphone,
  Tags,
  Clapperboard,
  Cpu,
  MessageSquare,
  Server,
};


const GROUP_ORDER: NavGroupId[] = [
  'overview',
  'content',
  'citizen_services',
  'coastal_info',
  'media_comm',
  'administration',
];

interface SidebarProps {
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

export function Sidebar({ mobileOpen, onCloseMobile }: SidebarProps) {
  const { admin } = useAuth();
  const { lang, t } = useLanguage();

  const visibleModules = SIDEBAR_MODULES.filter((item) =>
    admin ? item.allowedRoles.includes(admin.role) : true
  );

  const sidebarContent = (
    <div className="flex flex-col h-full bg-[#0B233C] text-slate-100 select-none">
      {/* Brand Header */}
      <div className="px-5 py-5 border-b border-white/10 flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-base font-bold tracking-tight text-white truncate">
            {t.brandTitle}
          </p>
          <p className="text-xs text-emerald-300/90 mt-0.5 truncate">
            {t.brandSubtitle}
          </p>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          className="lg:hidden p-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
          aria-label="Close navigation"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Scrollable 26-module Navigation */}
      <nav
        aria-label={lang === 'bn' ? 'প্রধান নেভিগেশন' : 'Primary navigation'}
        className="flex-1 overflow-y-auto px-3 py-4 space-y-6"
      >
        {GROUP_ORDER.map((groupId) => {
          const items = visibleModules.filter((m) => m.group === groupId);
          if (items.length === 0) return null;

          return (
            <div key={groupId}>
              <p className="px-3 mb-2 text-[11px] font-semibold tracking-wider text-slate-400">
                {lang === 'bn'
                  ? NAV_GROUP_LABELS[groupId].bn
                  : NAV_GROUP_LABELS[groupId].en}
              </p>
              <ul className="space-y-0.5">
                {items.map((item) => {
                  const Icon = ICON_MAP[item.iconName] ?? LayoutDashboard;
                  const label = lang === 'bn' ? item.labelBn : item.labelEn;
                  return (
                    <li key={item.id}>
                      <NavLink
                        to={item.path}
                        onClick={onCloseMobile}
                        className={({ isActive }) =>
                          `flex items-center gap-3 px-3 py-2 rounded-lg text-[13px] font-medium transition-colors whitespace-nowrap ${
                            isActive
                              ? 'bg-emerald-600/20 text-emerald-300 border border-emerald-500/30'
                              : 'text-slate-300 hover:bg-white/5 hover:text-white border border-transparent'
                          }`
                        }
                      >
                        <Icon className="w-4 h-4 shrink-0" />
                        <span className="truncate">{label}</span>
                      </NavLink>
                    </li>
                  );
                })}
              </ul>
            </div>
          );
        })}
      </nav>
    </div>
  );

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className="hidden lg:block w-[272px] shrink-0 border-r border-slate-200 h-screen sticky top-0">
        {sidebarContent}
      </aside>

      {/* Mobile Drawer Overlay */}
      {mobileOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div
            className="fixed inset-0 bg-slate-950/50"
            onClick={onCloseMobile}
            aria-hidden="true"
          />
          <aside className="relative w-[280px] max-w-[85vw] h-full z-10 shadow-xl">
            {sidebarContent}
          </aside>
        </div>
      ) : null}
    </>
  );
}
