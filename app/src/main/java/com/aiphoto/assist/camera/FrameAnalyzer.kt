package com.aiphoto.assist.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aiphoto.assist.composition.*
import com.aiphoto.assist.sensors.LevelSensor

/**
 * CameraX ImageAnalysis.Analyzer that builds FrameFeatures from sensors
 * and runs preset evaluation on each frame.
 *
 * MVP: only uses gyro roll. Subject detection / face yaw will be added later.
 */
class FrameAnalyzer(
    private val levelSensor: LevelSensor,
    private val presetManager: PresetManager,
    private val selectedPresetId: () -> String?,
    private val autoMode: () -> Boolean,
    private val onResult: (Preset, Evaluation) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val roll = levelSensor.rollDeg()

            // Build features — MVP: only roll, rest null
            // TODO: add horizon detection, subject detection, face yaw
            val features = FrameFeatures(
                rollDeg = roll,
                horizonYNorm = null,
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
