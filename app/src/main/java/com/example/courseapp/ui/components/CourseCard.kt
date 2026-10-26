package com.example.courseapp.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.theme.CourseAiEnd
import com.example.courseapp.ui.theme.CourseAiStart
import com.example.courseapp.ui.theme.CourseAlgoEnd
import com.example.courseapp.ui.theme.CourseAlgoStart
import com.example.courseapp.ui.theme.CourseCareerEnd
import com.example.courseapp.ui.theme.CourseCareerStart
import com.example.courseapp.ui.theme.CourseCompilerEnd
import com.example.courseapp.ui.theme.CourseCompilerStart
import com.example.courseapp.ui.theme.CourseCustom
import com.example.courseapp.ui.theme.CourseCustomDark
import com.example.courseapp.ui.theme.CourseDbEnd
import com.example.courseapp.ui.theme.CourseDbStart
import com.example.courseapp.ui.theme.CourseElective
import com.example.courseapp.ui.theme.CourseElectiveDark
import com.example.courseapp.ui.theme.CourseLab
import com.example.courseapp.ui.theme.CourseLabDark
import com.example.courseapp.ui.theme.CourseNetEnd
import com.example.courseapp.ui.theme.CourseNetStart
import com.example.courseapp.ui.theme.CourseOsEnd
import com.example.courseapp.ui.theme.CourseOsStart
import com.example.courseapp.ui.theme.CourseRequired
import com.example.courseapp.ui.theme.CourseRequiredDark
import com.example.courseapp.ui.theme.CourseSeEnd
import com.example.courseapp.ui.theme.CourseSeStart
import com.example.courseapp.ui.theme.ScheduleDimensions
import com.example.courseapp.ui.theme.ScheduleMotion
import com.example.courseapp.ui.theme.schedulePalette

fun courseGradientColors(course: Course, isDarkMode: Boolean): Pair<Color, Color> {
    if (course.customColor.isNotEmpty()) {
        try {
            val base = Color(android.graphics.Color.parseColor(course.customColor))
            val end = base.copy(alpha = if (isDarkMode) 0.70f else 0.86f)
            return if (isDarkMode) base.copy(alpha = 0.88f) to end else base to end
        } catch (_: Exception) {
        }
    }

    val name = course.name
    return when {
        name.contains("操作系统") && !name.contains("实验") ->
            if (isDarkMode) CourseOsStart.copy(alpha = 0.88f) to CourseOsEnd.copy(alpha = 0.84f)
            else CourseOsStart to CourseOsEnd
        name.contains("算法") ->
            if (isDarkMode) CourseAlgoStart.copy(alpha = 0.88f) to CourseAlgoEnd.copy(alpha = 0.84f)
            else CourseAlgoStart to CourseAlgoEnd
        name.contains("数据库") ->
            if (isDarkMode) CourseDbStart.copy(alpha = 0.88f) to CourseDbEnd.copy(alpha = 0.84f)
            else CourseDbStart to CourseDbEnd
        name.contains("人工智能") ->
            if (isDarkMode) CourseAiStart.copy(alpha = 0.88f) to CourseAiEnd.copy(alpha = 0.84f)
            else CourseAiStart to CourseAiEnd
        name.contains("网络") || name.contains("实验") ->
            if (isDarkMode) CourseNetStart.copy(alpha = 0.88f) to CourseNetEnd.copy(alpha = 0.84f)
            else CourseNetStart to CourseNetEnd
        name.contains("职业") || name.contains("规划") ->
            if (isDarkMode) CourseCareerStart.copy(alpha = 0.88f) to CourseCareerEnd.copy(alpha = 0.84f)
            else CourseCareerStart to CourseCareerEnd
        name.contains("软件") ->
            if (isDarkMode) CourseSeStart.copy(alpha = 0.88f) to CourseSeEnd.copy(alpha = 0.84f)
            else CourseSeStart to CourseSeEnd
        name.contains("编译") ->
            if (isDarkMode) CourseCompilerStart.copy(alpha = 0.88f) to CourseCompilerEnd.copy(alpha = 0.84f)
            else CourseCompilerStart to CourseCompilerEnd
        else -> when (course.type) {
            CourseType.REQUIRED ->
                if (isDarkMode) CourseRequiredDark to CourseOsEnd.copy(alpha = 0.84f)
                else CourseRequired to CourseOsEnd
            CourseType.ELECTIVE ->
                if (isDarkMode) CourseElectiveDark to CourseDbEnd.copy(alpha = 0.84f)
                else CourseElective to CourseDbEnd
            CourseType.LAB ->
                if (isDarkMode) CourseLabDark to CourseNetEnd.copy(alpha = 0.84f)
                else CourseLab to CourseNetEnd
            CourseType.CUSTOM ->
                if (isDarkMode) CourseCustomDark to CourseCareerEnd.copy(alpha = 0.84f)
                else CourseCustom to CourseCareerEnd
        }
    }
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
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = schedulePalette(isDarkMode, hasWallpaperBackground)
    val (rawStart, rawEnd) = remember(course, isDarkMode) { courseGradientColors(course, isDarkMode) }
    val startColor = if (hasWallpaperBackground) rawStart.copy(alpha = 0.90f) else rawStart
    val endColor = if (hasWallpaperBackground) rawEnd.copy(alpha = 0.82f) else rawEnd
    val shape = RoundedCornerShape(ScheduleDimensions.CourseCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) ScheduleMotion.PressedScale else 1f,
        animationSpec = tween(
            durationMillis = ScheduleMotion.FastMillis,
            easing = ScheduleMotion.StandardEasing
        ),
        label = "courseCardPressScale"
    )
    val teacherLine = remember(course.teacher) { course.teacher.trim() }
    val locationLine = remember(course.location) { course.location.trim() }
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
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
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
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (hasWallpaperBackground) 0.18f else 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (hasWallpaperBackground) 0.16f else 0.08f)
                        )
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
                        Modifier.border(1.dp, palette.courseStroke, shape)
                    }
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
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
                        color = Color.White.copy(alpha = 0.88f),
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
                        color = Color.White.copy(alpha = 0.88f),
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
                        color = Color.White.copy(alpha = 0.92f),
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
