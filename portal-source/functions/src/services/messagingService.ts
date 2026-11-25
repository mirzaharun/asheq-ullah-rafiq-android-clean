/**
 * Push Notification Messaging Service
 * Formats bilingual payloads, excludes PII, and handles FCM message delivery.
 */

import { MulticastMessage, Message } from 'firebase-admin/messaging';
import { getFCM } from './adminApp';
import { CONFIG } from '../config';
import { NoticeFeedDoc, ApplicationDoc } from '../types';

export interface DispatchResult {
  recipientCount: number;
  successCount: number;
  failureCount: number;
  errors?: string[];
}

export class MessagingService {
  /**
   * Generates bilingual notification copy for an application status change.
   * STRICT PRIVACY: Absolutely no applicant name, phone number, address,
   * or private citizen message is included in the notification text.
   */
  static getStatusNotificationText(
    status: string,
    language: 'bn' | 'en'
  ): { title: string; body: string } {
    const s = status.trim().toLowerCase();

    if (language === 'en') {
      const title = 'Application Status Update';
      switch (s) {
        case 'submitted':
          return { title, body: 'Your application has been received successfully.' };
        case 'pending':
          return { title, body: 'Your application is currently pending review.' };
        case 'reviewing':
        case 'under review':
          return { title, body: 'Your application is currently under official review.' };
        case 'approved':
        case 'resolved':
          return { title, body: 'Your application has been approved. Tap to view details.' };
        case 'scheduled':
          return { title, body: 'Your appointment has been scheduled. Tap to view details.' };
        case 'completed':
          return { title, body: 'Your application processing has been completed.' };
        case 'rejected':
        case 'rejected with explanation':
        case 'cancelled':
          return { title, body: 'An official decision has been made on your application.' };
        default:
          return { title, body: 'Your application status has been updated. Tap to view details.' };
      }
    } else {
      const title = 'আবেদনের অবস্থা হালনাগাদ';
      switch (s) {
        case 'submitted':
          return { title, body: 'আপনার আবেদন সফলভাবে গৃহীত হয়েছে।' };
        case 'pending':
          return { title, body: 'আপনার আবেদনটি অপেক্ষমাণ তালিকায় রয়েছে।' };
        case 'reviewing':
        case 'under review':
          return { title, body: 'আপনার আবেদনটি বর্তমানে পর্যালোচনা করা হচ্ছে।' };
        case 'approved':
        case 'resolved':
          return { title, body: 'আপনার আবেদনটি অনুমোদিত হয়েছে। বিস্তারিত দেখতে ট্যাপ করুন।' };
        case 'scheduled':
          return { title, body: 'আপনার সাক্ষাতের সময় নির্ধারণ করা হয়েছে। বিস্তারিত দেখতে ট্যাপ করুন।' };
        case 'completed':
          return { title, body: 'আপনার আবেদনটির কার্যক্রম সম্পন্ন হয়েছে।' };
        case 'rejected':
        case 'rejected with explanation':
        case 'cancelled':
          return { title, body: 'আপনার আবেদনটির বিষয়ে অফিসিয়াল সিদ্ধান্ত গ্রহণ করা হয়েছে।' };
        default:
          return { title, body: 'আপনার আবেদনের অবস্থা পরিবর্তন হয়েছে। বিস্তারিত দেখতে ট্যাপ করুন।' };
      }
    }
  }

