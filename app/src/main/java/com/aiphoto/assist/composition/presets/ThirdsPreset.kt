package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rule of Thirds — the most universal composition rule.
 * Guides subject placement to one of 4 power-points at 1/3 intersections.
 */
class ThirdsPreset : Preset {
    override val id = "thirds"
    override val displayName = "1/3 (Rule of Thirds)"

    override fun applicability(f: FrameFeatures): Float = 0.6f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val targets = listOf(
            1f / 3f to 1f / 3f,
            2f / 3f to 1f / 3f,
            1f / 3f to 2f / 3f,
            2f / 3f to 2f / 3f
        )
        val sb = f.subjectBox
        val hints = mutableListOf<Hint>()
        var score: Int

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            val nearest = targets.minBy { (x, y) -> ScoringUtils.dist2(cx, cy, x, y) }
            val dx = nearest.first - cx
            val dy = nearest.second - cy
            val dist = sqrt(ScoringUtils.dist2(cx, cy, nearest.first, nearest.second))
            score = ScoringUtils.distanceScore(dist, maxDist = 0.45f)

            if (abs(dx) > 0.03f || abs(dy) > 0.03f) {
                hints += MoveHint(dxNorm = dx, dyNorm = dy)
                hints += TextHint("Dịch chủ thể về điểm 1/3 gần nhất")
            }
        } else {
            score = ScoringUtils.rollOnlyScore(f.rollDeg)
            hints += TextHint("Đưa chủ thể vào một trong 4 điểm 1/3")
        }

        // Roll penalty
        val (penalty, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg, threshold = 2f, weight = 3f)
        score = (score - penalty).coerceAtLeast(0)
        rotateHint?.let { hints += it }

        return Evaluation(
            score = score,
            hints = hints.sortedByDescending { it.priority },
            overlay = OverlaySpec.Grid(GridType.THIRDS)
        )
    }
}
