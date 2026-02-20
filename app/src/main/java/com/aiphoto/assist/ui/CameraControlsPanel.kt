package com.aiphoto.assist.ui

import androidx.camera.core.Camera
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact camera controls panel with Zoom and Exposure sliders.
 *
 * - **Zoom**: 0..1 linear zoom (mapped to device min–max ratio by CameraX)
 * - **Exposure (EV)**: Integer steps within device-reported range
 *
 * @param camera       CameraX Camera instance (nullable until ready)
 * @param visible      Whether the panel is shown
 */
@Composable
fun CameraControlsPanel(
    camera: Camera?,
    visible: Boolean
) {
    if (camera == null) return

    val cameraInfo = camera.cameraInfo
    val cameraControl = camera.cameraControl

    // ── Zoom ──────────────────────────────────────────────────
    var zoomLinear by remember { mutableFloatStateOf(0f) }

    // Observe actual zoom ratio for display label
    val zoomState = cameraInfo.zoomState.value
    val currentZoomRatio = zoomState?.zoomRatio ?: 1f

    // ── Exposure ──────────────────────────────────────────────
    val evRange = cameraInfo.exposureState.exposureCompensationRange
    val evStep = cameraInfo.exposureState.exposureCompensationStep.toFloat()
    val evMin = evRange.lower
    val evMax = evRange.upper
    var evValue by remember { mutableIntStateOf(0) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Zoom slider ──────────────────────────────────
            ControlRow(
                icon = "🔍",
                label = "Zoom",
                valueText = String.format("%.1fx", currentZoomRatio)
            ) {
                Slider(
                    value = zoomLinear,
                    onValueChange = { value ->
                        zoomLinear = value
                        cameraControl.setLinearZoom(value)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = sliderColors()
                )
            }

            // ── Exposure slider ──────────────────────────────
            if (evMin < evMax) {
                ControlRow(
                    icon = "☀️",
                    label = "EV",
                    valueText = if (evValue >= 0) "+${String.format("%.1f", evValue * evStep)}"
                                else String.format("%.1f", evValue * evStep)
                ) {
                    Slider(
                        value = evValue.toFloat(),
                        onValueChange = { value ->
                            val ev = value.toInt()
                            evValue = ev
                            cameraControl.setExposureCompensationIndex(ev)
                        },
                        valueRange = evMin.toFloat()..evMax.toFloat(),
                        steps = (evMax - evMin - 1).coerceAtLeast(0),
                        modifier = Modifier.weight(1f),
                        colors = sliderColors()
                    )
                }
            }
        }
    }
}

// ─── Control Row ─────────────────────────────────────────────────

@Composable
private fun ControlRow(
    icon: String,
    label: String,
    valueText: String,
    slider: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(icon, fontSize = 16.sp)
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )
        slider()
        Text(
            text = valueText,
            color = Color(0xFF64FFDA),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(42.dp)
        )
    }
}

// ─── Slider Colors ───────────────────────────────────────────────

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFF64FFDA),
    activeTrackColor = Color(0xFF64FFDA),
    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
)
