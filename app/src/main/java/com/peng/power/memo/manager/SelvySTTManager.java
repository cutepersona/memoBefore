package com.peng.power.memo.manager;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peng.power.memo.model.STTResult;
import com.peng.power.memo.model.SendBuffer;
import com.peng.power.memo.model.TickedId;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SelvySTTManager implements Callback {

    private String TAG = this.getClass().getSimpleName();

    STTRetrofitInterface mRetrofitInterface = null;
    TickedId mTickedId = null;
    String mMidType = SelvyCommon.OPT_MID_RESULT;
    SelvyRecordManager mSelvyRecordManager = null;
    SelvyRecordManager.UICallBack mUICallBack = null;


    ArrayList<byte[]> mBufferList = null;
    int mTotalSendBuffer = 0;
    private boolean mWatiRequest = false;

    //[Todo]Http response
    public final int RES_UNKWON = -1;
    public final int RES_ONFAIL = -2;
    public final int RES_SUCESS = 200;
    public final int RES_BAD_REQUEST = 400;
    public final int RES_UNAUTHORIZED = 401;
    public final int RES_NOT_FOUND = 404;
    public final int RES_NOT_ALLOWED = 405;
    public static final int RES_BUSY = 429;
    public final int RES_INTERNAL_SERVER_ERROR = 500;

    STTResultCallBack mSTTResultCallBack = null;

    public interface STTResultCallBack {
        void onUpdate(final byte[] buffer);

        void onMidResult(String szResult);

        void onResult(String szResult);

        void onResultFinish(STTResult szResult, double recTime);

        void onError(int Type, String msg, int aCode);

        void onCancel();

        enum ERROR_STT {
            UNKNOWN,
            RECORED,
            RES_SERVER,
            REQUEST,
            BEFORE
        }
    }

    public SelvySTTManager(String aDomainSTT, AppConfig.CertType cert) {
        initSTTManager(aDomainSTT, cert);
    }

    public void initSTTManager(String aDomain, AppConfig.CertType aCertType) {

        switch (aCertType){
            case Nomal:
                Log.d(TAG, "Noah ==== [Selvy] call initSTTManager Nomal");
                mRetrofitInterface = SelvyCallClient.getClient(aDomain).create(STTRetrofitInterface.class);
                break;
            case Local:
                Log.d(TAG, "Noah ==== [Selvy] call initSTTManager Local");
                mRetrofitInterface = SelvyCallClient.getClienSSLSocketFactory(aDomain).create(STTRetrofitInterface.class);
                break;
            case Ignore:
                Log.d(TAG, "Noah ==== [Selvy] call initSTTManager Ignore");
                mRetrofitInterface = SelvyCallClient.getClienSSLIgnoreSocketFactory(aDomain).create(STTRetrofitInterface.class);
                break;
        }

        mTickedId = new TickedId();
        mSelvyRecordManager = new SelvyRecordManager();
        mBufferList = new ArrayList<>();
    }

    public void setResultCallback(STTResultCallBack aUIResultCallBack) {
        Log.i(TAG, "[Selvy] call_setResultCallback");
        mSTTResultCallBack = aUIResultCallBack;
    }

    public void prepare(STTPrepare body) {

        Log.d(TAG, "Noah ==== [Selvy] call_prepare");
        if (mWatiRequest) {
            setProcessWait(false, "prepare");
            //[Todo] Noti before rest api process
            processCancel(null, STTResultCallBack.ERROR_STT.BEFORE, "prepare mWatiRequest is true");
            return;
        }

        //[ToDo] Start Record
        if (startRecord() == false) {
            //[Todo]Error
            if (mSTTResultCallBack != null)
                mSTTResultCallBack.onError(STTResultCallBack.ERROR_STT.RECORED.ordinal(), "[Error]onResponse : startRecord false", RES_UNKWON);
            return;
        }

        //[Todo]Init ticketId
        mTickedId.ticketId = "";

        //[Todo]make Optional For ContentSave
        Log.d(TAG, "Noah ==== [Selvy] process_prepare");
        setProcessWait(true, "prepare");

        mMidType = body.getMidResult();
        Log.d(TAG, "Noah ==== [Selvy] process_prepare -" + mMidType);

        Call<STTResult> prearCall = mRetrofitInterface.prepare((Object) body.getJsonObject());
        Log.d(TAG, "Noah ==== [Selvy] call prepare method : " + prearCall.request().toString());

        mTotalSendBuffer = 0;
        prearCall.enqueue(this);
    }

    synchronized public void sendaudio(String aSender) {
        if (mWatiRequest) {
            //[Todo] Noti before rest api process
            Log.d(TAG, "[Selvy] call_sendaudio mWatiRequest Sender : " + aSender);
            return;
        }

        Log.d(TAG, "[Selvy] call_sendaudio Sender : " + aSender);
        if (mBufferList != null && mBufferList.size() > 0) {
            setProcessWait(true, "sendaudio");

            String json = getSendBuffer(mTickedId.ticketId);
            try {
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                Call<STTResult> sendaudioCall = mRetrofitInterface.sendaudio(jsonObject);

                sendaudioCall.enqueue(this);
            } catch (Exception e) {
                e.printStackTrace();
                processError(null, STTResultCallBack.ERROR_STT.REQUEST, "[Error] sendaudio Exception :  " + e.toString(), RES_UNKWON);
            }
        } else {
            //[Todo]Wait record buffer list
            Log.d(TAG, "[Selvy] sendaudio sendList empty");
        }
    }

    public void finish(boolean aSkipResponse) {
        Log.i(TAG, "[Selvy] call_finish");
        stopRecord();
        mBufferList = null;
        setProcessWait(true, "finish");
        Call<JsonObject> finishCalkll = mRetrofitInterface.finish(mTickedId);

        finishCalkll.enqueue(this);
        if (aSkipResponse)
            mTickedId.ticketId = null;
    }

    @Override
    public void onResponse(Call call, Response response) {
        try {
            Log.d(TAG, "Noah ==== [Selvy] call_onResponse from request : " + call.request().toString() + ", code : " + response.code());
            setProcessWait(false, "onResponse");
            if (response.code() == RES_SUCESS) {
                if (call.request().toString().contains(SelvyCommon.STT_PATH + STTRetrofitInterface.REQUEST_TYPE_PREPARE)) {
                    //[Todo]Case prepare
                    Log.d(TAG, "Noah ==== [Selvy] onResponse_prepare");
                    STTResult result = (STTResult) response.body();
                    mTickedId.ticketId = result.ticketId;
                } else if (call.request().toString().contains(SelvyCommon.STT_PATH + STTRetrofitInterface.REQUEST_TYPE_SENDAUDIO)) {
                    //[Todo]Case sendBuffer
                    STTResult pSTTResult = (STTResult) response.body();
                    String szResult = pSTTResult.analysisResult.midresult;
                    Log.d(TAG, "Noah ==== [Selvy] onResponse_sendaudio mMidType : " + mMidType);
                    Log.d(TAG, "Noah ==== [Selvy] onResponse_sendaudio result : " + pSTTResult.analysisResult.midresult);
                    Log.d(TAG, "Noah ==== [Selvy] onResponse_sendaudio progressCode : " + pSTTResult.analysisResult.progressCode);

                    String msg = "";
                    switch (pSTTResult.analysisResult.progressCode) {
                        case SelvyCommon.STT_PO_NONE:
                        case SelvyCommon.STT_PO_START:
                            //[ToDo] Send Rest Full API
                            sendaudio("onResponse");
                            if (mSTTResultCallBack != null && mMidType.equals(SelvyCommon.STT_MID_RESULT_ON)) {
                                msg = szResult;
                                mSTTResultCallBack.onMidResult(msg);
                                Log.d(TAG, "Noah ==== [Selvy] onResponse_sendaudio onMidResult : " + szResult);
                            }
                            break;
                        case SelvyCommon.STT_PO_END:
                            mTotalSendBuffer++;
                            //[Todo] Sucess Get Result
                            //stopRecord();
                            finish(false);
                            mSTTResultCallBack.onResultFinish(pSTTResult, (double) mTotalSendBuffer * 0.2);
                            break;
                        case SelvyCommon.STT_PO_TIMEOUT_START:
                            //[Todo] Timdout Process End
                            stopRecord();
                            if (mSTTResultCallBack != null && mMidType.equals(SelvyCommon.STT_MID_RESULT_ON))
                                mSTTResultCallBack.onMidResult(szResult);
                            processError(call, STTResultCallBack.ERROR_STT.RES_SERVER, "[Timeout] onResponse " + call.request().toString(), response.code());
                            break;
                        case SelvyCommon.STT_PO_TIMEOUT_END:
                            //[Todo] End Timdout Get Result
                            finish(false);
                            break;
                        case SelvyCommon.STT_PO_ERROR:
                            //[Todo] Error Process End
                            stopRecord();
                            processError(call, STTResultCallBack.ERROR_STT.RES_SERVER, "[Error] onResponse " + call.request().toString(), response.code());
                            break;
                    }
                } else if (call.request().toString().contains(SelvyCommon.STT_PATH + STTRetrofitInterface.REQUEST_TYPE_FINISH)) {
                    Log.d(TAG, "Noah ==== [Selvy] onResponse_finish");
                } else {
                    processError(call, STTResultCallBack.ERROR_STT.RES_SERVER, "[Error] onResponse " + call.request().toString(), response.code());
                }

            } else {
                //[Todo]Error
                if (RES_BUSY == response.code()) {
                    //[Todo]Server channel error
                    processError(call, STTResultCallBack.ERROR_STT.REQUEST, response.code() + "[Error]Server busy", response.code());
                } else if (RES_INTERNAL_SERVER_ERROR == response.code()) {
                    processError(call, STTResultCallBack.ERROR_STT.REQUEST, response.code() + "[Error]Server check", response.code());
                } else {
                    processError(call, STTResultCallBack.ERROR_STT.REQUEST, "[Error] onResponse " + call.request().toString() + " - ", response.code());
                }
                mBufferList = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            processError(call, STTResultCallBack.ERROR_STT.REQUEST, "[Exception] onResponse " + call.request().toString() + " - ", response.code());
        }
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        processError(call, STTResultCallBack.ERROR_STT.RES_SERVER, "onFailure", RES_ONFAIL);
    }

    private void processCancel(Call call, STTResultCallBack.ERROR_STT type, String msg) {
        Log.d(TAG, "Noah ==== [Cancel] " + msg);
        setProcessWait(false, "processCancel");
        if (call != null)
            call.cancel();
        stopRecord();
        if (mSTTResultCallBack != null) {
            mSTTResultCallBack.onCancel();
        }
        mTickedId.ticketId = "";
        finish(true);
    }

    int MAX_ERR_MSG = 30;

    private void processError(Call call, STTResultCallBack.ERROR_STT type, String msg, int aCode) {
        int maxLen = msg.length();
        if (maxLen > MAX_ERR_MSG)
            maxLen = MAX_ERR_MSG;

        Log.d(TAG, "Noah ==== [Selvy][Error] " + msg.substring(0, maxLen) + ", Type : " + type.toString() + ", aCode : " + aCode);
        setProcessWait(false, "processError");
        if (call != null)
            call.cancel();
        stopRecord();
        if (mSTTResultCallBack != null)
            mSTTResultCallBack.onError(type.ordinal(), msg, aCode);
    }

    private boolean startRecord() {
        mBufferList = new ArrayList<>();
        Log.d(TAG, "Noah ==== [Selvy] startRecord call ");
        mUICallBack = new SelvyRecordManager.UICallBack() {
            @Override
            public void onUpdate(final byte[] buffer) {
                Log.d(TAG, "[Selvy] onUpdate len : " + buffer.length + ", onUpdate ticketId : \"  + mTickedId.ticketId");
                //[Todo] send UI callback
                if (mSTTResultCallBack != null)
                    mSTTResultCallBack.onUpdate(buffer);

                if (mBufferList != null)
                    mBufferList.add(buffer);

                //[ToDo] Send Rest Full API
                if (mRetrofitInterface != null)
                    sendaudio("Record");
                else {
                    //[Todo]Error
                }
            }
        };

        mSelvyRecordManager.setUICallBack(mUICallBack);

        if (mSelvyRecordManager != null && mSelvyRecordManager.IsRecord()) {
            mSelvyRecordManager.stop();
            return false;
        } else {
            mSelvyRecordManager.start();
            return true;
        }
    }

    private void stopRecord() {
        Log.d(TAG, "Noah ==== stopRecord() ==== ");
        if (mSelvyRecordManager != null) {
            mSelvyRecordManager.setUICallBack(null);
            mBufferList = null;
            mSelvyRecordManager.stop();
        }
    }

    private String makeBufferToJson(String aTicketId, byte[] buffer) {
        Log.d(TAG, "makeBufferToJson aTicketId : " + aTicketId + ", length : " + buffer.length);
        try {
            String szBase64Buffer = Base64.encodeToString(buffer, Base64.NO_WRAP);
            SendBuffer sendBuffer = new SendBuffer();
            sendBuffer.ticketId = aTicketId;
            sendBuffer.audio.content = szBase64Buffer;
            Gson gson = new Gson();
            String json = gson.toJson(sendBuffer);
            return json;
        } catch (Exception e) {
            Log.e("[Error]", "[Error]" + e.toString());
        }
        return null;
    }

    private synchronized void setProcessWait(final boolean aWait, final String caller) {
        Log.d(TAG, "[Selvy] setProcessWait caller : " + caller + ", aWait : " + aWait);
        mWatiRequest = aWait;
    }

    synchronized private String getSendBuffer(String aTicketId) {
        String szRet = "";

        int nSize = mBufferList.size() - 1;
        Log.d(TAG, "[Selvy] getSendBuffer nSize : " + nSize);

        int nBufferSize = mBufferList.get(0).length;
        byte[] pBufferRet = null;

        if (nSize == 0) {
            pBufferRet = mBufferList.remove(0);
            mTotalSendBuffer++;
        } else {
            pBufferRet = new byte[nBufferSize * nSize];
            for (int i = 0; i < nSize; i++) {
                byte[] pBuf = mBufferList.remove(0);
                mTotalSendBuffer++;
                System.arraycopy(pBuf, 0, pBufferRet, i * nBufferSize, pBuf.length);
            }
        }
        szRet = makeBufferToJson(aTicketId, pBufferRet);
        return szRet;
    }
}

