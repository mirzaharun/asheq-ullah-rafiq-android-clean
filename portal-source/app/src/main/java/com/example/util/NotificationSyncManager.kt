package com.example.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages FCM device token synchronization, topic subscription,
 * and user notification preferences for the Asheq Ullah Rafiq Digital Portal.
 *
 * Targets named Firestore database:
 * ai-studio-android-ashequll-1eaee2e6-a2b4-4b98-a823-0ccf82508943
 */
object NotificationSyncManager {
    private const val TAG = "NotificationSyncMgr"
    private const val TOPIC_NOTICES_ALL = "notices_all"
    private const val TOPIC_NOTICES_BN = "notices_bn"
    private const val TOPIC_NOTICES_EN = "notices_en"

    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var cachedToken: String? = null

    /**
     * Resolves the named Firestore database instance configured in strings.xml
     */
    fun getDb(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext
        val databaseId = appContext
            .getString(R.string.firestore_database_id)
            .trim()
        require(databaseId.isNotBlank() && databaseId != "(default)") {
            "A named Firestore database ID must be configured for this app."
        }
        return FirebaseFirestore.getInstance(databaseId)
    }

    /**
     * Returns a stable hardware/install device identifier to distinguish devices.
     */
    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrBlank()) androidId else "dev_${UUID.randomUUID().toString().take(12)}"
        } catch (_: Exception) {
            "dev_${UUID.randomUUID().toString().take(12)}"
        }
    }

    /**
     * Retrieves current FCM token and synchronizes it to Firestore if a user is authenticated.
     */
    fun syncToken(context: Context, tokenOverride: String? = null) {
        scope.launch {
            try {
                val token = tokenOverride ?: try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed retrieving FCM token from FirebaseMessaging: ${e.message}")
                    null
                }

                if (token.isNullOrBlank()) {
                    return@launch
                }

                cachedToken = token

                val authUser = Firebase.auth.currentUser ?: return@launch
                val uid = authUser.uid
                val db = getDb(context)
                val deviceId = getDeviceId(context)

                val userDocRef = db.collection("users").document(uid)
                val deviceTokenRef = userDocRef.collection("fcm_tokens").document(deviceId)

                // 1. Save device-specific token in fcm_tokens subcollection
                val deviceTokenData = hashMapOf(
                    "token" to token,
                    "deviceId" to deviceId,
                    "deviceModel" to (Build.MODEL ?: "Android Device"),
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                deviceTokenRef.set(deviceTokenData).await()

                // 2. Update primary user doc with array of active tokens without removing other devices
                val userUpdates = hashMapOf(
                    "fcmToken" to token,
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "lastActiveAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(userUpdates, com.google.firebase.firestore.SetOptions.merge()).await()

                Log.d(TAG, "Successfully synced FCM token for user $uid on device $deviceId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing FCM token: ${e.message}")
            }
        }
    }

    /**
     * Synchronizes topic subscriptions based on user's language and notice preference.
     * Strategy: Notice broadcasts use FCM Topics exclusively to avoid duplicate delivery.
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
                    // Subscribe to appropriate topics
                    messaging.subscribeToTopic(TOPIC_NOTICES_ALL).await()
                    if (language.equals("en", ignoreCase = true)) {
                        messaging.subscribeToTopic(TOPIC_NOTICES_EN).await()
                        messaging.unsubscribeFromTopic(TOPIC_NOTICES_BN).await()
                    } else {
                        messaging.subscribeToTopic(TOPIC_NOTICES_BN).await()
                        messaging.unsubscribeFromTopic(TOPIC_NOTICES_EN).await()
                    }
                    Log.d(TAG, "Subscribed to notice topics (Lang: $language)")
                } else {
                    // Opt-out: reliably unsubscribe from all notice topics
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_ALL).await()
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_BN).await()
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_EN).await()
                    Log.d(TAG, "Opted out: unsubscribed from all notice topics")
                }

                // If user is logged in, sync preferences to Firestore users/{uid}
                val authUser = Firebase.auth.currentUser
                if (authUser != null) {
                    val db = getDb(context)
                    val prefs = hashMapOf(
                        "enabled" to true,
                        "applicationStatus" to true,
                        "notices" to noticesEnabled,
                        "language" to language
                    )
                    db.collection("users").document(authUser.uid).set(
                        mapOf(
                            "notificationPreferences" to prefs,
                            "language" to language,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing notice preferences: ${e.message}")
            }
        }
    }

    /**
     * Safely handles user logout by deactivating only this device's token.
     * Preserves other devices' tokens belonging to this citizen.
     */
    fun onLogout(context: Context, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                val authUser = Firebase.auth.currentUser
                val token = cachedToken
                val deviceId = getDeviceId(context)

                if (authUser != null) {
                    val db = getDb(context)
                    val userDocRef = db.collection("users").document(authUser.uid)
                    val deviceTokenRef = userDocRef.collection("fcm_tokens").document(deviceId)

                    // 1. Mark this specific device's token as inactive
                    try {
                        deviceTokenRef.update(
                            mapOf(
                                "active" to false,
                                "deactivatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (_: Exception) {}

                    // 2. Remove this device's token from array without touching others
                    if (!token.isNullOrBlank()) {
                        try {
                            userDocRef.update(
                                mapOf("fcmTokens" to FieldValue.arrayRemove(token))
                            ).await()
                        } catch (_: Exception) {}
                    }
                }

                // 3. Unsubscribe this device from broadcast topics upon logout
                try {
                    val messaging = FirebaseMessaging.getInstance()
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_ALL).await()
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_BN).await()
                    messaging.unsubscribeFromTopic(TOPIC_NOTICES_EN).await()
                } catch (_: Exception) {}

                Log.d(TAG, "Completed logout token cleanup for device $deviceId")
            } catch (e: Exception) {
                Log.w(TAG, "Error during logout cleanup: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }
}
