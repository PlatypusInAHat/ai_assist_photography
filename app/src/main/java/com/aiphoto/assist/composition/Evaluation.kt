package com.aiphoto.assist.composition

import android.graphics.RectF
import com.aiphoto.assist.composition.hints.Hint

/**
 * Features extracted from the current camera frame + sensors.
 * Fields are nullable — populated progressively as detectors are added.
 */
data class FrameFeatures(
    /** Roll angle in degrees from gyro (leveling) */
    val rollDeg: Float,
    /** Detected horizon line Y position, normalized 0..1 (top=0, bottom=1) */
    val horizonYNorm: Float? = null,
    /** Bounding box of main subject, normalized 0..1 */
    val subjectBox: RectF? = null,
    /** Face yaw estimate: negative=looking left, positive=looking right */
    val faceYaw: Float? = null
)

/**
 * Result of evaluating a composition preset against current frame.
 */
data class Evaluation(
    /** Score 0..100; higher = better composition */
    val score: Int,
    /** Ordered list of hints (highest priority first) */
    val hints: List<Hint>,
    /** Which overlay to draw */
    val overlay: OverlaySpec,
    /** Detected subject bounding box (normalised 0..1), for UI overlay */
    val subjectBox: RectF? = null
)

/**
 * Describes which overlay graphics to render on screen.
 */
sealed interface OverlaySpec {
    data class Grid(val type: GridType) : OverlaySpec
    data class Diagonal(val showBoth: Boolean = true) : OverlaySpec
    data class Horizon(val yNorm: Float) : OverlaySpec
    data class Targets(val points: List<Pair<Float, Float>>) : OverlaySpec
}

enum class GridType { THIRDS, PHI }
