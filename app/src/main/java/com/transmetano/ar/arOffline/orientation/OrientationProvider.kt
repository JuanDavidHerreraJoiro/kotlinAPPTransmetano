package com.transmetano.ar.arOffline.orientation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.view.Surface.*
import android.view.WindowManager
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@Suppress("MagicNumber")
internal class OrientationProvider @Inject constructor(
    private val windowManager: WindowManager
) {
    private var alpha = 0.94f
    private var lastCosA = 0f
    private var lastSinA = 0f
    private var lastCosP = 0f
    private var lastSinP = 0f

    private var azimuth = 0f
    private var pitch = 0f

    private lateinit var mGravity: FloatArray
    private lateinit var mMagnetic: FloatArray

    var mRotationMatrix = FloatArray(9)

    fun handleSensorEvent(sensorEvent: SensorEvent): OrientationData {

        if (sensorEvent.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagnetic = sensorEvent.values.clone()
        } else {
            mGravity = sensorEvent.values.clone()
        }

        if (this::mGravity.isInitialized && this::mMagnetic.isInitialized) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity, mMagnetic)) {

                val adjustedRotationMatrix = getAdjustedRotationMatrix(mRotationMatrix)
                val orientation = FloatArray(3)

                SensorManager.getOrientation(adjustedRotationMatrix, orientation)
                azimuth = lowPassDegreesFilterA(orientation[0])
                pitch = lowPassDegreesFilterP(orientation[1])

            }
        }

        return OrientationData(azimuth, pitch)
    }

    private fun lowPassDegreesFilterA(azimuthRadians: Float): Float {
        lastSinA = alpha * lastSinA + (1 - alpha) * sin(azimuthRadians)
        lastCosA = alpha * lastCosA + (1 - alpha) * cos(azimuthRadians)

        return ((Math.toDegrees(atan2(lastSinA, lastCosA).toDouble()) + 360) % 360).toFloat()
    }

    private fun lowPassDegreesFilterP(azimuthRadians: Float): Float {
        lastSinP = alpha * lastSinP + (1 - alpha) * sin(azimuthRadians)
        lastCosP = alpha * lastCosP + (1 - alpha) * cos(azimuthRadians)

        return ((Math.toDegrees(atan2(lastSinP, lastCosP).toDouble()) + 360) % 360).toFloat()
    }

    private fun getAdjustedRotationMatrix(rotationMatrix: FloatArray): FloatArray {
        val axisXY = getProperAxis()

        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix, axisXY.first,
            axisXY.second, adjustedRotationMatrix
        )
        return adjustedRotationMatrix
    }

    private fun getProperAxis(): Pair<Int, Int> {
        val worldAxisX: Int
        val worldAxisY: Int
        when (windowManager.defaultDisplay?.rotation) {
            ROTATION_90 -> {
                worldAxisX = SensorManager.AXIS_Z
                worldAxisY = SensorManager.AXIS_MINUS_X
            }
            ROTATION_180 -> {
                worldAxisX = SensorManager.AXIS_MINUS_X
                worldAxisY = SensorManager.AXIS_MINUS_Z
            }
            ROTATION_270 -> {
                worldAxisX = SensorManager.AXIS_MINUS_Z
                worldAxisY = SensorManager.AXIS_X
            }
            ROTATION_0 -> {
                worldAxisX = SensorManager.AXIS_X
                worldAxisY = SensorManager.AXIS_Z
            }
            else -> {
                worldAxisX = SensorManager.AXIS_X
                worldAxisY = SensorManager.AXIS_Z
            }
        }
        return Pair(worldAxisX, worldAxisY)
    }

}
