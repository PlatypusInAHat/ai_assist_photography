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
        var score = 68

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            val nearest = targets.minBy { (x, y) -> (cx - x) * (cx - x) + (cy - y) * (cy - y) }
            val dx = nearest.first - cx
            val dy = nearest.second - cy
            val dist = sqrt(
                (cx - nearest.first) * (cx - nearest.first) +
                (cy - nearest.second) * (cy - nearest.second)
            )
            score = (100 - (dist * 115)).toInt().coerceIn(0, 100)

            if (abs(dx) > 0.03f || abs(dy) > 0.03f) {
                hints += MoveHint(dx, dy)
                hints += TextHint("Đặt chủ thể theo Phi Grid để bố cục \"mềm\" hơn 1/3")
            }
        } else {
            hints += TextHint("Thử Phi Grid cho ảnh lifestyle/food")
        }

        // Leveling penalty
        val rollAbs = abs(f.rollDeg)
        if (rollAbs > 2f) {
            hints += RotateHint(degrees = -f.rollDeg)
            score = (score - (rollAbs * 2.5f)).toInt().coerceIn(0, 100)
        }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Grid(GridType.PHI))
    }
}
