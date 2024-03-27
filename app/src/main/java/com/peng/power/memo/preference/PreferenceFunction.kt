package com.peng.power.memo.preference

import android.content.Context
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject

class PreferenceFunction {
    companion object{
        fun getStringPreference(context: Context, dirName: String, key: String):String{
            return context.getSharedPreferences(dirName, 0).getString(key, "")!!
        }

        fun setStringPreference(context: Context, dirName: String, key: String, content: String){
            context.getSharedPreferences(dirName, 0).edit().putString(key, content).apply()
        }

        fun clearSharedPreferences(context: Context, dirName: String){
            context.getSharedPreferences(dirName, 0).edit().clear().apply()
        }



        private const val dataClassDirName = "data_class_dirname"

        fun setDataClassPreference(context: Context, key: String, dataClass: DataClassPreference){
            val gson = Gson()
            val json = gson.toJson(dataClass)
            context.getSharedPreferences(dataClassDirName, 0).edit().putString(key, json).apply()
        }


        fun <T> getDataClassPreference(context: Context, key: String, typeClass: Class<T>): T {
            val jsonString = getStringPreference(context, dataClassDirName, key)
            val gson = Gson()
            return gson.fromJson(jsonString, typeClass)
        }



    }



}

