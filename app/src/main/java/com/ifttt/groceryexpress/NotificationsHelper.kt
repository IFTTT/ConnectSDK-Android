package com.ifttt.groceryexpress

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Extension functions for sending local notifications.
 */
object NotificationsHelper {

    private const val NOTIFICATION_CHANNEL_ID = "grocery_express_notifications"

    /**
     * Send a local notification via the generic app notification channel.
     */
    fun Context.sendNotification(message: CharSequence) {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setColor(Color.BLACK)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun Context.ensureNotificationChannel() {
        val notificationManager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null
        ) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.foreground_notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
