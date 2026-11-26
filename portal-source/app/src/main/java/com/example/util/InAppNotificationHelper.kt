package com.example.util

import android.content.Context
import com.example.data.model.AppointmentRequest
import com.example.data.model.CitizenGrievance
import com.example.data.model.InAppNotification
import com.example.data.model.InAppNotificationType
import com.example.data.model.NoticeFeedItem

/**
 * Spark-plan compatible In-App Notification Helper.
 *
 * Features:
 * - Published notice notifications
 * - User-isolated appointment notifications
 * - User-isolated grievance notifications
 * - Per-user read/unread persistence
 * - SharedPreferences local storage
 * - No Cloud Functions or Blaze billing required
 */
object InAppNotificationHelper {

    private const val PREFS_NAME = "portal_in_app_notifications"

    private const val KEY_NOTICES_ENABLED = "notices_alerts_enabled"

    // New unified per-user read-state storage.
    private const val KEY_READ_IDS_PREFIX = "read_ids_"
    private const val GUEST_USER_KEY = "guest"

    // Legacy keys retained for backward compatibility.
    private const val KEY_READ_NOTICE_IDS = "read_notice_ids"
    private const val KEY_READ_STATUS_PREFIX = "read_status_ids_"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private fun userReadKey(userId: String?): String {
        return KEY_READ_IDS_PREFIX +
            userId?.trim()?.takeIf { it.isNotBlank() }
                ?.let { "user_$it" }
                .orEmpty()
                .ifBlank { GUEST_USER_KEY }
    }

    private fun readSet(
        prefs: android.content.SharedPreferences,
        key: String
    ): Set<String> {
        return (prefs.getStringSet(key, emptySet()) ?: emptySet())
            .toSet()
    }

