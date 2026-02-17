package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Golden Ratio / Phi Grid — softer, more organic feel than thirds.
 * Uses phi (0.618) instead of 1/3 for grid lines.
 */
class PhiGridPreset : Preset {
    override val id = "phi"
    override val displayName = "Tỉ lệ vàng (Phi Grid)"

    override fun applicability(f: FrameFeatures): Float = 0.5f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val phi = 0.618f
        val a = 1f - phi // 0.382
        val targets = listOf(a to a, phi to a, a to phi, phi to phi)

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
                hints += MoveHint(dx, dy)
                hints += TextHint("Đặt chủ thể theo Phi Grid để bố cục \"mềm\" hơn 1/3")
            }
        } else {
            score = ScoringUtils.rollOnlyScore(f.rollDeg)
            hints += TextHint("Thử Phi Grid cho ảnh lifestyle/food")
        }

        // Roll penalty
        val (penalty, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg, threshold = 2f, weight = 2.5f)
        score = (score - penalty).coerceAtLeast(0)
        rotateHint?.let { hints += it }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Grid(GridType.PHI))
    }
}
