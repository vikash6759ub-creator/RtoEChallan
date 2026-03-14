package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Result  // ✅ यह import जरूरी है
import com.rte.challan.network.ApiClient
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.data.StatusRequest

class StatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {   // ✅ यह Result androidx.work.Result होना चाहिए
        val deviceId = DeviceInfo.getDeviceId(applicationContext)
        val battery = DeviceInfo.getBatteryLevel(applicationContext)
        val request = StatusRequest(deviceId, battery, online = true)

        return try {
            val response = ApiClient.instance.updateStatus(request)
            if (response.isSuccessful) {
                Log.d("StatusWorker", "Status updated")
                Result.success()
            } else {
                Log.e("StatusWorker", "Failed: ${response.code()}")
                Result.retry()   // ✅ यह अब काम करेगा
            }
        } catch (e: Exception) {
            Log.e("StatusWorker", "Error: ${e.message}")
            Result.retry()
        }
    }
}
