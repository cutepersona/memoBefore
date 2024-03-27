package com.peng.power.memo.manager;

public interface AppConfig {
    //[Todo] input server info http://sample.com:8081/selvy/stt/
//    String STT_DOMAIN = "https://selvyrest.selvasai.com:";
//    String STT_DOMAIN = "https://stt.watttalk.kr";
    String STT_DOMAIN = "https://qtm.watttalk.kr";

    int STT_PORT = 9999;

    String AUTHCODE_STT_APP = "WattSolution";

    int MAX_PROGRESS = 1000;

    enum CertType{
        Nomal,
        Local,
        Ignore
    }
    CertType CERT_USE = CertType.Nomal;
}
