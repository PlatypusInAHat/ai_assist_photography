package com.aiphoto.assist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aiphoto.assist.composition.Evaluation
import com.aiphoto.assist.composition.GridType
import com.aiphoto.assist.composition.OverlaySpec

// Colors for overlay
private val GridColor = Color.White.copy(alpha = 0.45f)
private val GridIntersectionColor = Color(0xFF64FFDA).copy(alpha = 0.6f)
private val DiagonalColor = Color.White.copy(alpha = 0.35f)
private val HorizonColor = Color.Yellow.copy(alpha = 0.7f)
private val TargetColor = Color(0xFF64FFDA).copy(alpha = 0.8f)
private val LevelIndicatorGood = Color(0xFF4CAF50).copy(alpha = 0.8f)
private val LevelIndicatorBad = Color(0xFFFF6B6B).copy(alpha = 0.8f)

/**
 * Full-screen canvas overlay for drawing composition guides.
 */
@Composable
fun OverlayCanvas(
    evaluation: Evaluation?,
    rollDeg: Float = 0f
) {
    Canvas(Modifier.fillMaxSize()) {
        if (evaluation == null) return@Canvas

        when (val ov = evaluation.overlay) {
            is OverlaySpec.Grid -> drawGrid(ov.type)
            is OverlaySpec.Diagonal -> drawDiagonals()
            is OverlaySpec.Horizon -> drawHorizonLine(ov.yNorm)
            is OverlaySpec.Targets -> drawTargets(ov.points)
        }

        // Always draw level indicator
        drawLevelIndicator(rollDeg)
    }
}

// ─── Grid (Thirds / Phi) ─────────────────────────────────────────

private fun DrawScope.drawGrid(type: GridType) {
    val w = size.width
    val h = size.height

    val xs = when (type) {
        GridType.THIRDS -> listOf(w / 3f, 2f * w / 3f)
        GridType.PHI -> listOf(w * (1f - 0.618f), w * 0.618f)
    }
    val ys = when (type) {
        GridType.THIRDS -> listOf(h / 3f, 2f * h / 3f)
        GridType.PHI -> listOf(h * (1f - 0.618f), h * 0.618f)
    }

    // Draw grid lines
    xs.forEach { x ->
        drawLine(GridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
    }
    ys.forEach { y ->
        drawLine(GridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
    }

    // Draw intersection power-points
    xs.forEach { x ->
        ys.forEach { y ->
            drawCircle(
                color = GridIntersectionColor,
                radius = 8f,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = GridIntersectionColor.copy(alpha = 0.3f),
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

// ─── Diagonal ────────────────────────────────────────────────────

private fun DrawScope.drawDiagonals() {
    val w = size.width
    val h = size.height
    drawLine(DiagonalColor, Offset(0f, 0f), Offset(w, h), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(DiagonalColor, Offset(w, 0f), Offset(0f, h), strokeWidth = 1.5f, cap = StrokeCap.Round)
}

// ─── Horizon ─────────────────────────────────────────────────────

private fun DrawScope.drawHorizonLine(yNorm: Float) {
    val y = size.height * yNorm
    drawLine(HorizonColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.5f, cap = StrokeCap.Round)

    // Also draw thirds reference lines (faint)
    val third1 = size.height / 3f
    val third2 = 2f * size.height / 3f
    val refColor = Color.White.copy(alpha = 0.2f)
    drawLine(refColor, Offset(0f, third1), Offset(size.width, third1), strokeWidth = 1f)
    drawLine(refColor, Offset(0f, third2), Offset(size.width, third2), strokeWidth = 1f)
}

// ─── Targets ─────────────────────────────────────────────────────

private fun DrawScope.drawTargets(points: List<Pair<Float, Float>>) {
    points.forEach { (xN, yN) ->
        val x = size.width * xN
        val y = size.height * yN
        // Outer ring
        drawCircle(
            color = TargetColor,
            radius = 20f,
            center = Offset(x, y),
            style = Stroke(width = 2f)
        )
        // Inner dot
        drawCircle(
            color = TargetColor.copy(alpha = 0.5f),
            radius = 6f,
            center = Offset(x, y)
        )
        // Crosshair
        val arm = 28f
        drawLine(TargetColor.copy(alpha = 0.4f), Offset(x - arm, y), Offset(x + arm, y), 1f)
        drawLine(TargetColor.copy(alpha = 0.4f), Offset(x, y - arm), Offset(x, y + arm), 1f)
    }
}

// ─── Level Indicator ─────────────────────────────────────────────

private fun DrawScope.drawLevelIndicator(rollDeg: Float) {
    val isLevel = kotlin.math.abs(rollDeg) < 1.5f
    val color = if (isLevel) LevelIndicatorGood else LevelIndicatorBad
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val halfWidth = size.width * 0.15f

    // Short horizontal line at center — rotated by roll
    val rad = Math.toRadians(rollDeg.toDouble()).toFloat()
    val cos = kotlin.math.cos(rad)
    val sin = kotlin.math.sin(rad)

    val x1 = centerX - halfWidth * cos
    val y1 = centerY - halfWidth * sin
    val x2 = centerX + halfWidth * cos
    val y2 = centerY + halfWidth * sin

    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = 3f, cap = StrokeCap.Round)

    // Reference dot at center
    drawCircle(color, radius = 4f, center = Offset(centerX, centerY))
}
