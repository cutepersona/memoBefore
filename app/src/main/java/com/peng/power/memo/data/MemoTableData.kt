package com.peng.power.memo.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MemoTableData(var memo_seq:Int=0, var en_seq:Int?=-1, var hq_seq:Int?=-1, var br_seq:Int?=-1, var user_id:String="", var user_name:String="", var memo_contents:String ="", var save_time:Long=0 ) {

    fun setDataFromJson(jsonString:String){
        val temp:MemoTableData = Json.decodeFromString(jsonString)
        memo_seq = temp.memo_seq
        en_seq = temp.en_seq
        hq_seq = temp.hq_seq
        br_seq = temp.br_seq
        user_id = temp.user_id
        user_name = temp.user_name
        memo_contents = temp.memo_contents
        save_time = temp.save_time
    }


    fun toJson():String{
        return Json.encodeToString(this)
    }
}