import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '../lib/firebase';
import {
  AdminAuthError,
  logoutAdmin,
  signInWithAdminEmail,
  signInWithAdminGoogle,
  verifyAdminAccess,
} from '../services/authService';
import { AdminUserProfile } from '../types/portal';

export type AuthStatus =
  | 'loading'
  | 'unauthenticated'
  | 'verifying'
  | 'authenticated'
  | 'denied';

interface AuthContextValue {
  status: AuthStatus;
  adminUser: AdminUserProfile | null;
  authError: string | null;
  clearError: () => void;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  logout: () => Promise<void>;
  refreshVerification: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function mapFirebaseAuthError(err: unknown): string {
  if (err instanceof AdminAuthError) {
    return err.message;
  }
  if (err && typeof err === 'object' && 'code' in err) {
    const code = String((err as { code?: unknown }).code || '');
    switch (code) {
      case 'auth/invalid-credential':
      case 'auth/wrong-password':
      case 'auth/user-not-found':
        return 'ইমেইল বা পাসওয়ার্ড সঠিক নয়। / Invalid email or password.';
      case 'auth/invalid-email':
        return 'অনুগ্রহ করে সঠিক ইমেইল ঠিকানা লিখুন। / Please enter a valid email address.';
      case 'auth/too-many-requests':
        return 'অতিরিক্ত চেষ্টার কারণে সাময়িকভাবে ব্লক করা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন। / Too many attempts, try again later.';
      case 'auth/popup-closed-by-user':
        return 'গুগল সাইন-ইন পপআপ বন্ধ করা হয়েছে। / Google sign-in popup was closed.';
      case 'auth/network-request-failed':
        return 'নেটওয়ার্ক সংযোগে সমস্যা হয়েছে। / Network connection error.';
      default:
        break;
    }
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return 'লগইন যাচাইয়ে ত্রুটি ঘটেছে। / Authentication verification failed.';
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [adminUser, setAdminUser] = useState<AdminUserProfile | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);
  const preserveDeniedOnSignOutRef = useRef(false);
  const activeLoginInProgressRef = useRef(false);

  const runVerification = useCallback(async () => {
    const current = auth.currentUser;
    if (!current) {
      setAdminUser(null);
      setStatus('unauthenticated');
      return;
    }

    setStatus('verifying');
    try {
      const profile = await verifyAdminAccess(current);
      setAdminUser(profile);
      setAuthError(null);
      setStatus('authenticated');
    } catch (err) {
      const msg = mapFirebaseAuthError(err);
      preserveDeniedOnSignOutRef.current = true;
      setAuthError(msg);
      setAdminUser(null);
      setStatus('denied');
      await logoutAdmin().catch(() => undefined);
    }
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (activeLoginInProgressRef.current) {
        return;
      }

      if (!firebaseUser) {
        setAdminUser(null);
        if (preserveDeniedOnSignOutRef.current) {
          preserveDeniedOnSignOutRef.current = false;
          setStatus('denied');
        } else {
          setStatus('unauthenticated');
        }
        return;
      }

      setStatus('verifying');
      try {
        const profile = await verifyAdminAccess(firebaseUser);
        setAdminUser(profile);
        setAuthError(null);
        setStatus('authenticated');
      } catch (err) {
        const msg = mapFirebaseAuthError(err);
        preserveDeniedOnSignOutRef.current = true;
        setAuthError(msg);
        setAdminUser(null);
        setStatus('denied');
        await logoutAdmin().catch(() => undefined);
      }
    });

    return () => unsubscribe();
  }, []);

  const clearError = useCallback(() => {
    setAuthError(null);
    if (status === 'denied') {
      setStatus('unauthenticated');
    }
  }, [status]);

  const loginWithEmail = useCallback(async (email: string, password: string) => {
    setAuthError(null);
    preserveDeniedOnSignOutRef.current = false;
    activeLoginInProgressRef.current = true;
    setStatus('verifying');
    try {
      const profile = await signInWithAdminEmail(email, password);
      setAdminUser(profile);
      setStatus('authenticated');
    } catch (err) {
      const msg = mapFirebaseAuthError(err);
      setAdminUser(null);
      setAuthError(msg);
      setStatus(err instanceof AdminAuthError ? 'denied' : 'unauthenticated');
      throw err;
    } finally {
      activeLoginInProgressRef.current = false;
    }
  }, []);

  const loginWithGoogle = useCallback(async () => {
    setAuthError(null);
    preserveDeniedOnSignOutRef.current = false;
    activeLoginInProgressRef.current = true;
    setStatus('verifying');
    try {
      const profile = await signInWithAdminGoogle();
      setAdminUser(profile);
      setStatus('authenticated');
    } catch (err) {
      const msg = mapFirebaseAuthError(err);
      setAdminUser(null);
      setAuthError(msg);
      setStatus(err instanceof AdminAuthError ? 'denied' : 'unauthenticated');
      throw err;
    } finally {
      activeLoginInProgressRef.current = false;
    }
  }, []);

  const logout = useCallback(async () => {
    preserveDeniedOnSignOutRef.current = false;
    setAuthError(null);
    await logoutAdmin();
    setAdminUser(null);
    setStatus('unauthenticated');
  }, []);

  return (
    <AuthContext.Provider
      value={{
        status,
        adminUser,
        authError,
        clearError,
        loginWithEmail,
        loginWithGoogle,
        logout,
        refreshVerification: runVerification,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
