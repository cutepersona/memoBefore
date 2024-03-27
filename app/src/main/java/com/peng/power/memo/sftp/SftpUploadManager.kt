package com.peng.power.memo.sftp

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.preference.PreferenceFunction
import com.peng.power.memo.util.l
import kotlinx.coroutines.Job
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SftpUploadManager {
    var uploadStatus:MutableLiveData<UploadStatus> = MutableLiveData()

    var progressPercent:MutableLiveData<Int> = MutableLiveData()

    var isFirstNoticeForRemainUpload = true

    // Uplaod file manager pool
    val uploadTaskPool = HashMap<Int, HashMap<File, Job>>()

    fun stopAllUpload(){
        if(uploadTaskPool.size == 0)
            return

        for(element in uploadTaskPool){
            for(elementChild in element.value){
                elementChild.value.cancel()
            }
        }

        uploadTaskPool.clear()
    }

    fun stopUpload(memoSeq:Int){
        if(uploadTaskPool.size == 0 || !uploadTaskPool.containsKey(memoSeq))
            return

        uploadTaskPool[memoSeq]?.let{
            if(it.size == 0)
                return
            for(element in it){
                l.d("cancel job : ${element.key.path}")

                element.value.cancel()
                if(element.key.exists()){
                    element.key.delete()
                }
            }

        }

        uploadTaskPool.remove(memoSeq)

        l.d("stop upload (memoSeq : $memoSeq) & uploadTaskPool size : ${uploadTaskPool.size}")
    }


    fun addUploadTaskInPool(memoSeq:Int, file:File, job:Job){
        if(!uploadTaskPool.containsKey(memoSeq)){
            SignalingSendData.updateUploadStatusQuery(memoSeq, 1){ upload_status->
                when(upload_status.toString().toInt()){
                    -1->l.e("update upload status fail --> result: -1")
                    0->l.e("update upload status error --> result: 0")
                    1->l.e("update upload staus success --> result: 1")
                }
            }
        }


        if(uploadTaskPool[memoSeq] == null){
            uploadTaskPool[memoSeq] = HashMap()
        }


        uploadTaskPool[memoSeq]?.put(file, job)
    }

    fun removeUploadTaskInPool(memoSeq: Int, file:File){
        if(uploadTaskPool[memoSeq] == null){
            return
        }

        uploadTaskPool[memoSeq]?.let{ map ->
            if(map.containsKey(file)){
                map[file]?.cancel()
                map.remove(file)
            }
        }

        if(uploadTaskPool[memoSeq]?.size == 0){
            SignalingSendData.updateUploadStatusQuery(memoSeq, 0){
                l.d("result of updata upload status query : ${it.toString()}")
                l.d("upload file complte in memoseq:$memoSeq, change upload status 1->0")
            }
            broadcastDownloadComplete(memoSeq)
            uploadTaskPool.remove(memoSeq)
        }
    }

    const val dirUploadingInfo = "dirUploadingInfo"
    const val keyUploadingInfo = "keyUploadingInfo"

    fun saveUploadingInfo(context: Context){
        if(uploadTaskPool.size == 0)
            return

        val hashForSave = HashMap<String, Int>()
        for(element in uploadTaskPool){
            for(fileAndJobElement in element.value){
                val fileName = fileAndJobElement.key.name
                val memoSeq = element.key
                hashForSave[fileName] = memoSeq
            }
        }
        val jsonEncodedHash = Json.encodeToString(hashForSave)

        PreferenceFunction.setStringPreference(context, dirUploadingInfo, keyUploadingInfo, jsonEncodedHash)
    }


    fun deleteDownloadFile(){
        l.d("into delete download file")
        val insideOriginalPath = SignalingSendData.getRootPath() + SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
        val allFile = File(insideOriginalPath)
        if (allFile.listFiles() == null || allFile.listFiles()?.size == 0) {
            l.d("allFile.listFiles() == null || allFile.listFiles()?.size == 0")
        } else {
            for (file in allFile.listFiles()) {
                l.d("delete file name : ${file.name}")
                file.delete()
            }
        }

        val insideThumbnailPath = SignalingSendData.getRootPath() + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER
        val allFileThumbnail = File(insideThumbnailPath)
        l.d("path allfilethumbnail : ${allFileThumbnail.path}")
        if (allFileThumbnail.listFiles() == null || allFileThumbnail.listFiles()?.size == 0) {
            l.d("allFileThumbnail.listFiles() == null || allFileThumbnail.listFiles()?.size == 0")
        } else {
            for (file in allFileThumbnail.listFiles()) {
                l.d("delete file name : ${file.name}")
                file.delete()
            }
        }
    }

    private var hashUploadingInfo:HashMap<String, Int>?=null

    fun loadUploadingInfo(context:Context){
        val jsonString = PreferenceFunction.getStringPreference(context, dirUploadingInfo, keyUploadingInfo)
        if(jsonString.isEmpty()){
            l.d("jsonString is empty")
            return
        }

        hashUploadingInfo = Json.decodeFromString(jsonString)
    }

    fun getMemoSeq(fileName:String):Int? {
        l.d("getMemoSeq target file name : $fileName")
        if (hashUploadingInfo == null){
            l.e("hashUploadingInfo is null")
            return null
        }

        hashUploadingInfo?.let{
            for(element in it){
                l.d("getMemoSeq : hashUploading info - key:${element.key}, value:${element.value}")
            }
        }

        return hashUploadingInfo!![fileName]
    }


    fun isUploading(memoSeq: Int):Boolean{
        if(uploadTaskPool.size == 0)
            return false
        else if(uploadTaskPool[memoSeq] == null)
            return false
        else if(uploadTaskPool[memoSeq]?.size == 0)
            return false

        return true
    }

    private val downloadCompleteCallbackPool = HashMap<Int, ()->Unit>()

    fun setOnDownloadCompleteListener(memoSeq:Int, callback:()->Unit){
        if(downloadCompleteCallbackPool.containsKey(memoSeq)){
            l.d("download complete callback pool is contain")
            return
        }else{
            l.d("download complete callback pool is not contain")
            downloadCompleteCallbackPool[memoSeq] = callback
        }
    }

    private fun broadcastDownloadComplete(memoSeq:Int){
        if(downloadCompleteCallbackPool.size == 0)
            return

        downloadCompleteCallbackPool[memoSeq]?.let{
            it()
        }
    }

}