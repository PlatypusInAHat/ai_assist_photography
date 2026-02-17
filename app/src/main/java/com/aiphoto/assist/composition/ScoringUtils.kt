package com.aiphoto.assist.composition

import com.aiphoto.assist.composition.hints.Hint
import com.aiphoto.assist.composition.hints.RotateHint
import kotlin.math.abs

/**
 * Shared scoring helpers used by all composition presets.
 * Centralises magic numbers and ensures consistent behaviour.
 */
object ScoringUtils {

    private const val SQRT2 = 1.41421356f

    // ─── Distance → Score ────────────────────────────────────────────

    /**
     * Convert a normalised distance (0 = perfect, ~0.7 = worst) to a score 0..100.
     *
     * Formula: `(1 − (dist / maxDist)²) × 100`, clamped to 0..100.
     * The squared falloff feels more natural than linear: small offsets lose
     * fewer points, large offsets are punished harder.
     *
     * @param dist       Euclidean distance in normalised coords (0..~0.7).
     * @param maxDist    Distance at which score drops to 0 (default 0.5).
     */
    fun distanceScore(dist: Float, maxDist: Float = 0.5f): Int {
        val ratio = (dist / maxDist).coerceIn(0f, 1f)
        return ((1f - ratio * ratio) * 100f).toInt().coerceIn(0, 100)
    }

    // ─── Roll Penalty ────────────────────────────────────────────────

    /**
     * Compute a roll-based score penalty **and** an optional RotateHint.
     *
     * @param rollDeg    Current roll angle in degrees.
     * @param threshold  Minimum roll to trigger a hint (default 2°).
     * @param weight     Penalty multiplier per degree beyond threshold (default 3).
     * @return Pair(penalty: Int, hint: RotateHint?) — subtract penalty from score.
     */
    fun rollPenalty(
        rollDeg: Float,
        threshold: Float = 2f,
        weight: Float = 3f
    ): Pair<Int, RotateHint?> {
        val rollAbs = abs(rollDeg)
        if (rollAbs <= threshold) return 0 to null
        val penalty = ((rollAbs - threshold) * weight).toInt().coerceAtMost(30)
        return penalty to RotateHint(degrees = -rollDeg)
    }

    // ─── Geometry Helpers ────────────────────────────────────────────

    /** Squared euclidean distance between two normalised points. */
    fun dist2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return dx * dx + dy * dy
    }

    /** Distance from a point to the line y = x (main diagonal), normalised. */
    fun distToMainDiagonal(x: Float, y: Float): Float =
        abs(y - x) / SQRT2

    /** Distance from a point to the line y = 1−x (anti diagonal), normalised. */
    fun distToAntiDiagonal(x: Float, y: Float): Float =
        abs(y - (1f - x)) / SQRT2

    // ─── Roll-only Fallback Score ────────────────────────────────────

    /**
     * Fallback score used when no subject is detected.
     * Based purely on roll quality so auto-ranking can still differentiate.
     *
     * Returns 50 when level, decreasing as roll increases.
     */
    fun rollOnlyScore(rollDeg: Float): Int {
        val rollAbs = abs(rollDeg)
        return (50 - rollAbs * 2.5f).toInt().coerceIn(10, 50)
    }
}
