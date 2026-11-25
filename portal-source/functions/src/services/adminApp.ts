/**
 * Firebase Admin SDK Initialization Service
 * Explicitly targets named Firestore database:
 * ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */

import * as admin from 'firebase-admin';
import { getFirestore, Firestore } from 'firebase-admin/firestore';
import { getMessaging, Messaging } from 'firebase-admin/messaging';
import { CONFIG } from '../config';

let appInstance: admin.app.App | null = null;
let firestoreInstance: Firestore | null = null;
let messagingInstance: Messaging | null = null;

export function getAdminApp(): admin.app.App {
  if (!appInstance) {
    if (admin.apps.length > 0) {
      appInstance = admin.apps[0]!;
    } else {
      appInstance = admin.initializeApp();
    }
  }
  return appInstance;
}

export function getDb(): Firestore {
  if (!firestoreInstance) {
    const app = getAdminApp();
    try {
      firestoreInstance = getFirestore(app, CONFIG.databaseId);
    } catch (_err) {
      // Fallback to default if named database fails in local testing
      firestoreInstance = getFirestore(app);
    }
  }
  return firestoreInstance;
}

export function getFCM(): Messaging {
  if (!messagingInstance) {
    const app = getAdminApp();
    messagingInstance = getMessaging(app);
  }
  return messagingInstance;
}

// Support injecting custom/mock db or messaging for unit tests
export function setDbForTesting(mockDb: Firestore | null): void {
  firestoreInstance = mockDb;
}

export function setMessagingForTesting(mockMessaging: Messaging | null): void {
  messagingInstance = mockMessaging;
}
