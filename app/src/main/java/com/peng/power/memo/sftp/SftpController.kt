package com.peng.power.memo.sftp

import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.util.Log
import com.jcraft.jsch.*
import com.peng.power.memo.coroutine.BaseCoroutineScope
import com.peng.power.memo.coroutine.ClassCoroutineScope
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.util.l
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException


class SftpController(private val context: Context) : BaseCoroutineScope by ClassCoroutineScope() {

    //lateinit var callBack: (SignalingSendData.SftpStatus)->Unit

    //var dataUpload:DataUpload = PreferenceValue.getDataUpload(context)

    /**
     * Remote directory path. The path to the current remote directory.
     */
    private val mCurrentPath = "/home/"



    /**
     * Disconnects SFTP.
     */
    fun disconnect() {
        releaseCorounitne()
    }





    fun uploadTask(
            memoSeq:Int,
            session: Session,
            localFile: File,
            serverUploadType: String,
            callBackFun: () -> Unit
    ){
        l.d("upload task start -- localFile:${localFile}, name:${localFile.name}")

        val uploadJob = launch(Dispatchers.IO) {
            try{
                uploadFile(session, localFile, serverUploadType)
            }catch (e: IOException){
                l.e("IOException :: ${e.message}")
                //fileCountReset()
            }catch (e: SftpException){
                l.e("SftpException :: ${e.message}")
                //fileCountReset()
            }finally {
                l.e("upload task finally")
                callBackFun()

                withContext(Dispatchers.Main) {
                    // remove job in upload task pool
                    SftpUploadManager.removeUploadTaskInPool(memoSeq, localFile)

                    l.d("uploadTaskPool size : ${SftpUploadManager.uploadTaskPool.size}")
                    if(SftpUploadManager.uploadTaskPool.size == 0){
                        SftpUploadManager.uploadStatus.value = UploadStatus.None
                        totalUploadBytes = 0
                        currentUploadedBytes =0
                    }
                }
            }

        }

        SftpUploadManager.addUploadTaskInPool(memoSeq, localFile, uploadJob)
    }



    private var totalUploadBytes:Long = 0
    private var currentUploadedBytes:Long = 0


    inner class SystemOutProgressMonitor : SftpProgressMonitor {
        private var totalBytes:Long = 0
        private var currentBytes:Long = 0
        private var prePercent:Int = 0
        override fun init(op: Int, src: String, dest: String, max: Long) {
            println("STARTING: $op $src -> $dest total: $max")
            l.d("src:$src, dest:$dest")
            totalBytes = max
            totalUploadBytes += max
        }

        override fun count(bytes: Long): Boolean {
            currentUploadedBytes += bytes
            currentBytes += bytes

            val curPercent = ((currentBytes.toFloat() / totalBytes.toFloat()) * 100.0f).toInt()

            if(prePercent!= curPercent){
                prePercent = curPercent
                launch(Dispatchers.Main) {
                    val percent = ((currentUploadedBytes.toFloat() / totalUploadBytes.toFloat()) * 100.0f).toInt()
                    SftpUploadManager.progressPercent.value = percent
                    SftpUploadManager.uploadStatus.value = UploadStatus.Uploading
                }

                Log.d("count byte : ", "cur:$currentBytes, max:$totalBytes, percent : $prePercent")
            }

            return true
        }

        override fun end() {
            launch(Dispatchers.Main) {
                if(SftpUploadManager.uploadTaskPool.size == 0){
                    SftpUploadManager.uploadStatus.value = UploadStatus.None
                }
            }

            l.d("finish upload")
        }

        init {
        }
    }





    private fun uploadFile(session: Session, localFile: File, uploadType: String) {
        l.d("try upload file")
        val systemOutProgressMonitor = SystemOutProgressMonitor()

        var channelSftp:ChannelSftp?=null
        try{
            val channel = session.openChannel("sftp")
            channel.inputStream = null
            channel.connect()
            channelSftp = channel as ChannelSftp
        }catch (e:JSchException){
            l.e("jschexception on uploadfile : $e")
            val mSession = SessionController.getSession()
            if(mSession == null){
                l.e("mSession is null --- fail upload file")
                return
            }
            l.d("re try upload file -- new session")
            uploadFile(mSession, localFile, uploadType)
//            val channel = mSession.openChannel("sftp")
//            channel.inputStream = null
//            channel.connect()
//            channelSftp = channel as ChannelSftp
        }finally {
            channelSftp?.let{
                l.d("finally channesftp put")
                val path =
                        mCurrentPath + SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + uploadType

                it.cd(path)
                if(localFile.name.contains(".mp4")){
                    it.put(localFile.path, localFile.name, systemOutProgressMonitor, ChannelSftp.RESUME)
                }else{
                    it.put(localFile.path, localFile.name, systemOutProgressMonitor, ChannelSftp.APPEND)
                }


               it.disconnect()
                deleteFile(localFile.name)
            }
        }
    }

    private fun deleteFile(fileName: String) {
        val path =
            SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER + fileName
        val file = File(path)
        file.delete()
        Log.d("SftpController", "removefile-=-=-$fileName")
        setMediaScanner(path)
    }


