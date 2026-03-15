package com.rte.challan.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.rte.challan.network.ApiClient
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.data.StatusRequest
import java.util.concurrent.TimeUnit

class StatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = DeviceInfo.getDeviceId(applicationContext)
        val battery = DeviceInfo.getBatteryLevel(applicationContext)
        showToast("🔋 Battery: $battery%")

        val request = StatusRequest(deviceId, battery, online = true)

        return try {
            val response = ApiClient.instance.updateStatus(request)
            if (response.isSuccessful) {
                Log.d("StatusWorker", "Status updated: $battery%")
                showToast("✅ Status updated")
                scheduleSelf(30, TimeUnit.SECONDS)   // 30 सेकंड बाद फिर से
                Result.success()
            } else {
                Log.e("StatusWorker", "Failed: ${response.code()}")
                showToast("❌ Update failed: ${response.code()}")
                scheduleSelf(30, TimeUnit.SECONDS)   // फिर भी अगले 30 सेकंड में कोशिश करें
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("StatusWorker", "Error: ${e.message}")
            showToast("🚨 ${e.message}")
            scheduleSelf(30, TimeUnit.SECONDS)       // एरर पर भी फिर से शेड्यूल
            Result.failure()
        }
    }

    private fun scheduleSelf(delay: Long, unit: TimeUnit) {
        val request = OneTimeWorkRequestBuilder<StatusWorker>()
            .setInitialDelay(delay, unit)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
