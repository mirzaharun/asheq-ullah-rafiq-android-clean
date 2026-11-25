import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Eye, EyeOff, Lock, Mail, ShieldAlert } from 'lucide-react';
import { LanguageToggle } from '../components/common/LanguageToggle';
import { Modal } from '../components/common/Modal';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';
import { sendAdminPasswordReset } from '../services/firebase/authService';

export function LoginPage() {
  const {
    admin,
    initialized,
    loading,
    error,
    unauthorizedInfo,
    firestoreConnected,
    connectionError,
    login,
    clearError,
  } = useAuth();
  const { lang, t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberSession, setRememberSession] = useState(true);
  const [validationError, setValidationError] = useState<string | null>(null);

  const [forgotOpen, setForgotOpen] = useState(false);
  const [resetEmail, setResetEmail] = useState('');
  const [resetLoading, setResetLoading] = useState(false);
  const [resetFeedback, setResetFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const redirectTo =
    (location.state as { from?: string } | null)?.from &&
    (location.state as { from?: string }).from !== '/login'
      ? (location.state as { from: string }).from
      : '/dashboard';

  useEffect(() => {
    if (initialized && admin) {
      navigate(redirectTo, { replace: true });
    }
  }, [initialized, admin, navigate, redirectTo]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setValidationError(null);
    clearError();

    const trimmedEmail = email.trim();
    if (!trimmedEmail || !trimmedEmail.includes('@')) {
      setValidationError(
        lang === 'bn'
          ? 'অনুগ্রহ করে একটি বৈধ অ্যাডমিন ইমেইল ঠিকানা লিখুন।'
          : 'Please enter a valid administrator email address.'
      );
      return;
    }

    if (!password) {
      setValidationError(
        lang === 'bn'
          ? 'অনুগ্রহ করে আপনার পাসওয়ার্ড লিখুন।'
          : 'Please enter your administrator password.'
      );
      return;
    }

    const ok = await login(trimmedEmail, password, rememberSession, lang);
    if (ok) {
      navigate(redirectTo, { replace: true });
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col lg:grid lg:grid-cols-12">
      {/* Left Institutional Panel */}
      <section className="lg:col-span-5 bg-[#0B233C] text-white px-8 py-10 lg:px-12 lg:py-14 flex flex-col justify-between">
        <div>
          <div className="flex items-center justify-between gap-4">
            <span className="text-xs font-medium tracking-wide text-emerald-300">
              {t.adminConsoleLabel}
            </span>
            <div className="lg:hidden">
              <LanguageToggle />
            </div>
          </div>

          <div className="mt-10 lg:mt-16 max-w-md">
            <h1 className="text-3xl sm:text-4xl font-bold tracking-tight text-white leading-tight font-display">
              {t.brandTitle}
            </h1>
            <p className="mt-2 text-lg font-medium text-emerald-300">
              {t.brandSubtitle}
            </p>
            <p className="mt-6 text-sm text-slate-300 leading-relaxed">
              {lang === 'bn'
                ? 'কক্সবাজার-২ (মহেশখালী–কুতুবদিয়া) আসনের উন্নয়ন প্রকল্প, নাগরিক আবেদন, নোটিশ, সামুদ্রিক আবহাওয়া ও জনসেবা কার্যক্রমের কেন্দ্রীয় প্রশাসনিক পোর্টাল।'
                : 'Centralized web administration console for managing development projects, citizen service applications, public notices, coastal advisories, and portal operations.'}
            </p>
          </div>
        </div>

        <div className="hidden lg:block pt-8 border-t border-white/10 text-xs text-slate-400 space-y-1">
          <p>
            {lang === 'bn'
              ? 'শুধুমাত্র অনুমোদিত প্রশাসকদের জন্য সংরক্ষিত প্রবেশাধিকার'
              : 'Restricted access for verified portal administrators only'}
          </p>
        </div>
      </section>

      {/* Right Authentication Form */}
      <section className="lg:col-span-7 flex-1 flex flex-col justify-between p-6 sm:p-10 lg:p-14">
        <div className="hidden lg:flex justify-end">
          <LanguageToggle />
        </div>

        <div className="w-full max-w-md mx-auto my-auto py-8">
          <div className="bg-white border border-slate-200 rounded-xl p-6 sm:p-8">
            <div className="mb-6">
              <h2 className="text-2xl font-bold text-slate-900 font-display">
                {t.loginTitle}
              </h2>
              <p className="mt-1.5 text-sm text-slate-600 leading-relaxed">
                {t.loginSubtitle}
              </p>
            </div>

            {firestoreConnected === false && connectionError ? (
              <div
                role="alert"
                className="mb-5 p-3.5 rounded-lg bg-amber-50 border border-amber-200 flex items-start gap-2.5 text-xs text-amber-900"
              >
                <AlertCircle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
                <span>{connectionError}</span>
              </div>
            ) : null}

            {(validationError || error) && (
              <div
                role="alert"
                className="mb-5 p-3.5 rounded-lg bg-red-50 border border-red-200 flex items-start gap-2.5 text-sm text-red-800"
              >
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span>{validationError || error}</span>
              </div>
            )}

            {unauthorizedInfo && (
              <div
                role="alert"
                className="mb-5 p-4 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-950 space-y-2"
              >
                <div className="flex items-center gap-2 font-semibold text-amber-900">
                  <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0" />
                  <span>{t.unauthorizedTitle}</span>
                </div>
                <p className="leading-relaxed text-amber-900">{t.unauthorizedBody}</p>
                <div className="pt-1 font-mono text-[11px] text-amber-900 break-all">
                  <div>Email: {unauthorizedInfo.email}</div>
                  <div>UID: {unauthorizedInfo.uid}</div>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5" noValidate>
              <div>
                <label
                  htmlFor="admin-email"
                  className="block text-sm font-medium text-slate-700 mb-1.5"
                >
                  {t.emailLabel}
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="admin-email"
                    type="email"
                    autoComplete="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder={t.emailPlaceholder}
                    className="w-full pl-10 pr-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700 transition-colors"
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="admin-password"
                  className="block text-sm font-medium text-slate-700 mb-1.5"
                >
                  {t.passwordLabel}
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="admin-password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder={t.passwordPlaceholder}
                    className="w-full pl-10 pr-11 py-2.5 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-700/20 focus:border-blue-700 transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? t.hidePassword : t.showPassword}
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1.5 text-slate-500 hover:text-slate-800 rounded transition-colors cursor-pointer"
                  >
                    {showPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between text-sm">
                <label className="inline-flex items-center gap-2 text-slate-600 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={rememberSession}
                    onChange={(e) => setRememberSession(e.target.checked)}
                    className="w-4 h-4 rounded border-slate-300 text-blue-700 focus:ring-blue-700"
                  />
                  <span>{t.rememberSession}</span>
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setResetEmail(email.trim());
                    setResetFeedback(null);
                    setForgotOpen(true);
                  }}
                  className="text-xs font-semibold text-emerald-800 hover:text-emerald-900 transition-colors cursor-pointer"
                >
                  {lang === 'bn' ? 'পাসওয়ার্ড ভুলে গেছেন?' : 'Forgot password?'}
                </button>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 text-sm font-semibold text-white bg-emerald-800 hover:bg-emerald-900 disabled:opacity-60 disabled:cursor-not-allowed rounded-lg transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-sm"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>{t.signingIn}</span>
                  </>
                ) : (
                  <span>{t.signInButton}</span>
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Forgot Password Modal */}
        <Modal
          open={forgotOpen}
          onClose={() => setForgotOpen(false)}
          title={lang === 'bn' ? 'অ্যাডমিন পাসওয়ার্ড রিসেট' : 'Admin Password Reset'}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (!resetEmail || !resetEmail.includes('@')) {
                setResetFeedback({
                  type: 'error',
                  text: lang === 'bn' ? 'সঠিক ইমেইল ঠিকানা লিখুন।' : 'Please enter a valid email address.',
                });
                return;
              }
              setResetLoading(true);
              setResetFeedback(null);
              try {
                await sendAdminPasswordReset(resetEmail);
                setResetFeedback({
                  type: 'success',
                  text:
                    lang === 'bn'
                      ? 'আপনার ইমেইলে পাসওয়ার্ড রিসেট লিংক পাঠানো হয়েছে। ইনবক্স বা স্প্যাম ফোল্ডার চেক করুন।'
                      : 'Password reset link sent to your email. Check your inbox or spam folder.',
                });
              } catch (err) {
                setResetFeedback({
                  type: 'error',
                  text:
                    err instanceof Error
                      ? err.message
                      : lang === 'bn'
                      ? 'পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে।'
                      : 'Password reset failed.',
                });
              } finally {
                setResetLoading(false);
              }
            }}
            className="space-y-4"
          >
            <p className="text-xs text-slate-600 leading-relaxed">
              {lang === 'bn'
                ? 'আপনার অনুমোদিত অ্যাডমিন ইমেইল ঠিকানা লিখুন। ফায়ারবেস অথেন্টিকেশন থেকে সরাসরি পাসওয়ার্ড রিসেট করার নিরাপদ লিংক পাঠানো হবে।'
                : 'Enter your authorized administrator email. A secure password reset link will be sent directly from Firebase Authentication.'}
            </p>

            {resetFeedback ? (
              <div
                role="alert"
                className={`p-3 rounded-lg border text-xs flex items-start gap-2 ${
                  resetFeedback.type === 'success'
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                    : 'bg-red-50 border-red-200 text-red-800'
                }`}
              >
                {resetFeedback.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                )}
                <span>{resetFeedback.text}</span>
              </div>
            ) : null}

            <div>
              <label htmlFor="reset-email" className="block text-xs font-medium text-slate-700 mb-1">
                {t.emailLabel}
              </label>
              <input
                id="reset-email"
                type="email"
                required
                value={resetEmail}
                onChange={(e) => setResetEmail(e.target.value)}
                placeholder="admin@ashequllah.portal"
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-700/20 focus:border-emerald-700"
              />
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setForgotOpen(false)}
                className="px-3.5 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
              >
                {lang === 'bn' ? 'বাতিল' : 'Cancel'}
              </button>
              <button
                type="submit"
                disabled={resetLoading}
                className="px-4 py-2 text-xs font-semibold text-white bg-emerald-800 hover:bg-emerald-900 disabled:opacity-60 rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
              >
                {resetLoading ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>{lang === 'bn' ? 'পাঠানো হচ্ছে...' : 'Sending...'}</span>
                  </>
                ) : (
                  <span>{lang === 'bn' ? 'রিসেট লিংক পাঠান' : 'Send Reset Link'}</span>
                )}
              </button>
            </div>
          </form>
        </Modal>

        <div className="space-y-3 text-center lg:text-left">
          <p className="text-xs text-slate-500">
            {t.brandTitle} · {t.brandSubtitle}
          </p>
          <Link
            to="/legacy-login"
            className="text-xs font-semibold text-emerald-800 hover:text-emerald-900"
          >
            {lang === 'bn'
              ? 'বিকল্প লগইন পদ্ধতি (Google সহ)'
              : 'Use the alternate sign-in page (including Google)'}
          </Link>
        </div>
      </section>
    </div>
  );
}
