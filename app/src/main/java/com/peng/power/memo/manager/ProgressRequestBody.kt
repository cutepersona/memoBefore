package com.peng.kdhc.folder.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class ProgressRequestBody(
    private val mFile: File,
    private val content_type: String,
    private val mListener: UploadCallbacks,
) :
    RequestBody() {
    private val mPath: String? = null

    interface UploadCallbacks {
        fun onProgressUpdate(percentage: Int)
        fun onError()
        fun onFinish()
    }

    override fun contentType(): MediaType? {
        return "$content_type/*".toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return mFile.length()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        Log.e("writeTo","${sink}")
        val fileLength = mFile.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val `in` = FileInputStream(mFile)
        var uploaded: Long = 0
        try {
            var read: Int
            // Handler를 통한 메인 Thread 로 UI 비동기 진행
            val handler = Handler(Looper.getMainLooper())
            // while 문에서 FileInputStream을 통해 buffer를 읽어들인다
            // buffer를 계속읽다가 다 읽게되면 -1 반환
            while (`in`.read(buffer).also { read = it } != -1) {
                // update progress on UI thread
                // UI progress바 Update
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                // while문에서 handle.post는 가장 하단부에 있어야 99%를 넘어갈 수 있음.
                handler.post(ProgressUpdater(uploaded, fileLength))
            }

        } finally {
            `in`.close()
        }
    }

    private inner class ProgressUpdater(private val mUploaded: Long, private val mTotal: Long) :
        Runnable {
        override fun run() {
//                Log.e("ProgressUpdater--run", "${(100.0 * mUploaded/mTotal)}")
                mListener.onProgressUpdate((100.0 * mUploaded / mTotal).toInt())
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 4096
    }
}