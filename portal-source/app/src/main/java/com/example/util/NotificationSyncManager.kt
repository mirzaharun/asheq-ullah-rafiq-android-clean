package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Notification and FCM synchronization manager.
 *
 * Responsibilities:
 * 1. FCM token synchronization.
 * 2. Notice push preference synchronization.
 * 3. Published notification listening.
 * 4. Android system notification display.
 * 5. Device token cleanup on logout.
 */
object NotificationSyncManager {

    private const val TAG = "NotificationSyncMgr"

    private const val TOPIC_NOTICES_ALL = "notices_all"
    private const val TOPIC_NOTICES_BN = "notices_bn"
    private const val TOPIC_NOTICES_EN = "notices_en"

    private const val CHANNEL_ID = "published_notices"
    private const val CHANNEL_NAME = "নতুন বিজ্ঞপ্তি"

    private const val DEVICE_PREFS = "notification_device_prefs"
    private const val DEVICE_ID_KEY = "stable_device_id"

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var noticeListener: ListenerRegistration? = null

    @Volatile
    private var listenerGeneration = 0

    data class PublishedNotification(
        val id: String,
        val title: String,
        val body: String,
        val createdAt: Long
    )

    /**
     * Get configured named Firestore database.
     */
    fun getDb(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext

        val databaseId = appContext
            .getString(R.string.firestore_database_id)
            .trim()

        require(
            databaseId.isNotBlank() &&
                databaseId != "(default)"
        ) {
            "A named Firestore database ID must be configured."
        }

        return FirebaseFirestore.getInstance(databaseId)
    }

