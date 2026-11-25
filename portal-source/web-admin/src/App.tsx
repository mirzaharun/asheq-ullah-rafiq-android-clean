import React from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from './components/layout/AdminLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AuthProvider } from './contexts/AuthContext';
import { LanguageProvider } from './contexts/LanguageContext';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { ModuleWorkspacePage } from './pages/ModuleWorkspacePage';
import { UnauthorizedPage } from './pages/UnauthorizedPage';
import { AppReleaseManagerPage } from './pages/AppReleaseManagerPage';
import { PublicAppDownloadPage } from './pages/PublicAppDownloadPage';

export const App: React.FC = () => {
  return (
    <LanguageProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/download" element={<PublicAppDownloadPage />} />
            <Route
              path="/unauthorized"
              element={
                <ProtectedRoute>
                  <UnauthorizedPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AdminLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route
                path="app-releases"
                element={
                  <ProtectedRoute allowedRoles={['super_admin']}>
                    <AppReleaseManagerPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="modules/:moduleKey"
                element={<ModuleWorkspacePage />}
              />
            </Route>

            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </LanguageProvider>
  );
};

export default App;
