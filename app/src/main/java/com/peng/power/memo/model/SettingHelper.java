package com.peng.power.memo.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingHelper {

    public interface NAME{
        String AUTH         = "auth";
        String DOMAIN       = "domain";
        String PORT         = "port";
        String MODEL        = "com/peng/power/memo/model";
        String EPD_MARGIN   = "epd_margin";
        String MID_RESULT   = "mid_result";
    }

    SharedPreferences mPref = null;
    private static class Lazy {
        private static final SettingHelper INSTANCE = new SettingHelper();
        private Lazy() {}
    }

    public static SettingHelper getInstance() {
        return Lazy.INSTANCE;
    }
    protected SettingHelper() {}

    public SettingHelper init(Context context)
    {
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
        return Lazy.INSTANCE;
    }

    public void setValue(String name, boolean value)
    {
        if(mPref == null)
            return;
        mPref.edit().putBoolean( name, value).commit();
    }

    public void setValue(String name, int value)
    {
        if(mPref == null)
            return;
        mPref.edit().putInt( name, value).commit();
    }

    public void setValue(String name, float value)
    {
        if(mPref == null)
            return;
        mPref.edit().putFloat( name , value).commit();
    }

    public void setValue(String name, String value)
    {
        if(mPref == null)
            return;
        mPref.edit().putString( name, value).commit();
    }

    public boolean getValue(String name, boolean defaultvalue)
    {
        if(mPref == null)
            return defaultvalue;
        return mPref.getBoolean(name, defaultvalue);
    }

    public int getValue(String name, int defaultvalue)
    {
        if(mPref == null)
            return defaultvalue;
        return mPref.getInt(name, defaultvalue);
    }

    public float getValue(String name, float defaultvalue)
    {
        if(mPref == null)
            return defaultvalue;
        return mPref.getFloat(name, defaultvalue);
    }

    public String getValue(String name, String defaultvalue)
    {
        if(mPref == null)
            return defaultvalue;
        return mPref.getString(name, defaultvalue);
    }

//    public String getFullInfo()
//    {
//        String ret = "";
//
//        if(mPref == null)
//            return ret;
//
//        ret = ret.concat( getValue(NAME.DOMAIN, DOMAIN.DEFAULT) + ":" );
//        ret = ret.concat( getValue(NAME.PORT, PORT.DEFAULT) + " - com.peng.power.memo.model : " );
//        ret = ret.concat( getValue(NAME.MODEL, MODEL.DEFAULT) +"" );
//        return ret;
//    }
}
