package com.peng.power.memo.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import com.peng.power.memo.R
import com.peng.power.memo.databinding.DialogEditFileBinding

class EditFileDialog(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)  {
    val binding = DialogEditFileBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 22f

    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvBack.setTextSize(fontSize)
            binding.tvFileTitleNumber.setTextSize(30f)
            binding.tvShowDocument.setTextSize(fontSize)
            binding.tvDelete.setTextSize(fontSize)
        }

    }

    @SuppressLint("SetTextI18n")
    fun showDialog(
        position: Int,
        callback: (Int) -> Unit
    ){
        binding.tvFileTitleNumber.text =
            context.resources.getString(R.string.file) + " " + position

        binding.tvBack.setOnClickListener {
            dismiss()
        }

        binding.tvShowDocument.setOnClickListener {
            callback(0)
        }

        binding.tvDelete.setOnClickListener {
            callback(1)
        }
        show()
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }
}