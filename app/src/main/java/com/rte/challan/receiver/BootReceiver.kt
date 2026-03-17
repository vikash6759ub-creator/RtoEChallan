package com.rte.challan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rte.challan.service.BackgroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Boot ke alava Quick Boot (reboot) ko bhi handle karein
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            try {
                val serviceIntent = Intent(context, BackgroundService::class.java)
                
                // Android 12+ ke liye Foreground Service start karne ka sahi tarika
                ContextCompat.startForegroundService(context, serviceIntent)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
