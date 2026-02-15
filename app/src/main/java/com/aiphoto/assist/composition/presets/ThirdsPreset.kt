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
        var score = 70

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            val nearest = targets.minBy { (x, y) -> dist2(cx, cy, x, y) }
            val dx = nearest.first - cx
            val dy = nearest.second - cy
            val dist = sqrt(dist2(cx, cy, nearest.first, nearest.second))
            score = (100 - (dist * 120)).toInt().coerceIn(0, 100)

            if (abs(dx) > 0.03f || abs(dy) > 0.03f) {
                hints += MoveHint(dxNorm = dx, dyNorm = dy)
                hints += TextHint("Dịch chủ thể về điểm 1/3 gần nhất")
            }
        } else {
            hints += TextHint("Đưa chủ thể vào một trong 4 điểm 1/3")
        }

        // Leveling bonus/penalty
        val rollAbs = abs(f.rollDeg)
        if (rollAbs > 2f) {
            hints += RotateHint(degrees = -f.rollDeg)
            score = (score - (rollAbs * 3)).toInt().coerceIn(0, 100)
        }

        return Evaluation(
            score = score,
            hints = hints.sortedByDescending { it.priority },
            overlay = OverlaySpec.Grid(GridType.THIRDS)
        )
    }

    private fun dist2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return dx * dx + dy * dy
    }
}
