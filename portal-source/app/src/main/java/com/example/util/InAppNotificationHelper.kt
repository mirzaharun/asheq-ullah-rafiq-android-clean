package com.example.util

import android.content.Context
import com.example.data.model.AppointmentRequest
import com.example.data.model.CitizenGrievance
import com.example.data.model.InAppNotification
import com.example.data.model.InAppNotificationType
import com.example.data.model.NoticeFeedItem

/**
 * Spark-plan (free-tier) in-app notification builder & local read-state persistence.
 * Works purely from Firestore snapshot streams + SharedPreferences without Cloud Functions or Blaze billing.
 */
object InAppNotificationHelper {
    private const val PREFS_NAME = "portal_in_app_notifications"
    private const val KEY_NOTICES_ENABLED = "notices_alerts_enabled"
    private const val KEY_READ_NOTICE_IDS = "read_notice_ids"
    private const val KEY_READ_STATUS_PREFIX = "read_status_ids_"

    fun isNoticesAlertEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTICES_ENABLED, true)
    }

    fun setNoticesAlertEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTICES_ENABLED, enabled)
            .apply()
    }

    fun loadReadIds(context: Context, userId: String?): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticeReads = prefs.getStringSet(KEY_READ_NOTICE_IDS, emptySet()) ?: emptySet()
        val statusReads = if (!userId.isNullOrBlank()) {
            prefs.getStringSet("$KEY_READ_STATUS_PREFIX$userId", emptySet()) ?: emptySet()
        } else {
            emptySet()
        }
        return noticeReads + statusReads
    }

    fun markRead(context: Context, notificationId: String, userId: String?): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (notificationId.startsWith("notice:")) {
            val updated = (prefs.getStringSet(KEY_READ_NOTICE_IDS, emptySet()) ?: emptySet()) + notificationId
            prefs.edit().putStringSet(KEY_READ_NOTICE_IDS, updated).apply()
        } else if (!userId.isNullOrBlank()) {
            val key = "$KEY_READ_STATUS_PREFIX$userId"
            val updated = (prefs.getStringSet(key, emptySet()) ?: emptySet()) + notificationId
            prefs.edit().putStringSet(key, updated).apply()
        }
        return loadReadIds(context, userId)
    }

    fun markAllRead(context: Context, notificationIds: List<String>, userId: String?): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noticeBatch = notificationIds.filter { it.startsWith("notice:") }
        val existingNotices = prefs.getStringSet(KEY_READ_NOTICE_IDS, emptySet()) ?: emptySet()
        val editor = prefs.edit()
        if (noticeBatch.isNotEmpty()) {
            editor.putStringSet(KEY_READ_NOTICE_IDS, existingNotices + noticeBatch)
        }
        if (!userId.isNullOrBlank()) {
            val statusBatch = notificationIds.filterNot { it.startsWith("notice:") }
            if (statusBatch.isNotEmpty()) {
                val key = "$KEY_READ_STATUS_PREFIX$userId"
                val existingStatus = prefs.getStringSet(key, emptySet()) ?: emptySet()
                editor.putStringSet(key, existingStatus + statusBatch)
            }
        }
        editor.apply()
        return loadReadIds(context, userId)
    }

    fun buildNotifications(
        notices: List<NoticeFeedItem>,
        appointments: List<AppointmentRequest>,
        grievances: List<CitizenGrievance>,
        currentUserId: String?
    ): List<InAppNotification> {
        val noticeItems = notices
            .filter { it.isPublished }
            .mapIndexed { index, notice ->
                InAppNotification(
                    id = "notice:${notice.id}",
                    sourceId = notice.id,
                    type = InAppNotificationType.NOTICE,
                    titleBn = notice.titleBn,
                    titleEn = notice.titleEn.ifBlank { notice.titleBn },
                    bodyBn = notice.detailsBn,
                    bodyEn = notice.detailsEn.ifBlank { notice.detailsBn },
                    badgeBn = if (notice.isUrgent) "জরুরি বার্তা" else notice.categoryBn.ifBlank { "বিজ্ঞপ্তি" },
                    badgeEn = if (notice.isUrgent) "Urgent Alert" else notice.categoryEn.ifBlank { "Notice" },
                    dateBn = notice.dateBn,
                    dateEn = notice.dateEn.ifBlank { notice.dateBn },
                    timestamp = if (notice.createdAt > 0L) notice.createdAt else (1_000_000L - index),
                    isUrgent = notice.isUrgent
                )
            }

        if (currentUserId.isNullOrBlank()) {
            return noticeItems.sortedByDescending { it.timestamp }
        }

        val myAppointments = appointments
            .filter { it.userId == currentUserId }
            .map { req ->
                val tracking = req.trackingNumber.ifBlank { req.trackingId.ifBlank { req.id } }
                val statusBn = statusLabelBn(req.status)
                val statusEn = statusLabelEn(req.status)
                val eventKey = "app:${req.id}:${req.status}:${req.scheduledDate}:${req.adminNote.hashCode()}"
                val detailPartsBn = buildList {
                    add("অবস্থা: $statusBn")
                    if (req.scheduledDate.isNotBlank()) add("সাক্ষাতের তারিখ: ${req.scheduledDate}")
                    if (req.adminNote.isNotBlank()) add("দপ্তরের মন্তব্য: ${req.adminNote}")
                }.joinToString("\n")
                val detailPartsEn = buildList {
                    add("Status: $statusEn")
                    if (req.scheduledDate.isNotBlank()) add("Scheduled: ${req.scheduledDate}")
                    if (req.adminNote.isNotBlank()) add("Office Note: ${req.adminNote}")
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
                    dateBn = req.scheduledDate.ifBlank { "সাম্প্রতিক" },
                    dateEn = req.scheduledDate.ifBlank { "Recent" },
                    timestamp = if (req.timestamp > 0L) req.timestamp else req.createdAt,
                    isUrgent = req.status.equals("SCHEDULED", ignoreCase = true) ||
                        req.status.equals("Resolved", ignoreCase = true) ||
                        req.status.equals("Additional Information Required", ignoreCase = true),
                    trackingId = tracking,
                    statusNote = req.adminNote,
                    scheduledDate = req.scheduledDate
                )
            }

        val myGrievances = grievances
            .filter { it.userId == currentUserId }
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
                    dateBn = gr.createdAtFormatted.ifBlank { "সাম্প্রতিক" },
                    dateEn = "Recent",
                    timestamp = System.currentTimeMillis() - 1L,
                    isUrgent = false
                )
            }

        return (myAppointments + myGrievances + noticeItems).sortedByDescending { it.timestamp }
    }

    fun statusLabelBn(status: String): String {
        return when (status.trim().lowercase()) {
            "pending", "submitted" -> "জমাকৃত / অপেক্ষমান"
            "under review", "reviewing" -> "পর্যালোচনাধীন"
            "additional information required" -> "অতিরিক্ত তথ্য প্রয়োজন"
            "forwarded to relevant office" -> "সংশ্লিষ্ট দপ্তরে প্রেরিত"
            "in progress" -> "কার্যক্রম চলমান"
            "scheduled" -> "সাক্ষাতের সময় নির্ধারিত"
            "resolved", "completed" -> "নিষ্পন্ন হয়েছে"
            "rejected with explanation", "cancelled" -> "প্রত্যাখ্যাত / বাতিল"
            else -> status.ifBlank { "জমাকৃত" }
        }
    }

    fun statusLabelEn(status: String): String {
        return when (status.trim().lowercase()) {
            "pending" -> "Pending"
            "submitted" -> "Submitted"
            "reviewing", "under review" -> "Under Review"
            "additional information required" -> "Info Required"
            "forwarded to relevant office" -> "Forwarded"
            "in progress" -> "In Progress"
            "scheduled" -> "Scheduled"
            "resolved", "completed" -> "Resolved"
            "rejected with explanation", "cancelled" -> "Closed / Rejected"
            else -> status.ifBlank { "Submitted" }
        }
    }
}
