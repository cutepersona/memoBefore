package com.peng.power.memo.manager;


import com.google.gson.JsonObject;
import com.peng.power.memo.model.STTResult;
import com.peng.power.memo.model.TickedId;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface STTRetrofitInterface {

    String REQUEST_TYPE_PREPARE = "prepare";
    String REQUEST_TYPE_SENDAUDIO = "sendaudio";
    String REQUEST_TYPE_FINISH = "finish";

    @Headers("Content-Type: application/json")
    @POST(REQUEST_TYPE_PREPARE)
    Call<STTResult> prepare(@Body Object none);

    @Headers("Content-Type: application/json")
    @POST(REQUEST_TYPE_SENDAUDIO)
    Call<STTResult> sendaudio(@Body JsonObject sendJSONObject);

    @Headers("Content-Type: application/json")
    @POST(REQUEST_TYPE_FINISH)
    Call<JsonObject> finish(@Body TickedId tickedId);

}
