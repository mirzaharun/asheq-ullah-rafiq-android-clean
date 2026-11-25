import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useLanguage } from '../../hooks/useLanguage';
import { toLocalizedNumber } from '../../utils/formatters';

interface PaginationProps {
  currentPage: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  currentPage,
  totalItems,
  pageSize,
  onPageChange,
}: PaginationProps) {
  const { lang } = useLanguage();
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  if (totalItems <= pageSize) return null;

  const start = (currentPage - 1) * pageSize + 1;
  const end = Math.min(totalItems, currentPage * pageSize);

  return (
    <div className="px-4 py-3 border-t border-slate-200 bg-white flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-600">
      <div className="tabular-nums">
        {lang === 'bn'
          ? `মোট ${toLocalizedNumber(totalItems, lang)} টির মধ্যে ${toLocalizedNumber(start, lang)}–${toLocalizedNumber(end, lang)} প্রদর্শিত হচ্ছে`
          : `Showing ${start}–${end} of ${totalItems} entries`}
      </div>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 1}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
        >
          <ChevronLeft className="w-3.5 h-3.5" />
          <span>{lang === 'bn' ? 'পূর্ববর্তী' : 'Prev'}</span>
        </button>
        <span className="px-2 font-mono tabular-nums text-slate-700">
          {toLocalizedNumber(currentPage, lang)} / {toLocalizedNumber(totalPages, lang)}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages}
          className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
        >
          <span>{lang === 'bn' ? 'পরবর্তী' : 'Next'}</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
