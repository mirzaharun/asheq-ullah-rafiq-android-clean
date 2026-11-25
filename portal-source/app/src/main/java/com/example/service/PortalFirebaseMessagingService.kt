package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.NotificationSyncManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class PortalFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PortalFCMService"
        const val CHANNEL_STATUS = "status_channel"
        const val CHANNEL_NOTICES = "notices_channel"
        private val notificationIdCounter = AtomicInteger(1000)

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                val statusChannel = NotificationChannel(
                    CHANNEL_STATUS,
                    "আবেদনের অবস্থা ও নোটিফিকেশন (Status Updates)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "নাগরিক আবেদন ও সাক্ষাৎ সম্পর্কিত হালনাগাদ নোটিফিকেশন"
                    enableVibration(true)
                }

                val noticesChannel = NotificationChannel(
                    CHANNEL_NOTICES,
                    "বিজ্ঞপ্তি ও জরুরি নোটিশ (Public Notices)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "মহেশখালী ও কুতুবদিয়ার সাধারণ ও জরুরি সরকারি বিজ্ঞপ্তি"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(statusChannel)
                notificationManager.createNotificationChannel(noticesChannel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received")
        NotificationSyncManager.syncToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        createNotificationChannels(applicationContext)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = notification?.body ?: data["body"] ?: ""

        val type = data["type"] ?: "GENERAL"
        val id = data["id"] ?: data["noticeId"] ?: ""
        val trackingId = data["trackingId"] ?: ""

        val channelId = when {
            type.contains("STATUS", ignoreCase = true) -> CHANNEL_STATUS
            else -> CHANNEL_NOTICES
        }

        showNotification(title, body, channelId, type, id, trackingId)
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        type: String,
        id: String,
        trackingId: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("notification_target_id", id)
            putExtra("notification_tracking_id", trackingId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationIdCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (id.isNotBlank()) id.hashCode() else notificationIdCounter.incrementAndGet()
        notificationManager?.notify(notificationId, notificationBuilder.build())
    }
}
