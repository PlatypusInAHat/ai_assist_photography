package com.aiphoto.assist.composition

import android.graphics.RectF
import com.aiphoto.assist.composition.hints.Hint

// ─── Capture Modes (Scene Types) ─────────────────────────────────

enum class CaptureMode {
    AUTO, PORTRAIT, LANDSCAPE, STREET, ARCH, FOOD, SCAN
}

// ─── Preset IDs ──────────────────────────────────────────────────

enum class PresetId {
    AUTO, THIRDS, PHI, CENTER, DIAGONAL, HORIZON, LEADING, LOOKROOM
}

// ─── Grid Type ───────────────────────────────────────────────────

enum class GridType { THIRDS, PHI }

// ─── Overlay State (single source of truth for UI) ───────────────

/**
 * Unified UI state driven by the engine.
 * The CoachOverlay composable reads this to draw all guides.
 */
data class OverlayState(
    val mode: CaptureMode = CaptureMode.AUTO,
    val preset: PresetId = PresetId.AUTO,
    val score: Int = 0,
    val primaryText: String = "",
    val secondaryText: String = "",
    val grid: GridType? = null,
    val showDiagonals: Boolean = false,
    val horizonYNorm: Float? = null,
    val targetPoints: List<Pair<Float, Float>> = emptyList(),
    val rotateDeg: Float? = null,
    val moveDxDyNorm: Pair<Float, Float>? = null,
    val subjectBox: RectF? = null,
    val rollDeg: Float = 0f,
    val presetDisplayName: String = "Auto"
)

// ─── Frame Features (internal, passed to presets) ────────────────

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

// ─── Evaluation (internal, output of a preset) ──────────────────

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

// ─── Overlay Spec (internal, describes what to draw) ────────────

/**
 * Describes which overlay graphics to render on screen.
 */
sealed interface OverlaySpec {
    data class Grid(val type: GridType) : OverlaySpec
    data class Diagonal(val showBoth: Boolean = true) : OverlaySpec
    data class Horizon(val yNorm: Float) : OverlaySpec
    data class Targets(val points: List<Pair<Float, Float>>) : OverlaySpec
}
