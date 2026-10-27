package com.example.courseapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 全应用唯一的颜色源。
 *
 * 浅色：流光白 #FFFBEA + 鸢尾蓝 #0F64B5
 * 深色：藏蓝 #000035 + 火山橙 #FF6047
 *
 * 规则：
 * - 只放语义角色，不放具体控件颜色；SchedulePalette / ContentPalette / Material 方案都由这里派生；
 * - 对比度由 ColorContrastTest 卡住（正文 4.5、标题 7、按钮文字 4.5）。
 */
@Immutable
data class AppColors(
    val canvas: Color,
    val canvasTint: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val hairline: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentOn: Color,
    val accentText: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val glassSurface: Color,
    val glassEdge: Color,
    val glassScrim: Color,
    val glassInk: Color,
    val glassInkMuted: Color
)

val LightAppColors = AppColors(
    canvas = Color(0xFFFFFBEA),
    canvasTint = Color(0xFFFDF6E1),
    surface = Color(0xFFF4EFE0),
    surfaceRaised = Color(0xFFFAF6E9),
    surfaceSunken = Color(0xFFEDE7D5),
    hairline = Color(0x1A0F64B5),
    outline = Color(0x1A0F64B5),
    textPrimary = Color(0xFF092C4C),
    textSecondary = Color(0xFF40607A),
    textMuted = Color(0xFF526E88),
    accent = Color(0xFF0F64B5),
    accentOn = Color(0xFFFFFBEA),
    accentText = Color(0xFF0F64B5),
    accentSoft = Color(0x1F0F64B5),
    success = Color(0xFF1F7A4D),
    warning = Color(0xFF9A5B00),
    danger = Color(0xFFC0392B),
    glassSurface = Color(0xE0F4EFE0),
    glassEdge = Color(0x33092C4C),
    glassScrim = Color.Transparent,
    glassInk = Color(0xFF092C4C),
    glassInkMuted = Color(0xFF5C768D)
)

val DarkAppColors = AppColors(
    canvas = Color(0xFF0B0B0F),          // 近黑中性灰
    canvasTint = Color(0xFF101014),
    surface = Color(0xFF141419),
    surfaceRaised = Color(0xFF1C1C22),
    surfaceSunken = Color(0xFF08080B),
    hairline = Color(0x14FFFFFF),
    outline = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF5F6FA),
    textSecondary = Color(0xFFC6C7D2),
    textMuted = Color(0xFF8E90A0),
    accent = Color(0xFFE8553D),          // 火山橙压暗一档
    accentOn = Color(0xFF0B0B0F),
    accentText = Color(0xFFF97B64),
    accentSoft = Color(0x2EE8553D),
    success = Color(0xFF4ECB8A),
    warning = Color(0xFFF0B44B),
    danger = Color(0xFFF06A86),
    glassSurface = Color(0xE0141419),
    glassEdge = Color(0x1FFFFFFF),
    glassScrim = Color(0x330B0B0F),
    glassInk = Color(0xFFF5F6FA),
    glassInkMuted = Color(0xFF8E90A0)
)

fun appColors(isDarkMode: Boolean): AppColors =
    if (isDarkMode) DarkAppColors else LightAppColors
