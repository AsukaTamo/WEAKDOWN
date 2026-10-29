package com.example.courseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.CourseColorPreset

/**
 * 卡片颜色选择器（添加课程 / 编辑课程共用）。
 *
 * - 左侧「自动」= 跟随课程类型默认色，不需要用户再挑一次；
 * - 右侧 6 个色卡是液态玻璃色卡的迷你预览：浅色模式看淡彩底、深色模式看低透明染色；
 * - 下方提示当前生效的是类型默认色还是自定义色，避免"选了色但不知道什么时候生效"。
 */
@Composable
fun CourseColorPicker(
    selected: CourseColorPreset?,
    typeDefault: CourseColorPreset,
    isDarkMode: Boolean,
    labelColor: Color,
    accentColor: Color,
    onSelected: (CourseColorPreset?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAuto = selected == null
    val hint = if (selected == null) {
        "跟随课程类型默认色：${typeDefault.displayName}"
    } else {
        "自定义：${selected.displayName}"
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "卡片颜色",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .heightIn(min = 28.dp)
                    .elasticPress(RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isAuto) accentColor.copy(alpha = 0.14f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isAuto) accentColor.copy(alpha = 0.55f) else labelColor.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(9.dp)
                    )
                    .clickable { onSelected(null) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAuto) "自动 · ${typeDefault.displayName}" else "自动",
                    fontSize = 11.sp,
                    fontWeight = if (isAuto) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isAuto) accentColor else labelColor,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CourseColorPreset.entries.forEach { preset ->
                PresetSwatch(
                    preset = preset,
                    selected = selected == preset,
                    isDarkMode = isDarkMode,
                    labelColor = labelColor,
                    onClick = { onSelected(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            fontSize = 11.sp,
            color = labelColor.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PresetSwatch(
    preset: CourseColorPreset,
    selected: Boolean,
    isDarkMode: Boolean,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 深色模式给色卡垫一层深色底，28% 的染色才看得出是"深色玻璃"而不是黑块
    val swatchBase = if (isDarkMode) Color(0xFF14141A) else Color.White
    val swatchFill = if (isDarkMode) preset.darkFill else preset.lightFill
    val swatchInk = if (isDarkMode) preset.darkInk else preset.lightInk
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .elasticPress(shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "卡片颜色 ${preset.displayName}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(shape)
                .background(swatchBase)
                .background(swatchFill)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) swatchInk else swatchInk.copy(alpha = 0.28f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = swatchInk,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = preset.displayName,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) labelColor else labelColor.copy(alpha = 0.72f),
            maxLines = 1,
            softWrap = false
        )
    }
}
