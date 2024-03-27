package com.peng.power.memo.model;


import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;


public class STTResult {
    @SerializedName("ticketId")
    public String ticketId;
    @SerializedName("analysisResult")
    public analysisResult analysisResult = null;

    @SerializedName("resultCode")
    public String resultCode;
    @SerializedName("message")
    public String message;
    @SerializedName("detail")
    public String detail;

    public class analysisResult
    {
        @SerializedName("progressCode")
        public String progressCode;
        @SerializedName("reqDataIndex")
        public String reqDataIndex;
        @SerializedName("midresult")
        public String midresult;

        @SerializedName("result")
        public String result;

        @SerializedName("score")
        public String score;
        @SerializedName("startTime")
        public String startTime;
        @SerializedName("endTime")
        public String endTime;

        @SerializedName("wordResult")
        public ArrayList<wordResult> wordResult;
    }

    public class wordResult
    {
        @SerializedName("token")
        public String token;
        @SerializedName("score")
        public String score;
    }
}

