package com.peng.power.memo.data

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.peng.power.memo.R
import com.peng.power.memo.util.l

class CreateTextView : RelativeLayout {
    var commands: TextView? = null

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    fun setText(str: String?, listener: OnClickListener?) {
        commands!!.text = str
        commands!!.setOnClickListener(listener)
        l.d("setText: $str")
    }

    fun setTag(tag: Int) {
        commands!!.tag = tag
    }

    private fun initView() {
        val infService = Context.LAYOUT_INFLATER_SERVICE
        val li = context.getSystemService(infService) as LayoutInflater
        val v: View = li.inflate(R.layout.background_command, this, false)
        addView(v)
        commands = findViewById<View>(R.id.commands) as TextView
    }
}
