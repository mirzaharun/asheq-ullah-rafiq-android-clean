import { Languages } from 'lucide-react';
import { useLanguage } from '../../hooks/useLanguage';

export function LanguageToggle() {
  const { lang, toggleLang, t } = useLanguage();

  return (
    <button
      type="button"
      onClick={toggleLang}
      aria-label={lang === 'bn' ? 'Switch to English' : 'বাংলায় দেখুন'}
      className="inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 hover:text-blue-700 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
    >
      <Languages className="w-3.5 h-3.5 text-blue-700" />
      <span>{t.languageToggle}</span>
    </button>
  );
}
