package com.aiphoto.assist.ui

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aiphoto.assist.camera.CameraHelper
import com.aiphoto.assist.camera.FrameAnalyzer
import com.aiphoto.assist.composition.Evaluation
import com.aiphoto.assist.composition.Preset
import com.aiphoto.assist.composition.PresetManager
import com.aiphoto.assist.composition.hints.MoveHint
import com.aiphoto.assist.composition.hints.RotateHint
import com.aiphoto.assist.composition.hints.TextHint
import com.aiphoto.assist.composition.presets.*
import com.aiphoto.assist.sensors.LevelSensor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Main camera screen with preview, overlay guides, score, and preset picker.
 */
@Composable
fun CameraScreen() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var evaluation by remember { mutableStateOf<Evaluation?>(null) }
    var currentPreset by remember { mutableStateOf<Preset?>(null) }
    var selectedPresetId by remember { mutableStateOf<String?>(null) } // null = auto
    var autoMode by remember { mutableStateOf(true) }
    var rollDeg by remember { mutableFloatStateOf(0f) }

    // Sensor
    val levelSensor = remember { LevelSensor(ctx) }
    DisposableEffect(Unit) {
        levelSensor.start()
        onDispose { levelSensor.stop() }
    }

    // Preset manager
    val presetManager = remember {
        PresetManager(
            listOf(
                HorizonPreset(),
                LookroomPortraitPreset(),
                ThirdsPreset(),
                PhiGridPreset(),
                CenterSymmetryPreset(),
                DiagonalPreset()
            )
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ─── Camera Preview ───────────────────────────────────
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
                CameraHelper.startCamera(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    analyzer = FrameAnalyzer(
                        levelSensor = levelSensor,
                        presetManager = presetManager,
                        selectedPresetId = { selectedPresetId },
                        autoMode = { autoMode },
                        onResult = { p, e ->
                            currentPreset = p
                            evaluation = e
                            rollDeg = levelSensor.rollDeg()
                        }
                    )
                )
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // ─── Overlay Canvas ──────────────────────────────────
        OverlayCanvas(evaluation = evaluation, rollDeg = rollDeg)

        // ─── Top Bar: Preset name + Score ────────────────────
        TopInfoBar(
            presetName = currentPreset?.displayName ?: "Auto",
            score = evaluation?.score,
            rollDeg = rollDeg,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ─── Bottom: Hints + Preset Picker ───────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hint text
            HintPanel(evaluation = evaluation)

            Spacer(Modifier.height(12.dp))

            // Preset picker chips
            PresetPicker(
                selectedId = selectedPresetId,
                autoMode = autoMode,
                onSelect = { id ->
                    if (id == null) {
                        autoMode = true
                        selectedPresetId = null
                    } else {
                        autoMode = false
                        selectedPresetId = id
                    }
                }
            )
        }
    }
}

// ─── Top Info Bar ────────────────────────────────────────────────

@Composable
private fun TopInfoBar(
    presetName: String,
    score: Int?,
    rollDeg: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preset name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF64FFDA),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = presetName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Score badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Roll indicator
            val rollText = if (abs(rollDeg) < 1.5f) "✓ Cân" else "${rollDeg.roundToInt()}°"
            val rollColor = if (abs(rollDeg) < 1.5f) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
            Text(rollText, color = rollColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            // Score
            if (score != null) {
                val scoreColor = when {
                    score >= 80 -> Color(0xFF4CAF50)
                    score >= 60 -> Color(0xFFFFB74D)
                    else -> Color(0xFFFF6B6B)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scoreColor.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$score",
                        color = scoreColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Hint Panel ──────────────────────────────────────────────────

@Composable
private fun HintPanel(evaluation: Evaluation?) {
    if (evaluation == null) return

    val hints = evaluation.hints
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Show top text hint
        hints.filterIsInstance<TextHint>().firstOrNull()?.let { hint ->
            Text(
                text = hint.text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Show rotate hint
        hints.filterIsInstance<RotateHint>().firstOrNull()?.let { hint ->
            val dir = if (hint.degrees > 0) "phải" else "trái"
            Text(
                text = "↻ Xoay ${dir} ${abs(hint.degrees).roundToInt()}°",
                color = Color(0xFFFFB74D),
                fontSize = 13.sp
            )
        }

        // Show move hint
        hints.filterIsInstance<MoveHint>().firstOrNull()?.let { hint ->
            val parts = mutableListOf<String>()
            if (abs(hint.dxNorm) > 0.02f) {
                parts += if (hint.dxNorm > 0) "→ Dịch phải ${(abs(hint.dxNorm) * 100).roundToInt()}%"
                else "← Dịch trái ${(abs(hint.dxNorm) * 100).roundToInt()}%"
            }
            if (abs(hint.dyNorm) > 0.02f) {
                parts += if (hint.dyNorm > 0) "↓ Hạ xuống ${(abs(hint.dyNorm) * 100).roundToInt()}%"
                else "↑ Nâng lên ${(abs(hint.dyNorm) * 100).roundToInt()}%"
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString("  ·  "),
                    color = Color(0xFF64FFDA),
                    fontSize = 13.sp
                )
            }
        }
    }
}
