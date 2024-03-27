package com.peng.power.memo.manager;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;


import java.io.UnsupportedEncodingException;

public class STTPrepare {


    @SerializedName("engine")
    public engine engine;
    class engine{
        public int modelId;
        public int kwdId;
        public int codec;
        public int scoreUse;
    }

    @SerializedName("sttServer")
    public sttServer sttServer;
    public class sttServer{
        public String authCode;
        public String ticketIdPrefix;
    }

    @SerializedName("sttResult")
    public sttResult sttResult;
    public class sttResult{
        public int wordInfo;
        public String midresult;
    }

    @SerializedName("engineEpd")
    public engineEpd engineEpd;
    public class engineEpd{
        public int epdUse;
        //[Todo]20210617 optional
//        public int startTimeout;
//        public int durationTimeout;
        public int endMargin;
    }


    @SerializedName("contentSave")
    public contentSave  contentSave;

    class contentSave{

        @SerializedName("contentPath")
        public String  contentPath;

        @SerializedName("contentCodec")
        public String  contentCodec;

        @SerializedName("saveMode")
        public String saveMode;
    }

    //[Todo]20220617 optional
    @SerializedName("restServer")
    public restServer restServer;
    class restServer{
        @SerializedName("audioDecodeUse")
        public int audioDecodeUse;

        @SerializedName("audioFormat")
        public String audioFormat;
    }


    public STTPrepare setModelId(int modelId){
        engine.modelId = modelId;
        return this;
    }

    public STTPrepare setCodec(int codec){
        engine.codec = codec;
        return this;
    }

    public STTPrepare setScoreUse(int codec){
        engine.scoreUse = codec;
        return this;
    }

    public STTPrepare setAuthCode(String authCode){
        this.sttServer.authCode = authCode;
        return this;
    }

    public STTPrepare setTicketIdPrefix(String ticketIdPrefix){
        sttServer.ticketIdPrefix = ticketIdPrefix;
        return this;
    }

    public STTPrepare setWordInfo(int wordInfo){
        sttResult.wordInfo = wordInfo;
        return this;
    }

    public STTPrepare setMidResult(String midresult){
        sttResult.midresult = midresult;
        return this;
    }

    public String getMidResult() {
        return sttResult.midresult;
    }

    public STTPrepare setEpdUse(int epdUse){
        engineEpd.epdUse = epdUse;
        return this;
    }

    public STTPrepare setContentSave(String path) {
        return setContentSave(path, SelvyCommon.OPT_FILE_TYPE, SelvyCommon.OPT_SAVE_TYPE);
    }

    public STTPrepare setContentSave(String path, String aCodec, String saveMode){

        if(path == null || path.isEmpty()){
            contentSave = null;
            return this;
        }

        contentSave = new contentSave();
        try {
            String szBase64 = Base64.encodeToString(path.getBytes("UTF-8"), Base64.NO_WRAP);
            contentSave.contentPath = szBase64;
            contentSave.contentCodec = aCodec;
            //[Todo]20210617 add
            contentSave.saveMode = saveMode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }

    public STTPrepare setDecode(int aUse, String format){
        restServer = new restServer();
        restServer.audioDecodeUse = aUse;
        restServer.audioFormat = format;
        return this;
    }

    public STTPrepare setEPDEndMargin(int endMargin){
        engineEpd.endMargin = 3000;
        return this;
    }


    public STTPrepare()
    {
        engine = new engine();
        engine.kwdId = SelvyCommon.OPT_KWDID;
        engine.codec = SelvyCommon.STT_CODEC_16K;
        engine.scoreUse = SelvyCommon.OPT_SCORE_OFF;

        sttServer = new sttServer();
        sttServer.authCode = "";
        sttServer.ticketIdPrefix = "";

        sttResult = new sttResult();
        sttResult.wordInfo = SelvyCommon.OPT_WORD_INFO;
        sttResult.midresult = SelvyCommon.OPT_MID_RESULT;

        engineEpd = new engineEpd();
        engineEpd.epdUse = SelvyCommon.OPT_EPD;
        engineEpd.endMargin = SelvyCommon.OPT_ENDMARGIN;

//        if(abUseContentSave)
//        {
//            contentSave = new contentSave();
//            try {
//                String szBase64 = Base64.encodeToString(path.getBytes("UTF-8"), Base64.NO_WRAP);
//                contentSave.contentPath = szBase64;
//                contentSave.contentCodec = aCodec;
//                //[Todo]20210617 add
//                contentSave.saveMode = "truncate";
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
//        else
//        {
//            contentSave = null;
//        }
    }

    public JsonObject getJsonObject()
    {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        Log.d("Selvy", json);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(json).getAsJsonObject();
        return jsonObject;
    }
}
