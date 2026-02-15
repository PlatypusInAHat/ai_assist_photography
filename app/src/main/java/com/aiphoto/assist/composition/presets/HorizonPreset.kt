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
            60,
            listOf(TextHint("Không thấy đường chân trời rõ")),
            OverlaySpec.Grid(GridType.THIRDS)
        )

        val candidates = listOf(1f / 3f, 2f / 3f)
        val nearest = candidates.minBy { abs(hy - it) }
        val delta = nearest - hy

        val base = (100 - abs(delta) * 220).toInt().coerceIn(0, 100)
        val hints = mutableListOf<Hint>()

        if (abs(delta) > 0.03f) {
            hints += MoveHint(dxNorm = 0f, dyNorm = delta)
            hints += TextHint("Đưa đường chân trời về 1/3 để ảnh \"thoáng\" hơn")
        }

        // Leveling via roll
        if (abs(f.rollDeg) > 1.5f) {
            hints += RotateHint(-f.rollDeg)
        }

        return Evaluation(base, hints.sortedByDescending { it.priority }, OverlaySpec.Horizon(yNorm = hy))
    }
}
