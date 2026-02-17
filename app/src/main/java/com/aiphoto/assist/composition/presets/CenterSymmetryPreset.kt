package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Center / Symmetry — place subject dead center.
 * Best for architecture, doorways, corridors, symmetric scenes.
 */
class CenterSymmetryPreset : Preset {
    override val id = "center"
    override val displayName = "Chính giữa / Đối xứng"

    override fun applicability(f: FrameFeatures): Float = 0.4f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val hints = mutableListOf<Hint>()
        var score: Int
        val sb = f.subjectBox

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            val dx = 0.5f - cx
            val dy = 0.5f - cy
            val dist = sqrt(ScoringUtils.dist2(cx, cy, 0.5f, 0.5f))
            score = ScoringUtils.distanceScore(dist, maxDist = 0.35f)

            if (abs(dx) > 0.02f || abs(dy) > 0.02f) {
                hints += MoveHint(dx, dy)
                hints += TextHint("Canh chủ thể vào giữa để tạo cảm giác đối xứng")
            }
        } else {
            score = ScoringUtils.rollOnlyScore(f.rollDeg)
            hints += TextHint("Hợp với kiến trúc/cửa/hành lang")
        }

        // Center/symmetry is very sensitive to tilt
        val (penalty, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg, threshold = 1.5f, weight = 4f)
        score = (score - penalty).coerceAtLeast(0)
        rotateHint?.let { hints += it }

        return Evaluation(
            score,
            hints.sortedByDescending { it.priority },
            OverlaySpec.Targets(points = listOf(0.5f to 0.5f))
        )
    }
}
