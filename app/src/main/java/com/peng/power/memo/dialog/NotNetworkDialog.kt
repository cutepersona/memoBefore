package com.peng.power.memo.dialog

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.os.Process
import android.util.DisplayMetrics
import android.util.Log
import com.peng.power.memo.R
import com.peng.power.memo.databinding.DialogNotNetworkBinding

class NotNetworkDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    private val binding = DialogNotNetworkBinding.inflate(layoutInflater)

    var display: DisplayMetrics? = null
    var fontSize: Float = 25f

    private val mContext = context

    init {
        setContentView(binding.root)

        // 2023-02-06    Navigate520으로 인해 DP로 구분짓기 위함
        display = context?.resources?.displayMetrics

        // 2023-02-06   Navigate520
        if (display?.widthPixels == 1280 && display?.heightPixels == 720) {
            Log.d("Noah", " Navigate520 EditFileDialog")
            binding.tvNetworkDisconnect.setTextSize(fontSize)
            binding.tv01.setTextSize(23f)
        }

        binding.tv01.setOnClickListener {
            val am = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            am.killBackgroundProcesses("com.watt.powermemo")
            Process.killProcess(Process.myPid())
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed();
    }
}