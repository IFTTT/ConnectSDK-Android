package com.ifttt.groceryexpress

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Example implementation of a foreground service: a persistent service constantly running on the background with a
 * visible notification.
 *
 * The purpose of this service is to make sure the Grocery Express app can get more frequent location updates, without
 * the background location limit.
 *
 * More details see https://developer.android.com/about/versions/oreo/background-location-limits.
 */
class LocationForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = NotificationManagerCompat.from(this)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setColor(Color.BLACK)
            .build()

        if (SDK_INT >= O) {
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.foreground_notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN
                )
                notificationChannel.setShowBadge(false)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        private const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "location_foreground_service"

        fun startForegroundService(context: Context) {
            if (SDK_INT < O) {
                return
            }

            context.startForegroundService(Intent(context, LocationForegroundService::class.java))
        }

        fun stopForegroundService(context: Context) {
            if (SDK_INT < O) {
                return
            }

            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
}
