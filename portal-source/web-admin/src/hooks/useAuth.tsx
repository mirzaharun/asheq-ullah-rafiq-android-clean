import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { testFirestoreConnection } from '../services/firebase/app';
import {
  mapFirebaseAuthError,
  signInAdmin,
  signOutAdmin,
  subscribeToAuthChanges,
  verifyAdminAuthorization,
} from '../services/firebase/authService';
import type { AuthState } from '../types/auth';
import type { Language } from '../utils/i18n';

interface AuthContextValue extends AuthState {
  firestoreConnected: boolean | null;
  connectionError: string | null;
  login: (
    email: string,
    password: string,
    rememberSession: boolean,
    lang: Language
  ) => Promise<boolean>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    loading: true,
    initialized: false,
    admin: null,
    error: null,
    unauthorizedInfo: null,
  });

  const [firestoreConnected, setFirestoreConnected] = useState<boolean | null>(null);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    testFirestoreConnection().then((res) => {
      if (!mounted) return;
      setFirestoreConnected(res.connected);
      setConnectionError(res.error ?? null);
    });

    const unsubscribe = subscribeToAuthChanges(async (firebaseUser) => {
      if (!mounted) return;
      if (!firebaseUser) {
        setState((prev) => ({
          loading: false,
          initialized: true,
          admin: null,
          error: prev.error,
          unauthorizedInfo: prev.unauthorizedInfo,
        }));
        return;
      }

      try {
        const verified = await verifyAdminAuthorization(firebaseUser);
        if (!mounted) return;
        if (!verified) {
          const info = {
            email: firebaseUser.email ?? '',
            uid: firebaseUser.uid,
          };
          await signOutAdmin();
          setState({
            loading: false,
            initialized: true,
            admin: null,
            error: null,
            unauthorizedInfo: info,
          });
          return;
        }
        setState({
          loading: false,
          initialized: true,
          admin: verified,
          error: null,
          unauthorizedInfo: null,
        });
      } catch {
        if (!mounted) return;
        setState({
          loading: false,
          initialized: true,
          admin: null,
          error: null,
          unauthorizedInfo: null,
        });
      }
    });

    return () => {
      mounted = false;
      unsubscribe();
    };
  }, []);

  const login = useCallback(
    async (
      email: string,
      password: string,
      rememberSession: boolean,
      lang: Language
    ): Promise<boolean> => {
      setState((prev) => ({
        ...prev,
        loading: true,
        error: null,
        unauthorizedInfo: null,
      }));

      try {
        const result = await signInAdmin(email, password, rememberSession);
        if (!result.admin) {
          setState({
            loading: false,
            initialized: true,
            admin: null,
            error: null,
            unauthorizedInfo: result.unauthorizedInfo,
          });
          return false;
        }
        setState({
          loading: false,
          initialized: true,
          admin: result.admin,
          error: null,
          unauthorizedInfo: null,
        });
        return true;
      } catch (err) {
        setState((prev) => ({
          ...prev,
          loading: false,
          initialized: true,
          admin: null,
          error: mapFirebaseAuthError(err, lang),
        }));
        return false;
      }
    },
    []
  );

  const logout = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true }));
    try {
      await signOutAdmin();
      setState({
        loading: false,
        initialized: true,
        admin: null,
        error: null,
        unauthorizedInfo: null,
      });
    } catch {
      setState((prev) => ({ ...prev, loading: false }));
    }
  }, []);

  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: null, unauthorizedInfo: null }));
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        firestoreConnected,
        connectionError,
        login,
        logout,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
