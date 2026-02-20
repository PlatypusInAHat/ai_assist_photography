package com.aiphoto.assist.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Bottom sheet showing the captured photo thumbnail + score badge.
 * Auto-dismisses after 3 seconds if no action taken.
 *
 * @param photoUri  URI of the saved photo (MediaStore content URI).
 * @param score     Score at the time of capture.
 * @param presetName Name of the active preset.
 * @param onDismiss Called when the sheet should close.
 */
@Composable
fun CaptureReviewSheet(
    photoUri: Uri?,
    score: Int,
    presetName: String,
    onDismiss: () -> Unit
) {
    if (photoUri == null) return

    // Auto-dismiss after 3 seconds
    LaunchedEffect(photoUri) {
        delay(3000)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF1A1F25))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Ảnh vừa chụp",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                // Score + Preset info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📸 Đã lưu!",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score badge
                        val scoreColor = when {
                            score >= 80 -> Color(0xFF4CAF50)
                            score >= 60 -> Color(0xFFFFB74D)
                            else -> Color(0xFFFF6B6B)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(scoreColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$score",
                                color = scoreColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = presetName,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }

                // Dismiss button
                TextButton(onClick = onDismiss) {
                    Text("OK", color = Color(0xFF64FFDA), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
