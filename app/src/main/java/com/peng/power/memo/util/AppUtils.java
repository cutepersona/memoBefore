package com.peng.power.memo.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.peng.power.memo.R;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.peng.power.memo.manager.DEFINE;

public class AppUtils {
//    /**
//     * 항목이 Text or Number인지 검사
//     *
//     * @param item - 점검 항목
//     * @return istextFiled
//     */
//    public static boolean isTextFiled(TopMenuItem item) {
//        if (item.item_type == NETWORK.TYPE_TEXT || item.item_type == NETWORK.TYPE_NUMBER || item.item_type == NETWORK.TYPE_MEMO)
//            return true;
//        else
//            return false;
//    }
//
//    /**
//     * 항목이 single or muliti 인지 검사
//     *
//     * @param item - 점검 항목
//     * @return istextFiled
//     */
//    public static boolean isSeletedFiled(TopMenuItem item) {
//        if (item.item_type == NETWORK.TYPE_SINGLE_ITEM || item.item_type == NETWORK.TYPE_MULTI_ITEM)
//            return true;
//        else
//            return false;
//    }
//
//    /**
//     * 항목이 Photo or Video ㅇ니지 검사
//     *
//     * @param item - 점검 항목
//     * @return isPhotoAndVideoFiled
//     */
//    public static boolean isPhotoAndVideoFiled(TopMenuItem item) {
//        if (item.item_type == NETWORK.TYPE_PICTURE || item.item_type == NETWORK.TYPE_VIDEO)
//            return true;
//        else
//            return false;
//    }
//
//    /**
//     * 항목이 체크리스트 or 체크리스트 VIdeo인 경우
//     *
//     * @param item - 점검 항목
//     * @return isCheckListFiled
//     */
//    public static boolean isCheckListFiled(TopMenuItem item) {
//        if (item.item_type == NETWORK.TYPE_CHECK_LIST || item.item_type == NETWORK.TYPE_CHECKLIST_VIDEO)
//            return true;
//        else
//            return false;
//    }

    /**
     * 통신 결과 비교
     * No Data or 통신 실패 시 토스트 메세지 호출
     *
     * @param result_code - result code
     * @param context     - context
     * @return true - 정상 데이터 존재, false - 데이터 없거나 통신 실패
     */
    public static boolean checkResultCode(int result_code, Context context, boolean isShowToast) {
        switch (result_code) {
            case DEFINE.CALL_RESULT_SUCCESS:
//                DEBUG.d("# RESULT_CODE_SUCCES");
                return true;
            case DEFINE.CALL_RESULT_NO_DATA:
//                DEBUG.d("# RESULT_CODE_NO_DATA");
                Toast.makeText(context, context.getString(R.string.toast_no_data), Toast.LENGTH_SHORT).show();
                return false;
            case DEFINE.CALL_RESULT_FAILED:
//                DEBUG.d("# RESULT_CODE_FAIL");
//                Toast.makeText(context, context.getString(R.string.toast_result_error), Toast.LENGTH_SHORT).show();
                return false;
        }
        return false;
    }

    public static boolean checkResultCode(int result_code, Context context) {
        return checkResultCode(result_code, context, true);
    }

    /**
     * Broad Cast 호출 Helper
     */
    public static void sendLoaclBroadcast(Context context, String action) {
        Intent in = new Intent();
        in.setAction(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(in);
    }

//    public static void sendLoaclBroadcast(Context context, String action, Map<String, String> map, int integerParam) {
//        Intent in = new Intent();
//        // Map의 키(Key)를 가져와서 순회
//        for (String key : map.keySet()) {
//            String value = map.get(key);
//            in.putExtra(key, value);
//            DEBUG.d("Key: " + key + ", Value: " + value);
//        }
//        in.putExtra(CONSTANT.INTENT_KEY_INTEGER_PAYLOAD, integerParam);
//        in.setAction(action);
//
//        LocalBroadcastManager.getInstance(context).sendBroadcast(in);
//    }
//
//    public static void sendLoaclBroadcast(Context context, String action, String str) {
//        Intent in = new Intent();
//        in.putExtra(CONSTANT.INTENT_KEY_STRING_PAYLOAD, str);
//        in.setAction(action);
//
////        DEBUG.d("# [Util] sendLoaclBroadcast = action : " + action
////                + ", str : " + str);
//        LocalBroadcastManager.getInstance(context).sendBroadcast(in);
//    }

//    public static void sendLoaclBroadcast(Context context, String action, int cmd) {
//        Intent in = new Intent();
//        in.putExtra("cmd", cmd);
//        in.setAction(action);
//
////        DEBUG.d("# [Util] sendLoaclBroadcast = action : " + action
////                + ", cmd : " + cmd);
//        LocalBroadcastManager.getInstance(context).sendBroadcast(in);
//    }

    /**
     * 해당 서비스 실행중인지 확인하는 함수
     */
    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 현재 Activity Stack에 생성되어있는 activity count 반환
     * @return - activity stack count
     */
    public static int getStackedActivityCount(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        int count = 0;
        if (appTasks != null) {
            for (ActivityManager.AppTask appTask : appTasks) {
                count += appTask.getTaskInfo().numActivities;
            }
        }
        return count;
    }

    // 현재 시스템의 언어를 가져옵니다.
    public static String getCurrentLanguage() {
        String language;
        Locale currentLocale = Locale.getDefault();
        String currentLanguage = currentLocale.getLanguage();

        // 시스템 언어가 한국어인 경우
        if (currentLanguage.equals("ko")) {
            language = "ko-KR";
        }
        // 시스템 언어가 영어인 경우
        else if (currentLanguage.equals("en")) {
            language = "en-US"; // 혹은 다른 Naver STT 영어 버전으로 설정
        }
        // 기타 경우에는 기본값으로 설정
        else {
            language = "ko-KR"; // 또는 다른 기본값으로 설정
        }
        return language;
    }

    /**
     * 전달 받은 bitmap 전달 받은 경로로 저장
     */
    public static void saveBitmapToFile(Bitmap bitmap, String path) {
        File file = new File(path);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
