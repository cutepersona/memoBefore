package com.peng.power.memo.model;

import com.google.gson.annotations.SerializedName;
import com.peng.power.memo.manager.SelvyCommon;

public class SendBuffer {

    @SerializedName("lastContent")
    public String lastContent;

    @SerializedName("ticketId")
    public String ticketId;
    @SerializedName("audio")
    public Audio audio = null;

    public class Audio
    {
        @SerializedName("content")
        public String content;
    }

    public SendBuffer()
    {
        lastContent = SelvyCommon.OPT_FINISH_EPD;
        ticketId = "null";
        audio = new Audio();
    }
}
