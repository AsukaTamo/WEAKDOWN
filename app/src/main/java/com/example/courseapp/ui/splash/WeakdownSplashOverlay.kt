package com.example.courseapp.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val SplashDurationMillis = 4000
private const val StarColumns = 12
private const val StarRows = 12
private const val StarCount = StarColumns * StarRows
private const val SplashSeed = 20260728

private val SplashEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private enum class SplashMood {
    Angry,
    Smile
}

private data class CharacterLine(
    val mood: SplashMood,
    val text: String
)

private val CharacterLines = listOf(
    CharacterLine(SplashMood.Angry, "今天要好好上课，不许迟到！😤"),
    CharacterLine(SplashMood.Angry, "昨天是不是又熬夜了？！🌙"),
    CharacterLine(SplashMood.Smile, "下午去吃当当！🍚"),
    CharacterLine(SplashMood.Smile, "goog，goog ✨")
)

private data class SplashStar(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val driftY: Float,
    val radius: Float,
    val alpha: Float,
    val tintWeight: Float,
    val twinklePhase: Float,
    val twinkleSpeed: Float
)

@Composable
fun WeakdownSplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    var finished by remember { mutableStateOf(false) }
    val stars = remember { generateSplashStars() }
    val characterLine = remember { CharacterLines.random() }

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
        lerp((-72).dp.toPx(), (-104).dp.toPx(), titleLift)
    }
    val titleScale = lerp(0.96f, 1.015f, titleLift)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
    ) {
        Image(
            painter = painterResource(id = R.drawable.weakdown_mcd_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCCFFFDF3),
                        Color(0xBFFFF8DC),
                        Color(0xCCFFF3C4)
                    )
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x55FFD119),
                        Color(0x24E31B16),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.44f),
                    radius = size.minDimension * 0.82f
                )
            )

            val starAppearProgress = intervalProgress(value, start = 0.02f, end = 0.24f)
            val driftProgress = intervalProgress(value, start = 0.12f, end = 0.92f)
            val fadeProgress = fadeOutAfter(value, start = 0.84f)

            stars.forEach { star ->
                val start = Offset(star.startX * size.width, star.startY * size.height)
                val position = Offset(
                    x = start.x + star.driftX * size.minDimension * driftProgress,
                    y = start.y + star.driftY * size.minDimension * driftProgress
                )
                val twinkle = 0.78f + sin((value * PI.toFloat() * star.twinkleSpeed) + star.twinklePhase) * 0.22f
                val starAlpha = star.alpha *
                    starAppearProgress *
                    fadeProgress *
                    twinkle
                val starColor = lerpColor(
                    Color(0xFFFFC928),
                    Color(0xFFE31B16),
                    star.tintWeight
                )

                if (star.radius > 1.4f) {
                    drawCircle(
                        color = Color(0xFFFFD119).copy(alpha = starAlpha * 0.12f),
                        radius = star.radius * 3.5f,
                        center = position
                    )
                }
                drawCircle(
                    color = starColor.copy(alpha = starAlpha),
                    radius = star.radius,
                    center = position
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
            Image(
                painter = painterResource(id = R.drawable.weakdown_title_logo),
                contentDescription = "WJing course schedule title",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 280.dp, height = 200.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Courses in Orbit",
                color = Color(0xCC8A1A12),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            )
        }

        SplashCharacter(
            line = characterLine,
            progress = value,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SplashCharacter(
    line: CharacterLine,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val appear = intervalProgress(progress, start = 0.12f, end = 0.34f)
    val fade = fadeOutAfter(progress, start = 0.86f)
    val bubbleAppear = intervalProgress(progress, start = 0.28f, end = 0.48f)
    val bob = sin(progress * PI.toFloat() * 2.2f) * 5f

    Box(
        modifier = modifier
            .size(width = 204.dp, height = 394.dp)
            .padding(bottom = 14.dp)
            .graphicsLayer {
                alpha = appear * fade
                translationY = (1f - appear) * 34f + bob
                scaleX = 0.94f + appear * 0.06f
                scaleY = scaleX
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(
                id = if (line.mood == SplashMood.Angry) {
                    R.drawable.weakdown_pixel_angry
                } else {
                    R.drawable.weakdown_pixel_smile
                }
            ),
            contentDescription = if (line.mood == SplashMood.Angry) {
                "Angry pixel character"
            } else {
                "Smiling pixel character"
            },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 140.dp, height = 260.dp)
        )

        SpeechBubble(
            text = line.text,
            progress = bubbleAppear * fade,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp)
        )
    }
}

@Composable
private fun SpeechBubble(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 10f
                scaleX = 0.94f + progress * 0.06f
                scaleY = scaleX
            }
            .size(width = 168.dp, height = 115.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.weakdown_speech_bubble),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = text,
            color = Color(0xFF111827),
            fontSize = 9.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-4).dp)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .widthIn(max = 120.dp)
        )
    }
}

private fun generateSplashStars(): List<SplashStar> {
    val random = Random(SplashSeed)
    return List(StarCount) { index ->
        val column = index % StarColumns
        val row = index / StarColumns
        SplashStar(
            startX = ((column + 0.2f + random.nextFloat() * 0.6f) / StarColumns)
                .coerceIn(0.02f, 0.98f),
            startY = ((row + 0.18f + random.nextFloat() * 0.64f) / StarRows)
                .coerceIn(0.02f, 0.98f),
            driftX = (random.nextFloat() - 0.5f) * 0.12f,
            driftY = (random.nextFloat() - 0.5f) * 0.16f,
            radius = 0.65f + random.nextFloat() * 1.45f,
            alpha = 0.36f + random.nextFloat() * 0.58f,
            tintWeight = random.nextFloat() * 0.7f,
            twinklePhase = random.nextFloat() * PI.toFloat() * 2f,
            twinkleSpeed = 1.4f + random.nextFloat() * 2.4f
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

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = lerp(start.red, stop.red, fraction),
        green = lerp(start.green, stop.green, fraction),
        blue = lerp(start.blue, stop.blue, fraction),
        alpha = lerp(start.alpha, stop.alpha, fraction)
    )
}
