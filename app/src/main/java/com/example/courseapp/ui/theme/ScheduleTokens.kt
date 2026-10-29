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
    val CourseCorner = 10.dp
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

    /** 所有可按控件按下时的压缩比例：0.96 的微缩配合内阴影形成"按进去"的物理反馈。 */
    const val PressedScale = 0.96f
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
    val headerBackdrop: Color,
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
    val c = appColors(isDarkMode)
    // 顶栏 / 底栏：surface @ 88%，壁纸只透 12%，不再额外叠黑罩
    val barSurface = c.surface.copy(alpha = 0.88f)
    val gridGlass = if (hasWallpaper) Color.Transparent else c.surface
    return SchedulePalette(
        appCanvas = if (hasWallpaper) Color.Transparent else c.canvas,
        appCanvasEnd = if (hasWallpaper) Color.Transparent else c.canvasTint,
        pageFallback = c.canvas,
        pageScrim = Color.Transparent,
        topSurface = barSurface,
        topGlass = barSurface,
        topGlassEdge = c.glassEdge,
        topContent = c.textPrimary,
        topMuted = c.textMuted,
        // 胶囊是页面最显眼的主控件：浅色鸢尾蓝，深色火山橙
        weekChip = c.accent,
        weekChipContent = c.accentOn,
        gridSurface = gridGlass,
        gridGlass = gridGlass,
        gridLine = c.hairline,
        timeText = c.textMuted,
        mutedText = c.textMuted,
        dayText = c.textPrimary,
        headerText = c.textPrimary,
        // 星期栏 / 时间轴底衬：亚克力底衬，78% 不透明确保滚动经过的课程不会顶掉文字
        headerBackdrop = c.canvas.copy(alpha = 0.78f),
        // 今日高亮：实心强调色块
        todaySurface = c.accent,
        todayText = c.accentOn,
        courseStroke = Color.White.copy(alpha = if (hasWallpaper) 0.34f else 0.28f),
        courseHighlight = Color.White.copy(alpha = 0.82f),
        bottomSurface = barSurface,
        bottomGlass = barSurface,
        navSelected = c.accentOn,
        navInactive = c.textMuted,
        fabContainer = c.accent,
        fabMenuSurface = c.surfaceRaised
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
    val onAccent: Color,
    val accentSoft: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val divider: Color,
    val inputSurface: Color,
    val success: Color,
    val warning: Color,
    val destructive: Color
)

fun contentPalette(isDarkMode: Boolean): ContentPalette {
    val c = appColors(isDarkMode)
    return ContentPalette(
        canvasStart = c.canvas,
        canvasEnd = c.canvasTint,
        surface = c.surface,
        surfaceAlt = c.surfaceRaised,
        surfaceBorder = c.outline,
        accent = c.accent,
        onAccent = c.accentOn,
        accentSoft = c.accentSoft,
        title = c.textPrimary,
        body = c.textSecondary,
        muted = c.textMuted,
        divider = c.hairline,
        inputSurface = c.surfaceSunken,
        success = c.success,
        warning = c.warning,
        destructive = c.danger
    )
}

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
