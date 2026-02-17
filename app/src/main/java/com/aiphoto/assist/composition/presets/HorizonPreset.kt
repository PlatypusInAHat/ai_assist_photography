package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs

/**
 * Horizon 1/3 — place the horizon line at 1/3 or 2/3 for landscape shots.
 * High applicability when a horizon is detected.
 */
class HorizonPreset : Preset {
    override val id = "horizon"
    override val displayName = "Chân trời 1/3"

    override fun applicability(f: FrameFeatures): Float =
        if (f.horizonYNorm != null) 0.9f else 0.1f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val hy = f.horizonYNorm ?: return Evaluation(
            ScoringUtils.rollOnlyScore(f.rollDeg),
            listOf(TextHint("Không thấy đường chân trời rõ")),
            OverlaySpec.Grid(GridType.THIRDS)
        )

        val candidates = listOf(1f / 3f, 2f / 3f)
        val nearest = candidates.minBy { abs(hy - it) }
        val delta = abs(nearest - hy)

        // Use distanceScore with 1-D distance
        var score = ScoringUtils.distanceScore(delta, maxDist = 0.35f)
        val hints = mutableListOf<Hint>()

        if (delta > 0.03f) {
            val direction = nearest - hy   // positive = move down, negative = move up
            hints += MoveHint(dxNorm = 0f, dyNorm = direction)
            hints += TextHint("Đưa đường chân trời về 1/3 để ảnh \"thoáng\" hơn")
        }

        // Roll penalty — horizon shots are very sensitive to tilt
        val (penalty, rotateHint) = ScoringUtils.rollPenalty(f.rollDeg, threshold = 1.5f, weight = 4f)
        score = (score - penalty).coerceAtLeast(0)
        rotateHint?.let { hints += it }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Horizon(yNorm = hy))
    }
}
