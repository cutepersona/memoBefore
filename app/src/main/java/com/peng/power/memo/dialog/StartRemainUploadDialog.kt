package com.peng.power.memo.dialog

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatDialog
import com.peng.power.memo.databinding.DialogStartRemainUploadBinding

class StartRemainUploadDialog(context: Context?) :
        AppCompatDialog(context, 0) {

    val binding = DialogStartRemainUploadBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 25f


    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvRemainUploadContent1.setTextSize(fontSize)
            binding.tvRemainUploadContent2.setTextSize(fontSize)
            binding.tvOk.setTextSize(23f)
        }

        binding.tvOk.setOnClickListener {
            dismiss()
        }
    }




}