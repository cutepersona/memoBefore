package com.peng.power.memo.util;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.peng.power.memo.manager.DEFINE;
import com.peng.power.memo.util.gps.GpsTracker;

import lombok.Getter;
import lombok.Setter;

public class Global extends Application {

    public static Context mContext;

    // 신규 등록 or 조회
    public static boolean mIsNewRegistration = true;

    // 카메라 Fragment Camera or Video Status - 공동 사용 됨에 따른 변수
    @Getter @Setter
    public static int mCameraModeStatus;

    // Gps Tracker
    public static GpsTracker gpsTracker;

    // Device Model - 글라스 or Mobile
    public static boolean mIsGlassDevice = false;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = getApplicationContext();
        gpsTracker = new GpsTracker(this);
        FileManager.findInsertUploadFile(getApplicationContext());
        if (Build.MODEL.equals("T1100G") || Build.MODEL.equals("T1100S") || Build.MODEL.equals("T1200G") || Build.MODEL.equals("T21G")|| Build.MODEL.equals("MZ1000")) {
            mIsGlassDevice = true;
        } else
            mIsGlassDevice = false;
    }

    public static Context getContext(){
        return Global.mContext;
    }

    /**
     * 로컬 작업 저장소 Dir 경로
     */
    public static String getLocalDirPath()  {
        Log.d("Noah","==== " + mContext.getDatabasePath(DEFINE.LOCAL_FILE).getPath());
        return mContext.getDatabasePath(DEFINE.LOCAL_FILE).getPath();
    }

    /**
     * 비동기 저장하기 위한 Dir 경로
     */
    public static String getInsertDirPath()  {
        return mContext.getDatabasePath(DEFINE.INSERT_DIR_NAME).getPath();
    }

    /**
     * 임시 파일 내부 저장소 경로
     */
    public static String getTempFileDirPath()  {
        return mContext.getFileStreamPath("temp").getPath();
    }


    /**
     * gps 값 string으로 반환
     * 형식 : 위도,경도
     */
    public static String getGpsString() {
        gpsTracker.getmLocation();
        return gpsTracker.getLatitude() + "," + gpsTracker.getLongitude();
    }

    /**
     * 로그 파일 Dir Get
     */
    public static String getLogFileDir() {
        return mContext.getFileStreamPath("Log").getPath();
    }

}
