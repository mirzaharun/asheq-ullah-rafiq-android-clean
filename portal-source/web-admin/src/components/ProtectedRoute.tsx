import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ShieldAlert, Loader2 } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useLanguage } from '../contexts/LanguageContext';
import { AdminRole } from '../types/portal';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: readonly AdminRole[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles,
}) => {
  const { status, adminUser } = useAuth();
  const { t } = useLanguage();
  const location = useLocation();

  if (status === 'loading' || status === 'verifying') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 px-4 text-center">
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-8 max-w-sm w-full flex flex-col items-center gap-4">
          <div className="h-12 w-12 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">
              {status === 'verifying'
                ? t('অ্যাডমিন অনুমতি যাচাই হচ্ছে…', 'Verifying Admin Role…')
                : t('সেশন লোড হচ্ছে…', 'Loading Session…')}
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              {t(
                'সার্ভার-ভিত্তিক Firestore নিরাপত্তা ও রোল পরীক্ষা চলছে',
                'Checking server-backed Firestore permissions and RBAC role'
              )}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (status !== 'authenticated' || !adminUser) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(adminUser.role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};

export const InlineRoleNotice: React.FC<{
  title: string;
  description: string;
}> = ({ title, description }) => (
  <div className="rounded-xl border border-amber-200 bg-amber-50/70 p-4 flex items-start gap-3">
    <ShieldAlert className="h-5 w-5 text-amber-700 shrink-0 mt-0.5" />
    <div>
      <p className="text-sm font-semibold text-amber-900">{title}</p>
      <p className="text-xs text-amber-800 mt-0.5 leading-relaxed">{description}</p>
    </div>
  </div>
);
