package com.example.courseapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 与首页同一套青绿强调色，保证 Material 默认组件（对话框 / 按钮 / 开关）不出现第二套蓝。
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F8F7B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9EDE2),
    onPrimaryContainer = Color(0xFF062B26),
    secondary = Color(0xFF0B6B5C),
    onSecondary = Color.White,
    background = Color(0xFFF2F6F8),
    onBackground = Color(0xFF102027),
    surface = Color(0xFFFDFEFE),
    onSurface = Color(0xFF102027),
    surfaceVariant = Color(0xFFF7FAFB),
    onSurfaceVariant = Color(0xFF6B7885),
    outline = Color(0xFFE1E8EC),
    error = Error,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3FC7AE),
    onPrimary = Color(0xFF04231F),
    primaryContainer = Color(0xFF0B4E44),
    onPrimaryContainer = Color(0xFFB9EDE2),
    secondary = Color(0xFF7FDCC8),
    onSecondary = Color(0xFF04231F),
    background = Color(0xFF0B1017),
    onBackground = Color(0xFFE8ECF0),
    surface = Color(0xFF151A23),
    onSurface = Color(0xFFE8ECF0),
    surfaceVariant = Color(0xFF1B222C),
    onSurfaceVariant = Color(0xFF8D97A5),
    outline = Color(0xFF2A3240),
    error = Error,
    onError = Color.White
)

@Composable
fun CourseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
