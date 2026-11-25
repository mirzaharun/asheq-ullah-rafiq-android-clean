import type { ReactNode } from 'react';
import { useLanguage } from '../../hooks/useLanguage';
import { toLocalizedNumber } from '../../utils/formatters';

interface StatCardProps {
  label: string;
  value: number | null;
  sublabel?: string;
  icon: ReactNode;
  accent?: 'blue' | 'emerald' | 'amber' | 'slate';
  loading?: boolean;
}

const ACCENT_ICON_STYLES: Record<NonNullable<StatCardProps['accent']>, string> = {
  blue: 'bg-blue-50 text-blue-700',
  emerald: 'bg-emerald-50 text-emerald-700',
  amber: 'bg-amber-50 text-amber-700',
  slate: 'bg-slate-100 text-slate-700',
};

export function StatCard({
  label,
  value,
  sublabel,
  icon,
  accent = 'blue',
  loading = false,
}: StatCardProps) {
  const { lang } = useLanguage();

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col justify-between gap-3">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-slate-600 leading-snug">{label}</p>
        <div
          className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${ACCENT_ICON_STYLES[accent]}`}
          aria-hidden="true"
        >
          {icon}
        </div>
      </div>

      <div>
        {loading ? (
          <div className="h-8 w-20 bg-slate-100 rounded animate-pulse" />
        ) : (
          <p className="text-3xl font-bold tracking-tight text-slate-900 tabular-nums font-display">
            {value === null ? '—' : toLocalizedNumber(value, lang)}
          </p>
        )}
        {sublabel ? (
          <p className="mt-1.5 text-xs text-slate-500">{sublabel}</p>
        ) : null}
      </div>
    </div>
  );
}
