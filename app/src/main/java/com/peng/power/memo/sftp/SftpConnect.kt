package com.peng.power.memo.sftp

import android.content.Context
import android.util.Log
import com.jcraft.jsch.SftpProgressMonitor
import com.peng.power.memo.coroutine.BaseCoroutineScope
import com.peng.power.memo.coroutine.ClassCoroutineScope
import com.peng.power.memo.data.SignalingSendData
import java.io.File
import java.io.IOException
import java.util.*
import com.peng.power.memo.data.SignalingSendData.SftpStatus
import com.peng.power.memo.util.l
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SftpConnect(private val context:Context):BaseCoroutineScope by ClassCoroutineScope() {

    var filesName: List<String> = ArrayList()

    private var mSUI: SessionUserInfo? = null
    private val mFilenames = ArrayList<File>()
    private val EVENT_SET_IMAGE_UPLOAD = 1
    private val EVENT_FINISH_IMAGE_UPLOAD = 2

    var uploadFileTotalCount = 0


    var isUserFolderCheck = false
    //===========================================================

    //===========================================================
    fun connect(callback:(SftpStatus)->Unit) {
        val host = "106.10.44.115"
//        String host = "220.120.179.23";
//        String host = "kpjb.powertalk.kr";
        val user = "sftpuser01" //Administrator
        val password = "Powereng#2501" //
//        String user = "administrator"; //Administrator
//        String password = "Powereng#1";  //


        mSUI = SessionUserInfo(
            SignalingSendData.sftpData?.sftp_user,
            SignalingSendData.sftpData?.sftp_host,
            SignalingSendData.sftpData?.sftp_user_pw,
            SignalingSendData.sftpData?.sftp_port!!
        )
        SessionController.setUserInfo(mSUI)
        SessionController.connect(){
            callback(it)
        }
        Log.d("===========", "connect: check========")
    }


    fun disconnect() {
        try {
            SessionController.disconnect()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        releaseCorounitne()
    }



    fun createSftpFolder(folderName: String, callback: () -> Unit) {
        SessionController.createFolder(context, folderName){
            launch(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun downloadThumbnailFile(fileName: String, callback: () -> Unit) {
        SessionController.downloadThumbnailFile(context, fileName){
            launch(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun downloadFile(
        fileName: String,
        type: String,
        progressDialog: SftpProgressDialog,
        callback: (String) -> Unit
    ) {
        SessionController.downloadFile(context, fileName, type, progressDialog as SftpProgressMonitor){ path ->
            launch(Dispatchers.Main) {
                callback(path)
            }
        }
    }

    fun deleteFile(fileName: String, type: String, callback: () -> Unit) {
        Log.d("", "deleteFile:=============== $fileName")
        SessionController.deleteFile(context, fileName, type){
            launch(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun sftpKeepAlive() {
        SessionController.sftpKeepAlive()
    }

    fun fileUpload(
        serverUploadType: String,
        fileDir: String,
        fileName: String,
        memoSeq: Int,
        fileType: String,
        callback: (Any?) -> Unit
    ) {
        val file = File(fileDir + fileName)
        if (serverUploadType == SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER) {
            if (file.name.contains(".mp4")) {
                val thumbnail = "t_" + file.name.replace(".mp4", ".jpg")
                SignalingSendData.insertFileData(memoSeq, fileType, file.name, thumbnail){ result ->
                    SessionController.uploadFile(memoSeq, file, context, serverUploadType){
                        l.d("signaling file info add complete (mp4) --- filename : ${file.name}")
                        callback(result)
                    }
                }
            } else {
                SignalingSendData.insertFileData(memoSeq, fileType, file.name, "t_" + file.name){ result ->
                    SessionController.uploadFile(memoSeq, file, context, serverUploadType){
                        l.d("signaling file info add complete (jpg) --- filename : ${file.name}")
                        callback(result)
                    }
                }
            }
        }else{
            SessionController.uploadFile(memoSeq, file, context, serverUploadType){
                l.e("upload file complete == EVENT_SFTP_CREATE_THUMBNAIL_FOLDER ----- ${file.name}")
                callback(null)
            }
        }
    }

    fun fileReUpload() {
        // 이전에 업로드하던 메모번호와 파일이름 정보를 불러온다.
        SftpUploadManager.loadUploadingInfo(context)

        l.d("into file re upload")
        val insidePath = SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER
        val allFile = File(insidePath)
        for (file in allFile.listFiles()) {
            if(file.name == "temp"){
                l.d("found temp file --- delete temp file and continue~~ -- filepath ${file.path}")
                file.delete()
                continue
            }
            Log.d("SftpController", "getImageList////// " + file.name)
            if (file.name.contains("t_")) {
                val memoSeq = SftpUploadManager.getMemoSeq(file.name)
                if(memoSeq == null){
                    l.d("getMemoSeq == null")
                    file.delete()

                }else{
                    memoSeq.let{ memo_seq ->
                        SessionController.uploadFile(
                                memo_seq,
                                file,
                                context,
                                SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER
                        ){
                            l.e("file re upload complete - EVENT_SFTP_CREATE_THUMBNAIL_FOLDER")
                        }
                    }
                }


            } else {
                l.d("upload original folder")
                var memoSeq = SftpUploadManager.getMemoSeq(file.name)
                if(memoSeq == null){
                    l.d("getMemoSeq == null")
                    SignalingSendData.selectMemoSeqFromFileName(file.name){
                        l.d("select memo seq from file name : result memo seq -> ${it.toString()}")
                        memoSeq = it.toString().toInt()
                        if(memoSeq == -1){
                            l.e("not found memoseq where filename -- delete file path : ${file.path}")
                            if(file.delete()){
                                l.d("file delete success")
                            }else{
                                l.d("file delete fail")
                            }
                        }else{
                            if(file.name.contains("t_")){
                                memoSeq?.let{ memo_seq ->
                                    SessionController.uploadFile(
                                            memo_seq,
                                            file,
                                            context,
                                            SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER
                                    ){
                                        l.e("file re upload complete - EVENT_SFTP_CREATE_THUMBNAIL_FOLDER")
                                    }
                                }
                            }else{
                                memoSeq?.let{ memo_seq ->
                                    SessionController.uploadFile(
                                            memo_seq,
                                            file,
                                            context,
                                            SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
                                    ){
                                        l.e("file re upload complete - EVENT_SFTP_CREATE_ORIGINAL_FOLDER")
                                    }
                                }
                            }
                        }
                    }
                }else{
                    memoSeq?.let{ memo_seq ->
                        SessionController.uploadFile(
                                memo_seq,
                                file,
                                context,
                                SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
                        ){
                            l.e("file re upload complete - EVENT_SFTP_CREATE_ORIGINAL_FOLDER")
                        }
                    }

                }
            }
        }
    }



}