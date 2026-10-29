package com.example.courseapp.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.courseapp.ui.theme.ScheduleDimensions
import com.example.courseapp.ui.theme.courseCardStyle

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

/**
 * 首页课程卡：液态玻璃微磨砂色卡。
 *
 * 和顶栏/底栏同一套视觉语言（半透明染色 + 顶部受光 + 1px 描边 + 内倒角 + 柔和投影），
 * 但卡片放在 backdrop 子树里、且数量多，所以不做逐卡折射采样，改用 2D 分层近似：
 *
 * 1. [com.example.courseapp.ui.theme.CourseCardStyle.base] 深色模式下的磨砂底；
 * 2. 预设色卡染色（带 alpha，保持"能透出一点点壁纸"的玻璃感）；
 * 3. 左上角高光晕 + 竖向受光/背光渐变（磨砂玻璃的漫反射）；
 * 4. 1dp 描边 + 内侧 1.4dp 倒角高光，让边缘有厚度。
 *
 * 浅色模式正文用同色系深色，深色模式用同色系提亮色，不再一律白字。
 */
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
    val style = courseCardStyle(course, isDarkMode, hasWallpaperBackground)
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
                accent = style.glow,
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
                    ambientColor = style.shadowInk.copy(alpha = style.shadowInk.alpha * 0.5f),
                    spotColor = style.shadowInk
                )
                .clip(shape)
                .then(
                    if (style.base.alpha > 0f) {
                        Modifier.background(style.base, shape)
                    } else {
                        Modifier
                    }
                )
                // 色卡染色：上浅下深一点点，避免整块像一张纯色贴纸
                .background(
                    brush = Brush.verticalGradient(listOf(style.fillTop, style.fillBottom)),
                    shape = shape
                )
                // 磨砂玻璃的三层光：左上角高光晕 + 顶部受光 + 底部背光
                .drawWithCache {
                    val bloom = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.20f),
                            1f to Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, -size.height * 0.08f),
                        radius = size.width * 1.15f
                    )
                    val sheen = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = if (isDarkMode) 0.14f else 0.28f),
                            0.22f to Color.White.copy(alpha = if (isDarkMode) 0.04f else 0.05f),
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = if (isDarkMode) 0.24f else 0.05f)
                        )
                    )
                    // 内侧倒角：顶边亮、底边暗，玻璃边缘才有厚度而不是一圈细线
                    val rim = 1.4.dp.toPx()
                    val rimRadius = (ScheduleDimensions.CourseCorner.toPx() - rim / 2f).coerceAtLeast(0f)
                    val rimBrush = Brush.verticalGradient(
                        listOf(style.bevelTop, Color.Transparent, style.bevelBottom)
                    )
                    onDrawBehind {
                        drawRect(bloom)
                        drawRect(sheen)
                        drawRoundRect(
                            brush = rimBrush,
                            topLeft = Offset(rim / 2f, rim / 2f),
                            size = Size(size.width - rim, size.height - rim),
                            cornerRadius = CornerRadius(rimRadius, rimRadius),
                            style = Stroke(rim)
                        )
                    }
                }
                .then(
                    if (activeAnimation != null) {
                        // 旋转的锥形渐变描边（conic / sweep gradient）
                        Modifier.rotatingConicBorder(
                            rotation = activeAnimation.borderRotation,
                            colors = listOf(
                                Color.White,
                                style.glow,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                                style.glow.copy(alpha = 0.5f),
                                Color.White
                            ),
                            width = 2.dp,
                            corner = ScheduleDimensions.CourseCorner
                        )
                    } else {
                        Modifier.border(width = 1.dp, color = style.border, shape = shape)
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onLongClick?.invoke() }
                )
        ) {
            // 进行中：底部内嵌胶囊进度条。必须离描边留出间距，否则进度条会被倒角高光"吃掉"。
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(style.progressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(style.progress)
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
                    color = style.ink,
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
                        color = style.bodyInk,
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
                        color = style.bodyInk,
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
                        color = style.bodyInk,
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
