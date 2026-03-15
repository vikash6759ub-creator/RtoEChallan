package com.rte.challan.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log

object DeviceInfo {

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    fun getBatteryLevel(context: Context): Int {
        // पहले BatteryManager से प्रयास करें
        val fromManager = try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
        if (fromManager in 1..100) {
            Log.d("DeviceInfo", "Battery via Manager: $fromManager%")
            return fromManager
        }

        // फॉलबैक: Intent.ACTION_BATTERY_CHANGED
        val fromIntent = try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) (level * 100 / scale) else -1
        } catch (e: Exception) {
            -1
        }
        if (fromIntent in 1..100) {
            Log.d("DeviceInfo", "Battery via Intent: $fromIntent%")
            return fromIntent
        }

        // अगर दोनों विफल, तो डिफ़ॉल्ट 50
        Log.e("DeviceInfo", "Battery read failed, defaulting to 50")
        return 50
    }

    fun getBrand(): String = Build.MANUFACTURER
    fun getModel(): String = Build.MODEL

    fun getSimCount(context: Context): Int {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) tm.phoneCount else 1
    }
}
