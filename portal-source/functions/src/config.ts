/**
 * Configuration for Asheq Ullah Rafiq Digital Portal Cloud Functions
 * Project: asheq-ullah-rafiq-digital-p
 * Firestore Database: ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */

export const CONFIG = {
  projectId: process.env.GCP_PROJECT || 'asheq-ullah-rafiq-digital-p',
  databaseId: 'ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943',
  collections: {
    notices: 'notices',
    applications: 'applications',
    appointments: 'appointments',
    users: 'users',
    admins: 'admins',
    notificationEvents: 'notification_events',
  },
  topics: {
    noticesAll: 'notices_all',
    noticesBn: 'notices_bn',
    noticesEn: 'notices_en',
  },
  notificationChannels: {
    notices: 'notices_channel',
    applicationStatus: 'status_channel',
  },
} as const;
