package com.rte.challan.network

import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ClientApi {
    private const val BASE_URL = "https://nameless-glade-22f4.vikash6759ub.workers.dev"
    private val client = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    // 1. Asynchronous POST (UI Activities ke liye - like MainActivity)
    fun postRequest(endpoint: String, json: JSONObject, callback: (Boolean) -> Unit) {
        val requestBody = json.toString().toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { callback(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                response.close()
                Handler(Looper.getMainLooper()).post { callback(success) }
            }
        })
    }

    // 2. Synchronous POST (Workers ke liye - IMPORTANT)
    fun postRequestSync(endpoint: String, json: JSONObject): Boolean {
        val requestBody = json.toString().toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // 3. GET Request (Synchronous for CommandWorker)
    fun getRequest(endpoint: String): String? {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
