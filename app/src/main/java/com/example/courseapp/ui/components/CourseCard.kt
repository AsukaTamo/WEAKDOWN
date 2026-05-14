package com.example.courseapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.theme.*

fun courseGradientColors(course: Course, isDarkMode: Boolean): Pair<Color, Color> {
    // Custom color override takes priority
    if (course.customColor.isNotEmpty()) {
        try {
            val base = Color(android.graphics.Color.parseColor(course.customColor))
            val end = base.copy(alpha = if (isDarkMode) 0.7f else 0.85f)
            return if (isDarkMode) base.copy(alpha = 0.85f) to end else base to end
        } catch (_: Exception) { }
    }
    val name = course.name
    return when {
        name.contains("操作系统") && !name.contains("实验") ->
            if (isDarkMode) CourseOsStart.copy(alpha = 0.85f) to CourseOsEnd.copy(alpha = 0.85f)
            else CourseOsStart to CourseOsEnd
        name.contains("算法") ->
            if (isDarkMode) CourseAlgoStart.copy(alpha = 0.85f) to CourseAlgoEnd.copy(alpha = 0.85f)
            else CourseAlgoStart to CourseAlgoEnd
        name.contains("数据库") ->
            if (isDarkMode) CourseDbStart.copy(alpha = 0.85f) to CourseDbEnd.copy(alpha = 0.85f)
            else CourseDbStart to CourseDbEnd
        name.contains("人工智能") ->
            if (isDarkMode) CourseAiStart.copy(alpha = 0.85f) to CourseAiEnd.copy(alpha = 0.85f)
            else CourseAiStart to CourseAiEnd
        name.contains("网络") || name.contains("实验") ->
            if (isDarkMode) CourseNetStart.copy(alpha = 0.85f) to CourseNetEnd.copy(alpha = 0.85f)
            else CourseNetStart to CourseNetEnd
        name.contains("职业") || name.contains("规划") ->
            if (isDarkMode) CourseCareerStart.copy(alpha = 0.85f) to CourseCareerEnd.copy(alpha = 0.85f)
            else CourseCareerStart to CourseCareerEnd
        name.contains("软件") ->
            if (isDarkMode) CourseSeStart.copy(alpha = 0.85f) to CourseSeEnd.copy(alpha = 0.85f)
            else CourseSeStart to CourseSeEnd
        name.contains("编译") ->
            if (isDarkMode) CourseCompilerStart.copy(alpha = 0.85f) to CourseCompilerEnd.copy(alpha = 0.85f)
            else CourseCompilerStart to CourseCompilerEnd
        else -> when (course.type) {
            CourseType.REQUIRED ->
                if (isDarkMode) CourseRequiredDark to CourseOsEnd.copy(alpha = 0.85f)
                else CourseRequired to CourseOsEnd
            CourseType.ELECTIVE ->
                if (isDarkMode) CourseElectiveDark to CourseDbEnd.copy(alpha = 0.85f)
                else CourseElective to CourseDbEnd
            CourseType.LAB ->
                if (isDarkMode) CourseLabDark to CourseNetEnd.copy(alpha = 0.85f)
                else CourseLab to CourseNetEnd
            CourseType.CUSTOM ->
                if (isDarkMode) CourseCustomDark to CourseCareerEnd.copy(alpha = 0.85f)
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
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (startColor, endColor) = remember(course, isDarkMode) { courseGradientColors(course, isDarkMode) }
    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isActive) 6.dp else 4.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(colors = listOf(startColor, endColor)),
                shape = cardShape
            )
            .then(
                if (isActive) Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), cardShape)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), cardShape)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = course.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
                letterSpacing = 0.2.sp,
                modifier = Modifier.fillMaxWidth()
            )
            // Slot range + week info
            Text(
                text = "第${course.startSlot + 1}-${course.startSlot + course.slotCount}节 · ${course.weekRange}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.1.sp,
                modifier = Modifier.fillMaxWidth()
            )
            if (course.location.isNotEmpty()) {
                Text(
                    text = course.location,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Active progress bar or upcoming countdown
            if (isActive && progress > 0f) {
                Column {
                    Text(
                        text = "进行中",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = progress)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                }
            } else if (isUpcoming && minutesUntil > 0) {
                Text(
                    text = if (minutesUntil < 60) "${minutesUntil}分钟后开始"
                    else "${minutesUntil / 60}小时${minutesUntil % 60}分后开始",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
