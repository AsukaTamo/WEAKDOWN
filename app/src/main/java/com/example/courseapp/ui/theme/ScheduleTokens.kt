package com.example.courseapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ScheduleDimensions {
    val SlotHeight = 64.dp
    val HeaderHeight = 54.dp
    val TimeAxisWidth = 46.dp
    val CourseCorner = 7.dp
    val CourseHorizontalInset = 2.dp
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
    val pageScrim: Color,
    val topSurface: Color,
    val topContent: Color,
    val topMuted: Color,
    val weekChip: Color,
    val weekChipContent: Color,
    val gridSurface: Color,
    val gridLine: Color,
    val timeText: Color,
    val mutedText: Color,
    val dayText: Color,
    val todaySurface: Color,
    val todayText: Color,
    val courseStroke: Color,
    val courseHighlight: Color,
    val bottomSurface: Color,
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
        pageScrim = if (hasWallpaper) Color.Black.copy(alpha = 0.18f) else Color.Transparent,
        topSurface = if (hasWallpaper) {
            Color.Black.copy(alpha = if (isDarkMode) 0.46f else 0.36f)
        } else if (isDarkMode) {
            Color(0xE6141822)
        } else {
            Color.White.copy(alpha = 0.88f)
        },
        topContent = if (hasWallpaper || isDarkMode) Color.White else Color(0xFF102027),
        topMuted = if (hasWallpaper || isDarkMode) Color.White.copy(alpha = 0.68f) else Color(0xFF6B7885),
        weekChip = if (hasWallpaper) Color.Black.copy(alpha = if (isDarkMode) 0.42f else 0.34f) else Color(0xFF102027),
        weekChipContent = if (hasWallpaper) Color.White else Color.White,
        gridSurface = if (hasWallpaper) Color.Black.copy(alpha = 0.10f) else if (isDarkMode) Color(0xFF0F141D) else Color(0xFFF7FAFB),
        gridLine = if (hasWallpaper) Color.White.copy(alpha = 0.16f) else if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE1E8EC),
        timeText = if (hasWallpaper) Color.White.copy(alpha = 0.72f) else if (isDarkMode) Color(0xFF9CA7B4) else Color(0xFF83909C),
        mutedText = if (hasWallpaper) Color.White.copy(alpha = 0.56f) else if (isDarkMode) Color(0xFF768190) else Color(0xFFA4ADB7),
        dayText = if (hasWallpaper) Color.White.copy(alpha = 0.84f) else if (isDarkMode) Color(0xFFE8ECF0) else Color(0xFF34414A),
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
        navSelected = Color(0xFF0F8F7B),
        navInactive = if (hasWallpaper) Color.White.copy(alpha = 0.72f) else if (isDarkMode) Color(0xFF8D97A5) else Color(0xFF87919D),
        fabContainer = Color(0xFF111827),
        fabMenuSurface = if (hasWallpaper) Color(0xF5FFFFFF) else if (isDarkMode) CardDark else Color.White
    )
}
