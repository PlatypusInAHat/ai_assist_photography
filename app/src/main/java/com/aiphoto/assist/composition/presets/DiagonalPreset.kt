package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs
import kotlin.math.sqrt

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
        var score = 70
        val sb = f.subjectBox

        if (sb != null) {
            val cx = sb.centerX()
            val cy = sb.centerY()
            // Distance to main diagonal y=x (normalized)
            val dMain = abs(cy - cx) / sqrt(2f)
            // Distance to anti-diagonal y=1-x
            val dAnti = abs(cy - (1f - cx)) / sqrt(2f)
            val d = minOf(dMain, dAnti)

            score = (100 - d * 170).toInt().coerceIn(0, 100)
            if (d > 0.05f) {
                hints += TextHint("Thử xoay/góc máy để chủ thể nằm gần đường chéo")
            }
        } else {
            hints += TextHint("Hợp cho street, travel, food flat-lay")
        }

        // Leveling (diagonals can tolerate more tilt)
        val rollAbs = abs(f.rollDeg)
        if (rollAbs > 5f) {
            hints += RotateHint(degrees = -f.rollDeg)
            score = (score - (rollAbs * 2)).toInt().coerceIn(0, 100)
        }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Diagonal(true))
    }
}
