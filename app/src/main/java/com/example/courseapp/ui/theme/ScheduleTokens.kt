package com.example.courseapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ScheduleDimensions {
    val SlotHeight = 64.dp
    val HeaderHeight = 52.dp
    val TimeAxisWidth = 58.dp
    val CourseCorner = 12.dp
    val CourseInset = 3.dp
    val TopBarHeight = 58.dp
    val FabSize = 54.dp
}

object ScheduleMotion {
    const val FastMillis = 150
    const val StandardMillis = 240
    const val EmphasizedMillis = 300
    const val FabStaggerMillis = 45

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val PressedScale = 0.975f
    const val SelectedScale = 1.04f
}

data class SchedulePalette(
    val pageFallback: Color,
    val wallpaperScrim: Color,
    val topGlass: Color,
    val topGlassEdge: Color,
    val bottomGlass: Color,
    val gridGlass: Color,
    val gridLine: Color,
    val timeText: Color,
    val mutedText: Color,
    val headerText: Color,
    val todaySurface: Color,
    val todayText: Color,
    val courseStroke: Color,
    val courseHighlight: Color,
    val fabContainer: Color,
    val fabMenuSurface: Color
)

fun schedulePalette(isDarkMode: Boolean, hasWallpaper: Boolean): SchedulePalette {
    val wallpaperBase = if (isDarkMode) Color.Black else Color(0xFF0F172A)
    return SchedulePalette(
        pageFallback = if (isDarkMode) BgDark else BgLight,
        wallpaperScrim = wallpaperBase.copy(alpha = if (hasWallpaper) 0.16f else 0f),
        topGlass = if (hasWallpaper) Color(0x66202936) else if (isDarkMode) Color(0xEE111827) else Color(0xF2FFFFFF),
        topGlassEdge = if (hasWallpaper) Color.White.copy(alpha = 0.18f) else Primary.copy(alpha = 0.14f),
        bottomGlass = if (hasWallpaper) Color(0xD9FFFFFF) else if (isDarkMode) Color(0xEA1A1D26) else Color(0xF7FFFFFF),
        gridGlass = if (hasWallpaper) Color(0x33071118) else Color.Transparent,
        gridLine = if (hasWallpaper) Color.White.copy(alpha = 0.20f) else if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFDCE3EF),
        timeText = if (hasWallpaper) Color.White.copy(alpha = 0.74f) else if (isDarkMode) Color(0xFF9AA3B5) else TextSecondary,
        mutedText = if (hasWallpaper) Color.White.copy(alpha = 0.58f) else if (isDarkMode) Color(0xFF778093) else Color(0xFF9AA3B5),
        headerText = if (hasWallpaper) Color.White.copy(alpha = 0.86f) else if (isDarkMode) Color(0xFFE5E7EB) else TextPrimary,
        todaySurface = if (hasWallpaper) Color.White.copy(alpha = 0.18f) else if (isDarkMode) TodayHeaderBgDark else TodayHeaderBg,
        todayText = if (hasWallpaper) Color.White else if (isDarkMode) PrimaryLight else Primary,
        courseStroke = Color.White.copy(alpha = if (hasWallpaper) 0.34f else 0.24f),
        courseHighlight = Color.White.copy(alpha = 0.76f),
        fabContainer = Color(0xFF22C55E),
        fabMenuSurface = if (hasWallpaper) Color(0xEAF8FAFC) else if (isDarkMode) CardDark else CardLight
    )
}
