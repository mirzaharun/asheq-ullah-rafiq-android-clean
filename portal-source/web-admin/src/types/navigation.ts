import type { AdminRole } from './auth';

export type NavGroupId =
  | 'overview'
  | 'content'
  | 'citizen_services'
  | 'coastal_info'
  | 'media_comm'
  | 'administration';

export interface SidebarNavItem {
  id: string;
  path: string;
  labelBn: string;
  labelEn: string;
  group: NavGroupId;
  iconName: string;
  phase: 'B' | 'C' | 'D' | 'E' | 'F';
  collectionName?: string;
  allowedRoles: AdminRole[];
}
