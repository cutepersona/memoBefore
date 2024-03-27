package com.peng.power.memo.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String getTime(String format) {
        Date dt = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat(format);
        return timeFormat.format(dt);
    }


    /**
     * 날짜 연산 후 반환
     * "yyyyMMdd" 형식으로 입력 후 매개변수로 받은 format으로 반환
     * @param format 날짜 형식
     * @param calDay 연산 할 Day
     * @return 연산 된 날짜
     */
    public static String getTime(String format, int calDay, String currentTime) {
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = null;
        try {
            date = inputDateFormat.parse(currentTime);
            // 날짜 더하기
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, calDay);

            SimpleDateFormat timeFormat = new SimpleDateFormat(format);
            return timeFormat.format(new Date(calendar.getTimeInMillis()));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * second 를 hh:mm 형태로 반환
     * @param second second
     * @return hh:mm
     */
    public static String unixSecondToTime(long second) {
        Date date = new Date(second *1000L); // to mill second
        SimpleDateFormat dayTime = new SimpleDateFormat("HH:mm", Locale.ROOT);
        return dayTime.format(date);
    }

    /**
     * 경과시간을 hh:ss 형태로 반환
     * @param duration duration
     */
    public static String durationToTimeHHSS(long duration) {
        long ss = duration / 1000;
        long mm = ss / 60;
        ss = ss - (mm * 60);
        return String.format("%02d:%02d", mm, ss);
    }
}
