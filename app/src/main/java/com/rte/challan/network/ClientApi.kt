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
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Ye wahi method hai jo aapka Worker dhoondh raha hai
    fun postRequest(endpoint: String, json: JSONObject, callback: (Boolean) -> Unit) {
        val requestBody = json.toString().toRequestBody(JSON)
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
}
