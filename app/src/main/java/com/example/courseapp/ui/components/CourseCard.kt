package com.example.courseapp.ui.components

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
            return base.copy(alpha = 0.95f) to base.copy(alpha = if (isDarkMode) 0.72f else 0.82f)
        } catch (_: Exception) {
        }
    }

    val name = course.name
    return when {
        name.contains("操作系统") && !name.contains("实验") ->
            CourseOsStart to CourseOsEnd
        name.contains("算法") ->
            CourseAlgoStart to CourseAlgoEnd
        name.contains("数据库") ->
            CourseDbStart to CourseDbEnd
        name.contains("人工智能") ->
            CourseAiStart to CourseAiEnd
        name.contains("网络") || name.contains("实验") ->
            CourseNetStart to CourseNetEnd
        name.contains("职业") || name.contains("规划") ->
            CourseCareerStart to CourseCareerEnd
        name.contains("软件") ->
            CourseSeStart to CourseSeEnd
        name.contains("编译") ->
            CourseCompilerStart to CourseCompilerEnd
        else -> when (course.type) {
            CourseType.REQUIRED ->
                if (isDarkMode) CourseRequiredDark to CourseOsEnd else CourseRequired to CourseOsEnd
            CourseType.ELECTIVE ->
                if (isDarkMode) CourseElectiveDark to CourseDbEnd else CourseElective to CourseDbEnd
            CourseType.LAB ->
                if (isDarkMode) CourseLabDark to CourseNetEnd else CourseLab to CourseNetEnd
            CourseType.CUSTOM ->
                if (isDarkMode) CourseCustomDark to CourseCareerEnd else CourseCustom to CourseCareerEnd
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
    hasWallpaperBackground: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = schedulePalette(isDarkMode, hasWallpaperBackground)
    val (rawStart, rawEnd) = remember(course, isDarkMode) { courseGradientColors(course, isDarkMode) }
    val startColor = rawStart.copy(alpha = if (hasWallpaperBackground) 0.92f else 0.96f)
    val endColor = rawEnd.copy(alpha = if (hasWallpaperBackground) 0.78f else 0.86f)
    val shape = RoundedCornerShape(ScheduleDimensions.CourseCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) ScheduleMotion.PressedScale else 1f,
        animationSpec = tween(ScheduleMotion.FastMillis, easing = ScheduleMotion.StandardEasing),
        label = "courseCardPressScale"
    )
    val compact = course.slotCount <= 1
    val rawLocationText = course.location.trim()
    val rawTeacherText = course.teacher.trim()
    val courseDetailText = remember(rawTeacherText, rawLocationText) {
        buildList {
            if (rawTeacherText.isNotEmpty()) add(rawTeacherText)
            if (rawLocationText.isNotEmpty()) add("@$rawLocationText")
        }.joinToString("  ")
    }
    val useLightContent = isDarkMode || hasWallpaperBackground
    val primaryTextColor = if (useLightContent) Color.White else Color(0xFF102027)
    val secondaryTextColor = if (useLightContent) Color.White.copy(alpha = 0.88f) else Color(0xFF31424D)
    val progressColor = if (useLightContent) Color.White else Color(0xFF102027)
    val borderColor = when {
        isActive && useLightContent -> palette.courseHighlight
        isActive -> Color(0xFF0F8F7B).copy(alpha = 0.70f)
        useLightContent -> palette.courseStroke
        else -> Color(0xFF102027).copy(alpha = 0.24f)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isActive) 12.dp else 7.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(startColor, endColor)), shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.20f)
                    )
                ),
                shape
            )
            .border(
                width = if (isActive) 1.8.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Text(
                text = course.name,
                color = primaryTextColor,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (compact) 12.sp else 13.sp,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )

            if (courseDetailText.isNotEmpty()) {
                Text(
                    text = courseDetailText,
                    color = secondaryTextColor,
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = if (compact) 11.sp else 12.sp,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isActive && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(progressColor.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(progressColor, RoundedCornerShape(2.dp))
                    )
                }
            } else if (isUpcoming && minutesUntil > 0 && !compact) {
                Text(
                    text = if (minutesUntil < 60) {
                        "${minutesUntil}分钟后"
                    } else {
                        "${minutesUntil / 60}小时${minutesUntil % 60}分钟后"
                    },
                    color = secondaryTextColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}
