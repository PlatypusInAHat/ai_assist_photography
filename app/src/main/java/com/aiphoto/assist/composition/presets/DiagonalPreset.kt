package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*

/**
 * Diagonal / Dynamic — subject along the main diagonals.
 * Creates dynamic, energetic compositions for street, travel, food flat-lay.
 */
class DiagonalPreset : Preset {
    override val id = "diagonal"
    override val displayName = "Đường chéo (Dynamic)"

    override fun applicability(f: FrameFeatures): Float = 0.35f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val hints = mutableListOf<Hint>()
        var score: Int
        val sb = f.subjectBox

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            val d = minOf(
                ScoringUtils.distToMainDiagonal(cx, cy),
                ScoringUtils.distToAntiDiagonal(cx, cy)
            )
            score = ScoringUtils.distanceScore(d, maxDist = 0.35f)

            if (d > 0.05f) {
                hints += TextHint("Thử xoay/góc máy để chủ thể nằm gần đường chéo")
            }
        } else {
            score = ScoringUtils.rollOnlyScore(f.rollDeg)
            hints += TextHint("Hợp cho street, travel, food flat-lay")
        }

        // Diagonal compositions tolerate more tilt
        val (penalty, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg, threshold = 5f, weight = 2f)
        score = (score - penalty).coerceAtLeast(0)
        rotateHint?.let { hints += it }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Diagonal(true))
    }
}
