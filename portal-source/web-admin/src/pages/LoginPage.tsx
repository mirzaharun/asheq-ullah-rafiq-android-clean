import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Eye,
  EyeOff,
  Globe2,
  Loader2,
  Lock,
  Mail,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';

export const LoginPage: React.FC = () => {
  const { status, authError, clearError, loginWithEmail, loginWithGoogle } =
    useAuth();
  const { lang, toggleLang, t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fromPath =
    (location.state as { from?: { pathname?: string } } | null)?.from
      ?.pathname || '/dashboard';

  useEffect(() => {
    if (status === 'authenticated') {
      navigate(fromPath, { replace: true });
    }
  }, [status, fromPath, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) return;
    setSubmitting(true);
    try {
      await loginWithEmail(email, password);
    } catch {
      // Error surfaced via authError state
    } finally {
      setSubmitting(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setSubmitting(true);
    try {
      await loginWithGoogle();
    } catch {
      // Error surfaced via authError state
    } finally {
      setSubmitting(false);
    }
  };

  const isBusy = submitting || status === 'verifying' || status === 'loading';

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-950 via-emerald-900 to-slate-900 flex flex-col justify-center items-center p-4">
      {/* Top Language Switch */}
      <div className="w-full max-w-md flex justify-end mb-4">
        <button
          type="button"
          onClick={toggleLang}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold border border-white/15 transition-colors"
        >
          <Globe2 className="h-3.5 w-3.5" />
          <span>{lang === 'bn' ? 'বাংলা / EN' : 'EN / বাংলা'}</span>
        </button>
      </div>

      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-emerald-900/10 overflow-hidden">
        {/* Header */}
        <div className="bg-emerald-900 px-6 py-6 text-white">
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-2xl bg-emerald-700/80 border border-emerald-400/30 flex items-center justify-center shrink-0">
              <ShieldCheck className="h-6 w-6 text-emerald-100" />
            </div>
            <div>
              <h1 className="text-lg font-bold leading-snug">
                {t('আশেক উল্লাহ রফিক', 'Asheq Ullah Rafiq')}
              </h1>
              <p className="text-xs text-emerald-100/90 mt-0.5">
                {t(
                  'উন্নয়ন ও জনসংযোগ পোর্টাল — ওয়েব এডমিন',
                  'Development & Public Relations — Web Admin'
                )}
              </p>
            </div>
          </div>
        </div>

        {/* Form Body */}
        <div className="p-6 sm:p-7">
          <div className="mb-5">
            <h2 className="text-base font-bold text-slate-900">
              {t('অ্যাডমিনিস্ট্রেটর সাইন ইন', 'Administrator Sign In')}
            </h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              {t(
                'শুধুমাত্র অনুমোদিত সুপার এডমিন, এডিটর ও আবেদন ব্যবস্থাপক লগইন করতে পারবেন।',
                'Restricted to server-verified Super Admins, Editors, and Application Managers.'
              )}
            </p>
          </div>

          {authError && (
            <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-3 flex items-start gap-2.5 text-xs text-rose-800">
              <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <div className="flex-1 leading-relaxed">{authError}</div>
              <button
                type="button"
                onClick={clearError}
                className="text-rose-500 hover:text-rose-700 font-bold"
              >
                ×
              </button>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                {t('অ্যাডমিন ইমেইল', 'Administrator Email')}
              </label>
              <div className="relative">
                <Mail className="h-4 w-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@example.com"
                  className="w-full pl-10 pr-3.5 py-2.5 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-600/20 focus:border-emerald-600"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                {t('পাসওয়ার্ড', 'Password')}
              </label>
              <div className="relative">
                <Lock className="h-4 w-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-10 py-2.5 text-sm border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-600/20 focus:border-emerald-600"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={
                    showPassword
                      ? t('পাসওয়ার্ড লুকান', 'Hide password')
                      : t('পাসওয়ার্ড দেখুন', 'Show password')
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isBusy}
              className="w-full py-2.5 px-4 rounded-xl bg-emerald-800 hover:bg-emerald-900 disabled:opacity-60 text-white text-sm font-semibold flex items-center justify-center gap-2 transition-colors shadow-sm"
            >
              {isBusy && <Loader2 className="h-4 w-4 animate-spin" />}
              <span>
                {status === 'verifying'
                  ? t('ভূমিকা যাচাই চলছে…', 'Verifying RBAC Role…')
                  : t('সাইন ইন করুন', 'Sign In')}
              </span>
            </button>
          </form>

          <div className="my-5 flex items-center gap-3">
            <div className="h-px flex-1 bg-slate-200" />
            <span className="text-[11px] font-medium text-slate-400 uppercase">
              {t('অথবা', 'OR')}
            </span>
            <div className="h-px flex-1 bg-slate-200" />
          </div>

          <button
            type="button"
            disabled={isBusy}
            onClick={() => void handleGoogleSignIn()}
            className="w-full py-2.5 px-4 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-60 text-slate-700 text-sm font-semibold flex items-center justify-center gap-2.5 transition-colors"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="#4285F4"
                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"
              />
              <path
                fill="#EA4335"
                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"
              />
            </svg>
            <span>
              {t('Google দিয়ে সাইন ইন করুন', 'Continue with Google')}
            </span>
          </button>

          <p className="mt-5 text-[11px] text-center text-slate-400">
            {t(
              'নিরাপত্তা নীতি: সার্ভার-যাচাইকৃত admins ডকুমেন্ট বা কাস্টম ক্লেইম ব্যতীত প্রবেশাধিকার দেওয়া হয় না।',
              'Zero-trust policy: Privileges are verified strictly against Firebase rules & admins directory.'
            )}
          </p>
        </div>
      </div>
    </div>
  );
};
