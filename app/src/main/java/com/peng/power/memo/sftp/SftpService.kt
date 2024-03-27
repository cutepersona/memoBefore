package com.peng.power.memo.sftp

import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.peng.power.memo.coroutine.BaseCoroutineScope
import com.peng.power.memo.coroutine.ClassCoroutineScope
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.data.SignalingSendData.EVENT_CREATE_VIDEO_DATA_FOLDER
import com.peng.power.memo.data.SignalingSendData.EVENT_SFTP_CREATE_ORIGINAL_FOLDER
import com.peng.power.memo.data.SignalingSendData.EVENT_SFTP_CREATE_THUMBNAIL_FOLDER
import com.peng.power.memo.data.SignalingSendData.SftpStatus
import com.peng.power.memo.util.l
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import splitties.toast.toast
import java.io.File

class SftpService : Service(), BaseCoroutineScope by ClassCoroutineScope() {
    private val mBinder: IBinder = LocalBinder()

    var sftpConnect:SftpConnect?=null

    //private var dataUpload:DataUpload?=null

    private var connectFailCount = 0

    private var jobSftpConnect: Job?= null

    inner class LocalBinder: Binder(){
        fun getService(): SftpService = this@SftpService
    }

    override fun onBind(intent: Intent?): IBinder? {
        l.d("onBind")

        if(sftpConnect == null){
            sftpConnect = SftpConnect(this)
        }

        connectSftpInternal()

        return mBinder
    }


    override fun onDestroy() {
        super.onDestroy()
        releaseCorounitne()
        sftpConnect?.disconnect()
    }


    private fun checkRemainUploadFile(){
        val insidePath = SignalingSendData.getRootPath() + SignalingSendData.EVENT_CREATE_TEMP_FOLDER

        l.d("check remain upload file -- check inside path : $insidePath")

        l.d("data Upload count reset to 0")

        val allFile = File(insidePath)
        if (allFile.listFiles() == null || allFile.listFiles()?.size == 0) {
            l.d("allFile.listFiles() == null || allFile.listFiles()?.size == 0")
            return
        } else {
            sftpConnect?.fileReUpload()
        }
    }

    private fun connectSftpInternal(){
        createInsideFolder(SignalingSendData.EVENT_CREATE_TEMP_FOLDER)
        jobSftpConnect = launch {
            if(sftpConnect == null){
                l.e("sftpConnect is null")
                sftpConnect = SftpConnect(baseContext)
            }

            sftpConnect?.connect {
                l.d("sftp is not null")
                when(it){
                    SftpStatus.EVENT_SFTP_CONNECT_OK -> {
                        l.d("EVENT_SFTP_CONNECT_OK")
                        createFolders()
                        checkRemainUploadFile()
                    }
                    SftpStatus.EVENT_SFTP_CONNECT_FAIL -> {
                        l.d("EVENT_SFTP_CONNECT_FAIL")
                        connectFailCount++
                        if (connectFailCount < 3) {
                            jobSftpConnect?.cancel()
                        } else {
                            toast("SFTP Connect Fail::ID 또는 PW를 확인하세요")
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    }
                    else->l.e("else when")
                }
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        l.d("onCreate")
    }



    private val enterpriseFolder = SignalingSendData.sftpData?.sftp_root_folder!!
    private val rootFolder = SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER
    private val originalFolder = SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + EVENT_SFTP_CREATE_ORIGINAL_FOLDER
    private val thumbnailFolder = SignalingSendData.sftpData?.sftp_root_folder + SignalingSendData.EVENT_SFTP_CREATE_ROOT_FOLDER + EVENT_SFTP_CREATE_THUMBNAIL_FOLDER

    private fun createFolders(){
        createInsideFolder(EVENT_SFTP_CREATE_ORIGINAL_FOLDER)
        sftpConnect?.createSftpFolder(enterpriseFolder) {
            sftpConnect?.createSftpFolder(rootFolder) {
                removeTempFile(EVENT_SFTP_CREATE_ORIGINAL_FOLDER)
                createInsideFolder(EVENT_SFTP_CREATE_THUMBNAIL_FOLDER)
                sftpConnect?.createSftpFolder(originalFolder) {
                    removeTempFile(EVENT_SFTP_CREATE_THUMBNAIL_FOLDER)
                    createInsideFolder(EVENT_CREATE_VIDEO_DATA_FOLDER)
                    sftpConnect?.createSftpFolder(thumbnailFolder){
                        removeTempFile(EVENT_CREATE_VIDEO_DATA_FOLDER)
                    }
                }
            }
        }
    }


    private fun createInsideFolder(folderName: String) {
        val fileDir =
            (SignalingSendData.getRootPath()
                    + folderName + "temp")
        val dir = File(fileDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        setMediaScanner(fileDir)
    }


    private fun setMediaScanner(filename: String?) {
        object : MediaScannerConnectionClient {
            private var msc: MediaScannerConnection? = null
            override fun onMediaScannerConnected() {
                msc?.let{
                    it.scanFile(filename, null)
                    l.d("Noah onMediaScannerConnected")
                }

            }

            override fun onScanCompleted(path: String, uri: Uri?) {
                msc?.disconnect()
                //msc!!.disconnect()
                l.d("Noah onScanCompleted")
            }

            init {
                msc = MediaScannerConnection(applicationContext, this)
                msc?.connect()
                l.d("Noah MediaScannerConnection ")
            }
        }
    }


    private fun removeTempFile(mRootPath: String) {
        val deleteTempFile = (SignalingSendData.getRootPath()
                + mRootPath + "temp")
        val thumbnailFile = File(deleteTempFile)
        if (thumbnailFile.exists()) {
            thumbnailFile.delete()
            setMediaScanner(deleteTempFile)
        }
    }

}