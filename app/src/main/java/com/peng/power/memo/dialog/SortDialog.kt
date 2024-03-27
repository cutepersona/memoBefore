package com.peng.power.memo.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import com.peng.power.memo.R
import com.peng.power.memo.databinding.DialogSortBinding

class SortDialog(context: Context, private val callback:(Int)->Unit) :
    Dialog(context, R.style.InfoDialog) {

    private val binding:DialogSortBinding = DialogSortBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 22f

    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        initUI()
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        binding.tvAllMemoAsc.text = context.resources.getString(R.string.select) + " 1"
        binding.tvAllMemoDesc.text = context.resources.getString(R.string.select) + " 2"
        binding.tvMyMemoAsc.text = context.resources.getString(R.string.select) + " 3"
        binding.tvMyMemoDesc.text = context.resources.getString(R.string.select) + " 4"

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvBack.setTextSize(fontSize)
            binding.tvFileTitleNumber.setTextSize(27f)
            binding.tvAllMemoAsc.setTextSize(fontSize)
            binding.tvAllMemoDesc.setTextSize(fontSize)
            binding.tvMyMemoAsc.setTextSize(fontSize)
            binding.tvMyMemoDesc.setTextSize(fontSize)
            binding.tvAllMemoA.setTextSize(fontSize)
            binding.tvAllAsc.setTextSize(fontSize)
            binding.tvAllMemoB.setTextSize(fontSize)
            binding.tvAllDesc.setTextSize(fontSize)
            binding.tvMyMemoA.setTextSize(fontSize)
            binding.tvMyAsc.setTextSize(fontSize)
            binding.tvMyMemoB.setTextSize(fontSize)
            binding.tvMyDesc.setTextSize(fontSize)
        }

        binding.tvAllMemoAsc.setOnClickListener {
            callback(1)
            dismiss()
        }
        binding.tvAllMemoDesc.setOnClickListener {
            callback(2)
            dismiss()
        }
        binding.tvMyMemoAsc.setOnClickListener {
            callback(3)
            dismiss()
        }
        binding.tvMyMemoDesc.setOnClickListener {
            callback(4)
            dismiss()
        }

        binding.tvBack.setOnClickListener { dismiss() }
    }



    override fun onBackPressed() {
//        super.onBackPressed();
    }


}
