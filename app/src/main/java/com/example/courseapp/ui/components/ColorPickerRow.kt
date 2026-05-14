package com.example.courseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val presetColors = listOf(
    Color(0xFF5BA3E6) to "浅蓝",
    Color(0xFF3A6FC4) to "深蓝",
    Color(0xFF7E57C2) to "紫色",
    Color(0xFF5C3D9E) to "深紫",
    Color(0xFF26A69A) to "青绿",
    Color(0xFFE8883A) to "橙色",
    Color(0xFF5C7A99) to "灰蓝",
    Color(0xFF3F51B5) to "靛蓝",
    Color(0xFFEF5350) to "红色",
    Color(0xFF66BB6A) to "绿色"
)

@Composable
fun ColorPickerRow(
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "卡片颜色",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Auto" option — use type default
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(
                        width = if (selectedColor == null) 2.5.dp else 1.5.dp,
                        color = if (selectedColor == null) Color(0xFF2196F3) else Color(0xFFBDBDBD),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(null) },
                contentAlignment = Alignment.Center
            ) {
                Text("A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBDBDBD))
            }

            presetColors.forEach { (color, _) ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
