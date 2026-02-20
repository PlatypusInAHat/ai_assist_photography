package com.aiphoto.assist.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bottom HUD: animated score progress bar + primary/secondary hint text. */
@Composable
fun BottomHUD(score: Int, primary: String, secondary: String, modifier: Modifier = Modifier) {
    val animatedProgress by
            animateFloatAsState(
                    targetValue = score.coerceIn(0, 100) / 100f,
                    animationSpec = tween(300),
                    label = "hudProgress"
            )

    val scoreColor by
            animateColorAsState(
                    targetValue =
                            when {
                                score >= 80 -> Color(0xFF4CAF50)
                                score >= 60 -> Color(0xFFFFB74D)
                                else -> Color(0xFFFF6B6B)
                            },
                    label = "hudScoreColor"
            )

    Column(
            modifier =
                    modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Score bar
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = scoreColor,
                    trackColor = Color.White.copy(alpha = 0.15f),
            )
            Text(
                    text = "$score",
                    color = scoreColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
        }

        // Primary hint
        if (primary.isNotBlank()) {
            Text(
                    text = primary,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
            )
        }

        // Secondary hint
        if (secondary.isNotBlank()) {
            Text(text = secondary, color = Color(0xFFFFB74D), fontSize = 12.sp)
        }
    }
}
