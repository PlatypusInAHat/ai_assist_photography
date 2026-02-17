package com.aiphoto.assist.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aiphoto.assist.composition.*
import com.aiphoto.assist.sensors.LevelSensor
import com.aiphoto.assist.vision.HorizonDetector

/**
 * CameraX ImageAnalysis.Analyzer that builds FrameFeatures from sensors
 * and runs preset evaluation on each frame.
 *
 * Uses gyro roll + horizon detection. Subject detection / face yaw will be added later.
 */
class FrameAnalyzer(
    private val levelSensor: LevelSensor,
    private val horizonDetector: HorizonDetector,
    private val presetManager: PresetManager,
    private val selectedPresetId: () -> String?,
    private val autoMode: () -> Boolean,
    private val onResult: (Preset, Evaluation) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val roll = levelSensor.rollDeg()

            // Extract Y-plane for horizon detection
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)
            val horizonY = horizonDetector.detect(
                yBytes, image.width, image.height, yPlane.rowStride
            )

            // Build features
            // TODO: add subject detection, face yaw
            val features = FrameFeatures(
                rollDeg = roll,
                horizonYNorm = horizonY,
                subjectBox = null,
                faceYaw = null
            )

            val (preset, eval) = presetManager.evaluate(
                features,
                selectedId = selectedPresetId(),
                auto = autoMode()
            )
            onResult(preset, eval)
        } finally {
            image.close()
        }
    }
}
