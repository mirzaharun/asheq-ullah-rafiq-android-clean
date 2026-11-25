import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import type { AdminRole } from '../../types/auth';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: AdminRole[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { admin, initialized, loading } = useAuth();
  const { t, lang } = useLanguage();
  const location = useLocation();

  if (!initialized || loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="w-8 h-8 border-2 border-blue-700 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-medium text-slate-600">{t.sessionRestoring}</p>
        </div>
      </div>
    );
  }

  if (!admin) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(admin.role)) {
    return (
      <div className="p-8 max-w-xl mx-auto mt-12 bg-white border border-slate-200 rounded-xl">
        <div className="flex items-center gap-3 text-amber-700 mb-3">
          <ShieldAlert className="w-6 h-6 shrink-0" />
          <h2 className="text-lg font-semibold text-slate-900">
            {lang === 'bn' ? 'অনুমতি সীমিত' : 'Access Restricted'}
          </h2>
        </div>
        <p className="text-sm text-slate-600 leading-relaxed">
          {lang === 'bn'
            ? 'আপনার বর্তমান অ্যাডমিন ভূমিকার জন্য এই মডিউলে প্রবেশাধিকার নির্ধারিত নেই।'
            : 'Your current administrator role does not have permission to open this module.'}
        </p>
      </div>
    );
  }

  return <>{children}</>;
}