  /**
   * Dispatches targeted application status push notifications to a user's tokens.
   */
  static async sendApplicationStatusNotification(
    application: ApplicationDoc,
    newStatus: string,
    tokens: string[],
    language: 'bn' | 'en'
  ): Promise<DispatchResult> {
    if (!tokens || tokens.length === 0) {
      return { recipientCount: 0, successCount: 0, failureCount: 0 };
    }

    const { title, body } = this.getStatusNotificationText(newStatus, language);
    const trackingId = application.trackingId || application.trackingNumber || application.id || '';

    // Non-PII data payload for deep linking
    const dataPayload: Record<string, string> = {
      type: 'APPLICATION_STATUS',
      id: application.id || '',
      status: newStatus,
      trackingId,
      channelId: CONFIG.notificationChannels.applicationStatus,
    };

    const multicast: MulticastMessage = {
      tokens,
      notification: {
        title,
        body,
      },
      data: dataPayload,
      android: {
        priority: 'high',
        notification: {
          channelId: CONFIG.notificationChannels.applicationStatus,
          sound: 'default',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK',
        },
      },
    };

    try {
      const fcm = getFCM();
      const response = await fcm.sendEachForMulticast(multicast);
      return {
        recipientCount: tokens.length,
        successCount: response.successCount,
        failureCount: response.failureCount,
        errors: response.responses
          .filter((r) => !r.success)
          .map((r) => r.error?.message || 'Unknown FCM error'),
      };
    } catch (err: any) {
      console.error('[MessagingService] Failed sending status multicast:', err);
      return {
        recipientCount: tokens.length,
        successCount: 0,
        failureCount: tokens.length,
        errors: [err.message || String(err)],
      };
    }
  }

  /**
   * Formats and broadcasts a published notice notification exclusively via FCM Topics.
   * Single-path delivery prevents duplicate notifications.
   */
  static async broadcastNotice(
    noticeId: string,
    notice: NoticeFeedDoc
  ): Promise<DispatchResult> {
    const fcm = getFCM();
    let totalSuccess = 0;
    let totalFailure = 0;
    let totalRecipients = 0;
    const errors: string[] = [];

    const isUrgent = Boolean(notice.isUrgent);
    const titleBn = isUrgent ? `⚠️ জরুরি বিজ্ঞপ্তি: ${notice.titleBn}` : notice.titleBn;
    const titleEn = isUrgent
      ? `⚠️ Urgent Notice: ${notice.titleEn || notice.titleBn}`
      : (notice.titleEn || notice.titleBn);

    const bodyBn = (notice.detailsBn || notice.categoryBn || 'বিস্তারিত দেখতে ট্যাপ করুন।').slice(0, 180);
    const bodyEn = (notice.detailsEn || notice.categoryEn || 'Tap to view details.').slice(0, 180);

    const sharedDataPayload: Record<string, string> = {
      type: 'NOTICE',
      id: noticeId,
      noticeId,
      category: notice.categoryBn || notice.categoryEn || 'Notice',
      isUrgent: isUrgent ? 'true' : 'false',
      date: notice.dateBn || notice.dateEn || '',
      channelId: CONFIG.notificationChannels.notices,
    };

    // 1. Broadcast to Bengali FCM Topic (notices_bn)
    try {
      const topicBnMessage: Message = {
        topic: CONFIG.topics.noticesBn,
        notification: { title: titleBn, body: bodyBn },
        data: sharedDataPayload,
        android: {
          priority: isUrgent ? 'high' : 'normal',
          notification: { channelId: CONFIG.notificationChannels.notices },
        },
      };
      await fcm.send(topicBnMessage);
      totalSuccess += 1;
      totalRecipients += 1;
    } catch (err: any) {
      console.warn('[MessagingService] Topic notices_bn broadcast notice error:', err.message);
      errors.push(`Topic BN: ${err.message}`);
    }

    // 2. Broadcast to English FCM Topic (notices_en)
    try {
      const topicEnMessage: Message = {
        topic: CONFIG.topics.noticesEn,
        notification: { title: titleEn, body: bodyEn },
        data: sharedDataPayload,
        android: {
          priority: isUrgent ? 'high' : 'normal',
          notification: { channelId: CONFIG.notificationChannels.notices },
        },
      };
      await fcm.send(topicEnMessage);
      totalSuccess += 1;
      totalRecipients += 1;
    } catch (err: any) {
      console.warn('[MessagingService] Topic notices_en broadcast notice error:', err.message);
      errors.push(`Topic EN: ${err.message}`);
    }

    return {
      recipientCount: totalRecipients,
      successCount: totalSuccess,
      failureCount: totalFailure,
      errors: errors.length > 0 ? errors : undefined,
    };
  }
}
