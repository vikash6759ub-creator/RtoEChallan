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
        return try {
            // Method 1: Using BatteryManager (API 21+)
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            Log.d("DeviceInfo", "Battery via BatteryManager: $percentage%")
            percentage
        } catch (e: Exception) {
            Log.e("DeviceInfo", "BatteryManager error: ${e.message}")
            // Method 2: Fallback using Intent
            getBatteryLevelLegacy(context)
        }
    }

    private fun getBatteryLevelLegacy(context: Context): Int {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                val percentage = level * 100 / scale
                Log.d("DeviceInfo", "Battery via Intent: $percentage%")
                percentage
            } else {
                Log.e("DeviceInfo", "Battery level unknown, defaulting to 50")
                50
            }
        } catch (e: Exception) {
            Log.e("DeviceInfo", "Legacy battery error: ${e.message}")
            50
        }
    }

    fun getBrand(): String = Build.MANUFACTURER
    fun getModel(): String = Build.MODEL

    fun getSimCount(context: Context): Int {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tm.phoneCount
        } else {
            1
        }
    }
}
