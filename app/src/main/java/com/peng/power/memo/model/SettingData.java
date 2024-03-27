package com.peng.power.memo.model;


import com.peng.power.memo.manager.SelvyCommon;

public class SettingData {
    public String auth;
    public String domain;
    public int port;
    public int model;
    public int endmargin;
    public String midresult;

//    public String getURL() {
//        return String.format("%s%d%s", domain, port, SelvyCommon.STT_PATH);
//    }
    public String getURL() {
        return String.format("%s%s", domain, SelvyCommon.STT_PATH);
    }
}
