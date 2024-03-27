package com.peng.power.memo.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import com.peng.power.memo.databinding.DialogUserBinding

class UserDialog(context: Context?) :
    AppCompatDialog(context, 0) {
    private var binding = DialogUserBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)

        window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
        setCancelable(true)

        binding.dialogBottomBtn1.visibility = View.VISIBLE // 취소
        binding.dialogBottomBtn2.visibility = View.VISIBLE // 확인
        binding.dialogBottomBtn3.visibility = View.GONE  //변경

    }




    fun showDialog(title:String, content:String, actionBtn1:()->Unit, actionBtn2: () -> Unit, actionBtn3:() -> Unit){
        binding.dialogTitleTxt.text = title
        binding.dialogContentTxt.text = content

        binding.dialogBottomBtn1.visibility = View.VISIBLE
        binding.dialogBottomBtn2.visibility = View.VISIBLE
        binding.dialogBottomBtn3.visibility = View.VISIBLE

        binding.dialogBottomBtn1.setOnClickListener{
            actionBtn1()
            dismiss()
        }

        binding.dialogBottomBtn2.setOnClickListener {
            actionBtn2()
            dismiss()
        }

        binding.dialogBottomBtn3.setOnClickListener {
            actionBtn3()
            dismiss()
        }
        if(!isShowing)
            super.show()
    }


    fun showDialog(title:String, content:String, actionBtn1:()->Unit, actionBtn2: () -> Unit){
        binding.dialogTitleTxt.text = title
        binding.dialogContentTxt.text = content

        binding.dialogBottomBtn1.visibility = View.VISIBLE
        binding.dialogBottomBtn2.visibility = View.VISIBLE
        binding.dialogBottomBtn3.visibility = View.GONE

        binding.dialogBottomBtn1.setOnClickListener{
            actionBtn1()
            dismiss()
        }

        binding.dialogBottomBtn2.setOnClickListener {
            actionBtn2()
            dismiss()
        }
        if(!isShowing)
            super.show()
    }


    fun showDialogWithOkBtn(title:String, content:String, actionBtn1:()->Unit){
        binding.dialogTitleTxt.text = title
        binding.dialogContentTxt.text = content

        binding.dialogBottomBtn1.visibility = View.GONE
        binding.dialogBottomBtn2.visibility = View.VISIBLE
        binding.dialogBottomBtn3.visibility = View.GONE

        binding.dialogBottomBtn2.setOnClickListener{
            actionBtn1()
            dismiss()
        }
        if(!isShowing)
            super.show()
    }

    fun showDialogWithCancelBtn(title:String, content:String, actionBtn1:()->Unit){
        binding.dialogTitleTxt.text = title
        binding.dialogContentTxt.text = content

        binding.dialogBottomBtn1.visibility = View.VISIBLE
        binding.dialogBottomBtn2.visibility = View.GONE
        binding.dialogBottomBtn3.visibility = View.GONE

        binding.dialogBottomBtn1.setOnClickListener{
            actionBtn1()
            dismiss()
        }
        if(!isShowing)
            super.show()
    }


    fun showDialog(title:String, content:String){
        binding.dialogTitleTxt.text = title
        binding.dialogContentTxt.text = content

        binding.dialogBottomBtn1.visibility = View.GONE
        binding.dialogBottomBtn2.visibility = View.GONE
        binding.dialogBottomBtn3.visibility = View.GONE

        if(!isShowing)
            super.show()
    }


    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }
    }


}