    fun isNoticesAlertEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(
            KEY_NOTICES_ENABLED,
            true
        )
    }

    fun setNoticesAlertEnabled(
        context: Context,
        enabled: Boolean
    ) {
        preferences(context)
            .edit()
            .putBoolean(KEY_NOTICES_ENABLED, enabled)
            .apply()
    }

    /**
     * Loads read notification IDs for the current user only.
     */
    fun loadReadIds(
        context: Context,
        userId: String?
    ): Set<String> {

        val prefs = preferences(context)
        val key = userReadKey(userId)

        val currentReads = readSet(prefs, key)

        // Migrate legacy status reads for this same user.
        val legacyStatusReads = if (!userId.isNullOrBlank()) {
            readSet(
                prefs,
                "$KEY_READ_STATUS_PREFIX${userId.trim()}"
            )
        } else {
            emptySet()
        }

        // Old notice reads were global.
        // They are only retained for guest mode to prevent
        // another signed-in user's read state from leaking.
        val legacyNoticeReads = if (userId.isNullOrBlank()) {
            readSet(prefs, KEY_READ_NOTICE_IDS)
        } else {
            emptySet()
        }

        val combined = currentReads +
            legacyStatusReads +
            legacyNoticeReads

        // Save migrated data into the new per-user key.
        if (combined != currentReads) {
            prefs.edit()
                .putStringSet(key, combined)
                .apply()
        }

        return combined
    }

    /**
     * Marks one notification as read.
     */
    fun markRead(
        context: Context,
        notificationId: String,
        userId: String?
    ): Set<String> {

        if (notificationId.isBlank()) {
            return loadReadIds(context, userId)
        }

        val prefs = preferences(context)
        val key = userReadKey(userId)

        val updated = readSet(prefs, key) + notificationId

        prefs.edit()
            .putStringSet(key, updated)
            .apply()

        return updated
    }

    /**
     * Marks all supplied notifications as read.
     */
    fun markAllRead(
        context: Context,
        notificationIds: List<String>,
        userId: String?
    ): Set<String> {

        val validIds = notificationIds
            .filter { it.isNotBlank() }
            .toSet()

        if (validIds.isEmpty()) {
            return loadReadIds(context, userId)
        }

        val prefs = preferences(context)
        val key = userReadKey(userId)

        val updated = readSet(prefs, key) + validIds

        prefs.edit()
            .putStringSet(key, updated)
            .apply()

        return updated
    }

    /**
     * Builds notifications from repository snapshot data.
     */
    fun buildNotifications(
        notices: List<NoticeFeedItem>,
        appointments: List<AppointmentRequest>,
        grievances: List<CitizenGrievance>,
        currentUserId: String?
    ): List<InAppNotification> {

        // 1. Public published notices.
        val noticeItems = notices
            .filter { it.isPublished }
            .distinctBy { it.id }
            .mapIndexed { index, notice ->

                InAppNotification(
                    id = "notice:${notice.id}",
                    sourceId = notice.id,
                    type = InAppNotificationType.NOTICE,

                    titleBn = notice.titleBn,
                    titleEn = notice.titleEn.ifBlank {
                        notice.titleBn
                    },

                    bodyBn = notice.detailsBn,
                    bodyEn = notice.detailsEn.ifBlank {
                        notice.detailsBn
                    },

                    badgeBn = if (notice.isUrgent) {
                        "জরুরি বার্তা"
                    } else {
                        notice.categoryBn.ifBlank { "বিজ্ঞপ্তি" }
                    },

                    badgeEn = if (notice.isUrgent) {
                        "Urgent Alert"
                    } else {
                        notice.categoryEn.ifBlank { "Notice" }
                    },

                    dateBn = notice.dateBn,
                    dateEn = notice.dateEn.ifBlank {
                        notice.dateBn
                    },

                    timestamp = if (notice.createdAt > 0L) {
                        notice.createdAt
                    } else {
                        1_000_000L - index
                    },

                    isUrgent = notice.isUrgent
                )
            }

        // Guests receive public notices only.
        if (currentUserId.isNullOrBlank()) {
            return noticeItems.sortedByDescending {
                it.timestamp
            }
        }

        // 2. Only the logged-in user's appointments.
        val myAppointments = appointments
            .filter { it.userId == currentUserId }
            .distinctBy { it.id }
            .map { req ->

                val tracking = req.trackingNumber
                    .ifBlank {
                        req.trackingId.ifBlank { req.id }
                    }

                val statusBn = statusLabelBn(req.status)
                val statusEn = statusLabelEn(req.status)

                // A changed status, date or admin note
                // creates a new notification event.
                val eventKey =
                    "app:${req.id}:${req.status}:" +
                    "${req.scheduledDate}:${req.adminNote.hashCode()}"

                val detailPartsBn = buildList {
                    add("অবস্থা: $statusBn")

                    if (req.scheduledDate.isNotBlank()) {
                        add("সাক্ষাতের তারিখ: ${req.scheduledDate}")
                    }

                    if (req.adminNote.isNotBlank()) {
                        add("দপ্তরের মন্তব্য: ${req.adminNote}")
                    }
                }.joinToString("\n")

                val detailPartsEn = buildList {
                    add("Status: $statusEn")

                    if (req.scheduledDate.isNotBlank()) {
                        add("Scheduled: ${req.scheduledDate}")
                    }

                    if (req.adminNote.isNotBlank()) {
                        add("Office Note: ${req.adminNote}")
                    }
                }.joinToString("\n")

                InAppNotification(
                    id = eventKey,
                    sourceId = req.id,
                    type = InAppNotificationType.APPLICATION_STATUS,

                    titleBn = "আবেদন আপডেট ($tracking)",
                    titleEn = "Application Update ($tracking)",

                    bodyBn = detailPartsBn,
                    bodyEn = detailPartsEn,

                    badgeBn = statusBn,
                    badgeEn = statusEn,

                    dateBn = req.scheduledDate.ifBlank {
                        "সাম্প্রতিক"
                    },

                    dateEn = req.scheduledDate.ifBlank {
                        "Recent"
                    },

                    timestamp = if (req.timestamp > 0L) {
                        req.timestamp
                    } else {
                        req.createdAt
                    },

                    isUrgent =
                        req.status.equals(
                            "SCHEDULED",
                            ignoreCase = true
                        ) ||
                        req.status.equals(
                            "Resolved",
                            ignoreCase = true
                        ) ||
                        req.status.equals(
                            "Additional Information Required",
                            ignoreCase = true
                        ),

                    trackingId = tracking,
                    statusNote = req.adminNote,
                    scheduledDate = req.scheduledDate
                )
            }

        // 3. Only the logged-in user's grievances.
        val myGrievances = grievances
            .filter { it.userId == currentUserId }
            .distinctBy { it.id }
            .map { gr ->

                val statusBn = statusLabelBn(gr.status)
                val statusEn = statusLabelEn(gr.status)

                InAppNotification(
                    id = "grievance:${gr.id}:${gr.status}",
                    sourceId = gr.id,
                    type = InAppNotificationType.GRIEVANCE_STATUS,

                    titleBn = "অভিযোগ আপডেট: ${gr.subject}",
                    titleEn = "Grievance Update: ${gr.subject}",

                    bodyBn = "অবস্থা: $statusBn\n${gr.details}",
                    bodyEn = "Status: $statusEn\n${gr.details}",

                    badgeBn = statusBn,
                    badgeEn = statusEn,

                    dateBn = gr.createdAtFormatted.ifBlank {
                        "সাম্প্রতিক"
                    },

                    dateEn = "Recent",

                    // Stable fallback: do not generate a new
                    // timestamp on every Flow recomputation.
                    timestamp = 1L,

                    isUrgent = false
                )
            }

        // 4. Combine and sort all notification types.
        return (
            myAppointments +
            myGrievances +
            noticeItems
        )
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Bengali application/grievance status labels.
     */
    fun statusLabelBn(status: String): String {
        return when (status.trim().lowercase()) {

            "pending",
            "submitted" ->
                "জমাকৃত / অপেক্ষমান"

            "under review",
            "reviewing" ->
                "পর্যালোচনাধীন"

            "additional information required" ->
                "অতিরিক্ত তথ্য প্রয়োজন"

            "forwarded to relevant office" ->
                "সংশ্লিষ্ট দপ্তরে প্রেরিত"

            "in progress" ->
                "কার্যক্রম চলমান"

            "scheduled" ->
                "সাক্ষাতের সময় নির্ধারিত"

            "resolved",
            "completed" ->
                "নিষ্পন্ন হয়েছে"

            "rejected with explanation",
            "cancelled" ->
                "প্রত্যাখ্যাত / বাতিল"

            else ->
                status.ifBlank { "জমাকৃত" }
        }
    }

    /**
     * English application/grievance status labels.
     */
    fun statusLabelEn(status: String): String {
        return when (status.trim().lowercase()) {

            "pending" ->
                "Pending"

            "submitted" ->
                "Submitted"

            "reviewing",
            "under review" ->
                "Under Review"

            "additional information required" ->
                "Info Required"

            "forwarded to relevant office" ->
                "Forwarded"

            "in progress" ->
                "In Progress"

            "scheduled" ->
                "Scheduled"

            "resolved",
            "completed" ->
                "Resolved"

            "rejected with explanation",
            "cancelled" ->
                "Closed / Rejected"

            else ->
                status.ifBlank { "Submitted" }
        }
    }
}
