package com.aiphoto.assist.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aiphoto.assist.composition.CaptureMode

private val modeOptions = listOf(
    CaptureMode.AUTO to ("🤖" to "Auto"),
    CaptureMode.PORTRAIT to ("🧑" to "Portrait"),
    CaptureMode.LANDSCAPE to ("🏞️" to "Phong cảnh"),
    CaptureMode.STREET to ("🏙️" to "Street"),
    CaptureMode.ARCH to ("🏛️" to "Kiến trúc"),
    CaptureMode.FOOD to ("🍜" to "Ẩm thực"),
    CaptureMode.SCAN to ("📄" to "Scan"),
)

/**
 * Horizontal scrollable chip row for selecting capture mode (scene type).
 */
@Composable
fun ModePicker(
    selected: CaptureMode,
    onSelect: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modeOptions.forEach { (mode, emojiLabel) ->
            val (emoji, label) = emojiLabel
            val isSelected = mode == selected

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF8A65).copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.08f),
                label = "modeBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF8A65).copy(alpha = 0.7f)
                else Color.White.copy(alpha = 0.15f),
                label = "modeBorder"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(emoji, fontSize = 13.sp)
                Text(
                    text = label,
                    color = if (isSelected) Color(0xFFFF8A65) else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
