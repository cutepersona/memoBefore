package com.peng.power.memo.data

import android.content.Context
import android.graphics.PointF
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

class CircleLayoutManager(private val context: Context) : RecyclerView.LayoutManager() {
    // Size of each items
    private var mDecoratedChildWidth = 0
    private var mDecoratedChildHeight = 0

    //Property
    private var startLeft = 0
    private var startTop = 0
    /**
     *
     * @return Get the radius of the circle
     */
    /**
     *
     * @param mRadius the radius of the circle,default will be item's height
     */
    var radius = 0
    /**
     *
     * @return the interval angle between each items
     */
    /**
     * Default angle is 30
     * @param intervalAngle the interval angle between each items
     */
    var intervalAngle: Int
    private var offsetRotate // The offset angle for each items which will change according to the scroll offset
            : Float

    //the range of remove from parent
    private var minRemoveDegree: Int
    private var maxRemoveDegree: Int
    /**
     * Default is center in parent
     * @return the content offset of x
     */
    /**
     * Default is center in parent
     * @param contentOffsetX the content offset of x
     */
    //initial position of content
    var contentOffsetX = -1
    /**
     * Default is top in parent
     * @return the content offset of y
     */
    /**
     * Default is top in parent
     * @param contentOffsetY the content offset of y
     */
    var contentOffsetY = -1
    /**
     * Default is 0
     * @return the rotate of first child
     */
    /**
     * Default is 0
     * @param firstChildRotate the rotate of first child
     */
    var firstChildRotate = 0

