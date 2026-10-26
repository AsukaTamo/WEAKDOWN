package com.example.courseapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ScheduleDimensions {
    val SlotHeight = 64.dp
    val HeaderHeight = 54.dp
    val TimeAxisWidth = 46.dp
    val CourseCorner = 7.dp
    val CourseHorizontalInset = 2.dp
    val CourseInset = CourseHorizontalInset
    val TopBarHeight = 74.dp
    val FabSize = 56.dp
}

object ScheduleMotion {
    const val FastMillis = 140
    const val StandardMillis = 240
    const val EmphasizedMillis = 320
    const val FabStaggerMillis = 50

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val PressedScale = 0.97f
    const val SelectedScale = 1.08f
}

data class SchedulePalette(
    val appCanvas: Color,
    val appCanvasEnd: Color,
    val pageFallback: Color,
    val pageScrim: Color,
    val topSurface: Color,
    val topGlass: Color,
    val topGlassEdge: Color,
    val topContent: Color,
    val topMuted: Color,
    val weekChip: Color,
    val weekChipContent: Color,
    val gridSurface: Color,
    val gridGlass: Color,
    val gridLine: Color,
    val timeText: Color,
    val mutedText: Color,
    val dayText: Color,
    val headerText: Color,
    val todaySurface: Color,
    val todayText: Color,
    val courseStroke: Color,
    val courseHighlight: Color,
    val bottomSurface: Color,
    val bottomGlass: Color,
    val navSelected: Color,
    val navInactive: Color,
    val fabContainer: Color,
    val fabMenuSurface: Color
)

fun schedulePalette(isDarkMode: Boolean, hasWallpaper: Boolean): SchedulePalette {
    val darkCanvas = Color(0xFF0B1017)
    val lightCanvas = Color(0xFFF2F6F8)
    return SchedulePalette(
        appCanvas = if (hasWallpaper) Color.Transparent else if (isDarkMode) darkCanvas else lightCanvas,
        appCanvasEnd = if (hasWallpaper) Color.Transparent else if (isDarkMode) Color(0xFF111827) else Color(0xFFEAF2F0),
        pageFallback = if (isDarkMode) darkCanvas else lightCanvas,
        pageScrim = if (hasWallpaper) Color.Black.copy(alpha = 0.18f) else Color.Transparent,
        topSurface = if (hasWallpaper) {
            Color.Black.copy(alpha = if (isDarkMode) 0.46f else 0.36f)
        } else if (isDarkMode) {
            Color(0xE6141822)
        } else {
            Color.White.copy(alpha = 0.88f)
        },
        topGlass = if (hasWallpaper) {
            Color.Black.copy(alpha = if (isDarkMode) 0.46f else 0.36f)
        } else if (isDarkMode) {
            Color(0xE6141822)
        } else {
            Color.White.copy(alpha = 0.88f)
        },
        topGlassEdge = if (hasWallpaper) {
            Color.White.copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = if (isDarkMode) 0.06f else 0.42f)
        },
        topContent = if (hasWallpaper || isDarkMode) Color.White else Color(0xFF102027),
        topMuted = if (hasWallpaper || isDarkMode) Color.White.copy(alpha = 0.68f) else Color(0xFF6B7885),
        weekChip = if (hasWallpaper) Color.Black.copy(alpha = if (isDarkMode) 0.42f else 0.34f) else Color(0xFF102027),
        weekChipContent = if (hasWallpaper) Color.White else Color.White,
        gridSurface = if (hasWallpaper) Color.Black.copy(alpha = 0.10f) else if (isDarkMode) Color(0xFF0F141D) else Color(0xFFF7FAFB),
        gridGlass = if (hasWallpaper) Color.Black.copy(alpha = 0.10f) else if (isDarkMode) Color(0xFF0F141D) else Color(0xFFF7FAFB),
        gridLine = if (hasWallpaper) Color.White.copy(alpha = 0.16f) else if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE1E8EC),
        timeText = if (hasWallpaper) Color.White.copy(alpha = 0.72f) else if (isDarkMode) Color(0xFF9CA7B4) else Color(0xFF83909C),
        mutedText = if (hasWallpaper) Color.White.copy(alpha = 0.56f) else if (isDarkMode) Color(0xFF768190) else Color(0xFFA4ADB7),
        dayText = if (hasWallpaper) Color.White.copy(alpha = 0.84f) else if (isDarkMode) Color(0xFFE8ECF0) else Color(0xFF34414A),
        headerText = if (hasWallpaper) Color.White.copy(alpha = 0.84f) else if (isDarkMode) Color(0xFFE8ECF0) else Color(0xFF34414A),
        todaySurface = if (hasWallpaper) Color.White.copy(alpha = 0.20f) else Color(0xFFE1F4EF),
        todayText = if (hasWallpaper) Color.White else Color(0xFF0F8F7B),
        courseStroke = Color.White.copy(alpha = if (hasWallpaper) 0.34f else 0.30f),
        courseHighlight = Color.White.copy(alpha = 0.82f),
        bottomSurface = if (hasWallpaper) {
            Color.Black.copy(alpha = if (isDarkMode) 0.50f else 0.40f)
        } else if (isDarkMode) {
            Color(0xE8181D26)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        bottomGlass = if (hasWallpaper) {
            Color.Black.copy(alpha = if (isDarkMode) 0.50f else 0.40f)
        } else if (isDarkMode) {
            Color(0xE8181D26)
        } else {
            Color.White.copy(alpha = 0.86f)
        },
        navSelected = Color(0xFF0F8F7B),
        navInactive = if (hasWallpaper) Color.White.copy(alpha = 0.72f) else if (isDarkMode) Color(0xFF8D97A5) else Color(0xFF87919D),
        fabContainer = Color(0xFF111827),
        fabMenuSurface = if (hasWallpaper) Color(0xF5FFFFFF) else if (isDarkMode) CardDark else Color.White
    )
}

