package com.example.courseapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.courseapp.ui.components.elasticPress
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

/**
 * 「我的」页卡片玻璃。
 *
 * 和底栏同一套 Vitro 液态玻璃管线（真实采样 backdrop），但它是"厚磨砂"档：
 * - [Glass.tint] 给到 80% 左右，背景只剩两成，透过卡片只能看到一层模糊色；
 * - `refractionAmount = 0`，不做边缘折射，避免文字边缘被掰弯；
 * - `blurRadius` 提到 16dp，两层高斯把背景彻底揉开，只剩明暗和颜色倾向。
 *
 * 卡片必须画在 backdrop 之外（MainActivity 把 ProfileScreen 放在背景层之上），
 * 否则 Vitro 会采样到自己。
 */
fun profileCardGlass(isDarkMode: Boolean): Glass = Glass.Regular.copy(
    appearance = if (isDarkMode) GlassAppearance.Dark else GlassAppearance.Light,
    tint = if (isDarkMode) {
        Color(0xFF15151B).copy(alpha = 0.74f)
    } else {
        Color(0xFFFBF6E9).copy(alpha = 0.70f)
    },
    brightness = if (isDarkMode) 0.42f else 0.24f,
    blurRadius = 16.dp,
    refractionHeight = 14.dp,
    refractionAmount = 0.dp,
    dispersion = 0f,
    magnification = 1f,
    saturation = 1f,
    highlightIntensity = if (isDarkMode) 1.0f else 1.15f,
    shadowRadius = 22.dp,
    shadowOffset = 7.dp,
    shadowOpacity = if (isDarkMode) 0.30f else 0.11f
)

/** 玻璃面受光：顶边提亮、底边压暗，避免整块像一张纯色半透明贴纸。 */
fun profileCardFace(isDarkMode: Boolean): Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = if (isDarkMode) 0.06f else 0.20f),
    0.38f to Color.Transparent,
    1f to Color.Black.copy(alpha = if (isDarkMode) 0.12f else 0.025f)
)

/**
 * 卡片外圈的"色彩溢出"：和顶栏/底栏同一条 sweepGradient，只是透明度压到三分之一。
 * 它只贴在 1dp 边上，不参与折射，用来把卡片和底栏的材质语言接上。
 */
fun profileCardBleed(isDarkMode: Boolean): Brush {
    val alpha = if (isDarkMode) 0.34f else 0.30f
    return Brush.sweepGradient(
        listOf(
            Color(0xFF7BD8FF).copy(alpha = alpha),
            Color(0xFF9C8CFF).copy(alpha = alpha * 0.9f),
            Color(0xFF7CFFC4).copy(alpha = alpha * 0.7f),
            Color(0xFFFFB1D9).copy(alpha = alpha * 0.9f),
            Color(0xFFFF8CC8).copy(alpha = alpha),
            Color(0xFF7BD8FF).copy(alpha = alpha)
        )
    )
}

/**
 * 一块磨砂玻璃卡。点击反馈只叠一层很淡的强调色，不做缩放——
 * 缩放会把玻璃和它的阴影一起拉变形，玻璃边缘会"抖"。
 */
@Composable
fun GlassSurface(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    corner: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = remember(isDarkMode) { profileCardGlass(isDarkMode) }
    val face = remember(isDarkMode) { profileCardFace(isDarkMode) }
    val bleed = remember(isDarkMode) { profileCardBleed(isDarkMode) }

    Box(
        modifier = modifier
            // 先加弹性压缩：graphicsLayer 在外层，玻璃才会跟着内容一起缩；
            // 释放时 ReleaseSpring 带轻微过冲，就是原型要求的 Spring Overshoot。
            .elasticPress(shape = shape, enabled = onClick != null)
            .glassEffect(backdrop, glass, shape)
            .background(face, shape)
            .cardBevel(corner = corner, isDarkMode = isDarkMode)
            .border(1.dp, bleed, shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * 卡片自己的倒角：4.5dp 柔光带 + 1.5dp 清晰 rim。
 * 这是顶栏/底栏 `glassBevel` 的同款做法，只是半径跟卡片走，让卡片也有"玻璃厚度"。
 */
private fun Modifier.cardBevel(corner: Dp, isDarkMode: Boolean): Modifier = drawWithCache {
    val radius = corner.toPx()
    val soft = 4.5.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val softRadius = (radius - soft / 2f).coerceAtLeast(0f)
    val strokeRadius = (radius - stroke / 2f).coerceAtLeast(0f)
    val softBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDarkMode) 0.07f else 0.10f),
        0.40f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (isDarkMode) 0.14f else 0.07f)
    )
    val rimBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDarkMode) 0.40f else 0.62f),
        0.22f to Color.White.copy(alpha = 0.05f),
        0.58f to Color.Transparent,
        0.88f to Color.Black.copy(alpha = if (isDarkMode) 0.24f else 0.10f),
        1f to Color.White.copy(alpha = if (isDarkMode) 0.24f else 0.34f)
    )
    onDrawBehind {
        drawRoundRect(
            brush = softBrush,
            topLeft = Offset(soft / 2f, soft / 2f),
            size = Size(size.width - soft, size.height - soft),
            cornerRadius = CornerRadius(softRadius, softRadius),
            style = Stroke(soft)
        )
        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(strokeRadius, strokeRadius),
            style = Stroke(stroke)
        )
    }
}
