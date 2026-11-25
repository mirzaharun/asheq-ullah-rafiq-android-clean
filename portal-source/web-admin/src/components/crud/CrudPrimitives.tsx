import React from 'react';
import {
  AlertTriangle,
  Inbox,
  Search,
  ShieldAlert,
  X,
} from 'lucide-react';
import { useLanguage } from '../../contexts/LanguageContext';

export interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  accent?: 'emerald' | 'blue' | 'amber' | 'rose' | 'indigo' | 'slate';
  restricted?: boolean;
  error?: string | null;
}

const ACCENT_MAP: Record<NonNullable<StatCardProps['accent']>, string> = {
  emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  blue: 'bg-sky-50 text-sky-700 border-sky-200',
  amber: 'bg-amber-50 text-amber-700 border-amber-200',
  rose: 'bg-rose-50 text-rose-700 border-rose-200',
  indigo: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  slate: 'bg-slate-100 text-slate-700 border-slate-200',
};

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  accent = 'emerald',
  restricted = false,
  error = null,
}) => {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-xs hover:shadow-sm transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold text-slate-500 truncate">{title}</p>
          <p className="text-2xl font-bold text-slate-900 mt-1.5 tracking-tight">
            {restricted ? '—' : value}
          </p>
          {subtitle && (
            <p className="text-xs text-slate-500 mt-1 truncate">{subtitle}</p>
          )}
        </div>
        <div
          className={`h-11 w-11 rounded-xl border flex items-center justify-center shrink-0 ${ACCENT_MAP[accent]}`}
        >
          {icon}
        </div>
      </div>
      {error && (
        <p className="mt-3 text-[11px] text-rose-700 bg-rose-50 border border-rose-200 rounded-lg px-2.5 py-1.5">
          {error}
        </p>
      )}
    </div>
  );
};

export const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const normalized = status.toLowerCase();
  let cls = 'bg-slate-100 text-slate-700 border-slate-200';

  if (
    normalized.includes('resolved') ||
    normalized.includes('completed') ||
    normalized.includes('normal') ||
    normalized.includes('active') ||
    normalized.includes('স্বাভাবিক')
  ) {
    cls = 'bg-emerald-50 text-emerald-700 border-emerald-200';
  } else if (
    normalized.includes('pending') ||
    normalized.includes('submitted') ||
    normalized.includes('review') ||
    normalized.includes('progress') ||
    normalized.includes('scheduled') ||
    normalized.includes('ongoing')
  ) {
    cls = 'bg-amber-50 text-amber-800 border-amber-200';
  } else if (
    normalized.includes('reject') ||
    normalized.includes('cancel') ||
    normalized.includes('urgent') ||
    normalized.includes('suspend') ||
    normalized.includes('বন্ধ')
  ) {
    cls = 'bg-rose-50 text-rose-700 border-rose-200';
  } else if (normalized.includes('forwarded')) {
    cls = 'bg-sky-50 text-sky-700 border-sky-200';
  }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${cls}`}
    >
      {status}
    </span>
  );
};

export interface DataColumn<T> {
  key: string;
  header: string;
  align?: 'left' | 'right';
  render: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: readonly DataColumn<T>[];
  rows: readonly T[];
  rowKey: (row: T) => string;
  emptyTitle?: string;
  emptyDescription?: string;
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyTitle,
  emptyDescription,
}: DataTableProps<T>) {
  const { t } = useLanguage();

  if (rows.length === 0) {
    return (
      <EmptyState
        title={emptyTitle || t('কোনো তথ্য পাওয়া যায়নি', 'No records found')}
        description={
          emptyDescription ||
          t(
            'এই ফায়ারস্টোর কালেকশনে বর্তমানে কোনো ডকুমেন্ট নেই।',
            'There are currently no documents in this Firestore collection.'
          )
        }
      />
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50/80">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className={`px-4 py-3 text-xs font-bold uppercase tracking-wider text-slate-600 ${
                  col.align === 'right' ? 'text-right' : 'text-left'
                }`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {rows.map((row) => (
            <tr key={rowKey(row)} className="hover:bg-slate-50/70 transition-colors">
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={`px-4 py-3 text-slate-700 align-top ${
                    col.align === 'right' ? 'text-right' : 'text-left'
                  }`}
                >
                  {col.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

interface FilterToolbarProps {
  searchValue: string;
  onSearchChange: (val: string) => void;
  placeholder?: string;
  rightSlot?: React.ReactNode;
}

export const FilterToolbar: React.FC<FilterToolbarProps> = ({
  searchValue,
  onSearchChange,
  placeholder,
  rightSlot,
}) => {
  const { t } = useLanguage();
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div className="relative flex-1 max-w-md">
        <Search className="h-4 w-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchValue}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={placeholder || t('অনুসন্ধান করুন…', 'Search records…')}
          className="w-full pl-9 pr-9 py-2 text-sm bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-600/20 focus:border-emerald-600"
        />
        {searchValue && (
          <button
            type="button"
            onClick={() => onSearchChange('')}
            aria-label={t('ফিল্টার মুছুন', 'Clear filter')}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
      {rightSlot && <div className="flex items-center gap-2">{rightSlot}</div>}
    </div>
  );
};

export const PermissionErrorBanner: React.FC<{
  title?: string;
  message: string;
}> = ({ title, message }) => {
  const { t } = useLanguage();
  return (
    <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 flex items-start gap-3">
      <ShieldAlert className="h-5 w-5 text-rose-700 shrink-0 mt-0.5" />
      <div className="text-xs text-rose-800">
        <p className="font-bold text-rose-900">
          {title || t('Firestore নিরাপত্তা বা ডাটাবেজ ত্রুটি', 'Firestore Rule / Query Notice')}
        </p>
        <p className="mt-0.5 leading-relaxed">{message}</p>
      </div>
    </div>
  );
};

export const EmptyState: React.FC<{
  title: string;
  description: string;
}> = ({ title, description }) => (
  <div className="py-10 px-4 text-center flex flex-col items-center justify-center">
    <div className="h-12 w-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mb-3">
      <Inbox className="h-6 w-6" />
    </div>
    <p className="text-sm font-semibold text-slate-800">{title}</p>
    <p className="text-xs text-slate-500 mt-1 max-w-md">{description}</p>
  </div>
);

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel,
  loading = false,
  onConfirm,
  onCancel,
}) => {
  const { t } = useLanguage();
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6">
        <div className="flex items-start gap-3">
          <div className="h-10 w-10 rounded-xl bg-rose-50 text-rose-700 flex items-center justify-center shrink-0">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-900">{title}</h3>
            <p className="text-sm text-slate-600 mt-1 leading-relaxed">{description}</p>
          </div>
        </div>
        <div className="mt-6 flex items-center justify-end gap-2.5">
          <button
            type="button"
            disabled={loading}
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            {cancelLabel || t('বাতিল', 'Cancel')}
          </button>
          <button
            type="button"
            disabled={loading}
            onClick={onConfirm}
            className="px-4 py-2 text-sm font-semibold text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-60 rounded-xl transition-colors"
          >
            {loading
              ? t('প্রক্রিয়া চলছে…', 'Processing…')
              : confirmLabel || t('নিশ্চিত করুন', 'Confirm')}
          </button>
        </div>
      </div>
    </div>
  );
};
