package com.example.courseapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 组件（对话框 / 开关 / 菜单 / 时间选择器）的配色由 AppColors 派生，
// 不再单独维护一套 hex，避免"页面一种青绿、对话框另一种蓝"。
private fun lightScheme(c: AppColors): ColorScheme = lightColorScheme(
    primary = c.accent,
    onPrimary = c.accentOn,
    primaryContainer = Color(0xFFD3E4FA),
    onPrimaryContainer = Color(0xFF04203F),
    secondary = Color(0xFF0B4C8A),
    onSecondary = Color.White,
    background = c.canvas,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceRaised,
    onSurfaceVariant = c.textMuted,
    outline = c.outline,
    error = c.danger,
    onError = Color.White
)

private fun darkScheme(c: AppColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = c.accentOn,
    primaryContainer = Color(0xFF4A1508),
    onPrimaryContainer = Color(0xFFFFD9D2),
    secondary = c.accentText,
    onSecondary = c.accentOn,
    background = c.canvas,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceRaised,
    onSurfaceVariant = c.textMuted,
    outline = c.outline,
    error = c.danger,
    onError = Color(0xFF2A0906)
)

@Composable
fun CourseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = appColors(darkTheme)
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(colors) else lightScheme(colors),
        typography = Typography,
        content = content
    )
}
