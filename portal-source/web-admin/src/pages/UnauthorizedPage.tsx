import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, LogOut, ShieldBan } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { getRoleLabel } from '../services/authService';

export const UnauthorizedPage: React.FC = () => {
  const { adminUser, logout } = useAuth();
  const { lang, t } = useLanguage();

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white border border-rose-200 rounded-3xl p-7 shadow-sm text-center">
        <div className="h-14 w-14 rounded-2xl bg-rose-50 text-rose-700 flex items-center justify-center mx-auto mb-4">
          <ShieldBan className="h-7 w-7" />
        </div>
        <h1 className="text-lg font-bold text-slate-900">
          {t('অনুমতি সংরক্ষিত (Access Denied)', 'Access Denied (RBAC)')}
        </h1>
        <p className="text-sm text-slate-600 mt-2 leading-relaxed">
          {t(
            'আপনার বর্তমান অ্যাকাউন্ট রোলের জন্য এই মডিউল বা পেজটি উন্মুক্ত নয়।',
            'Your verified administrator role does not have permission to access this module.'
          )}
        </p>

        {adminUser && (
          <div className="mt-4 bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-xs text-slate-600">
            <span className="font-semibold text-slate-800">{adminUser.email}</span>
            <span className="mx-2 text-slate-300">•</span>
            <span>{getRoleLabel(adminUser.role, lang)}</span>
          </div>
        )}

        <div className="mt-6 flex flex-col sm:flex-row gap-2.5 justify-center">
          <Link
            to="/dashboard"
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-800 hover:bg-emerald-900 text-white text-sm font-semibold transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>{t('ড্যাশবোর্ডে ফিরুন', 'Back to Dashboard')}</span>
          </Link>
          <button
            type="button"
            onClick={() => void logout()}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-semibold transition-colors"
          >
            <LogOut className="h-4 w-4" />
            <span>{t('লগআউট', 'Sign Out')}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
