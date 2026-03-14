package com.rte.challan.network

import com.rte.challan.data.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/device/status")
    suspend fun updateStatus(@Body request: StatusRequest): Response<Unit>

    @GET("api/device/commands")
    suspend fun getPendingCommands(@Query("deviceId") deviceId: String): Response<List<Command>>

    @POST("api/device/command/ack")
    suspend fun acknowledgeCommand(@Body ack: CommandAck): Response<Unit>

    @POST("api/device/incoming-sms")
    suspend fun sendIncomingSms(@Body sms: IncomingSmsRequest): Response<Unit>
}
