package com.peng.power.memo.data

import android.content.Context
import android.graphics.PointF
import android.util.Log
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.Display
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import splitties.systemservices.windowManager


class ScrollZoomLayoutManager(private val context: Context, private val itemSpace: Int) :
    RecyclerView.LayoutManager() {
    /**
     * item width
     */
    // Size of each items
    var eachItemWidth = 0
    private var mDecoratedChildHeight = 0

    //Property
    private var startLeft = 0
    private var startTop = 0
    // The offset distance for each items which will change according to the scroll offset
    private var offsetScroll = 0
    /**
     * Default is top in parent
     *
     * @return the content offset of y
     */
    /**
     * Default is top in parent
     *
     * @param contentOffsetY the content offset of y
     */
    //initial top position of content
    private var contentOffsetY = -1
    private var offsetDistance = 0
    /**
     *
     * @return the max scale rate.. default is 1.2f
     */
    /**
     *
     * @param maxScale the max scale rate.. default is 1.2f
     */
    private var maxScale : Float   //max scale rate defalut is 1.2f

    //Sparse array for recording the attachment and x position of each items
    private val itemAttached = SparseBooleanArray()
    private val itemsX = SparseArray<Int>()

    companion object {
        private var SCALE_RATE = 1.3f //1.2  //1.5f
    }

    val display = context?.resources?.displayMetrics

    init {
        if (display?.widthPixels == 1280 && display?.heightPixels == 720){
            SCALE_RATE = 1.9f
        } else {
            SCALE_RATE = 1.3f
        }
        maxScale = SCALE_RATE
    }



    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            offsetScroll = 0
            return
        }

        //calculate the size of child
        if (childCount == 0) {
            val scrap = recycler.getViewForPosition(0)
            addView(scrap)
            measureChildWithMargins(scrap, 0, 0)
            eachItemWidth = getDecoratedMeasuredWidth(scrap)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap)
            offsetDistance = (eachItemWidth * ((maxScale - 1f) / 2f + 1) + itemSpace).toInt()
            startLeft = (horizontalSpace - eachItemWidth) / 2
            startTop =
                if (contentOffsetY == -1) (verticalSpace - mDecoratedChildHeight) / 2 else contentOffsetY
            detachAndScrapView(scrap, recycler)
        }

        //record the state of each items
        var x = 0
        for (i in 0 until itemCount) {
            itemsX.put(i, x)
            itemAttached.put(i, false)
            x += offsetDistance
        }
        detachAndScrapAttachedViews(recycler)
        fixScrollOffset()
        layoutItems(recycler, state)
    }

    private fun layoutItems(
        recycler: Recycler,
        state: RecyclerView.State,
    ) {
        if (state.isPreLayout) return

        //remove the views which out of range
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            view?.let{
                val position = getPosition(it)
                if (itemsX[position] - offsetScroll + startLeft > horizontalSpace
                    || itemsX[position] - offsetScroll + startLeft < -eachItemWidth - paddingLeft
                ) {
                    itemAttached.put(position, false)
                    removeAndRecycleView(view, recycler)
                }
            }

        }

        //add the views which do not attached and in the range
        for (i in 0 until itemCount) {
            if (itemsX[i] - offsetScroll + startLeft <= horizontalSpace
                && itemsX[i] - offsetScroll + startLeft >= -eachItemWidth - paddingLeft
            ) {
                if (!itemAttached[i]) {
                    val scrap = recycler.getViewForPosition(i)
                    measureChildWithMargins(scrap, 0, 0)
                    addView(scrap)
                    val x = itemsX[i] - offsetScroll
                    val scale = calculateScale(startLeft + x)
                    scrap.rotation = 0f
                    layoutDecorated(
                        scrap, startLeft + x, startTop,
                        startLeft + x + eachItemWidth, startTop + mDecoratedChildHeight
                    )
                    itemAttached.put(i, true)
                    scrap.scaleX = scale
                    scrap.scaleY = scale
                }
            }
        }
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        var willScroll = dx
        val targetX = offsetScroll + dx

        //handle the boundary
        if (targetX < 0) {
            willScroll = -offsetScroll
        } else if (targetX > maxOffsetX) {
            willScroll = maxOffsetX - offsetScroll
        }
        offsetScroll += willScroll //increase the offset x so when re-layout it can recycle the right views

        //handle position and scale
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val scale = calculateScale(v!!.left)
            layoutDecorated(v, v.left - willScroll, v.top, v.right - willScroll, v.bottom)
            v.scaleX = scale
            v.scaleY = scale
        }

        //handle recycle
        layoutItems(recycler, state)
        return willScroll
    }

    /**
     *
     * @param x start positon of the view you want scale
     * @return the scale rate of current scroll offset
     */
    private fun calculateScale(x: Int): Float {
        val deltaX = Math.abs(x - (horizontalSpace - eachItemWidth) / 2)
        var diff = 0f
        if (eachItemWidth - deltaX > 0) diff = (eachItemWidth - deltaX).toFloat()
        return (maxScale - 1f) / eachItemWidth * diff + 1
    }

    private val horizontalSpace: Int
        private get() = width - paddingRight - paddingLeft
    private val verticalSpace: Int
        private get() = height - paddingBottom - paddingTop

    /**
     * fix the offset x in case item out of boundary
     */
    private fun fixScrollOffset() {
        if (offsetScroll < 0) {
            offsetScroll = 0
        }
        if (offsetScroll > maxOffsetX) {
            offsetScroll = maxOffsetX
        }
    }

    /**
     * @return the max offset distance
     */
    private val maxOffsetX: Int
        private get() = (itemCount - 1) * offsetDistance

    private fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return PointF(direction.toFloat(), 0F)
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position > itemCount - 1) return
        val target = position * offsetDistance
        if (target == offsetScroll) return
        offsetScroll = target
        fixScrollOffset()
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int,
    ) {
        val smoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return this@ScrollZoomLayoutManager.computeScrollVectorForPosition(targetPosition)
            }
        }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?,
    ) {
        removeAllViews()
        offsetScroll = 0
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }//        Log.d("asdasdasd", "getCurrentPosition: "+ Math.round(offsetScroll/(float)offsetDistance));

    /**
     * @return Get the current positon of views
     */
    val currentPosition: Int
        get() =//        Log.d("asdasdasd", "getCurrentPosition: "+ Math.round(offsetScroll/(float)offsetDistance));
            Math.round(offsetScroll / offsetDistance.toFloat())

    /**
     * @return Get the dx should be scrolled to the center
     */
    val offsetCenterView: Int
        get() = currentPosition * offsetDistance - offsetScroll




}
