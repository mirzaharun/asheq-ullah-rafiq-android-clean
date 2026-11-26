package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Notification and FCM synchronization manager.
 *
 * Named Firestore database:
 * ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */
object NotificationSyncManager {

    private const val TAG = "NotificationSyncMgr"

    private const val TOPIC_NOTICES_ALL = "notices_all"
    private const val TOPIC_NOTICES_BN = "notices_bn"
    private const val TOPIC_NOTICES_EN = "notices_en"

    private const val CHANNEL_ID = "published_notices"
    private const val CHANNEL_NAME = "নতুন বিজ্ঞপ্তি"

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    private var noticeListener: ListenerRegistration? = null

    /**
     * Notification model for the app's Notification Center.
     */
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
        return try {
            val androidId = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            if (!androidId.isNullOrBlank()) {
                androidId
            } else {
                "dev_${UUID.randomUUID().toString().take(12)}"
            }

        } catch (e: Exception) {
            Log.w(TAG, "Unable to get Android ID", e)
            "dev_${UUID.randomUUID().toString().take(12)}"
        }
    }

    /**
     * Synchronize FCM token with Firestore.
     */
    fun syncToken(
        context: Context,
        tokenOverride: String? = null
    ) {
        scope.launch {
            try {
                val token = tokenOverride ?: try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.w(TAG, "FCM token retrieval failed", e)
                    null
                }

                if (token.isNullOrBlank()) {
                    return@launch
                }

                cachedToken = token

                val authUser = Firebase.auth.currentUser
                    ?: return@launch

                val uid = authUser.uid
                val db = getDb(context)
                val deviceId = getDeviceId(context)

                val userDocRef = db
                    .collection("users")
                    .document(uid)

                val deviceTokenRef = userDocRef
                    .collection("fcm_tokens")
                    .document(deviceId)

                // Save device-specific FCM token.
                val deviceTokenData = hashMapOf(
                    "token" to token,
                    "deviceId" to deviceId,
                    "deviceModel" to (Build.MODEL ?: "Android Device"),
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                deviceTokenRef.set(deviceTokenData).await()

                // Update user document.
                val userUpdates = hashMapOf(
                    "fcmToken" to token,
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "lastActiveAt" to FieldValue.serverTimestamp()
                )

                userDocRef.set(
                    userUpdates,
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

                Log.d(
                    TAG,
                    "FCM token synced for user $uid"
                )

            } catch (e: Exception) {
                Log.e(TAG, "FCM token sync failed", e)
            }
        }
    }

    /**
     * Synchronize notification topic preferences.
     */
    fun syncNoticePreferences(
        context: Context,
        noticesEnabled: Boolean,
        language: String = "bn"
    ) {
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

                    Log.d(TAG, "Notice topics subscribed")

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

                    Log.d(TAG, "Notice topics unsubscribed")
                }

                val authUser = Firebase.auth.currentUser

                if (authUser != null) {

                    val db = getDb(context)

                    val prefs = hashMapOf(
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

            } catch (e: Exception) {
                Log.e(TAG, "Notice preference sync failed", e)
            }
        }
    }

    /**
     * Listen to published notifications from Firestore.
     *
     * The callback supplies data to the app's Notification Center.
     * New notifications are also displayed in the Android notification bar.
     *
     * This listener works while the app process is active.
     */
    fun listenPublishedNotifications(
        context: Context,
        onNotifications: (List<PublishedNotification>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val appContext = context.applicationContext

        stopNotificationListener()

        try {
            val db = getDb(appContext)

            var firstSnapshot = true

            noticeListener = db.collection("notifications")
                .whereEqualTo("isPublished", true)
                .addSnapshotListener { snapshot, error ->

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

                    // Convert Firestore documents into app notification models.
                    val notifications = snapshot.documents.map { doc ->

                        val title = doc.getString("titleBn")
                            ?: doc.getString("titleEn")
                            ?: "নতুন বিজ্ঞপ্তি"

                        val body = doc.getString("bodyBn")
                            ?: doc.getString("messageBn")
                            ?: doc.getString("bodyEn")
                            ?: doc.getString("messageEn")
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

                    // Send data to app Notification Center.
                    onNotifications(notifications)

                    // Do not show old notifications on initial load.
                    if (firstSnapshot) {
                        firstSnapshot = false
                        return@addSnapshotListener
                    }

                    // Show only newly added published notifications.
                    for (change in snapshot.documentChanges) {

                        if (
                            change.type ==
                            Query.DocumentChange.Type.ADDED
                        ) {
                            val doc = change.document

                            val title = doc.getString("titleBn")
                                ?: doc.getString("titleEn")
                                ?: "নতুন বিজ্ঞপ্তি"

                            val body = doc.getString("bodyBn")
                                ?: doc.getString("messageBn")
                                ?: doc.getString("bodyEn")
                                ?: doc.getString("messageEn")
                                ?: ""

                            showSystemNotification(
                                appContext,
                                doc.id,
                                title,
                                body
                            )
                        }
                    }
                }

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
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            // Android 8.0+ notification channel.
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

            // Android 13+ permission check.
            if (
                Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    TAG,
                    "POST_NOTIFICATIONS permission not granted"
                )
                return
            }

            val notification = NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(body)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
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
            Log.e(TAG, "System notification display failed", e)
        }
    }

    /**
     * Stop Firestore notification listener.
     */
    fun stopNotificationListener() {
        noticeListener?.remove()
        noticeListener = null

        Log.d(TAG, "Notification listener stopped")
    }

    /**
     * Cleanup this device's FCM token on logout.
     *
     * Call this before Firebase sign-out.
     */
    fun onLogout(
        context: Context,
        onComplete: () -> Unit = {}
    ) {
        scope.launch {
            try {
                val authUser = Firebase.auth.currentUser
                val token = cachedToken
                val deviceId = getDeviceId(context)

                if (authUser != null) {

                    val db = getDb(context)

                    val userDocRef = db
                        .collection("users")
                        .document(authUser.uid)

                    val deviceTokenRef = userDocRef
                        .collection("fcm_tokens")
                        .document(deviceId)

                    // Deactivate this device's token.
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

                    // Remove only this device's token.
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
                    Log.w(TAG, "Topic unsubscribe failed", e)
                }

                // Stop Firestore listener.
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
