package com.example.courseapp.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SplashDurationMillis = 4000
private const val StarColumns = 12
private const val StarRows = 12
private const val StarCount = StarColumns * StarRows
private const val SplashSeed = 20260728

private val SplashEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private data class SplashStar(
    val startX: Float,
    val startY: Float,
    val gatherX: Float,
    val gatherY: Float,
    val scatterX: Float,
    val scatterY: Float,
    val radius: Float,
    val alpha: Float,
    val tintWeight: Float,
    val twinklePhase: Float
)

@Composable
fun WeakdownSplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    val stars = remember { generateSplashStars() }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SplashDurationMillis,
                easing = LinearEasing
            )
        )
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    val value = progress.value
    val overlayAlpha = fadeOutAfter(value, start = 0.86f)
    val titleAlpha = intervalProgress(value, start = 0.22f, end = 0.38f) *
        fadeOutAfter(value, start = 0.82f)
    val titleLift = intervalProgress(value, start = 0.38f, end = 0.82f)
    val density = LocalDensity.current
    val titleTranslationY = with(density) {
        lerp(20.dp.toPx(), (-10).dp.toPx(), titleLift)
    }
    val titleScale = lerp(0.96f, 1.015f, titleLift)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07111F),
                        Color(0xFF0A1B2E),
                        Color(0xFF020713)
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3338BDF8),
                        Color(0x1414B8A6),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.48f),
                    radius = size.minDimension * 0.72f
                )
            )

            val starAppearProgress = intervalProgress(value, start = 0.02f, end = 0.24f)
            val gatherProgress = intervalProgress(value, start = 0.26f, end = 0.74f)
            val scatterProgress = intervalProgress(value, start = 0.76f, end = 0.93f)
            val center = Offset(size.width * 0.5f, size.height * 0.48f)
            val drift = -8f * value

            stars.forEach { star ->
                val start = Offset(star.startX * size.width, star.startY * size.height)
                val gathered = Offset(
                    x = center.x + star.gatherX * size.minDimension,
                    y = center.y + star.gatherY * size.minDimension
                )
                val scattered = Offset(
                    x = gathered.x + star.scatterX * size.minDimension,
                    y = gathered.y + star.scatterY * size.minDimension
                )
                val gatheringPosition = lerp(start, gathered, gatherProgress)
                val position = lerp(gatheringPosition, scattered, scatterProgress)
                val twinkle = 0.86f + sin((value * PI.toFloat() * 2.2f) + star.twinklePhase) * 0.14f
                val starAlpha = star.alpha *
                    starAppearProgress *
                    lerp(0.78f, 1f, gatherProgress) *
                    fadeOutAfter(value, start = 0.84f) *
                    twinkle
                val starColor = lerpColor(
                    Color.White,
                    Color(0xFF7DD3FC),
                    star.tintWeight
                )

                if (star.radius > 1.4f) {
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = starAlpha * 0.16f),
                        radius = star.radius * 3.5f,
                        center = position.copy(y = position.y + drift)
                    )
                }
                drawCircle(
                    color = starColor.copy(alpha = starAlpha),
                    radius = star.radius,
                    center = position.copy(y = position.y + drift)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleTranslationY
                    scaleX = titleScale
                    scaleY = titleScale
                }
        ) {
            Text(
                text = "WEAKDOWN",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Courses in Orbit",
                color = Color(0xBFE0F2FE),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun generateSplashStars(): List<SplashStar> {
    val random = Random(SplashSeed)
    return List(StarCount) { index ->
        val column = index % StarColumns
        val row = index / StarColumns
        val angle = random.nextFloat() * (PI.toFloat() * 2f)
        val scatterDistance = 0.22f + random.nextFloat() * 0.24f
        SplashStar(
            startX = ((column + 0.2f + random.nextFloat() * 0.6f) / StarColumns)
                .coerceIn(0.02f, 0.98f),
            startY = ((row + 0.18f + random.nextFloat() * 0.64f) / StarRows)
                .coerceIn(0.02f, 0.98f),
            gatherX = (random.nextFloat() - 0.5f) * 0.38f,
            gatherY = (random.nextFloat() - 0.5f) * 0.16f,
            scatterX = cos(angle) * scatterDistance,
            scatterY = sin(angle) * scatterDistance,
            radius = 0.65f + random.nextFloat() * 1.45f,
            alpha = 0.36f + random.nextFloat() * 0.58f,
            tintWeight = random.nextFloat() * 0.7f,
            twinklePhase = random.nextFloat() * PI.toFloat() * 2f
        )
    }
}

private fun fadeOutAfter(progress: Float, start: Float): Float {
    return 1f - SplashEasing.transform(((progress - start) / (1f - start)).coerceIn(0f, 1f))
}

private fun intervalProgress(progress: Float, start: Float, end: Float): Float {
    return SplashEasing.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

private fun lerp(start: Offset, stop: Offset, fraction: Float): Offset {
    return Offset(
        x = lerp(start.x, stop.x, fraction),
        y = lerp(start.y, stop.y, fraction)
    )
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = lerp(start.red, stop.red, fraction),
        green = lerp(start.green, stop.green, fraction),
        blue = lerp(start.blue, stop.blue, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction)
    )
}