    private fun setMediaScanner(filename: String) {
        object : MediaScannerConnectionClient {
            private var msc: MediaScannerConnection? = null
            init {
                msc = MediaScannerConnection(context, this)
                msc?.connect()
            }

            override fun onMediaScannerConnected() {
                msc?.scanFile(filename, null)
            }

            override fun onScanCompleted(path: String, uri: Uri?) {
                msc?.disconnect()
            }
        }
    }



    fun deleteTask(
            session: Session,
            fileName: String,
            type: String,
            callBackFun: () -> Unit
    ){
        launch {
            try{
                if (!session.isConnected) {
                    session.connect()
                }
                val channel = session.openChannel("sftp")
                val sftpChannel = channel as ChannelSftp
                sftpChannel.connect()
                val filePath =
                    mCurrentPath + SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + type + fileName
                Log.d("", "deleteFile:=======-=-=-=-=-========== $filePath")
                sftpChannel.rm(filePath)
                sftpChannel.disconnect()
                //
                if (type == SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER) {
                    l.d("EVENT_DELETE_ORIGINAL_FILE_OK")
                    //callBackFun()
                } else if (type == SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER) {
                    l.d("EVENT_SFTP_CREATE_THUMBNAIL_FOLDER")
                    //callBackFun()
                }
            }catch (e: JSchException){
                e.printStackTrace()
            }catch (e: SftpException){
                e.printStackTrace()
            }catch (e: Exception){
                e.printStackTrace()
            }finally {
                l.d("finally delete task")
                callBackFun()
            }
        }
    }





    fun createFolder(
            session: Session,
            folderName: String,
            callBackFun: () -> Unit
    ){
        launch {
            try{
                var attrs: SftpATTRS? = null
                if(!session.isConnected)
                    session.connect()

                val channel:Channel = session.openChannel("sftp")
                channel.inputStream = null
                channel.connect()

                val channelSftp:ChannelSftp = channel as ChannelSftp
                try{
                    attrs = channelSftp.stat(mCurrentPath + folderName)
                }catch (e: Exception){
                   l.e("not found")
                }

                if(attrs != null){
                    l.d("Directory exists IsDir=${attrs.isDir}")
                }else{
                    l.d("Creating dir $folderName")
                    channelSftp.mkdir(mCurrentPath + folderName)
                }
                channelSftp.disconnect()
                callBackFun()
            }catch (e: JSchException){
                l.e("JSchException ${e.message}")
            }catch (e: SftpException){
                l.e("SftpException ${e.message}")
            }catch (e: Exception){
                l.e("Exception ${e.message}")
            }
        }
    }


    fun downloadThumbnailFile(session: Session, fileName: String, callBackFun: () -> Unit){
        l.d("download thumbnail file start")
        launch {
            try{
                if (!session.isConnected) {
                    session.connect()
                }
                val channel = session.openChannel("sftp")
                val sftpChannel = channel as ChannelSftp
                sftpChannel.connect()
                val thumbNailPath =
                    (mCurrentPath + SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER
                            + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER + fileName)

                val outFile =
                    (SignalingSendData.getRootPath()
                            + SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER + fileName)

                sftpChannel[thumbNailPath, outFile]
                sftpChannel.disconnect()

                l.d("EVENT_SFTP_THUMBNAIL_FILE_DOWN_LOAD_END")
                callBackFun()

            }catch (e: JSchException){
                l.e("JSchException : ${e.message}")
            }catch (e: SftpException){
                l.e("SftpException : ${e.message}")
            }catch (e: Exception){
                l.e("Exception : ${e.message}")
            }
        }
    }


    fun downloadTask(
            session: Session,
            srcPath: String,
            type: String,
            spm: SftpProgressMonitor,
            callBackFun: (String) -> Unit
    ){
        var isFileNotFound = false
        var isSessionDown = false
        launch {
            var channelSftp:ChannelSftp?=null
            try{
                l.d("download task start")
                if(!session.isConnected)
                    session.connect()
                val channel = session.openChannel("sftp")
                channelSftp = channel as ChannelSftp
                channelSftp.connect()
            }catch (e: JSchException){
                l.e("jsch exception on downloadFile : $e")
                val mSession = SessionController.getSession()
                if(mSession == null){
                    l.e("mSession is null --- fail upload file")
                    return@launch
                }
                l.d("re try upload file -- new session")
                downloadTask(mSession, srcPath, type, spm, callBackFun)
                isSessionDown = true
            }catch (e: SftpException){
                l.e("SftpException : ${e.message}")
                isFileNotFound = true
            }catch (e: Exception){
                l.e("Exception : ${e.message}")
            }finally {
                if(!isSessionDown){
                    channelSftp?.let{ channel ->
                        val filePath =
                                mCurrentPath + SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + type + srcPath

                        val outFile =
                            SignalingSendData.getRootPath() + type + srcPath
                        try{
                            channel[filePath, outFile, spm, ChannelSftp.OVERWRITE]
                            channel.disconnect()
                        }catch (e:SftpException){
                            l.e("SftpException : ${e.message}")
                            e.message?.let{
                                if(it.contains("such file")){
                                    isFileNotFound = true
                                }
                            }
                        }
                        if(isFileNotFound){
                            callBackFun("null")
                        }else{
                            callBackFun(srcPath)
                        }
                    }
                    spm.end()
                    l.d("EVENT_SFTP_FILE_DOWN_LOAD_END")
                }


            }
        }
    }




}