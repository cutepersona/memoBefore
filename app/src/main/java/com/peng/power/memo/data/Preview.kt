package com.peng.power.memo.data

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.peng.power.memo.util.l
import kotlinx.coroutines.*
import java.io.IOException

class Preview(context: Context?, var mSurfaceView: SurfaceView) : ViewGroup(context),
    SurfaceHolder.Callback {

    private val defaultScope = CoroutineScope(Dispatchers.Default)
    private val TAG = "Preview"
    var mHolder: SurfaceHolder
    var mPreviewSize: Camera.Size? = null
    var mSupportedPreviewSizes: List<Camera.Size>? = null
    var mCamera: Camera? = null
    fun setCamera(camera: Camera?) {
        l.e("set camera")
        if (mCamera != null) {
            l.d("mCamera != null")
            // Call stopPreview() to stop updating the preview surface.
            mCamera!!.stopPreview()

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera!!.release()
            mCamera = null
        }
        mCamera = camera
        if (mCamera != null) {
            l.d("mCamera != null")
            defaultScope.launch {
                l.d("defatult scope is started")
                val localSizes = mCamera?.getParameters()?.supportedPreviewSizes
                mSupportedPreviewSizes = localSizes
                withContext(Dispatchers.Main){
                    l.d("request layout")
                    requestLayout()
                }

                // get Camera parameters
                val params = mCamera!!.parameters
                val focusModes = params.supportedFocusModes
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    // set the focus mode
                    params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                    // set Camera parameters
                    mCamera!!.parameters = params
                }

                l.d("set preview display")
                try {
                    mCamera!!.setPreviewDisplay(mHolder)
                } catch (e: IOException) {
                    l.e("exception : $e")
                    e.printStackTrace()
                }

                // Important: Call startPreview() to start updating the preview
                // surface. Preview must be started before you can take a picture.
                l.d("start preview")
                mCamera!!.startPreview()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed && childCount > 0) {
            val child = getChildAt(0)
            val width = r - l
            val height = b - t
            var previewWidth = width
            var previewHeight = height
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize!!.width
                previewHeight = mPreviewSize!!.height
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                val scaledChildWidth = previewWidth * height / previewHeight
                child.layout(
                    (width - scaledChildWidth) / 2, 0,
                    (width + scaledChildWidth) / 2, height
                )
            } else {
                val scaledChildHeight = previewHeight * width / previewWidth
                child.layout(
                    0, (height - scaledChildHeight) / 2,
                    width, (height + scaledChildHeight) / 2
                )
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        //Toast.makeText(getContext(), "surfaceCreated", Toast.LENGTH_LONG).show();
        Log.d("@@@", "surfaceCreated")

        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera!!.setPreviewDisplay(holder)
            }
        } catch (exception: IOException) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera!!.stopPreview()
        }
        defaultScope.cancel()
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mCamera != null) {
            val parameters = mCamera!!.parameters
            val allSizes = parameters.supportedPreviewSizes
            var size = allSizes[0] // get top size
            for (i in allSizes.indices) {
                if (allSizes[i].width > size.width) size = allSizes[i]
            }
            //set max Preview Size
            parameters.setPreviewSize(size.width, size.height)

            // Important: Call startPreview() to start updating the preview surface.
            // Preview must be started before you can take a picture.
            mCamera!!.startPreview()
        }
    }

    init {
        //        addView(mSurfaceView);
        mHolder = mSurfaceView.holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
}