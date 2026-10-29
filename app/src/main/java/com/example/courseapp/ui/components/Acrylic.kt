package com.example.courseapp.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * 亚克力（Acrylic）质感：半透明底色 + 顶部受光 + 细噪点 + 1dp 描边。
 *
 * 首页星期表头原来只是一块 50% 不透明的底色，壁纸一花就和文字糊在一起。
 * 这里把层数叠到和 Windows Acrylic / 底栏玻璃同一套观感：
 *
 * 1. [tint] 半透明底（alpha 由调用方给，表头用 78%）；
 * 2. 竖向受光渐变：顶部亮、底部压暗，让方块有厚度；
 * 3. 亚克力标志性的细噪点（固定种子的确定性散点，只影响观感，不参与重组）；
 * 4. 1dp 细描边收边。
 *
 * 噪点数量随面积走并封顶 40 个，7 个表头格子加起来也只有几百次 drawCircle，滚动时不会成为瓶颈。
 */
fun Modifier.acrylicSurface(
    tint: Color,
    corner: Dp,
    borderColor: Color,
    isDarkMode: Boolean,
    grainAlpha: Float = if (isDarkMode) 0.05f else 0.035f
): Modifier = drawWithCache {
    val radius = corner.toPx()
    val grain = if (grainAlpha > 0f) acrylicGrain(size.width, size.height) else emptyList()
    val grainColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = grainAlpha)
    val sheen = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = if (isDarkMode) 0.14f else 0.28f),
            0.42f to Color.White.copy(alpha = if (isDarkMode) 0.03f else 0.06f),
            1f to Color.Black.copy(alpha = if (isDarkMode) 0.16f else 0.05f)
        )
    )
    val stroke = 1.dp.toPx()
    onDrawBehind {
        drawRoundRect(color = tint, cornerRadius = CornerRadius(radius, radius))
        drawRoundRect(brush = sheen, cornerRadius = CornerRadius(radius, radius))
        grain.forEach { center ->
            drawCircle(color = grainColor, radius = 0.7f, center = center)
        }
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius((radius - stroke / 2f).coerceAtLeast(0f)),
            style = Stroke(stroke)
        )
    }
}

/** 确定性噪点：同一个尺寸永远生成同一批坐标，不会每帧闪。 */
private fun acrylicGrain(width: Float, height: Float): List<Offset> {
    if (width <= 0f || height <= 0f) return emptyList()
    val count = (width * height / 260f).toInt().coerceIn(12, 40)
    val random = Random((width.toInt() * 31 + height.toInt()).coerceAtLeast(1))
    return List(count) {
        Offset(random.nextFloat() * width, random.nextFloat() * height)
    }
}
