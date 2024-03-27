package com.peng.power.memo.sftp

import android.app.ProgressDialog
import android.content.Context
import com.jcraft.jsch.SftpProgressMonitor

class SftpProgressDialog
/**
 * Constructor
 *
 * @param context
 * @param theme
 */
    (context: Context?, theme: Int) :
    ProgressDialog(context, theme), SftpProgressMonitor {
    /**
     * Size of file to transfer
     */
    private var mSize: Long = 0

    /**
     * Current progress count
     */
    private var mCount: Long = 0
    //
    // SftpProgressMonitor methods
    //
    /**
     * Gets the data uploaded since the last count.
     */
    override fun count(arg0: Long): Boolean {
        mCount += arg0
        this.progress = (mCount.toFloat() / mSize.toFloat() * max.toFloat()).toInt()
        return true
    }

    /**
     * Data upload is ended. Dismiss progress dialog.
     */
    override fun end() {
        this.progress = this.max
        dismiss()
    }

    /**
     * Initializes the SftpProgressMonitor
     */
    override fun init(arg0: Int, arg1: String, arg2: String, arg3: Long) {
        mSize = arg3
    }
}
