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

class StatusWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val client = OkHttpClient()
    private val WORKER_URL = "https://your-worker.workers.dev" // <--- Apna URL yahan dalein

    override fun doWork(): Result {
        val context = applicationContext
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        try {
            // Admin Panel ki main list (/api/devices) ko update karne ke liye data
            val statusJson = JSONObject().apply {
                put("id", deviceId)
                put("battery", getBatteryLevel(context).toString())
                put("brand", Build.MANUFACTURER)
                put("model", Build.MODEL)
            }

            val body = statusJson.toString().toRequestBody("application/json".toMediaType())
            
            // Hum /api/update-status ya /api/devices POST use karenge
            val request = Request.Builder()
                .url("$WORKER_URL/api/update-status") 
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            return if (response.isSuccessful) Result.success() else Result.retry()

        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
