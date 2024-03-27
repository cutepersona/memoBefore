package com.peng.power.memo.preference

import android.content.Context
import com.peng.power.memo.util.l

object PreferenceValue {
    private const val userDataKey = "UserDataKey"
    private const val appDetailJsonKey = "AppDetailJsonKey"
    private const val appDetialJsonDir = "AppDetailJsonDir"

    private var dataUserPreference : DataUserPreference?=null
    private var dataUser: DataUser?=null


    fun getAppDetailJson(context:Context):String{
        return PreferenceFunction.getStringPreference(context, appDetialJsonDir, appDetailJsonKey)
    }

    fun setAppDetailJson(context:Context, appDetailJson:String) {
        PreferenceFunction.setStringPreference(context, appDetialJsonDir, appDetailJsonKey, appDetailJson)
    }


    fun getDataUser(context: Context): DataUser {
        if(dataUser == null){
            if(dataUserPreference == null){
                dataUserPreference = PreferenceFunction.getDataClassPreference(context, userDataKey, DataUserPreference::class.java)
            }

            if(dataUserPreference == null)
                dataUserPreference = DataUserPreference()

            dataUser = DataUser(dataUserPreference!!)

            dataUser?.setValueChangedListener {
                PreferenceFunction.setDataClassPreference(context, userDataKey, dataUser!!.getDataPreference())
            }
        }

        return dataUser!!
    }




    class DataUserPreference: DataClassPreference {
        var userName:String = "김영준"
        var userId:String = "kjy"
        var enSeq:Int = -1
        var hqSeq:Int = -1
        var brSeq:Int = -1
        var sort:Int = 2 //내림차순
        var page:Int = 1
        var zoom:Int = 0 //zoom level 1
        var menuStatus:Boolean = true
        var thermal:Int = 0    // 열화상   0 : 열화상 사용 X, 1 : 열화상 사용
    }



}