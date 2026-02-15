package com.aiphoto.assist.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Gyro / rotation-vector based level sensor.
 * Provides roll angle in degrees for horizon leveling.
 */
class LevelSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientations = FloatArray(3)

    @Volatile
    private var rollRad: Float = 0f

    /** Start listening to rotation sensor */
    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /** Stop listening */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** Current roll in degrees (negative = tilted left, positive = tilted right) */
    fun rollDeg(): Float = Math.toDegrees(rollRad.toDouble()).toFloat()

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientations)
            // orientations[2] = roll (rotation around the axis pointing forward)
            rollRad = orientations[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
