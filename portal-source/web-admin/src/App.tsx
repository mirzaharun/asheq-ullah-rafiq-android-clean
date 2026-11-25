import { ErrorBoundary } from './components/common/ErrorBoundary';
import { AuthProvider as LegacyAuthProvider } from './contexts/AuthContext';
import { LanguageProvider as LegacyLanguageProvider } from './contexts/LanguageContext';
import { AuthProvider } from './hooks/useAuth';
import { LanguageProvider } from './hooks/useLanguage';
import { AppRoutes } from './routes/AppRoutes';

export default function App() {
  return (
    <ErrorBoundary>
      <LegacyLanguageProvider>
        <LegacyAuthProvider>
          <LanguageProvider>
            <AuthProvider>
              <AppRoutes />
            </AuthProvider>
          </LanguageProvider>
        </LegacyAuthProvider>
      </LegacyLanguageProvider>
    </ErrorBoundary>
  );
}
