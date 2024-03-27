package com.peng.power.memo.util

class FD_FILE {
    // 카메라 촬영 시 저장되는 Dir Name -> ... capture/capture 경로로 저장 됨
    val CAMERA_CAPTURE_FILE_PATH = "capture"

    // 영상 촬영 시 저장 되는 Dir Name -> ... video/video 경로로 저장 됨
    val CAMERA_VIDEO_FILE_PATH = "video"

    // 일반 사진 저장소 dir name
    val WORK_PICTURE_DIR_NAME = "picture"

    // Local 저장 작업 중인 파일 경로 (신규 저장시 로컬 저장 작업)
    val LOCAL_FILE = "local"

    // Insert 해야하는 파일 경로 (비동기 업로드 대기)
    val INSERT_DIR_NAME = "insert"

    // 기록 중인 파일 이름
    val SPEECH_ORIGIN_FILE_NAME = "origin"

    // 병합 된 파일 이름
    val SPEECH_MERGE_FILE_NAME = "merge"

    // 기록 된 파티션 파일 이름 (문장 단위로 분할)
    val SPEECH_PART_FILE_NAME = "part"

    // Pcm 확장자
    val PCM_FORMAT = ".pcm"

    // Wav 확장자
    val WAV_FORMAT = ".wav"

    // jpg 확장자 (사진 파일)
    val JPG_FORMAT = ".jpg"

    // mp4 확장자 (영상 파일)
    val MP4_FORMAT = ".mp4"

    // TXT 확장자
    val TXT_FORMAT = ".txt"

    // Insert File Dir Max length -> 9999 is 임의의 최대 값
    val INSERT_PATH_MAX_LENGTH = 9999

    // 파일 비동기 업로드 위한 저장 폴더 이름
    val DATA_UPLOAD_PATH = "data_"

    // 파일 업로드 Interval
    val FILE_UPLOAD_INTERVAL = 10000

    /**
     * 파일 업로드 결과 값
     */
    val FILE_UPLOAD_VAULE_SUCCESS = 1 // 파일 업로드 실패 vaule

    val FILE_UPLOAD_VAULE_NON_RETRY = 0 // 파일 재 요청 안함

}