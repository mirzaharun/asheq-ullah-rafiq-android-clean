import {
  browserLocalPersistence,
  browserSessionPersistence,
  onAuthStateChanged,
  sendPasswordResetEmail,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
  type User,
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from './app';
import { OperationType, reportFirestoreError } from './errorHandler';
import type { AdminIdentity, AdminRole } from '../../types/auth';

const ALLOWED_ROLES: ReadonlySet<AdminRole> = new Set([
  'super_admin',
  'editor',
  'application_manager',
]);

function normalizeAdminRole(rawRole: unknown, isAdminFlag?: boolean): AdminRole | null {
  if (typeof rawRole === 'string') {
    const cleaned = rawRole.trim().toLowerCase();
    if (cleaned === 'admin' || cleaned === 'super_admin' || cleaned === 'superadmin') {
      return 'super_admin';
    }
    if (cleaned === 'editor') {
      return 'editor';
    }
    if (cleaned === 'application_manager' || cleaned === 'applicationmanager') {
      return 'application_manager';
    }
  }
  if (isAdminFlag === true) {
    return 'super_admin';
  }
  return null;
}

export async function verifyAdminAuthorization(user: User): Promise<AdminIdentity | null> {
  try {
    const tokenResult = await user.getIdTokenResult(true);
    const claims = tokenResult.claims;
    const claimRole = normalizeAdminRole(claims.role, claims.admin === true);

    if (claimRole && ALLOWED_ROLES.has(claimRole)) {
      return {
        uid: user.uid,
        email: user.email ?? '',
        displayName: user.displayName || user.email?.split('@')[0] || 'Admin',
        role: claimRole,
        active: true,
        verificationSource: 'custom_claim',
      };
    }
  } catch {
    // Continue to Firestore role document verification
  }

  try {
    const adminDocRef = doc(db, 'admins', user.uid);
    const adminSnap = await getDoc(adminDocRef);
    if (adminSnap.exists()) {
      const data = adminSnap.data();
      const isActive = data.active !== false;
      const role = normalizeAdminRole(data.role, data.isAdmin === true);
      if (isActive && role && ALLOWED_ROLES.has(role)) {
        return {
          uid: user.uid,
          email: (typeof data.email === 'string' && data.email) || user.email || '',
          displayName:
            (typeof data.displayName === 'string' && data.displayName) ||
            user.displayName ||
            user.email?.split('@')[0] ||
            'Admin',
          role,
          active: true,
          verificationSource: 'admins_collection',
        };
      }
    }
  } catch (err) {
    reportFirestoreError(err, OperationType.GET, `admins/${user.uid}`);
  }

  try {
    const userDocRef = doc(db, 'users', user.uid);
    const userSnap = await getDoc(userDocRef);
    if (userSnap.exists()) {
      const data = userSnap.data();
      const isActive = data.active !== false;
      const role = normalizeAdminRole(data.role, data.isAdmin === true);
      if (isActive && role && ALLOWED_ROLES.has(role) && data.isAdmin === true) {
        return {
          uid: user.uid,
          email: (typeof data.email === 'string' && data.email) || user.email || '',
          displayName:
            (typeof data.displayName === 'string' && data.displayName) ||
            user.displayName ||
            user.email?.split('@')[0] ||
            'Admin',
          role,
          active: true,
          verificationSource: 'users_collection',
        };
      }
    }
  } catch (err) {
    reportFirestoreError(err, OperationType.GET, `users/${user.uid}`);
  }

  return null;
}

export async function signInAdmin(
  email: string,
  password: string,
  rememberSession: boolean
): Promise<{
  admin: AdminIdentity | null;
  unauthorizedInfo: { email: string; uid: string } | null;
}> {
  await setPersistence(
    auth,
    rememberSession ? browserLocalPersistence : browserSessionPersistence
  );

  const credential = await signInWithEmailAndPassword(auth, email.trim(), password);
  const verifiedAdmin = await verifyAdminAuthorization(credential.user);

  if (!verifiedAdmin) {
    const unauthorizedInfo = {
      email: credential.user.email ?? email.trim(),
      uid: credential.user.uid,
    };
    await signOut(auth);
    return { admin: null, unauthorizedInfo };
  }

  return { admin: verifiedAdmin, unauthorizedInfo: null };
}

export async function signOutAdmin(): Promise<void> {
  await signOut(auth);
}

export function subscribeToAuthChanges(
  callback: (user: User | null) => void
): () => void {
  return onAuthStateChanged(auth, callback);
}

export function mapFirebaseAuthError(error: unknown, lang: 'bn' | 'en'): string {
  const code =
    typeof error === 'object' && error !== null && 'code' in error
      ? String((error as { code: unknown }).code)
      : '';

  if (lang === 'bn') {
    switch (code) {
      case 'auth/invalid-email':
        return 'অনুগ্রহ করে একটি সঠিক ইমেইল ঠিকানা লিখুন।';
      case 'auth/user-disabled':
        return 'এই অ্যাডমিন অ্যাকাউন্টটি নিষ্ক্রিয় করা হয়েছে।';
      case 'auth/user-not-found':
      case 'auth/wrong-password':
      case 'auth/invalid-credential':
        return 'ইমেইল অথবা পাসওয়ার্ড সঠিক নয়। অনুগ্রহ করে পুনরায় চেষ্টা করুন।';
      case 'auth/too-many-requests':
        return 'অতিরিক্ত ভুল প্রচেষ্টার কারণে লগইন সাময়িকভাবে স্থগিত করা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।';
      case 'auth/network-request-failed':
        return 'নেটওয়ার্ক সংযোগে সমস্যা হয়েছে। আপনার ইন্টারনেট সংযোগ পরীক্ষা করুন।';
      default:
        return 'লগইন সম্পন্ন করা যায়নি। অনুগ্রহ করে আপনার তথ্য যাচাই করুন।';
    }
  }

  switch (code) {
    case 'auth/invalid-email':
      return 'Please enter a valid email address.';
    case 'auth/user-disabled':
      return 'This administrator account has been disabled.';
    case 'auth/user-not-found':
    case 'auth/wrong-password':
    case 'auth/invalid-credential':
      return 'Invalid email address or password. Please verify your credentials.';
    case 'auth/too-many-requests':
      return 'Too many failed attempts. Please wait a few minutes before retrying.';
    case 'auth/network-request-failed':
      return 'Network request failed. Please check your internet connection.';
    default:
      return 'Authentication failed. Please check your administrator credentials.';
  }
}

export async function sendAdminPasswordReset(email: string): Promise<void> {
  await sendPasswordResetEmail(auth, email.trim());
}

