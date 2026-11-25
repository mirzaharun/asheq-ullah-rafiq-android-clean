/**
 * Comprehensive Automated Unit Tests for Server-side Cloud Functions Push Notifications
 * Phase 4.2 Remediation Verification:
 * 1. Bilingual Notification Copy Generation
 * 2. Strict PII Exclusion Verification
 * 3. Citizen Preference Filtering & Opt-out
 * 4. Notice Trigger & Topic Exclusivity (Single-Path Delivery)
 * 5. Application Status Transition & Mirror Collection Deduplication
 * 6. Token Schema Compatibility (fcmToken, fcmTokens, and fcm_tokens subcollection)
 * 7. 5-Minute Recovery for Abandoned in_progress Event Locks
 * 8. Repeated Genuine Status Transitions (Permits repeated transitions while deduplicating mirrors)
 */

const { test, describe, beforeEach } = require('node:test');
const assert = require('node:assert/strict');

// Import compiled services and triggers
const {
  MessagingService,
  UserPreferenceService,
  IdempotencyService,
  setDbForTesting,
  setMessagingForTesting,
  handleNoticeWrittenLogic,
  handleApplicationStatusChangeLogic,
  CONFIG,
} = require('../lib');

/**
 * In-Memory Mock Firestore for isolated unit testing
 */
class MockFirestore {
  constructor() {
    this.storage = new Map(); // key: "collection/docId" -> data
  }

  _getKey(collection, docId) {
    return `${collection}/${docId}`;
  }

  collection(collName) {
    const self = this;
    return {
      doc: (docId) => ({
        id: docId,
        get: async () => {
          const key = self._getKey(collName, docId);
          const data = self.storage.get(key);
          return {
            exists: !!data,
            data: () => (data ? { ...data } : undefined),
          };
        },
        set: async (data, options) => {
          const key = self._getKey(collName, docId);
          if (options && options.merge) {
            const existing = self.storage.get(key) || {};
            self.storage.set(key, { ...existing, ...data });
          } else {
            self.storage.set(key, data);
          }
        },
        update: async (fields) => {
          const key = self._getKey(collName, docId);
          const existing = self.storage.get(key) || {};
          self.storage.set(key, { ...existing, ...fields });
        },
        delete: async () => {
          const key = self._getKey(collName, docId);
          self.storage.delete(key);
        },
        collection: (subCollName) => ({
          doc: (subDocId) => ({
            set: async (subData) => {
              const subKey = `${self._getKey(collName, docId)}/${subCollName}/${subDocId}`;
              self.storage.set(subKey, subData);
            },
          }),
          where: (_field, op, val) => ({
            get: async () => {
              const prefix = `${self._getKey(collName, docId)}/${subCollName}/`;
              const docs = [];
              for (const [k, v] of self.storage.entries()) {
                if (k.startsWith(prefix)) {
                  if (op === '!=' && v.active === val) continue;
                  docs.push({ data: () => ({ ...v }) });
                }
              }
              return { docs, forEach: (fn) => docs.forEach(fn) };
            },
          }),
        }),
      }),
      limit: (n) => ({
        get: async () => {
          const prefix = `${collName}/`;
          const docs = [];
          for (const [k, v] of self.storage.entries()) {
            if (k.startsWith(prefix)) {
              docs.push({ data: () => ({ ...v }) });
              if (docs.length >= n) break;
            }
          }
          return { docs };
        },
      }),
      get: async () => {
        const prefix = `${collName}/`;
        const docs = [];
        for (const [k, v] of self.storage.entries()) {
          if (k.startsWith(prefix)) {
            docs.push({ data: () => ({ ...v }) });
          }
        }
        return { docs, forEach: (fn) => docs.forEach(fn) };
      },
    };
  }

  async runTransaction(updateFunction) {
    const self = this;
    const staged = new Map();
    const mockTx = {
      get: async (docRef) => {
        const docId = docRef.id || 'default_event';
        const key = `${CONFIG.collections.notificationEvents}/${docId}`;
        const data = self.storage.get(key);
        return {
          exists: !!data,
          data: () => (data ? { ...data } : undefined),
        };
      },
      set: (docRef, data, options) => {
        const docId = docRef.id || 'default_event';
        const key = `${CONFIG.collections.notificationEvents}/${docId}`;
        if (options && options.merge) {
          const existing = self.storage.get(key) || {};
          staged.set(key, { ...existing, ...data });
        } else {
          staged.set(key, data);
        }
      },
    };

    const result = await updateFunction(mockTx);
    for (const [k, v] of staged.entries()) {
      self.storage.set(k, v);
    }
    return result;
  }
}

