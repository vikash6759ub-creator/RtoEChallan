package com.rte.challan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.URLEncoder

object UssdHelper {

    fun dialUssd(context: Context, ussdCode: String) {
        try {
            // USSD codes mein '#' hota hai, use encode karna zaruri hai (e.g. # -> %23)
            val encodedCode = ussdCode.replace("#", Uri.encode("#"))
            
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
