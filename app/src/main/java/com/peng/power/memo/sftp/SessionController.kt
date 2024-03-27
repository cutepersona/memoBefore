package com.peng.power.memo.sftp

import android.content.Context
import android.util.Log
import com.jcraft.jsch.*
import com.peng.power.memo.coroutine.BaseCoroutineScope
import com.peng.power.memo.coroutine.ClassCoroutineScope
import java.io.File
import java.io.IOException
import java.util.*
import com.peng.power.memo.data.SignalingSendData.SftpStatus
import com.peng.power.memo.util.l

// Singleton
object SessionController : BaseCoroutineScope by ClassCoroutineScope() {
    /**
     * JSch Session
     */
    private var mSession: Session? = null

    /**
     * JSch UserInfo
     */
    private var mSessionUserInfo: SessionUserInfo? = null


    /**
     * Controls SFTP interface
     */
    private var mSftpController: SftpController? = null

    private val jsch = JSch()


    /**
     * Sets the user info for Session connection. User info includes
     * username, hostname and user password.
     *
     * @param sessionUserInfo Session User Info
     */
    fun setUserInfo(sessionUserInfo: SessionUserInfo?) {
        mSessionUserInfo = sessionUserInfo
    }

    fun getJsch():JSch{
        return jsch
    }

    fun getSession():Session?{
        try{
            mSession = jsch.getSession(
                    mSessionUserInfo?.getUser(), mSessionUserInfo?.getHost(),
                    mSessionUserInfo?.getPort()!!
            ) // port 22
            mSession?.let{
                it.userInfo = mSessionUserInfo
                val properties = Properties().apply {
                    setProperty("StrictHostKeyChecking", "no")
                }
                it.setConfig(properties)
                it.connect()
            }
        }catch (e:JSchException){
            l.e("jsch exception on getSession : $e")
        }finally {
            return mSession
        }
    }



    /**
     * Opens SSH connection to remote host.
     */
    fun connect(callback:(SftpStatus)->Unit) {
        Log.d("SessionController", "connect90::::::")

        mSession = null
        try {
            mSession = jsch.getSession(
                mSessionUserInfo?.getUser(), mSessionUserInfo?.getHost(),
                mSessionUserInfo?.getPort()!!
            ) // port 22
            mSession?.let{
                it.userInfo = mSessionUserInfo
                val properties = Properties().apply {
                    setProperty("StrictHostKeyChecking", "no")
                }
                it.setConfig(properties)
                it.connect()
            }
        } catch (e: JSchException) {
            l.e("JschException: ${e.message}, Fail to get session ${mSessionUserInfo?.getUser()} ${mSessionUserInfo?.getHost()}")
        } catch (ex: Exception) {
            l.e("Exception:" + ex.message)
        }
        l.d("Session connected? " + mSession?.isConnected)
        if (mSession?.isConnected!!)
            callback(SftpStatus.EVENT_SFTP_CONNECT_OK)
        else
            callback(SftpStatus.EVENT_SFTP_CONNECT_FAIL)

    }




    /**
     * Disconnects session and all channels.
     *
     * @throws IOException
     */

    fun disconnect() {
        releaseCorounitne()

        mSftpController?.disconnect()
        mSession?.disconnect()
    }




    fun createFolder(
        context: Context,
        folderName: String,
        callback: () -> Unit
    ) {
        if (mSftpController == null) {
            mSftpController = SftpController(context)
            l.d("createFolder:========= ")
        }

        try {
            mSftpController?.createFolder(mSession!!, folderName){
                callback()
            }
        } catch (e: JSchException) {
            e.printStackTrace()
        } catch (e: SftpException) {
            e.printStackTrace()
        }
    }

    fun downloadThumbnailFile(context: Context, fileName: String, callback: () -> Unit) {
        l.d("session controller download thumbnail file")
        if (mSftpController == null) {
            mSftpController = SftpController(context)
            Log.d("SftpController", "downloadThumbnailFile:========= ")
        }
        if(mSftpController == null){
            l.d("mSftpController is null")
        }
        if(mSession == null){
            l.d("mSession is null")
        }
        l.d("filename : $fileName")

        mSftpController?.downloadThumbnailFile(mSession!!, fileName) {
            callback()
        }
    }


    /**
     * Downloads file from remote server.
     *
     * @param spm
     * @return
     * @throws JSchException
     * @throws SftpException
     */
    fun downloadFile(
        context: Context,
        fileName: String,
        type: String,
        spm: SftpProgressMonitor,
        callback: (String) -> Unit
    ): Boolean {
        if (mSftpController == null) {
            mSftpController = SftpController(context)
        }
        mSftpController?.downloadTask(mSession!!, fileName, type, spm){ path ->
            callback(path)
        }
        return true
    }


    /**
     * Uploads files to remote server.
     *
     * @param file list of files to upload
     */
    fun uploadFile(memoSeq:Int, file: File, context: Context, serverUploadType: String, callback: () -> Unit) {
        if (mSftpController == null) {
            mSftpController = SftpController(context)
            Log.d("JSI", "uploadFiles:========= " + file.name)
        }
        Log.d("JSI", "uploadFiles:=========22222 " + file.name)
        mSftpController?.uploadTask(memoSeq, mSession!!, file, serverUploadType) {
            l.d("session controller upload task complete")
            callback()
        }
    }


    fun deleteFile(context: Context, fileName: String, type: String, callback: () -> Unit) {
        if (mSftpController == null) {
            mSftpController = SftpController(context)
        }
        mSftpController?.deleteTask(mSession!!, fileName, type) {
            callback()
        }
    }

    fun sftpKeepAlive() {
        try {
            mSession?.serverAliveInterval = 60
            mSession?.sendKeepAliveMsg()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}