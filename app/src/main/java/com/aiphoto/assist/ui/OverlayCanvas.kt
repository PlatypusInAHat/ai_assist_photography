package com.aiphoto.assist.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aiphoto.assist.composition.GridType
import com.aiphoto.assist.composition.OverlayState

// Colors for overlay
private val GridColor = Color.White.copy(alpha = 0.45f)
private val GridIntersectionColor = Color(0xFF64FFDA).copy(alpha = 0.6f)
private val DiagonalColor = Color.White.copy(alpha = 0.35f)
private val HorizonColor = Color.Yellow.copy(alpha = 0.7f)
private val TargetColor = Color(0xFF64FFDA).copy(alpha = 0.8f)
private val LevelIndicatorGood = Color(0xFF4CAF50).copy(alpha = 0.8f)
private val LevelIndicatorBad = Color(0xFFFF6B6B).copy(alpha = 0.8f)
private val SubjectBoxColor = Color(0xFF64FFDA).copy(alpha = 0.7f)
private val MoveArrowColor = Color(0xFF4CAF50).copy(alpha = 0.8f)

/**
 * Full-screen canvas overlay driven by [OverlayState].
 */
@Composable
fun CoachOverlay(state: OverlayState, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // Grid
        state.grid?.let { drawGrid(it) }

        // Diagonals
        if (state.showDiagonals) drawDiagonals()

        // Horizon
        state.horizonYNorm?.let { drawHorizonLine(it) }

        // Targets
        if (state.targetPoints.isNotEmpty()) drawTargets(state.targetPoints)

        // Subject bounding box
        state.subjectBox?.let { drawSubjectBox(it) }

        // Move arrow
        state.moveDxDyNorm?.let { (dx, dy) -> drawMoveArrow(dx, dy) }

        // Level indicator
        drawLevelIndicator(state.rollDeg)
    }
}

// ─── Move Arrow ──────────────────────────────────────────────────

private fun DrawScope.drawMoveArrow(dxNorm: Float, dyNorm: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val scale = minOf(size.width, size.height) * 0.25f
    val ex = cx + dxNorm * scale
    val ey = cy + dyNorm * scale

    // Arrow line
    drawLine(MoveArrowColor, Offset(cx, cy), Offset(ex, ey), strokeWidth = 5f, cap = StrokeCap.Round)

    // Arrow head (circle)
    drawCircle(MoveArrowColor, radius = 10f, center = Offset(ex, ey))
}

// ─── Subject Bounding Box ────────────────────────────────────────

private fun DrawScope.drawSubjectBox(box: RectF) {
    val left = size.width * box.left
    val top = size.height * box.top
    val right = size.width * box.right
    val bottom = size.height * box.bottom
    val w = right - left
    val h = bottom - top

    // Dashed rectangle
    drawRect(
        color = SubjectBoxColor,
        topLeft = Offset(left, top),
        size = Size(w, h),
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    )

    // Corner brackets
    val cornerLen = minOf(w, h) * 0.15f
    val cs = 3.5f
    val c = SubjectBoxColor

    drawLine(c, Offset(left, top), Offset(left + cornerLen, top), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(left, top), Offset(left, top + cornerLen), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(right, top), Offset(right - cornerLen, top), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(right, top), Offset(right, top + cornerLen), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(left, bottom), Offset(left + cornerLen, bottom), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(left, bottom), Offset(left, bottom - cornerLen), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(right, bottom), Offset(right - cornerLen, bottom), cs, cap = StrokeCap.Round)
    drawLine(c, Offset(right, bottom), Offset(right, bottom - cornerLen), cs, cap = StrokeCap.Round)

    // Center crosshair
    val ccx = left + w / 2f
    val ccy = top + h / 2f
    val arm = 6f
    drawLine(c.copy(alpha = 0.5f), Offset(ccx - arm, ccy), Offset(ccx + arm, ccy), 1.5f)
    drawLine(c.copy(alpha = 0.5f), Offset(ccx, ccy - arm), Offset(ccx, ccy + arm), 1.5f)
}

// ─── Grid ────────────────────────────────────────────────────────

private fun DrawScope.drawGrid(type: GridType) {
    val w = size.width; val h = size.height
    val xs = when (type) {
        GridType.THIRDS -> listOf(w / 3f, 2f * w / 3f)
        GridType.PHI -> listOf(w * (1f - 0.618f), w * 0.618f)
    }
    val ys = when (type) {
        GridType.THIRDS -> listOf(h / 3f, 2f * h / 3f)
        GridType.PHI -> listOf(h * (1f - 0.618f), h * 0.618f)
    }

    xs.forEach { x -> drawLine(GridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f) }
    ys.forEach { y -> drawLine(GridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f) }

    xs.forEach { x ->
        ys.forEach { y ->
            drawCircle(GridIntersectionColor, 8f, Offset(x, y), style = Stroke(2f))
            drawCircle(GridIntersectionColor.copy(alpha = 0.3f), 4f, Offset(x, y))
        }
    }
}

// ─── Diagonal ────────────────────────────────────────────────────

private fun DrawScope.drawDiagonals() {
    drawLine(DiagonalColor, Offset(0f, 0f), Offset(size.width, size.height), 1.5f, cap = StrokeCap.Round)
    drawLine(DiagonalColor, Offset(size.width, 0f), Offset(0f, size.height), 1.5f, cap = StrokeCap.Round)
}

// ─── Horizon ─────────────────────────────────────────────────────

private fun DrawScope.drawHorizonLine(yNorm: Float) {
    val y = size.height * yNorm
    drawLine(HorizonColor, Offset(0f, y), Offset(size.width, y), 2.5f, cap = StrokeCap.Round)
    val ref = Color.White.copy(alpha = 0.2f)
    drawLine(ref, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), 1f)
    drawLine(ref, Offset(0f, 2f * size.height / 3f), Offset(size.width, 2f * size.height / 3f), 1f)
}

// ─── Targets ─────────────────────────────────────────────────────

private fun DrawScope.drawTargets(points: List<Pair<Float, Float>>) {
    points.forEach { (xN, yN) ->
        val x = size.width * xN; val y = size.height * yN
        drawCircle(TargetColor, 20f, Offset(x, y), style = Stroke(2f))
        drawCircle(TargetColor.copy(alpha = 0.5f), 6f, Offset(x, y))
        val arm = 28f
        drawLine(TargetColor.copy(alpha = 0.4f), Offset(x - arm, y), Offset(x + arm, y), 1f)
        drawLine(TargetColor.copy(alpha = 0.4f), Offset(x, y - arm), Offset(x, y + arm), 1f)
    }
}

// ─── Level Indicator ─────────────────────────────────────────────

private fun DrawScope.drawLevelIndicator(rollDeg: Float) {
    val isLevel = kotlin.math.abs(rollDeg) < 1.5f
    val color = if (isLevel) LevelIndicatorGood else LevelIndicatorBad
    val cx = size.width / 2f; val cy = size.height / 2f
    val hw = size.width * 0.15f
    val rad = Math.toRadians(rollDeg.toDouble()).toFloat()
    val cos = kotlin.math.cos(rad); val sin = kotlin.math.sin(rad)
    drawLine(color, Offset(cx - hw * cos, cy - hw * sin), Offset(cx + hw * cos, cy + hw * sin), 3f, cap = StrokeCap.Round)
    drawCircle(color, 4f, Offset(cx, cy))
}
