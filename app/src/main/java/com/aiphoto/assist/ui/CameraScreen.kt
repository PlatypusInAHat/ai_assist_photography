package com.aiphoto.assist.ui

import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.aiphoto.assist.vision.FaceDetectorWrapper
import com.aiphoto.assist.vision.HorizonDetector
import com.aiphoto.assist.vision.SubjectDetectorWrapper
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Main camera screen with preview, overlay guides, score, preset picker,
 * shutter button, and capture review.
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

    // Capture state
    var showFlash by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedScore by remember { mutableIntStateOf(0) }
    var capturedPresetName by remember { mutableStateOf("") }

    // Haptic tracking — vibrate when score transitions to ≥80
    var wasAbove80 by remember { mutableStateOf(false) }

    // Sensor
    val levelSensor = remember { LevelSensor(ctx) }
    DisposableEffect(Unit) {
        levelSensor.start()
        onDispose { levelSensor.stop() }
    }

    // Vision detectors
    val horizonDetector = remember { HorizonDetector() }
    val faceDetector = remember { FaceDetectorWrapper() }
    val subjectDetector = remember { SubjectDetectorWrapper() }

    DisposableEffect(Unit) {
        onDispose {
            faceDetector.close()
            subjectDetector.close()
        }
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

    // Haptic buzz when score crosses 80
    val currentScore = evaluation?.score ?: 0
    LaunchedEffect(currentScore) {
        val isAbove80 = currentScore >= 80
        if (isAbove80 && !wasAbove80) {
            // Vibrate — short buzz
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = ctx.getSystemService(VibratorManager::class.java)
                    vm?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val v = ctx.getSystemService(Vibrator::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } catch (_: Exception) { }
        }
        wasAbove80 = isAbove80
    }

    // Flash auto-dismiss
    LaunchedEffect(showFlash) {
        if (showFlash) {
            delay(200)
            showFlash = false
        }
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
                        horizonDetector = horizonDetector,
                        faceDetector = faceDetector,
                        subjectDetector = subjectDetector,
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

        // ─── Flash overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // ─── Top Bar: Preset name + Score ────────────────────
        TopInfoBar(
            presetName = currentPreset?.displayName ?: "Auto",
            score = evaluation?.score,
            rollDeg = rollDeg,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ─── Bottom: Hints + Shutter + Preset Picker ─────────
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

            Spacer(Modifier.height(16.dp))

            // Shutter button
            ShutterButton(
                score = evaluation?.score,
                onClick = {
                    showFlash = true
                    capturedScore = evaluation?.score ?: 0
                    capturedPresetName = currentPreset?.displayName ?: "Auto"
                    CameraHelper.capturePhoto(
                        context = ctx,
                        onSaved = { uri -> capturedPhotoUri = uri }
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

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

        // ─── Capture Review Sheet ────────────────────────────
        if (capturedPhotoUri != null) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                CaptureReviewSheet(
                    photoUri = capturedPhotoUri,
                    score = capturedScore,
                    presetName = capturedPresetName,
                    onDismiss = { capturedPhotoUri = null }
                )
            }
        }
    }
}

// ─── Shutter Button ──────────────────────────────────────────────

@Composable
private fun ShutterButton(
    score: Int?,
    onClick: () -> Unit
) {
    // Ring color based on score
    val ringColor = when {
        score == null -> Color.White.copy(alpha = 0.5f)
        score >= 80 -> Color(0xFF4CAF50)    // green
        score >= 60 -> Color(0xFFFFB74D)    // amber
        else -> Color(0xFFFF6B6B)           // red
    }

    // Subtle pulse when score >= 80
    val scale by animateFloatAsState(
        targetValue = if (score != null && score >= 80) 1.05f else 1f,
        animationSpec = tween(300),
        label = "shutterScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(72.dp)
            .border(4.dp, ringColor, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner dot
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.6f))
        )
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
    // Animated score counter
    val animatedScore by animateIntAsState(
        targetValue = score ?: 0,
        animationSpec = tween(durationMillis = 300),
        label = "scoreAnim"
    )

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
                    animatedScore >= 80 -> Color(0xFF4CAF50)
                    animatedScore >= 60 -> Color(0xFFFFB74D)
                    else -> Color(0xFFFF6B6B)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scoreColor.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$animatedScore",
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