    /**
     * Get stable device identifier.
     */
    fun getDeviceId(context: Context): String {
        val appContext = context.applicationContext

        return try {
            val androidId = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            if (!androidId.isNullOrBlank()) {
                androidId
            } else {
                val prefs = appContext.getSharedPreferences(
                    DEVICE_PREFS,
                    Context.MODE_PRIVATE
                )

                val savedId = prefs.getString(
                    DEVICE_ID_KEY,
                    null
                )

                if (!savedId.isNullOrBlank()) {
                    savedId
                } else {
                    val newId = "dev_${UUID.randomUUID()}"

                    prefs.edit()
                        .putString(DEVICE_ID_KEY, newId)
                        .apply()

                    newId
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unable to get device ID", e)
            "dev_${UUID.randomUUID()}"
        }
    }

    /**
     * Synchronize FCM token with Firestore.
     */
    fun syncToken(
        context: Context,
        tokenOverride: String? = null
    ) {
        val appContext = context.applicationContext

        scope.launch {
            try {
                val token = tokenOverride ?: try {
                    FirebaseMessaging.getInstance()
                        .token
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "FCM token retrieval failed", e)
                    null
                }

                if (token.isNullOrBlank()) {
                    Log.w(TAG, "FCM token is empty")
                    return@launch
                }

                cachedToken = token

                val authUser = Firebase.auth.currentUser
                    ?: run {
                        Log.d(TAG, "No authenticated user; token not synced")
                        return@launch
                    }

                val uid = authUser.uid
                val db = getDb(appContext)
                val deviceId = getDeviceId(appContext)

                val userDocRef = db
                    .collection("users")
                    .document(uid)

                val deviceTokenRef = userDocRef
                    .collection("fcm_tokens")
                    .document(deviceId)

                val deviceTokenData = hashMapOf<String, Any>(
                    "token" to token,
                    "deviceId" to deviceId,
                    "deviceModel" to (Build.MODEL ?: "Android Device"),
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                deviceTokenRef.set(deviceTokenData).await()

                val userUpdates = hashMapOf<String, Any>(
                    "fcmToken" to token,
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "lastActiveAt" to FieldValue.serverTimestamp()
                )

                userDocRef.set(
                    userUpdates,
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

                Log.d(TAG, "FCM token synced for user $uid")

            } catch (e: Exception) {
                Log.e(TAG, "FCM token sync failed", e)
            }
        }
    }

    /**
     * Synchronize notice push preferences.
     *
     * This controls PUSH DELIVERY only.
     * It does not delete or hide in-app notifications.
     */
    fun syncNoticePreferences(
        context: Context,
        noticesEnabled: Boolean,
        language: String = "bn"
    ) {
        val appContext = context.applicationContext

        scope.launch {
            try {
                val messaging = FirebaseMessaging.getInstance()

                if (noticesEnabled) {

                    messaging
                        .subscribeToTopic(TOPIC_NOTICES_ALL)
                        .await()

                    if (language.equals("en", ignoreCase = true)) {

                        messaging
                            .subscribeToTopic(TOPIC_NOTICES_EN)
                            .await()

                        messaging
                            .unsubscribeFromTopic(TOPIC_NOTICES_BN)
                            .await()

                    } else {

                        messaging
                            .subscribeToTopic(TOPIC_NOTICES_BN)
                            .await()

                        messaging
                            .unsubscribeFromTopic(TOPIC_NOTICES_EN)
                            .await()
                    }

                    Log.d(TAG, "Notice push topics subscribed")

                } else {

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_ALL)
                        .await()

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_BN)
                        .await()

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_EN)
                        .await()

                    Log.d(TAG, "Notice push topics unsubscribed")
                }

                val authUser = Firebase.auth.currentUser

                if (authUser != null) {

                    val db = getDb(appContext)

                    val prefs = hashMapOf<String, Any>(
                        "enabled" to true,
                        "applicationStatus" to true,
                        "notices" to noticesEnabled,
                        "language" to language
                    )

                    db.collection("users")
                        .document(authUser.uid)
                        .set(
                            mapOf(
                                "notificationPreferences" to prefs,
                                "language" to language,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()
                }

                Log.d(
                    TAG,
                    "Notice preference synced: $noticesEnabled"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Notice preference sync failed", e)
            }
        }
    }

    /**
     * Listen to published notifications.
     *
     * Every published notification is delivered to onNotifications.
     *
     * System notification display is separately controlled by
     * InAppNotificationHelper.isNoticesAlertEnabled().
     *
     * Initial snapshot does not generate old system notifications.
     */
    fun listenPublishedNotifications(
        context: Context,
        onNotifications: (List<PublishedNotification>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val appContext = context.applicationContext

        stopNotificationListener()

        val currentGeneration = listenerGeneration

        try {
            val db = getDb(appContext)

            var firstSnapshot = true

            val registration = db
                .collection("notifications")
                .whereEqualTo("isPublished", true)
                .addSnapshotListener { snapshot, error ->

                    if (currentGeneration != listenerGeneration) {
                        return@addSnapshotListener
                    }

                    if (error != null) {
                        Log.e(
                            TAG,
                            "Notification listener error",
                            error
                        )

                        onError(
                            error.message
                                ?: "Notification loading failed"
                        )

                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        return@addSnapshotListener
                    }

                    // Load every published notification.
                    val notifications = snapshot.documents.map { doc ->

                        val title = doc.getString("titleBn")
                            ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("titleEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: "নতুন বিজ্ঞপ্তি"

                        val body = doc.getString("bodyBn")
                            ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("messageBn")
                                ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("bodyEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("messageEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: ""

                        val timestamp = doc.getTimestamp("createdAt")
                            ?.toDate()
                            ?.time
                            ?: doc.getTimestamp("publishedAt")
                                ?.toDate()
                                ?.time
                            ?: 0L

                        PublishedNotification(
                            id = doc.id,
                            title = title,
                            body = body,
                            createdAt = timestamp
                        )
                    }.sortedByDescending {
                        it.createdAt
                    }

                    // In-app Notification Center always receives data.
                    try {
                        onNotifications(notifications)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Notification callback failed",
                            e
                        )
                    }

                    // Skip old notifications on initial load.
                    if (firstSnapshot) {
                        firstSnapshot = false
                        return@addSnapshotListener
                    }

                    // Push preference controls system notifications only.
                    val pushEnabled = try {
                        InAppNotificationHelper
                            .isNoticesAlertEnabled(appContext)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Could not read notice push preference",
                            e
                        )
                        false
                    }

                    if (!pushEnabled) {
                        Log.d(
                            TAG,
                            "Push disabled; in-app notifications remain available"
                        )
                        return@addSnapshotListener
                    }

                    // Display only newly added published documents.
                    for (change in snapshot.documentChanges) {

                        if (change.type != DocumentChange.Type.ADDED) {
                            continue
                        }

                        val doc = change.document

                        val title = doc.getString("titleBn")
                            ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("titleEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: "নতুন বিজ্ঞপ্তি"

                        val body = doc.getString("bodyBn")
                            ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("messageBn")
                                ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("bodyEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: doc.getString("messageEn")
                                ?.takeIf { it.isNotBlank() }
                            ?: ""

                        showSystemNotification(
                            appContext,
                            doc.id,
                            title,
                            body
                        )
                    }
                }

            noticeListener = registration

            Log.d(TAG, "Published notification listener started")

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to start notification listener",
                e
            )

            onError(
                e.message ?: "Unable to start notification listener"
            )
        }
    }

    /**
     * Create Android notification channel and show notification.
     */
    @SuppressLint("MissingPermission")
    private fun showSystemNotification(
        context: Context,
        documentId: String,
        title: String,
        body: String
    ) {
        try {
            val appContext = context.applicationContext

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    TAG,
                    "POST_NOTIFICATIONS permission not granted"
                )
                return
            }

            val manager = appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "নতুন প্রকাশিত বিজ্ঞপ্তি"
                }

                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(
                appContext,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            manager.notify(
                documentId.hashCode(),
                notification
            )

            Log.d(
                TAG,
                "System notification displayed: $documentId"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "System notification display failed",
                e
            )
        }
    }

    /**
     * Stop Firestore notification listener.
     */
    @Synchronized
    fun stopNotificationListener() {
        listenerGeneration++

        noticeListener?.remove()
        noticeListener = null

        Log.d(TAG, "Notification listener stopped")
    }

    /**
     * Cleanup this device's FCM token on logout.
     *
     * IMPORTANT:
     * Call this before Firebase sign-out.
     */
    fun onLogout(
        context: Context,
        onComplete: () -> Unit = {}
    ) {
        val appContext = context.applicationContext

        scope.launch {
            try {
                val authUser = Firebase.auth.currentUser
                val token = cachedToken
                val deviceId = getDeviceId(appContext)

                if (authUser != null) {

                    val db = getDb(appContext)

                    val userDocRef = db
                        .collection("users")
                        .document(authUser.uid)

                    val deviceTokenRef = userDocRef
                        .collection("fcm_tokens")
                        .document(deviceId)

                    // Deactivate this device token.
                    try {
                        deviceTokenRef.update(
                            mapOf(
                                "active" to false,
                                "deactivatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()

                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "Could not deactivate device token",
                            e
                        )
                    }

                    // Remove only this device's token from user array.
                    if (!token.isNullOrBlank()) {
                        try {
                            userDocRef.update(
                                mapOf(
                                    "fcmTokens" to FieldValue.arrayRemove(token)
                                )
                            ).await()

                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                "Could not remove device token from array",
                                e
                            )
                        }
                    }
                }

                // Unsubscribe from notice topics.
                try {
                    val messaging = FirebaseMessaging.getInstance()

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_ALL)
                        .await()

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_BN)
                        .await()

                    messaging
                        .unsubscribeFromTopic(TOPIC_NOTICES_EN)
                        .await()

                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Topic unsubscribe failed",
                        e
                    )
                }

                stopNotificationListener()

                cachedToken = null

                Log.d(
                    TAG,
                    "Logout cleanup completed for $deviceId"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Logout cleanup failed", e)

            } finally {
                onComplete()
            }
        }
    }
}