    //Sparse array for recording the attachment and rotate angle of each items
    private val itemAttached = SparseBooleanArray()
    private val itemsRotate = SparseArray<Float>()
    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            offsetRotate = 0f
            return
        }

        //calculate the size of child
        if (childCount == 0) {
            val scrap = recycler.getViewForPosition(0)
            addView(scrap)
            measureChildWithMargins(scrap, 0, 0)
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap)
            startLeft =
                if (contentOffsetX == -1) (horizontalSpace - mDecoratedChildWidth) / 2 else contentOffsetX
            startTop = if (contentOffsetY == -1) 0 else contentOffsetY
            radius = mDecoratedChildHeight
            detachAndScrapView(scrap, recycler)
        }

        //record the state of each items
        var rotate = firstChildRotate.toFloat()
        for (i in 0 until itemCount) {
            itemsRotate.put(i, rotate)
            itemAttached.put(i, false)
            rotate += intervalAngle.toFloat()
        }
        detachAndScrapAttachedViews(recycler)
        fixRotateOffset()
        layoutItems(recycler, state)
    }

    private fun layoutItems(
        recycler: Recycler,
        state: RecyclerView.State, oritention: Int = SCROLL_RIGHT
    ) {
        if (state.isPreLayout) return

        //remove the views which out of range
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val position = getPosition(view!!)
            if (itemsRotate[position] - offsetRotate > maxRemoveDegree
                || itemsRotate[position] - offsetRotate < minRemoveDegree
            ) {
                itemAttached.put(position, false)
                removeAndRecycleView(view, recycler)
            }
        }

        //add the views which do not attached and in the range
        for (i in 0 until itemCount) {
            if (itemsRotate[i] - offsetRotate <= maxRemoveDegree
                && itemsRotate[i] - offsetRotate >= minRemoveDegree
            ) {
                if (!itemAttached[i]) {
                    val scrap = recycler.getViewForPosition(i)
                    measureChildWithMargins(scrap, 0, 0)
                    if (oritention == SCROLL_LEFT) addView(scrap, 0) else addView(scrap)
                    val rotate = itemsRotate[i] - offsetRotate
                    val left = calLeftPosition(rotate)
                    val top = calTopPosition(rotate)
                    scrap.rotation = rotate
                    layoutDecorated(
                        scrap,
                        startLeft + left,
                        startTop + top,
                        startLeft + left + mDecoratedChildWidth,
                        startTop + top + mDecoratedChildHeight
                    )
                    itemAttached.put(i, true)
                }
            }
        }
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        var willScroll = dx
        var theta = dx / DISTANCE_RATIO // the angle every item will rotate for each dx
        val targetRotate = offsetRotate + theta

        //handle the boundary
        if (targetRotate < 0) {
            willScroll = (-offsetRotate * DISTANCE_RATIO).toInt()
        } else if (targetRotate > maxOffsetDegree) {
            willScroll = ((maxOffsetDegree - offsetRotate) * DISTANCE_RATIO).toInt()
        }
        theta = willScroll / DISTANCE_RATIO
        offsetRotate += theta //increase the offset rotate so when re-layout it can recycle the right views

        //re-calculate the rotate x,y of each items
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            val newRotate = view!!.rotation - theta
            val offsetX = calLeftPosition(newRotate)
            val offsetY = calTopPosition(newRotate)
            layoutDecorated(
                view,
                startLeft + offsetX,
                startTop + offsetY,
                startLeft + offsetX + mDecoratedChildWidth,
                startTop + offsetY + mDecoratedChildHeight
            )
            view.rotation = newRotate
        }

        //different direction child will overlap different way
        if (dx < 0) layoutItems(recycler, state, SCROLL_LEFT) else layoutItems(
            recycler,
            state,
            SCROLL_RIGHT
        )
        return willScroll
    }

    /**
     *
     * @param rotate the current rotate of view
     * @return the x of view
     */
    private fun calLeftPosition(rotate: Float): Int {
        return (radius * Math.cos(Math.toRadians((90 - rotate).toDouble()))).toInt()
    }

    /**
     *
     * @param rotate the current rotate of view
     * @return the y of view
     */
    private fun calTopPosition(rotate: Float): Int {
        return (radius - radius * Math.sin(Math.toRadians((90 - rotate).toDouble()))).toInt()
    }

    private val horizontalSpace: Int
        private get() = width - paddingRight - paddingLeft
    private val verticalSpace: Int
        private get() = height - paddingBottom - paddingTop

    /**
     * fix the offset rotate angle in case item out of boundary
     */
    private fun fixRotateOffset() {
        if (offsetRotate < 0) {
            offsetRotate = 0f
        }
        if (offsetRotate > maxOffsetDegree) {
            offsetRotate = maxOffsetDegree
        }
    }

    /**
     *
     * @return the max degrees according to current number of views and interval angle
     */
    private val maxOffsetDegree: Float
        private get() = ((itemCount - 1) * intervalAngle).toFloat()

    private fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1 else 1
        return PointF(direction.toFloat(), 0f)
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position > itemCount - 1) return
        val targetRotate = (position * intervalAngle).toFloat()
        if (targetRotate == offsetRotate) return
        offsetRotate = targetRotate
        fixRotateOffset()
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val smoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return this@CircleLayoutManager.computeScrollVectorForPosition(targetPosition)
            }
        }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        removeAllViews()
        offsetRotate = 0f
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     *
     * @return Get the current positon of views
     */
    val currentPosition: Int
        get() = Math.round(offsetRotate / intervalAngle)

    /**
     *
     * @return Get the dx should be scrolled to the center
     */
    val offsetCenterView: Int
        get() = ((currentPosition * intervalAngle - offsetRotate) * DISTANCE_RATIO).toInt()

    /**
     * The rotate of child view in range[min,max] will be shown,default will be [-90,90]
     * @param min min rotate that will be show
     * @param max max rotate that will be show
     */
    fun setDegreeRangeWillShow(min: Int, max: Int) {
        if (min > max) return
        minRemoveDegree = min
        maxRemoveDegree = max
    }

    companion object {
        private const val INTERVAL_ANGLE = 30 // The default interval angle between each items
        private const val DISTANCE_RATIO = 10f // Finger swipe distance divide item rotate angle

        //Flags of scroll dirction
        private const val SCROLL_LEFT = 1
        private const val SCROLL_RIGHT = 2
    }

    init {
        intervalAngle = INTERVAL_ANGLE
        offsetRotate = 0f
        minRemoveDegree = -90
        maxRemoveDegree = 90
    }
}
