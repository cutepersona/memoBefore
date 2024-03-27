package com.peng.power.memo.manager;

import android.app.Application;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MemoCallClient extends Application {

    public static MemoCallAPI getApiService(){
        return getInstance().create(MemoCallAPI.class);
    }

    private static Retrofit getInstance(){
        OkHttpClient client = new OkHttpClient.Builder().build();
//        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
        .baseUrl(DEFINE.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    }

}
