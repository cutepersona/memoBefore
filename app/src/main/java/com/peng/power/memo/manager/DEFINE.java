package com.peng.power.memo.manager;

public class DEFINE {

    public static final int CALL_RESULT_SUCCESS = 1000;
    public static final int CALL_RESULT_FAILED = 9000;
    public static final int CALL_RESULT_NO_DATA = 1001;

    public static final int CALL_IMPORT_SUCCESS = 0;

    public static final String BASE_URL = "https://dev.watttalk.kr:8221/";            // dev
//    public static final String BASE_URL = "https://meetcloud.watttalk.kr:8332/";              // meet
    public static final String SELVY_URL = "https://stt.watttalk.kr:9999/";


    // 카메라 촬영 시 저장되는 Dir Name -> ... capture/capture 경로로 저장 됨
    public static final String CAMERA_CAPTURE_FILE_PATH = "capture";
    // 영상 촬영 시 저장 되는 Dir Name -> ... video/video 경로로 저장 됨
    public static final String CAMERA_VIDEO_FILE_PATH = "video";

    // 일반 사진 저장소 dir name
    public static final String WORK_PICTURE_DIR_NAME = "picture";

    // Local 저장 작업 중인 파일 경로 (신규 저장시 로컬 저장 작업)
    public static final String LOCAL_FILE = "local";

    // Insert 해야하는 파일 경로 (비동기 업로드 대기)
    public static final String INSERT_DIR_NAME = "insert";

    // 기록 중인 파일 이름
    public static final String SPEECH_ORIGIN_FILE_NAME = "origin";

    // 병합 된 파일 이름
    public static final String SPEECH_MERGE_FILE_NAME = "merge";

    // 기록 된 파티션 파일 이름 (문장 단위로 분할)
    public static final String SPEECH_PART_FILE_NAME = "part";

    // Pcm 확장자
    public static final String PCM_FORMAT                = ".pcm";
    // Wav 확장자
    public static final String WAV_FORMAT                = ".wav";
    // jpg 확장자 (사진 파일)
    public static final String JPG_FORMAT                = ".jpg";
    // mp4 확장자 (영상 파일)
    public static final String MP4_FORMAT                = ".mp4";
    // TXT 확장자
    public static final String TXT_FORMAT                = ".txt";

    // Insert File Dir Max length -> 9999 is 임의의 최대 값
    public static final int INSERT_PATH_MAX_LENGTH = 9999;

    // 파일 비동기 업로드 위한 저장 폴더 이름
    public static final String DATA_UPLOAD_PATH = "data_";

    // 파일 업로드 Interval
    public static final int FILE_UPLOAD_INTERVAL = 10000;


    /**
     * 데이터 영역 Key
     */
    public static final String RESPONSE_KEY_DATA = "data";  //  data 영역 key

    /**
     * 파일 업로드 결과 값
     */
    public static final int FILE_UPLOAD_VAULE_SUCCESS = 1; // 파일 업로드 실패 vaule
    public static final int FILE_UPLOAD_VAULE_NON_RETRY = 0; // 파일 재 요청 안함

    /**
     * 점검 결과 Value
     */
    public static final int CHECK_RESULT_NOT_COMPLETE =    0;
    public static final int CHECK_RESULT_COMPLETE =        1;

    // 공백 문자
    public static final String STR_BLANK = " ";

    // 썸네일 축소 사이즈
    public static final int THUMBNAIL_DOWN_SCALE_SIZE = 10;

    /**
     * 해당 프로그램 상수 값
     */
    public static final int TOP_MEMU_ALL_VISBILE = -1; // 상단 모든 메뉴 표시 상태 값
    public static final int FILE_UPLOAD_DUMMY = -1; // 해당 값인 경우 파일 업로드 Dummy 로 판단, 제거
    public static final int MENU_INDEX_NO_EXIST = -1; // 메뉴 값이 전달 되지 않은 상태 값

    // 음성 입력 판단 Decibel
    public static final int SPEECH_INPUT_MIN_DECIBEL_VAULE = 1000;
    // 음성 입력 연속 N번 입력되어야 입력중이라고 판단 횟수
    public static final int CONTINUOUS_VOICE_INPUT_THRESHOLD  = 10;

    /**
     * 사진 촬영 값 반환 intent extrea status
     */
    public static final String CAMERA_RETURN_STATUS_KEY = "status_key";
    public static final String CAMERA_RETURN_STATUS_CAPTURE = "status_capture";
    public static final String CAMERA_RETURN_STATUS_GALLERY = "status_gallery";

    /**
     * Intent Key
     */
    public static final String INTENT_KEY_MENU_INDEX = "menu_index"; // Intent 선택된 메뉴 값 전달 Key
    public static final String INTENT_KEY_IS_GALLERY = "is_gallery"; // Intent Gallery 클릭 여부 전달
    public static final String INTENT_KEY_HAS_TEMP_DATA = "has_temp_data"; // 임시 저장 데이터 존재 여부
    public static final String INTENT_KEY_INSERT_DATA = "insert_data"; // Intent Insert Data
    public static final String INTENT_KEY_REVIEW_LIST = "menu_list"; // Intent 메뉴 항목 list
    public static final String INTENT_KEY_STRING_PAYLOAD = "string_payload"; // Broad cast 공통 사용 되는 String intent key
    public static final String INTENT_KEY_INTEGER_PAYLOAD = "int_payload"; // Broad cast 공통 사용 되는 Integer intent key
    public static final String KEY_PUSH_IMAGE_NAME = "push_image_file_name"; // push 전송 사진 파일 이름

    /**
     * 카메라 Fragment Mode value
     */
    public static final int CAMERA_FRAGMENT_CAPTURE_MODE = 0; // Picture Capture Mode
    public static final int CAMERA_FRAGMENT_VIDEO_MODE = 1; // Video Recoder Mode

    /**
     * 체크리스트 관련
     */
    public static final int TOP_RESULT_CANCEL_INDEX = 0; // 선택 안함 인덱스

    /**
     * 음성 인식 커맨드 추가 Header
     */
    public static final String VOICE_COMMAND_HEADER = "#BNF+EM V2.0;" +
            "!grammar Commands;\n" +
            "!start <Commands>;\n";

    /**
     * 체크리스트 관련 상수
     */
    // 1 : 상단 표시 메뉴 템플릿
    public static final int CHECKLIST_TEMPLETE_TOP_MENU = 1;
    // 2 : 다이얼로그 메뉴 템플릿
    public static final int CHECKLIST_TEMPLETE_DIALOG_MENU = 2;

    // 1 : CHECKLIST_ORGIN_MODE
    public static final int CHECKLIST_ORGIN_MODE = 1;
    // 2 : CHECKLIST_VIDEO_MODE
    public static final int CHECKLIST_VIDEO_MODE = 2;


//    public static boolean getDeviceTypeGlass()
//    {
//        boolean isGlassType = false;
//        if (Build.MODEL.equals("T1100G") || Build.MODEL.equals("T1100S") || Build.MODEL.equals("T1200G") || Build.MODEL.equals("T21G"))
//            isGlassType = true;
//        return isGlassType;
//    }

}
