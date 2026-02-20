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
import com.aiphoto.assist.composition.PresetId

private val presetOptions = listOf(
    PresetId.AUTO to ("🤖" to "Auto"),
    PresetId.THIRDS to ("🔲" to "1/3"),
    PresetId.PHI to ("🌀" to "Phi"),
    PresetId.CENTER to ("⊕" to "Giữa"),
    PresetId.DIAGONAL to ("⤡" to "Chéo"),
    PresetId.HORIZON to ("🌅" to "Chân trời"),
    PresetId.LEADING to ("↗️" to "Đường dẫn"),
    PresetId.LOOKROOM to ("🧑" to "Portrait"),
)

/**
 * Horizontal scrollable chip row for selecting composition presets.
 */
@Composable
fun PresetPicker(
    selected: PresetId,
    onSelect: (PresetId) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presetOptions.forEach { (preset, emojiLabel) ->
            val (emoji, label) = emojiLabel
            val isSelected = preset == selected
            PresetChip(
                label = label,
                emoji = emoji,
                isSelected = isSelected,
                onClick = { onSelect(preset) }
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF64FFDA).copy(alpha = 0.25f)
        else Color.White.copy(alpha = 0.1f),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF64FFDA).copy(alpha = 0.7f)
        else Color.White.copy(alpha = 0.2f),
        label = "chipBorder"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text = label,
            color = if (isSelected) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
