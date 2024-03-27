package com.peng.power.memo.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import com.peng.power.memo.R
import com.peng.power.memo.databinding.DialogImageFileSaveBinding

class ImageFileSaveDialog(context: Context, private val bitmap: Bitmap, private val callback:(Int)->Unit) : Dialog(context, R.style.DialogCustomTheme) {

    private val binding = DialogImageFileSaveBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 21f

    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 ")
            binding.infoCancelBtn.setTextSize(fontSize)
            binding.infoSaveBtn.setTextSize(fontSize)
        }

        show()
        binding.ivSaveImg.setImageBitmap(bitmap)
        binding.infoSaveBtn.setOnClickListener {
            callback(0)
            Thread.sleep(500)
            dismiss()
        }
        binding.infoCancelBtn.setOnClickListener {
            callback(1)
            dismiss()
        }

    }

}