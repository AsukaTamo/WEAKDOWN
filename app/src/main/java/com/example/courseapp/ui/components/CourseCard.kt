package com.example.courseapp.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.theme.CourseCustomEnd
import com.example.courseapp.ui.theme.CourseCustomEndDark
import com.example.courseapp.ui.theme.CourseCustomStart
import com.example.courseapp.ui.theme.CourseCustomStartDark
import com.example.courseapp.ui.theme.CourseElectiveEnd
import com.example.courseapp.ui.theme.CourseElectiveEndDark
import com.example.courseapp.ui.theme.CourseElectiveStart
import com.example.courseapp.ui.theme.CourseElectiveStartDark
import com.example.courseapp.ui.theme.CourseLabEnd
import com.example.courseapp.ui.theme.CourseLabEndDark
import com.example.courseapp.ui.theme.CourseLabStart
import com.example.courseapp.ui.theme.CourseLabStartDark
import com.example.courseapp.ui.theme.CourseRequiredEnd
import com.example.courseapp.ui.theme.CourseRequiredEndDark
import com.example.courseapp.ui.theme.CourseRequiredStart
import com.example.courseapp.ui.theme.CourseRequiredStartDark
import com.example.courseapp.ui.theme.ScheduleDimensions
import com.example.courseapp.ui.theme.ThemeFade
import com.example.courseapp.ui.theme.schedulePalette

fun courseGradientColors(course: Course, isDarkMode: Boolean): Pair<Color, Color> {
    // 主题切换期间按同一进度在浅色/深色两套卡片配色之间插值，
    // 每张卡片自己完成渐变，而不是靠整屏遮罩。
    val t = ThemeFade.fractionFor(isDarkMode)
    val light = lightCourseGradient(course)
    val dark = darkCourseGradient(course)
    return lerp(light.first, dark.first, t) to lerp(light.second, dark.second, t)
}

/** 浅色主题的卡片渐变：统一 92% 不透明度，壁纸背景也挡得住。 */
private fun lightCourseGradient(course: Course): Pair<Color, Color> {
    val seed = course.name.hashCode()
    val start: Color
    val end: Color
    val custom = if (course.customColor.isNotEmpty()) parseCustomColor(course) else null
    if (custom != null) {
        start = custom
        end = custom
    } else {
        val base = lightTypeGradient(course.type)
        start = courseVariant(base.first, seed)
        end = courseVariant(base.second, seed)
    }
    return start.copy(alpha = 0.92f) to end.copy(alpha = 0.92f)
}

/** 深色主题的卡片渐变：自定义色按透明度压暗，类型色直接使用深色基准。 */
private fun darkCourseGradient(course: Course): Pair<Color, Color> {
    val seed = course.name.hashCode()
    val custom = if (course.customColor.isNotEmpty()) parseCustomColor(course) else null
    if (custom != null) {
        return custom.copy(alpha = 0.88f) to custom.copy(alpha = 0.70f)
    }
    val base = darkTypeGradient(course.type)
    return courseVariant(base.first, seed) to courseVariant(base.second, seed)
}

private fun parseCustomColor(course: Course): Color? =
    try {
        Color(android.graphics.Color.parseColor(course.customColor))
    } catch (_: Exception) {
        null
    }

private fun lightTypeGradient(type: CourseType): Pair<Color, Color> = when (type) {
    CourseType.REQUIRED -> CourseRequiredStart to CourseRequiredEnd
    CourseType.ELECTIVE -> CourseElectiveStart to CourseElectiveEnd
    CourseType.LAB -> CourseLabStart to CourseLabEnd
    CourseType.CUSTOM -> CourseCustomStart to CourseCustomEnd
}

private fun darkTypeGradient(type: CourseType): Pair<Color, Color> = when (type) {
    CourseType.REQUIRED -> CourseRequiredStartDark to CourseRequiredEndDark
    CourseType.ELECTIVE -> CourseElectiveStartDark to CourseElectiveEndDark
    CourseType.LAB -> CourseLabStartDark to CourseLabEndDark
    CourseType.CUSTOM -> CourseCustomStartDark to CourseCustomEndDark
}

/** 在类型基准色上做确定性微调：色相 ±10°、饱和度 ±4%、亮度只降不升。 */
private fun courseVariant(color: Color, seed: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + ((seed % 5) - 2) * 5f + 360f) % 360f
    hsv[1] = (hsv[1] * (1f + (((seed / 5) % 3) - 1) * 0.04f)).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * (1f - ((seed / 15) % 3) * 0.04f)).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** 进行中课程专用的动画：描边旋转 + 光晕呼吸。只在 isActive 时才创建。 */
private class ActiveCourseAnimation(
    val borderRotation: State<Float>,
    val breathe: State<Float>
)

