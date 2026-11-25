/**
 * Firestore Trigger: onApplicationUpdated & onAppointmentUpdated
 * Listens for application and appointment status transitions.
 * Ensures strict user ownership, genuine status transitions only,
 * and deduplication across mirrored collections.
 * Database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */

import { onDocumentUpdated } from 'firebase-functions/v2/firestore';
import { CONFIG } from '../config';
import { ApplicationDoc } from '../types';
import { IdempotencyService } from '../services/idempotencyService';
import { UserPreferenceService } from '../services/userPreferenceService';
import { MessagingService } from '../services/messagingService';

export const handleApplicationStatusChangeLogic = async (
  recordId: string,
  sourceCollection: string,
  beforeData: ApplicationDoc | undefined,
  afterData: ApplicationDoc | undefined
): Promise<{ status: 'sent' | 'skipped' | 'failed'; reason?: string }> => {
  if (!beforeData || !afterData) {
    return { status: 'skipped', reason: 'Missing before or after snapshot' };
  }

  const oldStatus = (beforeData.status || '').trim();
  const newStatus = (afterData.status || '').trim();

  // 1. Genuine status transition check: only send if status actually changed
  if (!newStatus || oldStatus.toLowerCase() === newStatus.toLowerCase()) {
    console.log(
      `[handleApplicationStatusChange] No status transition for ${recordId} in ${sourceCollection} (Status: "${newStatus}"). Skipping.`
    );
    return { status: 'skipped', reason: 'No status transition' };
  }

  // 2. Ownership & User Validation
  const userId = afterData.userId || beforeData.userId;
  if (!userId || typeof userId !== 'string' || userId.trim().length === 0) {
    console.warn(
      `[handleApplicationStatusChange] Application ${recordId} has no valid userId. Skipping notification.`
    );
    return { status: 'skipped', reason: 'Missing userId' };
  }

  // 3. Deduplication across mirror collections (applications <-> appointments)
  // Both collections store the same application ID and status updates.
  const eventId = IdempotencyService.getApplicationStatusEventId(recordId, newStatus);
  const lockAcquired = await IdempotencyService.acquireEventLock(eventId, {
    type: 'application_status',
    targetId: recordId,
    userId,
    oldStatus,
    newStatus,
  });

  if (!lockAcquired) {
    console.log(
      `[handleApplicationStatusChange] Idempotency lock already exists for event ${eventId} (mirror trigger skip).`
    );
    return { status: 'skipped', reason: 'Duplicate event skipped by idempotency lock' };
  }

  try {
    // 4. Retrieve user tokens and validate notification preferences
    const userProfile = await UserPreferenceService.getUserProfileForStatusUpdate(userId);
    if (!userProfile || userProfile.tokens.length === 0) {
      console.log(
        `[handleApplicationStatusChange] User ${userId} has no active tokens or has status notifications disabled.`
      );
      await IdempotencyService.completeEvent(eventId, {
        status: 'skipped',
        reason: 'User opted out or has no registered tokens',
      });
      return { status: 'skipped', reason: 'User opted out or no tokens' };
    }

    // 5. Dispatch non-PII notification in user's preferred language
    const dispatchResult = await MessagingService.sendApplicationStatusNotification(
      afterData,
      newStatus,
      userProfile.tokens,
      userProfile.language
    );

    await IdempotencyService.completeEvent(eventId, {
      status: dispatchResult.successCount > 0 ? 'sent' : 'failed',
      recipientCount: dispatchResult.recipientCount,
      successCount: dispatchResult.successCount,
      failureCount: dispatchResult.failureCount,
      newStatus,
    });

    console.log(
      `[handleApplicationStatusChange] Application ${recordId} status notification ("${newStatus}") sent to user ${userId}: ` +
        `${dispatchResult.successCount} succeeded, ${dispatchResult.failureCount} failed.`
    );

    return { status: dispatchResult.successCount > 0 ? 'sent' : 'failed' };
  } catch (err: any) {
    console.error(`[handleApplicationStatusChange] Error processing status update for ${recordId}:`, err);
    await IdempotencyService.releaseEventLock(eventId);
    return { status: 'failed', reason: err.message };
  }
};

export const onApplicationUpdated = onDocumentUpdated(
  {
    document: `${CONFIG.collections.applications}/{appId}`,
    database: CONFIG.databaseId,
    region: 'us-central1',
  },
  async (event) => {
    const appId = event.params.appId;
    const beforeData = event.data?.before?.data() as ApplicationDoc | undefined;
    const afterData = event.data?.after?.data() as ApplicationDoc | undefined;

    await handleApplicationStatusChangeLogic(appId, CONFIG.collections.applications, beforeData, afterData);
  }
);

export const onAppointmentUpdated = onDocumentUpdated(
  {
    document: `${CONFIG.collections.appointments}/{apptId}`,
    database: CONFIG.databaseId,
    region: 'us-central1',
  },
  async (event) => {
    const apptId = event.params.apptId;
    const beforeData = event.data?.before?.data() as ApplicationDoc | undefined;
    const afterData = event.data?.after?.data() as ApplicationDoc | undefined;

    await handleApplicationStatusChangeLogic(apptId, CONFIG.collections.appointments, beforeData, afterData);
  }
);
