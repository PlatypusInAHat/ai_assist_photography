package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.MoveHint
import com.aiphoto.assist.composition.hints.TextHint

/**
 * Leading Lines preset — places subject along diagonals and scores based on diagonal proximity of
 * the subject.
 */
class LeadingLinesPreset : Preset {
    override val id = "leading"
    override val displayName = "Đường dẫn"

    override fun applicability(f: FrameFeatures): Float {
        // Slightly prefer when no face is detected (landscape/street)
        return if (f.faceYaw == null && f.subjectBox != null) 0.45f else 0.2f
    }

    override fun evaluate(f: FrameFeatures): Evaluation {
        val hints = mutableListOf<com.aiphoto.assist.composition.hints.Hint>()
        var score = 50

        val box = f.subjectBox
        if (box != null) {
            val cx = (box.left + box.right) / 2f
            val cy = (box.top + box.bottom) / 2f

            // Score: how close is subject to either diagonal?
            // Diagonal 1: y = x  → distance = |cx - cy| / sqrt(2)
            // Diagonal 2: y = 1-x → distance = |cx + cy - 1| / sqrt(2)
            val d1 = kotlin.math.abs(cx - cy) / 1.414f
            val d2 = kotlin.math.abs(cx + cy - 1f) / 1.414f
            val minDist = minOf(d1, d2)

            // Score: closer to diagonal = higher score
            score = (100 - (minDist * 300).toInt()).coerceIn(30, 100)

            // Penalty for level
            val (rollPen, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg)
            score = (score - rollPen).coerceIn(0, 100)
            rotateHint?.let { hints += it }

            // Move hint toward nearest diagonal
            if (minDist > 0.05f) {
                val dx = if (d1 < d2) (cy - cx) * 0.5f else (1f - cy - cx) * 0.5f
                val dy = 0f
                hints += MoveHint(dx, dy)
            }
        }

        hints += TextHint("Đặt chủ thể theo đường dẫn chéo", priority = 5)

        return Evaluation(
                score = score,
                hints = hints.sortedByDescending { it.priority },
                overlay = OverlaySpec.Diagonal()
        )
    }
}
