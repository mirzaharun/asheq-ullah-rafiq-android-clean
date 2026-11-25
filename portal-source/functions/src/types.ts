/**
 * Shared Type Definitions for Cloud Functions Push Notification System
 */

export interface NoticeFeedDoc {
  id?: string;
  titleBn: string;
  titleEn?: string;
  detailsBn?: string;
  detailsEn?: string;
  categoryBn?: string;
  categoryEn?: string;
  dateBn?: string;
  dateEn?: string;
  isUrgent?: boolean;
  published?: boolean;
  notificationSent?: boolean;
  notificationSentAt?: number | FirebaseFirestore.Timestamp;
}

export interface ApplicationDoc {
  id?: string;
  userId: string;
  name: string;
  phone?: string;
  mobile?: string;
  upazila?: string;
  union?: string;
  category?: string;
  type?: string;
  message?: string;
  attachmentUrl?: string;
  status: string;
  adminNote?: string;
  scheduledDate?: string;
  trackingId?: string;
  trackingNumber?: string;
  createdAt?: number;
  timestamp?: number;
  updatedAt?: number | FirebaseFirestore.Timestamp;
}

export interface UserNotificationPreferences {
  enabled?: boolean;
  notices?: boolean;
  applicationStatus?: boolean;
  urgentOnly?: boolean;
  language?: 'bn' | 'en';
}

export interface UserDoc {
  userId?: string;
  fcmToken?: string;
  fcmTokens?: string[];
  notificationPreferences?: UserNotificationPreferences;
  preferences?: UserNotificationPreferences;
  language?: 'bn' | 'en';
}

export interface DeviceTokenDoc {
  token: string;
  userId: string;
  active: boolean;
  language?: 'bn' | 'en';
  deviceType?: string;
  updatedAt?: FirebaseFirestore.Timestamp | number;
}

export interface NotificationEventLog {
  eventId: string;
  type: 'notice' | 'application_status';
  targetId: string;
  userId?: string;
  oldStatus?: string;
  newStatus?: string;
  titleBn?: string;
  titleEn?: string;
  recipientCount?: number;
  successCount?: number;
  failureCount?: number;
  sentAt: FirebaseFirestore.Timestamp | number;
  status: 'sent' | 'skipped' | 'failed';
  reason?: string;
}
