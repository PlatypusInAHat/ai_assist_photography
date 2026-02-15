package com.aiphoto.assist.composition.presets

import com.aiphoto.assist.composition.*
import com.aiphoto.assist.composition.hints.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Portrait Lookroom — leave space in the direction the subject is looking.
 * High applicability when both a face and face-yaw are detected.
 */
class LookroomPortraitPreset : Preset {
    override val id = "lookroom"
    override val displayName = "Portrait: hướng nhìn"

    override fun applicability(f: FrameFeatures): Float =
        if (f.subjectBox != null && f.faceYaw != null) 0.95f else 0.2f

    override fun evaluate(f: FrameFeatures): Evaluation {
        val sb = f.subjectBox ?: return Evaluation(
            60,
            listOf(TextHint("Cần nhận diện khuôn mặt")),
            OverlaySpec.Grid(GridType.PHI)
        )
        val yaw = f.faceYaw ?: 0f

        // yaw > 0: looking right => leave space on right => place subject left
        val targetX = when {
            yaw > 0.1f -> 0.38f
            yaw < -0.1f -> 0.62f
            else -> 0.5f
        }
        val targetY = 0.38f // eye line near 1/3 top / phi

        val cx = sb.centerX()
        val cy = sb.centerY()
        val dx = targetX - cx
        val dy = targetY - cy
        val dist = sqrt(dx * dx + dy * dy)
        val score = (100 - dist * 150).toInt().coerceIn(0, 100)

        val hints = mutableListOf<Hint>()
        if (abs(dx) > 0.03f || abs(dy) > 0.03f) {
            hints += MoveHint(dx, dy)
            hints += TextHint("Chừa khoảng theo hướng nhìn để ảnh tự nhiên hơn")
        }

        // Leveling
        val rollAbs = abs(f.rollDeg)
        if (rollAbs > 2f) {
            hints += RotateHint(-f.rollDeg)
        }

        return Evaluation(score, hints.sortedByDescending { it.priority }, OverlaySpec.Grid(GridType.PHI))
    }
}
