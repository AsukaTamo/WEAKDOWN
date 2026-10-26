package com.example.courseapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
            .border(
                width = if (isActive) 1.6.dp else 1.dp,
                color = if (isActive) palette.courseHighlight else palette.courseStroke,
                shape = shape
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
