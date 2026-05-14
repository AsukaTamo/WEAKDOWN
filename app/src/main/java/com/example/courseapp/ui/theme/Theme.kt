package com.example.courseapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = Accent,
    background = BgLight,
    surface = CardLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Divider,
    error = Error
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    secondary = Accent,
    background = BgDark,
    surface = CardDark,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = DividerDark,
    error = Error
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
