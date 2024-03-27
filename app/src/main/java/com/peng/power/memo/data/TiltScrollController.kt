package com.peng.power.memo.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.peng.power.memo.util.l

class TiltScrollController(ctx: Context, private val onTiltCallback:(x:Int, y:Int, delta:Float)->Unit) :
    SensorEventListener {

    companion object {
        private const val THRESHOLD_MOTION = 0.001f
        private const val SENSOR_DELAY_MICROS = 80 * 1000 // 32ms
    }

    private val mWindowManager: WindowManager = ctx.getSystemService(WindowManager::class.java)
    private val mSensorManager: SensorManager = ctx.getSystemService(SensorManager::class.java)
    private val mRotationSensor: Sensor
    private val mRotationMatrix = FloatArray(9)
    private val mAdjustedRotationMatrix = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var mInitialized = false
    private var mLastAccuracy = 0
    private var mOldX = 0f


    /**
     * Constructor.
     *
     * @param ctx            The context that the scroll view is running in.
     * @param scrollListener The listener for scroll events.
     */
    init {
        // Can be null if the sensor hardware is not available
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS)
    }



    override fun onSensorChanged(event: SensorEvent) {
        //l.d("onSensorChanged")
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return
        }
        if (event.sensor == mRotationSensor) {
            updateRotation(event.values.clone())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy
        }
    }

    /**
     * Update the rotation based on changes to the device's sensors.
     *
     * @param rotationVector The new rotation vectors.
     */
    private fun updateRotation(rotationVector: FloatArray) {
        //l.d("update rotation")
        // Get rotation's based on vector locations
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, rotationVector)
        val worldAxisForDeviceAxisX: Int
        val worldAxisForDeviceAxisY: Int
        when (mWindowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> {
                worldAxisForDeviceAxisX = SensorManager.AXIS_X
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z
            }
            Surface.ROTATION_90 -> {
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z
            }
            Surface.ROTATION_270 -> {
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z
                worldAxisForDeviceAxisY = SensorManager.AXIS_X
            }
            else -> {
                worldAxisForDeviceAxisX = SensorManager.AXIS_X
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z
            }
        }
        SensorManager.remapCoordinateSystem(
            mRotationMatrix,
            worldAxisForDeviceAxisX,
            worldAxisForDeviceAxisY,
            mAdjustedRotationMatrix
        )

        // Transform rotation matrix into azimuth/pitch/roll
        SensorManager.getOrientation(mAdjustedRotationMatrix, mOrientation)

        // Convert radians to degrees and flat
        val newX = Math.toDegrees(mOrientation[0].toDouble()).toFloat()
        //        float newZ = (float) Math.toDegrees(mOrientation[0]);

        // How many degrees has the users head rotated since last time.
        var deltaX = applyThreshold(angularRounding(newX - mOldX))
        //        float deltaZ = applyThreshold(angularRounding(newZ - mOldZ));

        // Ignore first head position in order to find base line
        if (!mInitialized) {
            mInitialized = true
            deltaX = 0f
            //            deltaZ = 0;
        }
        mOldX = newX
        var move = 0
        if (Math.abs(deltaX) > 0.1 && Math.abs(deltaX) <= 0.2) {
            move = if (deltaX > 0) {
                1
            } else {
                -1
            }
        } else if (Math.abs(deltaX) > 0.2 && Math.abs(deltaX) <= 0.4) {
            move = if (deltaX > 0) {
                2
            } else {
                -2
            }
        } else if (Math.abs(deltaX) > 0.4 && Math.abs(deltaX) <= 0.6) {
            move = if (deltaX > 0) {
                6
            } else {
                -6
            }
        } else if (Math.abs(deltaX) > 0.6 && Math.abs(deltaX) <= 1.0) {
            move = if (deltaX > 0) {
                8
            } else {
                -8
            }
        } else if (Math.abs(deltaX) > 1 /*&& abs(deltaX) <= 1.5*/) {
            move = if (deltaX > 0) {
                10
            } else {
                -10
            }
        }


        if (move != 0)
            onTiltCallback(move, 0, deltaX)
    }

    /**
     * Apply a minimum value to the input.
     * If input is below the threshold, return zero to remove noise.
     *
     * @param input The value to inspect.
     * @return The value of input if within the threshold, or 0 if it is outside.
     */
    private fun applyThreshold(input: Float): Float {
        return if (Math.abs(input) > THRESHOLD_MOTION) input else 0F
    }

    /**
     * Adjust the angle of rotation to take into account on the device orientation.
     *
     * @param input The rotation.
     * @return The rotation taking into account the device orientation.
     */
    private fun angularRounding(input: Float): Float {
        return when {
            input >= 180.0f -> {
                input - 360.0f
            }
            input <= -180.0f -> {
                360 + input
            }
            else -> {
                input
            }
        }
    }

    /**
     * Request access to sensors
     */
    fun requestAllSensors() {
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS)
    }

    /**
     * Release the sensors when they are no longer used
     */
    fun releaseAllSensors() {
        mSensorManager.unregisterListener(this, mRotationSensor)
    }






}