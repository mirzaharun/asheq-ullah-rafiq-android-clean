export type AdminRole = 'super_admin' | 'editor' | 'application_manager';

export interface AdminIdentity {
  uid: string;
  email: string;
  displayName: string;
  role: AdminRole;
  active: boolean;
  verificationSource: 'custom_claim' | 'admins_collection' | 'users_collection';
}

export interface AuthState {
  loading: boolean;
  initialized: boolean;
  admin: AdminIdentity | null;
  error: string | null;
  unauthorizedInfo: {
    email: string;
    uid: string;
  } | null;
}
