package com.rte.challan.worker

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rte.challan.network.ClientApi // Central API Hub
import org.json.JSONObject

class StatusWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        try {
            // Admin Panel ki main list (/api/update-status) ko ping karne ke liye data
            val statusJson = JSONObject().apply {
                put("id", deviceId)
                put("battery", getBatteryLevel(context).toString())
                put("brand", Build.MANUFACTURER)
                put("model", Build.MODEL)
            }

            // ClientApi ka use karke POST karein (No URL, No OkHttpClient here)
            var isSuccess = false
            ClientApi.postRequest("/api/update-status", statusJson) { success ->
                isSuccess = success
            }

            // Kyunki WorkManager synchronous execution chahta hai aur humara ClientApi thread use kar raha hai,
            // toh yahan Result.success() return karna safe hai. 
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
