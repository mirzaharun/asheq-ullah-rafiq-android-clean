import {
  GoogleAuthProvider,
  User,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from 'firebase/auth';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
} from 'firebase/firestore';
import { auth, db } from '../lib/firebase';
import {
  ADMIN_ROLES,
  AdminModuleDefinition,
  AdminRole,
  AdminUserProfile,
} from '../types/portal';

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export function isValidAdminRole(value: unknown): value is AdminRole {
  return typeof value === 'string' && (ADMIN_ROLES as readonly string[]).includes(value);
}

export function canEditCms(role: AdminRole | null | undefined): boolean {
  return role === 'super_admin' || role === 'editor';
}

export function canManageApplications(role: AdminRole | null | undefined): boolean {
  return role === 'super_admin' || role === 'application_manager';
}

export function isSuperAdmin(role: AdminRole | null | undefined): boolean {
  return role === 'super_admin';
}

export function canAccessModule(
  role: AdminRole | null | undefined,
  mod: AdminModuleDefinition
): boolean {
  if (!role) return false;
  return mod.allowedRoles.includes(role);
}

export function getRoleLabel(role: AdminRole, lang: 'bn' | 'en'): string {
  switch (role) {
    case 'super_admin':
      return lang === 'bn' ? 'সুপার এডমিন' : 'Super Admin';
    case 'editor':
      return lang === 'bn' ? 'এডিটর (CMS)' : 'Editor (CMS)';
    case 'application_manager':
      return lang === 'bn' ? 'আবেদন ব্যবস্থাপক' : 'Application Manager';
  }
}

export class AdminAuthError extends Error {
  readonly code:
    | 'ACCOUNT_DISABLED'
    | 'NOT_AUTHORIZED'
    | 'INVALID_ROLE'
    | 'AUTH_FAILED';

  constructor(
    code: 'ACCOUNT_DISABLED' | 'NOT_AUTHORIZED' | 'INVALID_ROLE' | 'AUTH_FAILED',
    message: string
  ) {
    super(message);
    this.code = code;
  }
}

/**
 * Verifies administrator privileges exclusively through server-backed Firestore/Auth state:
 * 1) `admins/{uid}` document (must exist, have `active === true`, and valid `role`)
 * 2) Firebase Auth custom token claim `role`
 * 3) Security-rule-enforced `list /admins` probe (succeeds ONLY for backend-verified Super Admins)
 *
 * Never trusts or grants privileges based on a hardcoded email in frontend code.
 */
export async function verifyAdminAccess(user: User): Promise<AdminUserProfile> {
  // Step 1: Check explicit admins/{uid} document
  try {
    const adminRef = doc(db, 'admins', user.uid);
    const adminSnap = await getDoc(adminRef);
    if (adminSnap.exists()) {
      const data = adminSnap.data();
      if (data.active === false) {
        throw new AdminAuthError(
          'ACCOUNT_DISABLED',
          'এই এডমিন অ্যাকাউন্টটি সাময়িকভাবে নিষ্ক্রিয় করা হয়েছে। / This admin account has been deactivated.'
        );
      }
      if (isValidAdminRole(data.role)) {
        return {
          uid: user.uid,
          email:
            (typeof data.email === 'string' && data.email) ||
            user.email ||
            '',
          displayName:
            (typeof data.displayName === 'string' && data.displayName) ||
            user.displayName ||
            user.email ||
            'Admin',
          role: data.role,
          active: true,
          verifiedVia: 'admins_doc',
          createdAt: data.createdAt,
          updatedAt: data.updatedAt,
        };
      }
      throw new AdminAuthError(
        'INVALID_ROLE',
        'এডমিন ডকুমেন্টে বৈধ রোল পাওয়া যায়নি। / Invalid role assigned in admin directory.'
      );
    }
  } catch (err) {
    if (err instanceof AdminAuthError) throw err;
    // Continue to token/probe verification if admins/{uid} doc is absent or unreadable
  }

  // Step 2: Check Firebase ID token custom claims
  try {
    const tokenResult = await user.getIdTokenResult(true);
    const claimRole = tokenResult.claims?.role;
    if (isValidAdminRole(claimRole)) {
      return {
        uid: user.uid,
        email: user.email || '',
        displayName: user.displayName || user.email || 'Admin',
        role: claimRole,
        active: true,
        verifiedVia: 'custom_claim',
      };
    }
  } catch {
    // Proceed to backend rules probe
  }

  // Step 3: Probe backend-enforced Super Admin list rule on /admins
  try {
    await getDocs(query(collection(db, 'admins'), limit(1)));
    return {
      uid: user.uid,
      email: user.email || '',
      displayName: user.displayName || user.email || 'Super Admin',
      role: 'super_admin',
      active: true,
      verifiedVia: 'rules_probe',
    };
  } catch {
    throw new AdminAuthError(
      'NOT_AUTHORIZED',
      'আপনার অ্যাকাউন্টে এডমিন প্যানেল ব্যবহারের অনুমতি নেই। / Your account is not authorized as an administrator.'
    );
  }
}

export async function signInWithAdminEmail(
  email: string,
  password: string
): Promise<AdminUserProfile> {
  const credential = await signInWithEmailAndPassword(auth, email.trim(), password);
  try {
    return await verifyAdminAccess(credential.user);
  } catch (err) {
    await signOut(auth).catch(() => undefined);
    throw err;
  }
}

export async function signInWithAdminGoogle(): Promise<AdminUserProfile> {
  const credential = await signInWithPopup(auth, googleProvider);
  try {
    return await verifyAdminAccess(credential.user);
  } catch (err) {
    await signOut(auth).catch(() => undefined);
    throw err;
  }
}

export async function logoutAdmin(): Promise<void> {
  await signOut(auth);
}
