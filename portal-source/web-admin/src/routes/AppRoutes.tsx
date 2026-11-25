import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../components/auth/ProtectedRoute';
import { AdminLayout } from '../layouts/AdminLayout';
import { ProtectedRoute as LegacyProtectedRoute } from '../components/ProtectedRoute';
import { AppReleaseManagerPage } from '../pages/AppReleaseManagerPage';
import { DashboardPage as LegacyDashboardPage } from '../pages/DashboardPage';
import { LoginPage as LegacyLoginPage } from '../pages/LoginPage';
import { ModuleWorkspacePage } from '../pages/ModuleWorkspacePage';
import { PublicAppDownloadPage } from '../pages/PublicAppDownloadPage';
import { UnauthorizedPage } from '../pages/UnauthorizedPage';
import { AppVersionManager } from '../modules/appVersion/AppVersionManager';
import { CitizenRequestsManager } from '../modules/applications/CitizenRequestsManager';
import { AuditLogsViewer } from '../modules/auditLogs/AuditLogsViewer';
import { BloodDonorsManager } from '../modules/bloodDonors/BloodDonorsManager';
import { SingletonInspectorManager } from '../modules/config/SingletonInspectorManager';
import { GalleryManager } from '../modules/gallery/GalleryManager';
import { NoticesManager } from '../modules/notices/NoticesManager';
import { NotificationManager } from '../modules/notifications/NotificationManager';
import { NewsManager } from '../modules/news/NewsManager';
import { ProjectsManager } from '../modules/projects/ProjectsManager';
import { ReportsAnalyticsView } from '../modules/reports/ReportsAnalyticsView';
import { TideScheduleManager } from '../modules/tide/TideScheduleManager';
import { UserRoleManager } from '../modules/users/UserRoleManager';
import { WatercraftSchedulesManager } from '../modules/watercraft/WatercraftSchedulesManager';
import { ContactOfficeManager } from '../modules/contact/ContactOfficeManager';
import { DataSourcesManager } from '../modules/dataSources/DataSourcesManager';
import { AiAssistantManager } from '../modules/aiAssistant/AiAssistantManager';
import { PrivacyConsentManager } from '../modules/privacy/PrivacyConsentManager';
import { SmsGatewayManager } from '../modules/sms/SmsGatewayManager';
import { PushServerManager } from '../modules/pushServer/PushServerManager';
import { DashboardPage as NewDashboardPage } from '../pages/NewDashboardPage';
import { LoginPage as NewLoginPage } from '../pages/NewLoginPage';
import { ModulePlaceholderPage } from '../pages/ModulePlaceholderPage';
import { ENTITY_CONFIGS } from '../modules/entities/entityConfigs';
import { EntityManager } from '../modules/entities/EntityManager';
import type { SidebarNavItem } from '../types/navigation';
import { SIDEBAR_MODULES } from '../utils/navigation';

function renderModuleElement(mod: SidebarNavItem): ReactNode {
  switch (mod.id) {
    case 'personal-profile':
      return (
        <SingletonInspectorManager
          collectionName="profile_info"
          titleBn="ব্যক্তিগত প্রোফাইল ব্যবস্থাপনা"
          titleEn="Personal Profile Management"
          defaultDocId="main"
          includeProfilePhotoUpload
        />
      );
    case 'home-management':
      return (
        <SingletonInspectorManager
          collectionName="config"
          titleBn="হোম পেজ ব্যবস্থাপনা"
          titleEn="Home Page Management"
          defaultDocId="home"
        />
      );
    case 'development-projects':
      return <ProjectsManager />;
    case 'citizen-applications':
      return <CitizenRequestsManager />;
    case 'notices':
      return <NoticesManager />;
    case 'news-press':
      return <NewsManager />;
    case 'blood-donors':
      return <BloodDonorsManager />;
    case 'watercraft-schedules':
      return <WatercraftSchedulesManager />;
    case 'tide-schedules':
      return <TideScheduleManager />;
    case 'photo-gallery':
      return <GalleryManager defaultMediaType="PHOTO" />;
    case 'video-gallery':
      return <GalleryManager defaultMediaType="VIDEO" />;
    case 'notifications':
      return <NotificationManager />;
    case 'app-version-manager':
      return <AppVersionManager />;
    case 'user-management':
      return <UserRoleManager />;
    case 'reports-analytics':
      return <ReportsAnalyticsView />;
    case 'audit-logs':
      return <AuditLogsViewer />;
    case 'contact-office':
      return <ContactOfficeManager />;
    case 'data-sources':
      return <DataSourcesManager />;
    case 'ai-assistant':
      return <AiAssistantManager />;
    case 'privacy-consent':
      return <PrivacyConsentManager />;
    case 'sms-gateway':
      return <SmsGatewayManager />;
    case 'push-server':
      return <PushServerManager />;
    case 'settings':
      return (
        <SingletonInspectorManager
          collectionName="config"
          titleBn="পোর্টাল সেটিংস"
          titleEn="Portal Settings"
          defaultDocId="app_settings"
        />
      );
    case 'android-app-releases':
      return (
        <LegacyProtectedRoute allowedRoles={['super_admin']}>
          <AppReleaseManagerPage />
        </LegacyProtectedRoute>
      );
    case 'live-dashboard':
      return <LegacyDashboardPage />;
    default: {
      if (mod.collectionName && ENTITY_CONFIGS[mod.collectionName]) {
        return <EntityManager config={ENTITY_CONFIGS[mod.collectionName]} />;
      }
      return <ModulePlaceholderPage module={mod} />;
    }
  }
}


export function AppRoutes() {
  const routedModules = SIDEBAR_MODULES.filter((m) => m.id !== 'dashboard');

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<NewLoginPage />} />
        <Route path="/legacy-login" element={<LegacyLoginPage />} />
        <Route path="/download" element={<PublicAppDownloadPage />} />
        <Route
          path="/unauthorized"
          element={
            <LegacyProtectedRoute>
              <UnauthorizedPage />
            </LegacyProtectedRoute>
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
          <Route path="dashboard" element={<NewDashboardPage />} />
          <Route
            path="modules/:moduleKey"
            element={
              <LegacyProtectedRoute>
                <ModuleWorkspacePage />
              </LegacyProtectedRoute>
            }
          />

          {routedModules.map((mod) => (
            <Route
              key={mod.id}
              path={mod.path.replace(/^\//, '')}
              element={
                <ProtectedRoute allowedRoles={mod.allowedRoles}>
                  {renderModuleElement(mod)}
                </ProtectedRoute>
              }
            />
          ))}

          {/* Legacy route compatibility redirects */}
          <Route path="daily-activities" element={<Navigate to="/daily-schedules" replace />} />
          <Route path="emergency-services" element={<Navigate to="/emergency-contacts" replace />} />
          <Route path="union-information" element={<Navigate to="/unions" replace />} />
        </Route>


        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

