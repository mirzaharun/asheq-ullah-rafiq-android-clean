import { Info } from 'lucide-react';

interface SchemaNoticeBannerProps {
  title: string;
  description: string;
}

export function SchemaNoticeBanner({ title, description }: SchemaNoticeBannerProps) {
  return (
    <div className="bg-blue-50/70 border border-blue-200 rounded-xl p-4 flex items-start gap-3 text-sm text-slate-800">
      <Info className="w-4 h-4 text-blue-700 shrink-0 mt-0.5" />
      <div>
        <p className="font-semibold text-slate-900">{title}</p>
        <p className="mt-0.5 text-xs text-slate-600 leading-relaxed">{description}</p>
      </div>
    </div>
  );
}
