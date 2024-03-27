package com.peng.power.memo.model;

import com.google.gson.annotations.SerializedName;

public class FileUpDown {

    Request request;

    public Request getRequest(){
        if(request == null) request = new Request();
        return request;
    }

    public class Request{

    }

    @SerializedName("RESULT")
    private int resultCode;
    private String resultMessage;

    public int getResultCode() {
        return resultCode;
    }
    public void setResultCode(int resultCode) { this.resultCode = resultCode; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

//    @SerializedName("data")
//    private boolean data;
//
//    public boolean getResultData(){
//        return data;
//    }

    public class Items {

    }


}
