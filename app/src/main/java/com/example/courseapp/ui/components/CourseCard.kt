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
    val compact = course.slotCount <= 1

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
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = course.name,
                color = Color.White,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (compact) 12.sp else 13.sp,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (course.location.isNotEmpty()) {
                Text(
                    text = course.location,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!compact) {
                Text(
                    text = "第${course.startSlot + 1}-${course.startSlot + course.slotCount}节 · ${course.weekRange}",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 8.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (course.teacher.isNotEmpty()) {
                    Text(
                        text = course.teacher,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isActive && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.24f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
            } else if (isUpcoming && minutesUntil > 0 && !compact) {
                Text(
                    text = if (minutesUntil < 60) {
                        "${minutesUntil}分钟后开始"
                    } else {
                        "${minutesUntil / 60}小时${minutesUntil % 60}分钟后开始"
                    },
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
