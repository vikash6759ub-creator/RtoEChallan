package com.rte.challan.utils

import android.content.Context
import android.content.SharedPreferences

object DataManager {

    private const val PREFS_NAME = "RtoEChallanPrefs"
    private const val KEY_NAME = "customerName"
    private const val KEY_MOBILE = "mobileNumber"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserData(context: Context, name: String, mobile: String) {
        getPrefs(context).edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_MOBILE, mobile)
            apply()
        }
    }

    fun getUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_NAME, null)
    }

    fun getMobileNumber(context: Context): String? {
        return getPrefs(context).getString(KEY_MOBILE, null)
    }
}
