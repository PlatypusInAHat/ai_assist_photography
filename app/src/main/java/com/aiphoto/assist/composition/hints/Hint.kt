package com.aiphoto.assist.composition.hints

/**
 * Sealed interface for composition hints displayed as overlay guidance.
 * Each hint has a priority (higher = more important, shown first).
 */
sealed interface Hint {
    val priority: Int
}

/** Gợi ý xoay máy (degrees: dương = xoay phải, âm = xoay trái) */
data class RotateHint(
    val degrees: Float,
    override val priority: Int = 10
) : Hint

/** Gợi ý dịch chuyển máy/chủ thể (normalized -1..1) */
data class MoveHint(
    val dxNorm: Float,
    val dyNorm: Float,
    override val priority: Int = 9
) : Hint

/** Gợi ý đặt chủ thể vào vị trí cụ thể (normalized 0..1) */
data class PlaceAtHint(
    val xNorm: Float,
    val yNorm: Float,
    override val priority: Int = 8
) : Hint

/** Gợi ý dạng text tự do */
data class TextHint(
    val text: String,
    override val priority: Int = 5
) : Hint
