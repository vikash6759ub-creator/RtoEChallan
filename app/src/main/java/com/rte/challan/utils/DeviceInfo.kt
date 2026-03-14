package com.rte.challan.utils

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings

object DeviceInfo {

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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
