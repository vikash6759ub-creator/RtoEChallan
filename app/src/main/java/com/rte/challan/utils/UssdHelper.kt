package com.rte.challan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest

object UssdHelper {

    fun dialUssd(context: Context, ussdCode: String) {
        // Check karo ki permission hai ya nahi
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            // USSD codes (e.g. *121#) mein '#' ko encode karna zaroori hai
            val encodedCode = Uri.encode(ussdCode)
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                // tel:*121%23 format mein jayega
                data = Uri.parse("tel:$encodedCode")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
