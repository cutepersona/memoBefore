package com.peng.power.memo.data

import com.peng.power.memo.util.l
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object ConvertDate {
    fun getLocalDateTime(utcEpochSeconds: Long): String? {
        return try {
            val netDate = Date(utcEpochSeconds * 1000)

            if(Locale.getDefault() == Locale.KOREA){
                val simpleDate = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault())
                simpleDate.format(netDate)
            }else{
                val simpleDate = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
                simpleDate.format(netDate)
            }
        } catch (e: Exception) {
            e.toString()
        }
    }

    fun getUtcEpochSeconds():Long{
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return now.atZone(ZoneOffset.UTC).toEpochSecond()
    }

    fun getUtcEpochSecondsFromLocalDateTime(localSaveTime:String):Long{
        return try{
            l.d("local save time : $localSaveTime")
            var formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault())
            if(Locale.getDefault() != Locale.KOREA){
                formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm", Locale.getDefault())
            }

            val localDate = LocalDateTime.parse(localSaveTime, formatter)
            localDate.atZone(ZoneOffset.UTC).toEpochSecond() - (3600 * 9)
        }catch (e:Exception){
            l.e("exception : ${e.printStackTrace()}")
            0
        }
    }

}