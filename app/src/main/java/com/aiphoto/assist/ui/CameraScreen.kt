package com.aiphoto.assist.ui

import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.Tune
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiphoto.assist.CameraCoachViewModel
import com.aiphoto.assist.camera.CameraHelper
import com.aiphoto.assist.camera.RealtimeCoachEngine
import com.aiphoto.assist.composition.PresetId
import com.aiphoto.assist.composition.PresetManager
import com.aiphoto.assist.composition.presets.*
import com.aiphoto.assist.sensors.LevelSensor
import com.aiphoto.assist.vision.FaceDetectorWrapper
import com.aiphoto.assist.vision.HorizonDetector
import com.aiphoto.assist.vision.SubjectDetectorWrapper
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Main camera screen — driven by [CameraCoachViewModel].
 *
 * Layout (top → bottom):
 *  - Top bar: preset name + roll indicator
 *  - Mode picker chips
 *  - Camera preview + CoachOverlay
 *  - Camera controls panel (zoom / EV)
 *  - BottomHUD (score bar + hints)
 *  - Shutter row + Preset picker
 */
@Composable
fun CameraScreen(vm: CameraCoachViewModel = viewModel()) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsState()

    // Capture state
    var showFlash by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedScore by remember { mutableIntStateOf(0) }
    var capturedPresetName by remember { mutableStateOf("") }

    // Camera controls
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var showControls by remember { mutableStateOf(false) }

    // Haptic tracking
    var wasAbove80 by remember { mutableStateOf(false) }

    // Sensors + detectors (long-lived)
    val levelSensor = remember { LevelSensor(ctx) }
    val horizonDetector = remember { HorizonDetector() }
    val faceDetector = remember { FaceDetectorWrapper() }
    val subjectDetector = remember { SubjectDetectorWrapper() }

    DisposableEffect(Unit) {
        levelSensor.start()
        onDispose {
            levelSensor.stop()
            faceDetector.close()
            subjectDetector.close()
        }
    }

    // Preset manager (includes LeadingLinesPreset)
    val presetManager = remember {
        PresetManager(
            listOf(
                HorizonPreset(),
                LookroomPortraitPreset(),
                LeadingLinesPreset(),
                ThirdsPreset(),
                PhiGridPreset(),
                CenterSymmetryPreset(),
                DiagonalPreset()
            )
        )
    }

    // Haptic buzz when score crosses 80
    LaunchedEffect(state.score) {
        val isAbove80 = state.score >= 80
        if (isAbove80 && !wasAbove80) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm2 = ctx.getSystemService(VibratorManager::class.java)
                    vm2?.defaultVibrator?.vibrate(
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
        if (showFlash) { delay(200); showFlash = false }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ─── Camera Preview ──────────────────────────────────
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
                    analyzer = RealtimeCoachEngine(
                        levelSensor = levelSensor,
                        horizonDetector = horizonDetector,
                        faceDetector = faceDetector,
                        subjectDetector = subjectDetector,
                        presetManager = presetManager,
                        getUserSelected = { state.mode to state.preset },
                        onUpdate = vm::updateFromEngine
                    ),
                    onCameraReady = { camera -> cameraInstance = camera }
                )
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // ─── CoachOverlay ────────────────────────────────────
        CoachOverlay(state = state)

        // ─── Flash overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(150))
        ) {
            Box(Modifier.fillMaxSize().background(Color.White))
        }

        // ─── Top Bar ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Preset name + Roll indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF64FFDA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = state.presetDisplayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                val rollText = if (abs(state.rollDeg) < 1.5f) "✓ Cân" else "${state.rollDeg.roundToInt()}°"
                val rollColor = if (abs(state.rollDeg) < 1.5f) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
                Text(rollText, color = rollColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            // Mode picker
            ModePicker(
                selected = state.mode,
                onSelect = vm::setMode
            )
        }

        // ─── Bottom section ──────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera controls (zoom + EV)
            CameraControlsPanel(camera = cameraInstance, visible = showControls)

            Spacer(Modifier.height(4.dp))

            // Bottom HUD (score bar + hints)
            BottomHUD(
                score = state.score,
                primary = state.primaryText,
                secondary = state.secondaryText
            )

            Spacer(Modifier.height(12.dp))

            // Shutter row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Controls toggle
                IconButton(onClick = { showControls = !showControls }) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "Camera controls",
                        tint = if (showControls) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Shutter button
                ShutterButton(
                    score = state.score,
                    onClick = {
                        showFlash = true
                        capturedScore = state.score
                        capturedPresetName = state.presetDisplayName
                        CameraHelper.capturePhoto(
                            context = ctx,
                            onSaved = { uri -> capturedPhotoUri = uri }
                        )
                    }
                )

                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Preset picker
            PresetPicker(
                selected = state.preset,
                onSelect = vm::setPreset
            )
        }

        // ─── Capture Review ──────────────────────────────────
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
private fun ShutterButton(score: Int, onClick: () -> Unit) {
    val ringColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFB74D)
        else -> Color(0xFFFF6B6B)
    }
    val scale by animateFloatAsState(
        targetValue = if (score >= 80) 1.05f else 1f,
        animationSpec = tween(300), label = "shutterScale"
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
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(ringColor.copy(alpha = 0.6f))
        )
    }
}
