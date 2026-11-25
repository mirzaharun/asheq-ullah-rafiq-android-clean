import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AppLanguage } from '../types/portal';

const STORAGE_KEY = 'asheq_portal_admin_lang';

interface LanguageContextValue {
  lang: AppLanguage;
  setLang: (lang: AppLanguage) => void;
  toggleLang: () => void;
  t: (bn: string, en: string) => string;
}

const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [lang, setLangState] = useState<AppLanguage>(() => {
    try {
      const saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved === 'bn' || saved === 'en') return saved;
    } catch {
      // Ignore storage failures in restricted contexts
    }
    return 'bn';
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // Ignore storage failures
    }
    document.documentElement.lang = lang === 'bn' ? 'bn' : 'en';
  }, [lang]);

  const setLang = useCallback((next: AppLanguage) => {
    setLangState(next);
  }, []);

  const toggleLang = useCallback(() => {
    setLangState((prev) => (prev === 'bn' ? 'en' : 'bn'));
  }, []);

  const t = useCallback(
    (bn: string, en: string) => (lang === 'bn' ? bn : en),
    [lang]
  );

  return (
    <LanguageContext.Provider value={{ lang, setLang, toggleLang, t }}>
      {children}
    </LanguageContext.Provider>
  );
};

export function useLanguage(): LanguageContextValue {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useLanguage must be used within LanguageProvider');
  }
  return ctx;
}
