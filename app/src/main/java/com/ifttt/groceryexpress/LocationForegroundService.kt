package com.ifttt.groceryexpress

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ifttt.location.ConnectLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Example implementation of a foreground service: a persistent service constantly running on the background with a
 * visible notification.
 *
 * The purpose of this service is to make sure the Grocery Express app can get more frequent location updates, without
 * the background location limit.
 *
 * More details see https://developer.android.com/about/versions/oreo/background-location-limits.
 */
class LocationForegroundService : Service(), CoroutineScope {

    private lateinit var parentJob: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + parentJob

    override fun onCreate() {
        super.onCreate()

        parentJob = SupervisorJob()
    }

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

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        // Poll LocationManager for the device's current location, and update ConnectLocation
        // with any missed geo-fence events from Awareness API.
        launch(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    this@LocationForegroundService,
                    ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.requestSingleUpdate(Criteria().apply {
                    accuracy = Criteria.ACCURACY_FINE
                }, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        ConnectLocation.getInstance().reportEvent(
                            this@LocationForegroundService,
                            location.latitude,
                            location.longitude,
                            null
                        )

                        stopSelf()
                    }

                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: Bundle?
                    ) {
                        // No-op
                    }

                    override fun onProviderEnabled(provider: String?) {
                        // No-op
                    }

                    override fun onProviderDisabled(provider: String?) {
                        // No-op
                    }
                }, Looper.getMainLooper())
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {
        private const val LOCATION_POLLING_DELAY = 2 * 60L * 1000L
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