@Composable
private fun rememberActiveCourseAnimation(): ActiveCourseAnimation {
    val transition = rememberInfiniteTransition(label = "activeCourse")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing)
        ),
        label = "activeBorderRotation"
    )
    val breathe = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeGlowBreath"
    )
    return ActiveCourseAnimation(rotation, breathe)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseCard(
    course: Course,
    isDarkMode: Boolean = false,
    isActive: Boolean = false,
    isUpcoming: Boolean = false,
    progress: Float = 0f,
    minutesUntil: Int = 0,
    remainingMinutes: Int = 0,
    hasWallpaperBackground: Boolean = false,
    entranceEpoch: Int = 1,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = schedulePalette(isDarkMode, hasWallpaperBackground)
    // 主题过渡进度：卡片自己的背景/描边/正文色全部按它插值，和页面其余元素同步
    val themeFraction = ThemeFade.fractionFor(isDarkMode)
    val (rawStart, rawEnd) = courseGradientColors(course, isDarkMode)
    val darkBase = Color(0xD914141A)
    // 浅色是彩色渐变、深色是藏蓝底 + 彩色微光：底色向 darkBase 收拢
    val startColor = lerp(rawStart, darkBase, themeFraction)
    val endColor = lerp(rawEnd, darkBase, themeFraction)
    val cardAccent = lerp(rawStart, courseDarkAccent(course), themeFraction)
    val bodyColor = lerp(
        Color.White.copy(alpha = 0.88f),
        Color(0xFFD1D5F0),
        themeFraction
    )
    val textShadow = if (themeFraction < 0.5f) Shadow(
        color = Color(0x33000000),
        offset = Offset(0f, 1f),
        blurRadius = 2f
    ) else null
    val lightOverlayAlpha = 1f - themeFraction
    val darkOverlayAlpha = themeFraction
    val baseTextStyle = LocalTextStyle.current.copy(shadow = textShadow)
    val shape = RoundedCornerShape(ScheduleDimensions.CourseCorner)
    val teacherLine = remember(course.teacher) { course.teacher.trim() }
    val locationLine = remember(course.location) { course.location.trim() }
    // 首次进入首页的 Spring Cascade：启动页还在时先隐藏，启动页退场后按行列错峰向上弹入。
    // 之后无论是切 Tab、翻周还是重建页面，epoch 都已是 1，不会重放。
    val entrance = remember { Animatable(if (entranceEpoch == 0) 0f else 1f) }
    val entranceDistancePx = with(LocalDensity.current) { 26.dp.toPx() }
    LaunchedEffect(entranceEpoch) {
        if (entranceEpoch == 0 || entrance.value >= 1f) return@LaunchedEffect
        val staggerMs = (course.startSlot * 30 + course.dayOfWeek * 22).coerceAtMost(300)
        kotlinx.coroutines.delay(staggerMs.toLong())
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.52f,
                stiffness = 380f,
                visibilityThreshold = 0.0005f
            )
        )
    }
    val titleFontSize = if (course.slotCount <= 1) 10.sp else 11.sp
    val bodyFontSize = if (course.slotCount <= 1) 8.sp else 9.sp
    val titleMaxLines = if (course.slotCount <= 1) 2 else 3
    val bodyMaxLines = if (course.slotCount <= 1) 1 else 2
    val lineSpacing = if (course.slotCount <= 1) 1.dp else 2.dp

    // 卡片只有 ~45dp 宽，状态文案必须短："剩23分" / "15分后"。
    val statusText = when {
        isActive && remainingMinutes > 0 -> "剩${remainingMinutes}分"
        isActive -> "快下课"
        isUpcoming && minutesUntil > 0 -> "${minutesUntil}分后"
        else -> ""
    }
    val showStatusLine = statusText.isNotEmpty() && course.slotCount >= 2
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1200, easing = LinearEasing),
        label = "courseProgress"
    )


    // 只有进行中的课程才跑无限动画，其余卡片保持零额外开销。
    val activeAnimation = if (isActive) rememberActiveCourseAnimation() else null

    Box(
        modifier = modifier
            .elasticPress(shape)
            .graphicsLayer {
                val entranceProgress = entrance.value
                val entranceAlpha = entranceProgress.coerceIn(0f, 1f)
                val entranceScale = 0.96f + 0.04f * entranceAlpha
                scaleX = entranceScale
                scaleY = entranceScale
                translationY = (1f - entranceProgress) * entranceDistancePx
                alpha = entranceAlpha
            }
    ) {
        if (activeAnimation != null) {
            ActiveCourseGlow(
                accent = rawStart,
                corner = ScheduleDimensions.CourseCorner,
                breathe = activeAnimation.breathe
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = when {
                        isActive -> 10.dp
                        hasWallpaperBackground -> 7.dp
                        else -> 3.dp
                    },
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (hasWallpaperBackground) 0.24f else 0.10f),
                    spotColor = Color.Black.copy(alpha = if (hasWallpaperBackground) 0.28f else 0.12f)
                )
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(listOf(startColor, endColor)),
                    shape = shape
                )
                // 浅色高光/压暗与深色彩色微光同时存在，只是透明度跟着过渡进度此消彼长
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(
                                alpha = (if (hasWallpaperBackground) 0.18f else 0.12f) * lightOverlayAlpha
                            ),
                            Color.Transparent,
                            Color.Black.copy(
                                alpha = (if (hasWallpaperBackground) 0.16f else 0.08f) * lightOverlayAlpha
                            )
                        )
                    ),
                    shape = shape
                )
                .background(
                    brush = Brush.linearGradient(
                        listOf(cardAccent.copy(alpha = 0.18f * darkOverlayAlpha), Color.Transparent)
                    ),
                    shape = shape
                )
                .then(
                    if (activeAnimation != null) {
                        // 旋转的锥形渐变描边（conic / sweep gradient）
                        Modifier.rotatingConicBorder(
                            rotation = activeAnimation.borderRotation,
                            colors = listOf(
                                Color.White,
                                rawStart.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.5f),
                                Color.White
                            ),
                            width = 2.dp,
                            corner = ScheduleDimensions.CourseCorner
                        )
                    } else {
                        Modifier.border(
                            width = (1f + 0.5f * themeFraction).dp,
                            color = lerp(
                                palette.courseStroke,
                                cardAccent.copy(alpha = 0.85f),
                                themeFraction
                            ),
                            shape = shape
                        )
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onLongClick?.invoke() }
                )
        ) {
            // 进行中：底部内嵌胶囊进度条。必须离描边留出间距，否则白色进度会被白色高亮描边"吃掉"。
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(Color.White)
                            .progressSemantics(animatedProgress)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(lineSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 6.dp,
                        end = 6.dp,
                        top = 4.dp,
                        bottom = if (isActive) 11.dp else 4.dp
                    )
            ) {
                Text(
                    text = course.name,
                    color = Color.White,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = if (course.slotCount <= 1) 11.sp else 12.sp,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )

                if (teacherLine.isNotBlank()) {
                    Text(
                        text = teacherLine,
                        color = bodyColor,
                        style = baseTextStyle,
                        fontSize = bodyFontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = if (course.slotCount <= 1) 9.sp else 10.sp,
                        maxLines = bodyMaxLines,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (locationLine.isNotBlank()) {
                    Text(
                        text = "@$locationLine",
                        color = bodyColor,
                        style = baseTextStyle,
                        fontSize = bodyFontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = if (course.slotCount <= 1) 9.sp else 10.sp,
                        maxLines = bodyMaxLines,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showStatusLine) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = statusText,
                        color = bodyColor,
                        style = baseTextStyle,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 进行中课程的弥散光晕层，画在卡片后面：
 * - 贴边的一圈粗描边整体模糊 → 柔和的弥散光晕
 * - 卡片底部一团竖向渐变 + 模糊 → 呼吸背光
 * 两层都吃同一个呼吸进度，alpha / 缩放同步起伏。
 */
@Composable
private fun BoxScope.ActiveCourseGlow(
    accent: Color,
    corner: Dp,
    breathe: State<Float>
) {
    val shape = RoundedCornerShape(corner)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = Modifier.matchParentSize()) {
        // 底部呼吸弥散背光：横向放大到卡片两侧，向下溢出更多
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(46.dp)
                .offset(y = 18.dp)
                .then(
                    if (supportsBlur) {
                        Modifier.blur(radius = 20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer {
                    val b = breathe.value
                    alpha = b
                    scaleX = 1.35f + b * 0.15f
                    scaleY = 0.95f + b * 0.1f
                }
                .background(
                    brush = Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.8f))
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
        )

        // 弥散光晕描边
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (supportsBlur) {
                        Modifier.blur(radius = 10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer { alpha = breathe.value * 0.9f }
                .border(width = 6.dp, color = accent, shape = shape)
        )
    }
}

/**
 * 旋转的锥形渐变描边。
 * Compose 的 sweepGradient 没有起始角参数，所以直接构造平台 SweepGradient，
 * 每帧只更新 local matrix 旋转，避免重建 shader。
 */
private fun Modifier.rotatingConicBorder(
    rotation: State<Float>,
    colors: List<Color>,
    width: Dp,
    corner: Dp
): Modifier = this.drawWithCache {
    val strokePx = width.toPx()
    val cornerPx = corner.toPx()
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    val shader = android.graphics.SweepGradient(
        centerX,
        centerY,
        colors.map { it.toArgb() }.toIntArray(),
        null
    )
    val matrix = android.graphics.Matrix()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = strokePx
        this.shader = shader
    }

    onDrawBehind {
        matrix.setRotate(rotation.value, centerX, centerY)
        shader.setLocalMatrix(matrix)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(
                strokePx / 2f,
                strokePx / 2f,
                size.width - strokePx / 2f,
                size.height - strokePx / 2f,
                cornerPx,
                cornerPx,
                paint
            )
        }
    }
}

// ── 深色模式课程卡彩色描边：4 组循环，按课程名确定性取色 ──
private val darkCourseAccents = listOf(
    Color(0xFFE8553D), // 火山橙
    Color(0xFFFF9F43), // 琥珀金
    Color(0xFF00D2D3), // 电光青
    Color(0xFFA29BFE)  // 星云紫
)

internal fun courseDarkAccent(course: Course): Color =
    darkCourseAccents[Math.floorMod(course.name.hashCode(), darkCourseAccents.size)]
