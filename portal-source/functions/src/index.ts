/**
 * Asheq Ullah Rafiq Digital Portal — Cloud Functions
 * Project: asheq-ullah-rafiq-digital-p
 * Database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 *
 * Implements server-side FCM push notification triggers:
 * 1. onNoticeWritten: Dispatches broadcast notifications when notices are published.
 * 2. onApplicationUpdated: Dispatches personalized status notifications to citizens upon application status transitions.
 * 3. onAppointmentUpdated: Mirror collection trigger with automated deduplication.
 */

export { onNoticeWritten, handleNoticeWrittenLogic } from './triggers/onNoticeCreated';
export { onApplicationUpdated, onAppointmentUpdated, handleApplicationStatusChangeLogic } from './triggers/onApplicationUpdated';
export { CONFIG } from './config';
export { IdempotencyService } from './services/idempotencyService';
export { MessagingService } from './services/messagingService';
export { UserPreferenceService } from './services/userPreferenceService';
export { getAdminApp, getDb, getFCM, setDbForTesting, setMessagingForTesting } from './services/adminApp';
