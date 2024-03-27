package com.peng.power.memo.dialog

import android.app.Dialog
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import com.peng.power.memo.R
import com.peng.power.memo.databinding.DialogDeleteBinding

class DeleteDialog(context:Context) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
    private val binding = DialogDeleteBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 25f

    private var callbackFileDelete:(()->Unit)?=null
    private var callbackMemoDelete:(()->Unit)?=null

    private var fileSeq:Int = 0
    private var originalName:String = ""
    private var thumbnailName:String = ""
    private var memoSeq:Int = 0


    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvDeleteContents.setTextSize(fontSize)
            binding.deleteCancelBtn.setTextSize(21f)
            binding.deleteBtn.setTextSize(21f)
        }

        binding.deleteBtn.setOnClickListener {
            callbackFileDelete?.let {
                it()
            }
            callbackMemoDelete?.let{
                it()
            }
            dismiss()
        }

        binding.deleteCancelBtn.setOnClickListener {
            dismiss()
        }

    }



    fun showDialog(fileSeq: Int, originalName: String, thumbnailName: String, callback:()->Unit){
        callbackMemoDelete = null
        binding.tvDeleteContents.text = context.resources.getString(R.string.file_delete_confirm)

        this.fileSeq = fileSeq
        this.originalName = originalName
        this.thumbnailName = thumbnailName

        callbackFileDelete = callback
        show()
    }


    fun showDialog(memoSeq: Int, callback:()->Unit){
        callbackFileDelete = null
        binding.tvDeleteContents.text = context.resources.getString(R.string.delete_confirm)

        this.memoSeq = memoSeq

        callbackMemoDelete = callback
        show()
    }


    override fun onBackPressed() {
//        super.onBackPressed();
    }
}


