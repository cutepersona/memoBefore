package com.peng.power.memo.dialog

import android.content.Context
import android.os.Process
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatDialog
import com.peng.power.memo.data.SignalingSendData
import com.peng.power.memo.databinding.DialogQuitBinding
import com.peng.power.memo.sftp.SftpUploadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class QuitDialog(context: Context?) :
        AppCompatDialog(context, 0) {
    val uiScope = CoroutineScope(Dispatchers.Main)

    val binding = DialogQuitBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 23f

    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvQuitContent1.setTextSize(25f)
            binding.tvQuitContent2.setTextSize(25f)
            binding.tvCancel.setTextSize(fontSize)
            binding.tvQuit.setTextSize(fontSize)
        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }
    }

    fun showDialog(onClickQuit:()->Unit){
        if(isShowing)
            dismiss()

        binding.tvQuit.setOnClickListener {
            onClickQuit()
            dismiss()
        }

        show()
    }



}