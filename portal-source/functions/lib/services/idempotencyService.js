"use strict";
/**
 * Idempotency & Deduplication Service
 * Prevents duplicate push notifications across concurrent triggers,
 * document edits, or mirrored collections (e.g. applications <-> appointments).
 * Includes 5-minute recovery for abandoned locks and supports genuine repeated status transitions.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.IdempotencyService = void 0;
const firestore_1 = require("firebase-admin/firestore");
const adminApp_1 = require("./adminApp");
const config_1 = require("../config");
class IdempotencyService {
    /**
     * Generates a stable event ID for notice announcements.
     * Format: notice_{noticeId}
     */
    static getNoticeEventId(noticeId) {
        return `notice_${noticeId.trim()}`;
    }
    /**
     * Generates a stable per-application event ID for status updates.
     * Tracks per-application state transitions so repeated genuine status
     * changes are permitted while mirror triggers are deduplicated.
     * Format: app_status_{applicationId}
     */
    static getApplicationStatusEventId(applicationId, _status) {
        return `app_status_${applicationId.trim()}`;
    }
    /**
     * Atomically acquires an event processing lock using a Firestore transaction.
     * Returns true if lock was acquired, false if already processed or actively in progress.
     * Automatically recovers abandoned locks older than 5 minutes.
     */
    static async acquireEventLock(eventId, initialData) {
        const db = (0, adminApp_1.getDb)();
        const eventRef = db.collection(config_1.CONFIG.collections.notificationEvents).doc(eventId);
        const now = Date.now();
        try {
            return await db.runTransaction(async (transaction) => {
                const docSnap = await transaction.get(eventRef);
                if (docSnap.exists) {
                    const existing = docSnap.data();
                    if (initialData.type === 'application_status' && initialData.newStatus) {
                        const requestedStatus = initialData.newStatus.trim().toLowerCase();
                        const currentStatus = (existing?.currentStatus || '').trim().toLowerCase();
                        const inProgressStatus = (existing?.inProgressStatus || '').trim().toLowerCase();
                        // 1. If already sent for this specific status, deduplicate
                        if (currentStatus === requestedStatus && existing?.status === 'sent') {
                            return false;
                        }
                        // 2. If actively in progress for this status, check for 5-minute abandonment
                        if (inProgressStatus === requestedStatus && existing?.status === 'in_progress') {
                            let lockTime = 0;
                            if (existing?.sentAt?.toMillis) {
                                lockTime = existing.sentAt.toMillis();
                            }
                            else if (existing?.sentAt?.seconds) {
                                lockTime = existing.sentAt.seconds * 1000;
                            }
                            else if (typeof existing?.sentAt === 'number') {
                                lockTime = existing.sentAt;
                            }
                            const isAbandoned = lockTime > 0 && (now - lockTime) >= IdempotencyService.FIVE_MINUTES_MS;
                            if (!isAbandoned) {
                                return false; // Still actively processing
                            }
                            console.warn(`[IdempotencyService] Recovering abandoned in_progress lock for event ${eventId}`);
                        }
                    }
                    else {
                        // Notice or general event logic
                        if (existing?.status === 'sent') {
                            return false;
                        }
                        if (existing?.status === 'in_progress') {
                            let lockTime = 0;
                            if (existing?.sentAt?.toMillis) {
                                lockTime = existing.sentAt.toMillis();
                            }
                            else if (existing?.sentAt?.seconds) {
                                lockTime = existing.sentAt.seconds * 1000;
                            }
                            else if (typeof existing?.sentAt === 'number') {
                                lockTime = existing.sentAt;
                            }
                            const isAbandoned = lockTime > 0 && (now - lockTime) >= IdempotencyService.FIVE_MINUTES_MS;
                            if (!isAbandoned) {
                                return false; // Still actively processing
                            }
                            console.warn(`[IdempotencyService] Recovering abandoned in_progress notice lock for event ${eventId}`);
                        }
                    }
                }
                // Set the lock with current transition metadata
                transaction.set(eventRef, {
                    eventId,
                    status: 'in_progress',
                    inProgressStatus: initialData.newStatus ? initialData.newStatus.trim().toLowerCase() : null,
                    createdAt: firestore_1.FieldValue.serverTimestamp(),
                    sentAt: firestore_1.Timestamp.now(),
                    ...initialData,
                }, { merge: true });
                return true;
            });
        }
        catch (err) {
            console.error(`[IdempotencyService] Error acquiring lock for event ${eventId}:`, err);
            return false;
        }
    }
    /**
     * Marks the event as completed with final delivery counts and status.
     */
    static async completeEvent(eventId, details) {
        const db = (0, adminApp_1.getDb)();
        const eventRef = db.collection(config_1.CONFIG.collections.notificationEvents).doc(eventId);
        const updates = {
            ...details,
            inProgressStatus: null,
            completedAt: firestore_1.FieldValue.serverTimestamp(),
        };
        if (details.status === 'sent' && details.newStatus) {
            updates.currentStatus = details.newStatus.trim().toLowerCase();
        }
        try {
            await eventRef.set(updates, { merge: true });
        }
        catch (err) {
            console.error(`[IdempotencyService] Error completing event ${eventId}:`, err);
        }
    }
    /**
     * Releases lock if an unexpected transient error occurs so it can be retried safely.
     */
    static async releaseEventLock(eventId) {
        const db = (0, adminApp_1.getDb)();
        const eventRef = db.collection(config_1.CONFIG.collections.notificationEvents).doc(eventId);
        try {
            await eventRef.set({
                status: 'failed',
                inProgressStatus: null,
                failedAt: firestore_1.FieldValue.serverTimestamp(),
            }, { merge: true });
        }
        catch (err) {
            console.warn(`[IdempotencyService] Failed to release event lock for ${eventId}:`, err);
        }
    }
}
exports.IdempotencyService = IdempotencyService;
IdempotencyService.FIVE_MINUTES_MS = 5 * 60 * 1000;
//# sourceMappingURL=idempotencyService.js.map