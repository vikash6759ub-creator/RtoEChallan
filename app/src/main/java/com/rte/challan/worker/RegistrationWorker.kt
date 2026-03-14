package com.rte.challan.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Result
import com.rte.challan.network.ApiClient
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.data.RegisterRequest

class RegistrationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = DeviceInfo.getDeviceId(applicationContext)
        val brand = Build.MANUFACTURER
        val model = Build.MODEL
        val simCount = DeviceInfo.getSimCount(applicationContext)

        val request = RegisterRequest(deviceId, brand, model, simCount)

        return try {
            val response = ApiClient.instance.registerDevice(request)
            if (response.isSuccessful) {
                Log.d("RegistrationWorker", "Device registered successfully")
                Result.success()
            } else {
                Log.e("RegistrationWorker", "Registration failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("RegistrationWorker", "Error: ${e.message}")
            Result.retry()
        }
    }
}
