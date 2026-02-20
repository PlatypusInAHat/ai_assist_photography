package com.aiphoto.assist.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.MoveHint
import com.aiphoto.assist.composition.hints.RotateHint
import com.aiphoto.assist.composition.hints.TextHint
import com.aiphoto.assist.sensors.LevelSensor
import com.aiphoto.assist.vision.FaceDetectorWrapper
import com.aiphoto.assist.vision.HorizonDetector
import com.aiphoto.assist.vision.SubjectDetectorWrapper

/**
 * Realtime coach engine — replaces FrameAnalyzer.
 *
 * Pipeline per frame:
 * 1. Gyro → roll degrees
 * 2. Horizon detection (Sobel, synchronous, lightweight)
 * 3. Face detection (ML Kit, async, throttled ~80ms)
 * 4. Object detection (ML Kit, async fallback, throttled ~80ms)
 * 5. PresetManager → Evaluation → convert to OverlayState
 *
 * Throttling: detector runs every ~80ms, full scene evaluation every frame.
 */
class RealtimeCoachEngine(
    private val levelSensor: LevelSensor,
    private val horizonDetector: HorizonDetector,
    private val faceDetector: FaceDetectorWrapper,
    private val subjectDetector: SubjectDetectorWrapper,
    private val presetManager: PresetManager,
    private val getUserSelected: () -> Pair<CaptureMode, PresetId>,
    private val onUpdate: (OverlayState) -> Unit
) : ImageAnalysis.Analyzer {

    // Scheduler timestamps
    private var lastDetectMs = 0L

    // Mode → default preset mapping
    private fun defaultPresetForMode(mode: CaptureMode): PresetId = when (mode) {
        CaptureMode.PORTRAIT -> PresetId.LOOKROOM
        CaptureMode.LANDSCAPE -> PresetId.HORIZON
        CaptureMode.STREET -> PresetId.DIAGONAL
        CaptureMode.ARCH -> PresetId.CENTER
        CaptureMode.FOOD -> PresetId.PHI
        CaptureMode.SCAN -> PresetId.CENTER
        CaptureMode.AUTO -> PresetId.AUTO
    }

    // PresetId → preset string id mapping
    private fun presetIdToStringId(id: PresetId): String? = when (id) {
        PresetId.AUTO -> null
        PresetId.THIRDS -> "thirds"
        PresetId.PHI -> "phi"
        PresetId.CENTER -> "center"
        PresetId.DIAGONAL -> "diagonal"
        PresetId.HORIZON -> "horizon"
        PresetId.LEADING -> "leading"
        PresetId.LOOKROOM -> "lookroom"
    }

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            val (mode, userPreset) = getUserSelected()

            // ── 1. Gyro ──────────────────────────────────────────
            val roll = levelSensor.rollDeg()

            // ── 2. Horizon detection (always, lightweight) ────────
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)
            val horizonY = horizonDetector.detect(
                yBytes, image.width, image.height, yPlane.rowStride
            )
            yBuffer.rewind()

            // ── 3. Throttled detection (~80ms) ────────────────────
            val runDetect = (now - lastDetectMs) > 80
            if (runDetect) {
                lastDetectMs = now

                // Face detection (async)
                faceDetector.detect(image)

                // Object detection (fallback when no face)
                val faceResult = faceDetector.latestResult
                if (faceResult == null) {
                    subjectDetector.detect(image)
                }
            }

            // ── 4. Read latest detection results ──────────────────
            val faceResult = faceDetector.latestResult
            val subjectResult = subjectDetector.latestResult
            val subjectBox = faceResult?.box ?: subjectResult?.box
            val faceYaw = faceResult?.yawDeg

            // ── 5. Build FrameFeatures ────────────────────────────
            val features = FrameFeatures(
                rollDeg = roll,
                horizonYNorm = horizonY,
                subjectBox = subjectBox,
                faceYaw = faceYaw
            )

            // ── 6. Resolve preset ─────────────────────────────────
            val resolvedPreset = if (userPreset == PresetId.AUTO) {
                defaultPresetForMode(mode)
            } else {
                userPreset
            }
            val isAuto = resolvedPreset == PresetId.AUTO || userPreset == PresetId.AUTO
            val presetStringId = presetIdToStringId(resolvedPreset)

            // ── 7. Evaluate via PresetManager ─────────────────────
            val (preset, eval) = presetManager.evaluate(
                features,
                selectedId = presetStringId,
                auto = isAuto
            )

            // ── 8. Convert Evaluation → OverlayState ─────────────
            val overlayState = evaluationToOverlayState(
                mode = mode,
                userPreset = userPreset,
                preset = preset,
                eval = eval,
                features = features
            )

            onUpdate(overlayState)
        } finally {
            image.close()
        }
    }

    /**
     * Convert internal Evaluation to the unified OverlayState for UI.
     */
    private fun evaluationToOverlayState(
        mode: CaptureMode,
        userPreset: PresetId,
        preset: Preset,
        eval: Evaluation,
        features: FrameFeatures
    ): OverlayState {
        // Extract grid/diagonals/horizon/targets from OverlaySpec
        var grid: GridType? = null
        var showDiagonals = false
        var horizonYNorm: Float? = null
        var targetPoints: List<Pair<Float, Float>> = emptyList()

        when (val ov = eval.overlay) {
            is OverlaySpec.Grid -> grid = ov.type
            is OverlaySpec.Diagonal -> showDiagonals = true
            is OverlaySpec.Horizon -> horizonYNorm = ov.yNorm
            is OverlaySpec.Targets -> targetPoints = ov.points
        }

        // Extract primary text from hints
        val textHint = eval.hints.filterIsInstance<TextHint>().firstOrNull()
        val primary = textHint?.text ?: ""

        // Extract secondary info
        val rotateHint = eval.hints.filterIsInstance<RotateHint>().firstOrNull()
        val secondary = rotateHint?.let {
            val dir = if (it.degrees > 0) "phải" else "trái"
            "↻ Xoay ${dir} ${kotlin.math.abs(it.degrees).toInt()}°"
        } ?: ""

        // Extract move hint
        val moveHint = eval.hints.filterIsInstance<MoveHint>().firstOrNull()
        val moveDxDy = moveHint?.let { it.dxNorm to it.dyNorm }

        return OverlayState(
            mode = mode,
            preset = userPreset,
            score = eval.score,
            primaryText = primary,
            secondaryText = secondary,
            grid = grid,
            showDiagonals = showDiagonals,
            horizonYNorm = horizonYNorm,
            targetPoints = targetPoints,
            rotateDeg = rotateHint?.degrees,
            moveDxDyNorm = moveDxDy,
            subjectBox = eval.subjectBox,
            rollDeg = features.rollDeg,
            presetDisplayName = preset.displayName
        )
    }
}