/**
 * 非首页页面（课程管理 / 我的 / 导入 / 作息 / 各类弹层）的统一色板。
 * 与首页同一套青绿强调色与玻璃卡片质感，避免切换 Tab 时画风跳变。
 */
data class ContentPalette(
    val canvasStart: Color,
    val canvasEnd: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceBorder: Color,
    val accent: Color,
    val accentSoft: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val divider: Color,
    val inputSurface: Color,
    val destructive: Color
)

fun contentPalette(isDarkMode: Boolean): ContentPalette = ContentPalette(
    canvasStart = if (isDarkMode) Color(0xFF0B1017) else Color(0xFFF2F6F8),
    canvasEnd = if (isDarkMode) Color(0xFF111827) else Color(0xFFEAF2F0),
    surface = if (isDarkMode) Color(0xFF151A23) else Color(0xFFFDFEFE),
    surfaceAlt = if (isDarkMode) Color(0xFF1B222C) else Color(0xFFF7FAFB),
    surfaceBorder = if (isDarkMode) Color.White.copy(alpha = 0.07f) else Color(0xFFE1E8EC),
    accent = if (isDarkMode) Color(0xFF3FC7AE) else Color(0xFF0F8F7B),
    accentSoft = if (isDarkMode) Color(0xFF3FC7AE).copy(alpha = 0.16f) else Color(0xFF0F8F7B).copy(alpha = 0.10f),
    title = if (isDarkMode) Color(0xFFE8ECF0) else Color(0xFF102027),
    body = if (isDarkMode) Color(0xFFC5CED8) else Color(0xFF34414A),
    muted = if (isDarkMode) Color(0xFF8D97A5) else Color(0xFF6B7885),
    divider = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE1E8EC),
    inputSurface = if (isDarkMode) Color(0xFF0F141D) else Color(0xFFF5F8FA),
    destructive = Color(0xFFE0524A)
)

/** 统一卡片外观：圆角 + 实色面 + 细描边。 */
fun Modifier.contentCard(
    palette: ContentPalette,
    radius: Dp = 16.dp,
    filled: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .then(if (filled) Modifier.background(palette.surface) else Modifier)
        .border(0.8.dp, palette.surfaceBorder, shape)
}
