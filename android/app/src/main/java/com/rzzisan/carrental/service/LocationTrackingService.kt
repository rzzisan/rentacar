package com.rzzisan.carrental.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.rzzisan.carrental.MainActivity
import com.rzzisan.carrental.R
import com.rzzisan.carrental.data.network.ApiClient
import com.rzzisan.carrental.data.network.LocationBody
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.coroutineContext

class LocationTrackingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null

    companion object {
        private const val CHANNEL_ID      = "location_tracking"
        private const val NOTIFICATION_ID = 1001
        const val  EXTRA_RENTAL_ID        = "rental_id"
        private const val PREF_NAME       = "location_tracking_prefs"
        private const val PREF_RENTAL_ID  = "active_rental_id"

        fun start(context: Context, rentalId: Int) {
            // SharedPreferences-এ rentalId সংরক্ষণ (service restart-এ টিকে থাকবে)
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(PREF_RENTAL_ID, rentalId).apply()
            val intent = Intent(context, LocationTrackingService::class.java)
                .putExtra(EXTRA_RENTAL_ID, rentalId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(PREF_RENTAL_ID).apply()
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Intent থেকে rentalId পাও। null intent (START_STICKY restart) হলে
        // SharedPreferences থেকে শেষবারের rentalId পুনরুদ্ধার করো।
        val rentalId = intent?.getIntExtra(EXTRA_RENTAL_ID, 0)
            ?.takeIf { it != 0 }
            ?: getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_RENTAL_ID, 0)

        if (rentalId == 0) { stopSelf(); return START_NOT_STICKY }

        val notifIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ট্রিপ চলছে")
            .setContentText("লোকেশন শেয়ার হচ্ছে...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        trackingJob?.cancel()
        trackingJob = scope.launch { trackLoop(rentalId) }

        return START_STICKY
    }

    private suspend fun trackLoop(rentalId: Int) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this@LocationTrackingService)

        while (coroutineContext.isActive) {
            try {
                val loc = getLocation(fusedClient)
                if (loc != null) {
                    ApiClient.service.postLocation(
                        LocationBody(
                            rentalId = rentalId,
                            latitude = loc.first,
                            longitude = loc.second,
                            accuracy = loc.third
                        )
                    )
                }
            } catch (_: Exception) {
                // Network failure: skip this point, retry next interval
            }
            delay(5 * 60 * 1000L) // 5 minutes
        }
    }

    private suspend fun getLocation(
        fusedClient: com.google.android.gms.location.FusedLocationProviderClient
    ): Triple<Double, Double, Float>? {
        val hasPerm = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) return null

        return try {
            @Suppress("MissingPermission")
            var loc = fusedClient.lastLocation.await()

            if (loc == null) {
                val cts = CancellationTokenSource()
                loc = withTimeoutOrNull(10_000L) {
                    @Suppress("MissingPermission")
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).await()
                }
                if (loc == null) cts.cancel()
            }

            loc?.let { Triple(it.latitude, it.longitude, it.accuracy) }
        } catch (_: Exception) { null }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ট্রিপ লোকেশন ট্র্যাকিং",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "সক্রিয় ট্রিপ চলাকালীন লোকেশন ট্র্যাক করে"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
