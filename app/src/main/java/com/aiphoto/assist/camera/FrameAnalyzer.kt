package com.aiphoto.assist.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aiphoto.assist.composition.*
import com.aiphoto.assist.sensors.LevelSensor
import com.aiphoto.assist.vision.FaceDetectorWrapper
import com.aiphoto.assist.vision.HorizonDetector
import com.aiphoto.assist.vision.SubjectDetectorWrapper

/**
 * CameraX ImageAnalysis.Analyzer that builds FrameFeatures from sensors
 * and vision detectors, then runs preset evaluation on each frame.
 *
 * Pipeline per frame:
 * 1. Gyro → roll degrees
 * 2. Y-plane → horizon detection (Sobel-based)
 * 3. ML Kit → face detection (async, reads previous result)
 * 4. ML Kit → object detection (async fallback when no face)
 * 5. Build FrameFeatures → PresetManager → Evaluation
 */
class FrameAnalyzer(
    private val levelSensor: LevelSensor,
    private val horizonDetector: HorizonDetector,
    private val faceDetector: FaceDetectorWrapper,
    private val subjectDetector: SubjectDetectorWrapper,
    private val presetManager: PresetManager,
    private val selectedPresetId: () -> String?,
    private val autoMode: () -> Boolean,
    private val onResult: (Preset, Evaluation) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val roll = levelSensor.rollDeg()

            // ── Horizon detection (synchronous, lightweight) ─────────
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)
            val horizonY = horizonDetector.detect(
                yBytes, image.width, image.height, yPlane.rowStride
            )
            // Rewind buffer so ML Kit can still read the image
            yBuffer.rewind()

            // ── Face detection (async — submit frame, read *previous* result) ──
            faceDetector.detect(image)
            val faceResult = faceDetector.latestResult

            // ── Subject detection (async — only when no face detected) ──
            if (faceResult == null) {
                subjectDetector.detect(image)
            }
            val subjectResult = subjectDetector.latestResult

            // ── Build FrameFeatures ──────────────────────────────────
            // Priority: face box > object box
            val subjectBox = faceResult?.box ?: subjectResult?.box
            val faceYaw = faceResult?.yawDeg

            val features = FrameFeatures(
                rollDeg = roll,
                horizonYNorm = horizonY,
                subjectBox = subjectBox,
                faceYaw = faceYaw
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
