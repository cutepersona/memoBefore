package com.peng.power.memo.data

import androidx.recyclerview.widget.RecyclerView

class CenterScrollListener : RecyclerView.OnScrollListener() {
    private var mAutoSet = false
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        val layoutManager = recyclerView.layoutManager
        if (layoutManager !is CircleLayoutManager && layoutManager !is ScrollZoomLayoutManager) {
            mAutoSet = true
            return
        }
        if (!mAutoSet) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val dx = if (layoutManager is CircleLayoutManager) {
                    (layoutManager as CircleLayoutManager?)?.offsetCenterView ?: 0
                } else {
                    (layoutManager as ScrollZoomLayoutManager?)?.offsetCenterView ?: 0
                }
                recyclerView.smoothScrollBy(dx, 0)
            }
            mAutoSet = true
        }
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
            mAutoSet = false
        }
    }
}