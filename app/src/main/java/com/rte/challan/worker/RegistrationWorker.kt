package com.rte.challan.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result // ✅ Sahi import jo screenshot mein missing tha
import com.rte.challan.network.ApiClient
// import com.rte.challan.data.RegistrationRequest // Agar aapka koi request model hai toh use karein

class RegistrationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("RegistrationWorker", "Device registration started...")

        return try {
            // Yahan aapka registration logic aayega
            // Example:
            // val response = ApiClient.instance.registerDevice(...)
            
            Log.d("RegistrationWorker", "Registration Successful")
            Result.success()
        } catch (e: Exception) {
            Log.e("RegistrationWorker", "Registration Failed: ${e.message}")
            // Pehli baar fail ho toh retry karna sahi rehta hai
            Result.retry()
        }
    }
}