/**
 * Mock FCM Messaging Service
 */
class MockMessaging {
  constructor() {
    this.sentMessages = [];
    this.sentMulticasts = [];
  }

  async send(message) {
    this.sentMessages.push(message);
    return 'projects/test-project/messages/msg_12345';
  }

  async sendEachForMulticast(multicast) {
    this.sentMulticasts.push(multicast);
    return {
      responses: multicast.tokens.map(() => ({ success: true, messageId: 'msg_tok_123' })),
      successCount: multicast.tokens.length,
      failureCount: 0,
    };
  }
}

describe('Asheq Ullah Rafiq Portal - Server-Side FCM Push Notification System', () => {
  let mockDb;
  let mockMessaging;

  beforeEach(() => {
    mockDb = new MockFirestore();
    mockMessaging = new MockMessaging();
    setDbForTesting(mockDb);
    setMessagingForTesting(mockMessaging);
  });

  describe('1. Bilingual Notification Copy Generation', () => {
    test('Correctly translates all status transitions to Bengali', () => {
      const statuses = [
        { status: 'submitted', expectedWord: 'গৃহীত' },
        { status: 'pending', expectedWord: 'অপেক্ষমাণ' },
        { status: 'under review', expectedWord: 'পর্যালোচনা' },
        { status: 'approved', expectedWord: 'অনুমোদিত' },
        { status: 'scheduled', expectedWord: 'সাক্ষাতের' },
        { status: 'completed', expectedWord: 'সম্পন্ন' },
        { status: 'rejected', expectedWord: 'সিদ্ধান্ত' },
      ];

      for (const { status, expectedWord } of statuses) {
        const result = MessagingService.getStatusNotificationText(status, 'bn');
        assert.equal(result.title, 'আবেদনের অবস্থা হালনাগাদ');
        assert.ok(
          result.body.includes(expectedWord),
          `Status "${status}" should contain "${expectedWord}" in body: "${result.body}"`
        );
      }
    });

    test('Correctly translates all status transitions to English', () => {
      const statuses = [
        { status: 'submitted', expectedWord: 'received' },
        { status: 'pending', expectedWord: 'pending' },
        { status: 'under review', expectedWord: 'review' },
        { status: 'approved', expectedWord: 'approved' },
        { status: 'scheduled', expectedWord: 'scheduled' },
        { status: 'completed', expectedWord: 'completed' },
        { status: 'rejected', expectedWord: 'decision' },
      ];

      for (const { status, expectedWord } of statuses) {
        const result = MessagingService.getStatusNotificationText(status, 'en');
        assert.equal(result.title, 'Application Status Update');
        assert.ok(
          result.body.toLowerCase().includes(expectedWord.toLowerCase()),
          `Status "${status}" should contain "${expectedWord}" in body: "${result.body}"`
        );
      }
    });
  });

  describe('2. Strict PII Exclusion Verification', () => {
    test('Push notification title, body, and data payload MUST NOT contain citizen sensitive PII', async () => {
      const citizenApplicationWithPII = {
        id: 'APP-998877',
        trackingId: 'TRK-COX-2026-99',
        userId: 'user_cit_001',
        status: 'approved',
        applicantName: 'মোহাম্মদ করিম উল্লাহ',
        citizenName: 'মোহাম্মদ করিম উল্লাহ',
        phone: '01819876543',
        mobileNumber: '01819876543',
        nationalId: '19851234567890123',
        nid: '19851234567890123',
        village: 'ডুলাহাজারা',
        upazila: 'চকরিয়া',
        district: 'কক্সবাজার',
        fullAddress: 'গ্রাম: ডুলাহাজারা, থানা: চকরিয়া, জেলা: কক্সবাজার',
        petitionDescription: 'আমার বসতভিটার সীমানা সংক্রান্ত বিরোধ নিষ্পত্তির আবেদন।',
        privateCitizenNotes: 'গোপনীয় পারিবারক আর্থিক সহায়তা প্রয়োজন।',
      };

      const tokens = ['device_token_citizen_001_xyz'];
      const result = await MessagingService.sendApplicationStatusNotification(
        citizenApplicationWithPII,
        'approved',
        tokens,
        'bn'
      );

      assert.equal(result.successCount, 1);
      assert.equal(mockMessaging.sentMulticasts.length, 1);

      const dispatched = mockMessaging.sentMulticasts[0];
      const payloadString = JSON.stringify(dispatched);

      // Verify no citizen names
      assert.ok(!payloadString.includes('করিম উল্লাহ'), 'Must not contain citizen name in payload');
      // Verify no phone numbers
      assert.ok(!payloadString.includes('01819876543'), 'Must not contain citizen phone number');
      // Verify no NID
      assert.ok(!payloadString.includes('19851234567890123'), 'Must not contain national ID');
      // Verify no citizen address
      assert.ok(!payloadString.includes('ডুলাহাজারা'), 'Must not contain citizen village/address');
      // Verify no petition content
      assert.ok(!payloadString.includes('বসতভিটার সীমানা'), 'Must not contain petition content');
      assert.ok(!payloadString.includes('পারিবারক'), 'Must not contain citizen notes');

      // Verify safe metadata ONLY
      assert.equal(dispatched.data.type, 'APPLICATION_STATUS');
      assert.equal(dispatched.data.id, 'APP-998877');
      assert.equal(dispatched.data.status, 'approved');
      assert.equal(dispatched.data.trackingId, 'TRK-COX-2026-99');
      assert.equal(dispatched.data.channelId, CONFIG.notificationChannels.applicationStatus);
    });
  });

  describe('3. Citizen Preference Filtering & Opt-out', () => {
    test('Skips notification if citizen has globally disabled notifications (enabled: false)', async () => {
      const citizenUid = 'user_opted_out_001';
      mockDb.storage.set(`users/${citizenUid}`, {
        uid: citizenUid,
        fcmToken: 'token_opted_out_abc',
        notificationPreferences: {
          enabled: false,
          applicationStatus: true,
          notices: true,
        },
      });

      const profile = await UserPreferenceService.getUserProfileForStatusUpdate(citizenUid);
      assert.equal(profile, null, 'Must return null for user with enabled: false');
    });

    test('Skips notification if citizen has disabled application status notifications (applicationStatus: false)', async () => {
      const citizenUid = 'user_opted_out_status';
      mockDb.storage.set(`users/${citizenUid}`, {
        uid: citizenUid,
        fcmToken: 'token_active_def',
        notificationPreferences: {
          enabled: true,
          applicationStatus: false,
          notices: true,
        },
      });

      const profile = await UserPreferenceService.getUserProfileForStatusUpdate(citizenUid);
      assert.equal(profile, null, 'Must return null for user with applicationStatus: false');
    });

    test('Accepts notification if citizen has enabled status notifications', async () => {
      const citizenUid = 'user_enabled_001';
      mockDb.storage.set(`users/${citizenUid}`, {
        uid: citizenUid,
        fcmToken: 'token_valid_1234567890',
        notificationPreferences: {
          enabled: true,
          applicationStatus: true,
          language: 'bn',
        },
      });

      const profile = await UserPreferenceService.getUserProfileForStatusUpdate(citizenUid);
      assert.ok(profile !== null, 'Must return profile for opted-in user');
      assert.equal(profile.userId, citizenUid);
      assert.equal(profile.tokens.length, 1);
      assert.equal(profile.tokens[0], 'token_valid_1234567890');
      assert.equal(profile.language, 'bn');
    });
  });

  describe('4. Notice Trigger & Topic Exclusivity (Single-Path Delivery)', () => {
    test('Skips unpublished notice (published: false)', async () => {
      const result = await handleNoticeWrittenLogic('notice_draft_01', undefined, {
        titleBn: 'খসড়া বিজ্ঞপ্তি',
        published: false,
      });
      assert.equal(result.status, 'skipped');
      assert.equal(result.reason, 'Notice not published');
    });

    test('Dispatches broadcast exclusively via FCM Topics without direct token duplication', async () => {
      const notice = {
        titleBn: 'কক্সবাজার উন্নয়ন মেলা ২০২৬',
        titleEn: 'Coxs Bazar Development Fair 2026',
        categoryBn: 'উন্নয়ন',
        published: true,
        isUrgent: false,
      };

      const result = await handleNoticeWrittenLogic('notice_pub_01', undefined, notice);
      assert.equal(result.status, 'sent');

      // Verify FCM topics were broadcasted
      assert.equal(mockMessaging.sentMessages.length, 2, 'Must broadcast to exactly 2 topics (BN and EN)');
      const bnTopic = mockMessaging.sentMessages.find((m) => m.topic === CONFIG.topics.noticesBn);
      const enTopic = mockMessaging.sentMessages.find((m) => m.topic === CONFIG.topics.noticesEn);
      assert.ok(bnTopic, 'Topic notices_bn was targeted');
      assert.ok(enTopic, 'Topic notices_en was targeted');

      // Single-path verification: MUST NOT perform direct token multicast
      assert.equal(
        mockMessaging.sentMulticasts.length,
        0,
        'Notice broadcast must NOT use direct token multicasts (prevents duplicate delivery)'
      );
    });

    test('Prevents duplicate broadcast on notice edits (already published and notified)', async () => {
      const beforeNotice = {
        titleBn: 'মূল বিজ্ঞপ্তি',
        published: true,
        notificationSent: true,
      };
      const afterNotice = {
        titleBn: 'মূল বিজ্ঞপ্তি (বানান সংশোধন)',
        published: true,
        notificationSent: true,
      };

      const result = await handleNoticeWrittenLogic('notice_pub_02', beforeNotice, afterNotice);
      assert.equal(result.status, 'skipped');
      assert.equal(result.reason, 'Duplicate notice update prevented');
    });
  });

  describe('5. Application Status Transition & Mirror Collection Deduplication', () => {
    test('Skips when before and after status are identical', async () => {
      const before = { id: 'APP-100', userId: 'user_1', status: 'pending', note: 'First note' };
      const after = { id: 'APP-100', userId: 'user_1', status: 'pending', note: 'Second note' };

      const result = await handleApplicationStatusChangeLogic('APP-100', 'applications', before, after);
      assert.equal(result.status, 'skipped');
      assert.equal(result.reason, 'No status transition');
    });

    test('Skips when application has no valid userId', async () => {
      const before = { id: 'APP-101', status: 'pending' };
      const after = { id: 'APP-101', status: 'approved' };

      const result = await handleApplicationStatusChangeLogic('APP-101', 'applications', before, after);
      assert.equal(result.status, 'skipped');
      assert.equal(result.reason, 'Missing userId');
    });

    test('Sends notification on genuine status transition to approved', async () => {
      const userId = 'citizen_cox_77';
      mockDb.storage.set(`users/${userId}`, {
        uid: userId,
        fcmToken: 'fcm_token_valid_citizen_cox',
        notificationPreferences: { enabled: true, applicationStatus: true, language: 'bn' },
      });

      const before = { id: 'APP-200', userId, status: 'pending' };
      const after = { id: 'APP-200', userId, status: 'approved', trackingId: 'TRK-2026-200' };

      const result = await handleApplicationStatusChangeLogic('APP-200', 'applications', before, after);
      assert.equal(result.status, 'sent');

      assert.equal(mockMessaging.sentMulticasts.length, 1);
      const dispatched = mockMessaging.sentMulticasts[0];
      assert.equal(dispatched.tokens[0], 'fcm_token_valid_citizen_cox');
      assert.equal(dispatched.notification.title, 'আবেদনের অবস্থা হালনাগাদ');
      assert.ok(dispatched.notification.body.includes('অনুমোদিত'));
    });

    test('Prevents duplicate notification when mirror collections (applications & appointments) both trigger', async () => {
      const userId = 'citizen_mirror_test';
      const recordId = 'APPT-SHARED-555';

      mockDb.storage.set(`users/${userId}`, {
        uid: userId,
        fcmToken: 'fcm_token_mirror_test_user',
        notificationPreferences: { enabled: true, applicationStatus: true },
      });

      const before = { id: recordId, userId, status: 'pending' };
      const after = { id: recordId, userId, status: 'approved' };

      // 1. First trigger fires from 'applications' collection
      const resultFromApplications = await handleApplicationStatusChangeLogic(
        recordId,
        'applications',
        before,
        after
      );
      assert.equal(resultFromApplications.status, 'sent');
      assert.equal(mockMessaging.sentMulticasts.length, 1);

      // 2. Mirror trigger fires immediately after from 'appointments' collection
      const resultFromAppointments = await handleApplicationStatusChangeLogic(
        recordId,
        'appointments',
        before,
        after
      );

      // Must be skipped due to identical eventId and existing lock!
      assert.equal(resultFromAppointments.status, 'skipped');
      assert.equal(resultFromAppointments.reason, 'Duplicate event skipped by idempotency lock');

      // Crucial: No second push message was sent to the citizen
      assert.equal(
        mockMessaging.sentMulticasts.length,
        1,
        'Citizen must NOT receive a duplicate push notification from the mirror collection'
      );
    });
  });

  describe('6. Token Schema Compatibility', () => {
    test('Correctly aggregates tokens from fcmToken, fcmTokens array, and fcm_tokens subcollection', async () => {
      const uid = 'multi_device_citizen_88';
      mockDb.storage.set(`users/${uid}`, {
        uid,
        fcmToken: 'token_direct_primary',
        fcmTokens: ['token_direct_primary', 'token_second_tablet'],
        notificationPreferences: { enabled: true, applicationStatus: true, language: 'en' },
      });

      // Subcollection tokens: 1 active phone, 1 deactivated old phone
      mockDb.storage.set(`users/${uid}/fcm_tokens/device_phone_active`, {
        token: 'token_subcoll_phone_active',
        active: true,
      });
      mockDb.storage.set(`users/${uid}/fcm_tokens/device_old_deactivated`, {
        token: 'token_subcoll_old_stale',
        active: false,
      });

      const profile = await UserPreferenceService.getUserProfileForStatusUpdate(uid);
      assert.ok(profile !== null);
      assert.equal(profile.language, 'en');

      // Active tokens must be included
      assert.ok(profile.tokens.includes('token_direct_primary'));
      assert.ok(profile.tokens.includes('token_second_tablet'));
      assert.ok(profile.tokens.includes('token_subcoll_phone_active'));

      // Inactive / deactivated token MUST be excluded
      assert.ok(
        !profile.tokens.includes('token_subcoll_old_stale'),
        'Deactivated token from fcm_tokens must NOT be included'
      );
    });
  });

  describe('7. 5-Minute Recovery for Abandoned in_progress Event Locks', () => {
    test('Denies lock when an in_progress event is younger than 5 minutes', async () => {
      const eventId = 'notice_active_lock_test';
      // Simulate recent lock (2 minutes old)
      mockDb.storage.set(`notification_events/${eventId}`, {
        eventId,
        status: 'in_progress',
        sentAt: Date.now() - (2 * 60 * 1000),
      });

      const acquired = await IdempotencyService.acquireEventLock(eventId, {
        type: 'notice',
        targetId: 'notice_active_lock_test',
      });
      assert.equal(acquired, false, 'Lock younger than 5 minutes must remain active');
    });

    test('Recovers lock when an in_progress event is older than 5 minutes (abandoned crash recovery)', async () => {
      const eventId = 'notice_abandoned_lock_test';
      // Simulate crashed lock (6 minutes old)
      mockDb.storage.set(`notification_events/${eventId}`, {
        eventId,
        status: 'in_progress',
        sentAt: Date.now() - (6 * 60 * 1000),
      });

      const acquired = await IdempotencyService.acquireEventLock(eventId, {
        type: 'notice',
        targetId: 'notice_abandoned_lock_test',
      });
      assert.equal(acquired, true, 'Lock older than 5 minutes must be successfully recovered');
    });
  });

  describe('8. Repeated Genuine Status Transitions', () => {
    test('Allows genuine repeated status transitions (Pending -> Approved -> Pending -> Approved)', async () => {
      const userId = 'citizen_lifecycle_99';
      const recordId = 'APP-LIFECYCLE-1';

      mockDb.storage.set(`users/${userId}`, {
        uid: userId,
        fcmToken: 'fcm_token_lifecycle_user',
        notificationPreferences: { enabled: true, applicationStatus: true, language: 'bn' },
      });

      // Transition 1: Submitted -> Approved
      const res1 = await handleApplicationStatusChangeLogic(
        recordId,
        'applications',
        { id: recordId, userId, status: 'Submitted' },
        { id: recordId, userId, status: 'Approved' }
      );
      assert.equal(res1.status, 'sent', 'First transition to Approved must be sent');
      assert.equal(mockMessaging.sentMulticasts.length, 1);

      // Transition 2: Approved -> In Progress (reopened by authority)
      const res2 = await handleApplicationStatusChangeLogic(
        recordId,
        'applications',
        { id: recordId, userId, status: 'Approved' },
        { id: recordId, userId, status: 'In Progress' }
      );
      assert.equal(res2.status, 'sent', 'Transition to In Progress must be sent');
      assert.equal(mockMessaging.sentMulticasts.length, 2);

      // Transition 3: In Progress -> Approved (re-approved)
      const res3 = await handleApplicationStatusChangeLogic(
        recordId,
        'applications',
        { id: recordId, userId, status: 'In Progress' },
        { id: recordId, userId, status: 'Approved' }
      );
      assert.equal(res3.status, 'sent', 'Repeated transition to Approved must be sent');
      assert.equal(mockMessaging.sentMulticasts.length, 3);

      // Mirror trigger for Transition 3: appointments collection fires same transition
      const resMirror = await handleApplicationStatusChangeLogic(
        recordId,
        'appointments',
        { id: recordId, userId, status: 'In Progress' },
        { id: recordId, userId, status: 'Approved' }
      );
      assert.equal(resMirror.status, 'skipped', 'Mirror trigger for repeated transition must be skipped');
      assert.equal(mockMessaging.sentMulticasts.length, 3, 'Must not send duplicate for mirror trigger');
    });
  });
});
