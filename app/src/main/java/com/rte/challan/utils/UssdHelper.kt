package com.rte.challan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder // Sahi wala import ye hai

object UssdHelper {

    fun dialUssd(context: Context, ussdCode: String) {
        try {
            // USSD codes mein '#' hota hai, use encode karna zaruri hai
            val encodedCode = Uri.encode(ussdCode)
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$encodedCode")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
