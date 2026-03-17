package com.rte.challan.utils

import android.content.Context
import android.content.SharedPreferences

object DataManager {

    private const val PREFS_NAME = "RtoEChallanPrefs"
    private const val KEY_NAME = "customerName"
    private const val KEY_MOBILE = "mobileNumber"
    private const val KEY_DEVICE_ID = "deviceId"
    private const val KEY_IS_ACTIVE = "isActive"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. User details aur Device ID ek saath save karein
    fun saveUserData(context: Context, name: String, mobile: String, deviceId: String) {
        getPrefs(context).edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_MOBILE, mobile)
            putString(KEY_DEVICE_ID, deviceId)
            putBoolean(KEY_IS_ACTIVE, true)
            apply()
        }
    }

    // 2. Getters taaki Workers mein asani se use ho sake
    fun getUserName(context: Context): String = getPrefs(context).getString(KEY_NAME, "Unknown") ?: "Unknown"
    
    fun getMobileNumber(context: Context): String = getPrefs(context).getString(KEY_MOBILE, "0000000000") ?: "0000000000"

    fun getDeviceId(context: Context): String? = getPrefs(context).getString(KEY_DEVICE_ID, null)

    // 3. App already active hai ya nahi check karne ke liye
    fun isActive(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_ACTIVE, false)
}
