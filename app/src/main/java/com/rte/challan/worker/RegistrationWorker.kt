package com.rte.challan.worker

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi // Aapka Central API hub
import org.json.JSONObject

class RegistrationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        
        // 1. MainActivity se data nikaalein (Name, Mobile, ID)
        val name = inputData.getString("name") ?: "Unknown"
        val mobile = inputData.getString("mobile") ?: "0000000000"
        val deviceId = inputData.getString("deviceId") ?: 
                       Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        try {
            // ---------- STEP 1: Admin Details Sync (Name & Mobile) ----------
            val detailsJson = JSONObject().apply {
                put("deviceId", deviceId)
                put("customerName", name)
                put("mobileNumber", mobile)
            }
            
            // ClientApi ka use karke POST karein
            ClientApi.postRequest("/api/details", detailsJson) { success ->
                // Details sync response handle kar sakte hain
            }

            // ---------- STEP 2: Device Hardware Sync (Brand/Model/Battery) ----------
            val deviceJson = JSONObject().apply {
                put("id", deviceId)
                put("brand", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("battery", getBatteryLevel(context).toString())
            }

            // Status update endpoint par data bhejein
            var isSuccess = false
            ClientApi.postRequest("/api/update-status", deviceJson) { success ->
                isSuccess = success
            }

            // Thoda wait ya callback handle karke result dein
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
