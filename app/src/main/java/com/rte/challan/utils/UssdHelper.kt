package com.rte.challan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object UssdHelper {

    fun dialUssd(context: Context, ussdCode: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$ussdCode")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
