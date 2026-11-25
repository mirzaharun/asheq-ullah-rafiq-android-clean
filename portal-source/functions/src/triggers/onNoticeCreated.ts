/**
 * Firestore Trigger: onNoticeCreated
 * Listens for new or newly published notices in the `notices` collection.
 * Database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */

import { onDocumentWritten } from 'firebase-functions/v2/firestore';
import { CONFIG } from '../config';
import { NoticeFeedDoc } from '../types';
import { IdempotencyService } from '../services/idempotencyService';
import { MessagingService } from '../services/messagingService';

export const handleNoticeWrittenLogic = async (
  noticeId: string,
  beforeData: NoticeFeedDoc | undefined,
  afterData: NoticeFeedDoc | undefined
): Promise<{ status: 'sent' | 'skipped' | 'failed'; reason?: string }> => {
  // If deleted or empty, nothing to do
  if (!afterData) {
    return { status: 'skipped', reason: 'Document deleted' };
  }

  // Check if notice is marked as published (defaults to true if not explicitly false)
  if (afterData.published === false) {
    console.log(`[onNoticeWritten] Notice ${noticeId} is unpublished. Skipping.`);
    return { status: 'skipped', reason: 'Notice not published' };
  }

  // If this was an edit to an already published notice and notification was already sent,
  // do not broadcast duplicate notifications on title or content typo fixes.
  const wasAlreadyPublished = beforeData && beforeData.published !== false;
  if (wasAlreadyPublished && (afterData.notificationSent || beforeData.notificationSent)) {
    console.log(`[onNoticeWritten] Notice ${noticeId} was already published and notified. Preventing duplicate broadcast.`);
    return { status: 'skipped', reason: 'Duplicate notice update prevented' };
  }

  const eventId = IdempotencyService.getNoticeEventId(noticeId);
  const lockAcquired = await IdempotencyService.acquireEventLock(eventId, {
    type: 'notice',
    targetId: noticeId,
    titleBn: afterData.titleBn,
    titleEn: afterData.titleEn,
  });

  if (!lockAcquired) {
    console.log(`[onNoticeWritten] Lock already exists for notice event ${eventId}. Skipping.`);
    return { status: 'skipped', reason: 'Idempotency lock exists' };
  }

  try {
    // Broadcast notice exclusively to FCM topics (notices_bn, notices_en)
    const dispatchResult = await MessagingService.broadcastNotice(
      noticeId,
      afterData
    );

    await IdempotencyService.completeEvent(eventId, {
      status: dispatchResult.successCount > 0 ? 'sent' : 'failed',
      recipientCount: dispatchResult.recipientCount,
      successCount: dispatchResult.successCount,
      failureCount: dispatchResult.failureCount,
    });

    console.log(
      `[onNoticeWritten] Successfully processed notice ${noticeId}: ${dispatchResult.successCount} sent, ${dispatchResult.failureCount} failed.`
    );
    return { status: 'sent' };
  } catch (err: any) {
    console.error(`[onNoticeWritten] Unexpected failure broadcasting notice ${noticeId}:`, err);
    await IdempotencyService.releaseEventLock(eventId);
    return { status: 'failed', reason: err.message };
  }
};

export const onNoticeWritten = onDocumentWritten(
  {
    document: `${CONFIG.collections.notices}/{noticeId}`,
    database: CONFIG.databaseId,
    region: 'us-central1',
  },
  async (event) => {
    const noticeId = event.params.noticeId;
    const beforeData = event.data?.before?.data() as NoticeFeedDoc | undefined;
    const afterData = event.data?.after?.data() as NoticeFeedDoc | undefined;

    await handleNoticeWrittenLogic(noticeId, beforeData, afterData);
  }
);
