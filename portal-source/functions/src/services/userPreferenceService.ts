/**
 * User Preferences & Device Token Service
 * Ensures notification preferences are strictly respected and tokens
 * are securely bound to the authenticated user ID.
 */

import { getDb } from './adminApp';
import { CONFIG } from '../config';
import { UserDoc, UserNotificationPreferences } from '../types';

export interface UserNotificationProfile {
  userId: string;
  tokens: string[];
  language: 'bn' | 'en';
  preferences: UserNotificationPreferences;
}

export class UserPreferenceService {
  /**
   * Fetches registered tokens and preference settings for a specific user ID.
   * Returns null if user has notifications disabled globally or specifically for this type.
   */
  static async getUserProfileForStatusUpdate(
    userId: string
  ): Promise<UserNotificationProfile | null> {
    if (!userId || typeof userId !== 'string') {
      return null;
    }

    const db = getDb();
    const userRef = db.collection(CONFIG.collections.users).doc(userId);

    try {
      const userSnap = await userRef.get();
      const userData = (userSnap.data() as UserDoc) || {};

      const preferences = userData.notificationPreferences || userData.preferences || {};

      // If user globally disabled notifications or disabled application status notifications, respect it
      if (preferences.enabled === false || preferences.applicationStatus === false) {
        console.log(`[UserPreferenceService] User ${userId} has status notifications disabled.`);
        return null;
      }

      const language: 'bn' | 'en' = preferences.language || userData.language || 'bn';
      const tokenSet = new Set<string>();

      // 1. Check direct token fields on user document
      if (typeof userData.fcmToken === 'string' && userData.fcmToken.trim().length > 10) {
        tokenSet.add(userData.fcmToken.trim());
      }
      if (Array.isArray(userData.fcmTokens)) {
        userData.fcmTokens.forEach((t) => {
          if (typeof t === 'string' && t.trim().length > 10) {
            tokenSet.add(t.trim());
          }
        });
      }

      // 2. Check fcm_tokens subcollection (users/{userId}/fcm_tokens)
      const subTokensSnap = await userRef.collection('fcm_tokens').where('active', '!=', false).get();
      subTokensSnap.forEach((doc: FirebaseFirestore.QueryDocumentSnapshot) => {
        const data = doc.data();
        if (typeof data.token === 'string' && data.token.trim().length > 10) {
          tokenSet.add(data.token.trim());
        }
      });

      const tokens = Array.from(tokenSet);
      if (tokens.length === 0) {
        console.log(`[UserPreferenceService] No active FCM tokens found for user ${userId}.`);
        return null;
      }

      return {
        userId,
        tokens,
        language,
        preferences,
      };
    } catch (err) {
      console.error(`[UserPreferenceService] Failed to retrieve tokens for user ${userId}:`, err);
      return null;
    }
  }

  /**
   * Retrieves active tokens for public notices, separated by language.
   * Respects user notification preferences (skips users with notices: false).
   */
  static async getNoticeTokens(): Promise<{ bnTokens: string[]; enTokens: string[] }> {
    const db = getDb();
    const bnTokens: string[] = [];
    const enTokens: string[] = [];

    try {
      const usersSnap = await db.collection(CONFIG.collections.users).limit(500).get();

      for (const userDoc of usersSnap.docs) {
        const data = userDoc.data() as UserDoc;
        const prefs = data.notificationPreferences || data.preferences || {};

        if (prefs.enabled === false || prefs.notices === false) {
          continue;
        }

        const lang: 'bn' | 'en' = prefs.language || data.language || 'bn';
        const targetList = lang === 'en' ? enTokens : bnTokens;

        if (typeof data.fcmToken === 'string' && data.fcmToken.length > 10) {
          targetList.push(data.fcmToken);
        }
        if (Array.isArray(data.fcmTokens)) {
          data.fcmTokens.forEach((t) => {
            if (typeof t === 'string' && t.length > 10) {
              targetList.push(t);
            }
          });
        }
      }
    } catch (err) {
      console.warn('[UserPreferenceService] Failed scanning user docs for notice tokens:', err);
    }

    return {
      bnTokens: Array.from(new Set(bnTokens)),
      enTokens: Array.from(new Set(enTokens)),
    };
  }
}
