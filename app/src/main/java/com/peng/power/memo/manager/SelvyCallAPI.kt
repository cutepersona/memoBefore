package com.peng.power.memo.manager

import com.google.gson.JsonObject
import com.peng.power.memo.model.STTResult
import com.peng.power.memo.model.TickedId
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface SelvyCallAPI {

    @POST("selvy/stt/prepare")
    fun prepare(@Body none: Any?): Call<STTResult>

    @POST("selvy/stt/sendaudio")
    fun sendaudio(@Body sendJSONObject: JsonObject?): Call<STTResult>

    @POST("selvy/stt/finish")
    fun finish(@Body tickedId: TickedId?): Call<JsonObject>
}