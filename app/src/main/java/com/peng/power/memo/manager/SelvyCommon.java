package com.peng.power.memo.manager;


//[Todo] For SelvyREST_Server 2.2
public interface SelvyCommon {
    int SHORT_TO_BYTE = 2;

    String STT_PATH = "/selvy/stt/";

    //Response Type
    String STT_PO_NONE = "P01";
    String STT_PO_START = "P02";
    String STT_PO_END = "P03";
    String STT_PO_TIMEOUT_START = "P04";
    String STT_PO_TIMEOUT_END = "P05";
    String STT_PO_ERROR = "P06";

    //Record Type Option
    int SAMPLE_RATE_8K = 8000;
    int SAMPLE_RATE_16K = 16000;
    //Record Config unit 100ms
    int SEND_BUFFER_UNIT = 2;
    int MAX_RECORD_COUNT = 30;


    //Selvas Demo Sever
    int OPT_BASE_MODEL = 0;
    int OPT_SELVY_16K_KOR = 1;
    int OPT_SELVY_16K_ENG = 3;
    int OPT_MODEL_MEGA = 3;
    int OPT_MODEL = 1;



    //END MARGIN unit ms
    int OPT_ENDMARGIN_DEFALT = 0;   //700ms
    int OPT_ENDMARGIN_400 = 400;
    int OPT_ENDMARGIN_300 = 300;
    int OPT_ENDMARGIN = OPT_ENDMARGIN_300;

    int OPT_KWDID= -1;

    int STT_CODEC_8K = 0;
    int STT_CODEC_16K = 1;
    int OPT_CODEC = STT_CODEC_16K;

    int STT_EPD_OFF = 0;
    int STT_EPD_ON = 1;
    int OPT_EPD = STT_EPD_ON;

    int STT_WORD_INFO_OFF = 0;
    int STT_WORD_INFO_ON = 1;
    int OPT_WORD_INFO = STT_WORD_INFO_OFF;

    String STT_MID_RESULT_ON = "M01";
    String STT_MID_RESULT_OFF = "M02";
    String OPT_MID_RESULT = STT_MID_RESULT_ON;

    //Send Type
    String STT_FINISH_END = "F01";
    String STT_FINISH_EPD = "F02";
    String OPT_FINISH_EPD = STT_FINISH_EPD;

    //Support Save
    boolean STT_FILE_OFF = false;
    boolean STT_FILE_ON = true;
    boolean OPT_FILE = STT_FILE_OFF;

    //Option Save Type
    String STT_FILE_MP3 = "mp3";
    String STT_FILE_ORIGINAL = "original";
    String OPT_FILE_TYPE = STT_FILE_MP3;

    //Option contentSave saveMode
    String STT_SAVE_APPEND = "append";
    String STT_SAVE_TRUNCATE = "truncate";
    String OPT_SAVE_TYPE = STT_SAVE_TRUNCATE;

    //Option engine Score
    int STT_SCORE_OFF = 0;
    int STT_SCORE_ON = 1;
    int OPT_SCORE_OFF = STT_SCORE_OFF;
}

