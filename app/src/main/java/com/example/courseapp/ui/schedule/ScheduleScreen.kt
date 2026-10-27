package com.example.courseapp.ui.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import android.graphics.BitmapFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Course
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.CourseCard
import com.example.courseapp.ui.components.CourseContextMenu
import com.example.courseapp.ui.components.CourseMoveSheet
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ScheduleViewModel
import com.example.courseapp.viewmodel.TimeSlot

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val SLOT_HEIGHT = ScheduleDimensions.SlotHeight
private val HEADER_HEIGHT = ScheduleDimensions.HeaderHeight
private val TIME_AXIS_WIDTH = ScheduleDimensions.TimeAxisWidth

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    drawBackground: Boolean = true,
    entranceEpoch: Int = 1
) {
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val currentWeek by viewModel.currentWeek.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val timeSlots by viewModel.timeSlots.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")
    val backgroundUri by viewModel.backgroundUri.collectAsStateWithLifecycle()
    val scrimAlpha by viewModel.scrimAlpha.collectAsStateWithLifecycle()
    val showScheduleGuides by viewModel.showScheduleGuides.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val calendarWeek by viewModel.calendarWeek.collectAsStateWithLifecycle()

    var snackbarState by remember { mutableStateOf(SnackbarState()) }
    var contextMenuCourse by remember { mutableStateOf<Course?>(null) }
    var moveSheetCourse by remember { mutableStateOf<Course?>(null) }
    var pendingMove by remember { mutableStateOf<Triple<Course, Int, Int>?>(null) }

    LaunchedEffect(snackbarFlow.value.first) {
        val (msg, type) = snackbarFlow.value
        if (msg.isNotEmpty()) {
            snackbarState = SnackbarState(msg, type, true)
        }
    }

    val hasBackground = backgroundUri.isNotEmpty()
    val palette = schedulePalette(isDarkMode, hasBackground)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.appCanvas,
                        palette.appCanvasEnd
                    )
                )
            )
    ) {
        if (drawBackground && backgroundUri.isNotEmpty()) {
            val file = java.io.File(backgroundUri)
            if (file.exists()) {
                val bitmap = remember(backgroundUri) {
                    BitmapFactory.decodeFile(backgroundUri)?.asImageBitmap()
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val maskColor = appColors(isDarkMode).canvas
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(maskColor.copy(alpha = scrimAlpha))
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to maskColor.copy(alpha = 0.18f),
                                        0.18f to Color.Transparent,
                                        0.72f to Color.Transparent,
                                        1.00f to maskColor.copy(alpha = 0.18f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val gridScrollState = rememberScrollState()
            val maxSelectableWeek = activeSemester?.totalWeeks?.coerceAtLeast(1)
                ?: ScheduleViewModel.DEFAULT_SELECTABLE_WEEK_LIMIT
            val weekPages = remember(maxSelectableWeek, allCourses, activeSemester) {
                buildScheduleWeekPages(
                    maxWeek = maxSelectableWeek,
                    allCourses = allCourses,
                    getWeekDates = viewModel::getWeekDates
                )
            }
            val pagerState = rememberPagerState(
                initialPage = (currentWeek - 1).coerceIn(0, maxSelectableWeek - 1),
                pageCount = { maxSelectableWeek }
            )
            val latestCurrentWeek by rememberUpdatedState(currentWeek)

            LaunchedEffect(currentWeek, maxSelectableWeek) {
                val targetPage = (currentWeek - 1).coerceIn(0, maxSelectableWeek - 1)
                if (pagerState.currentPage != targetPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }

            LaunchedEffect(pagerState, maxSelectableWeek) {
                snapshotFlow { pagerState.currentPage to pagerState.targetPage }
                    .collect { (currentPage, targetPage) ->
                        val previewPage = if (targetPage != currentPage) {
                            targetPage
                        } else {
                            currentPage
                        }
                        viewModel.previewWeek(previewPage + 1)
                    }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }
                    .collect { settledPage ->
                        val settledWeek = settledPage + 1
                        if (settledWeek != latestCurrentWeek) {
                            viewModel.selectWeek(settledWeek)
                        }
                    }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it }
            ) { pageIndex ->
                val week = pageIndex + 1
                val page = weekPages[week] ?: emptyScheduleWeekPage(week, viewModel.getWeekDates(week))

                ScheduleGrid(
                    coursesByDay = page.coursesByDay,
                    todayIndex = clock.dayIndex,
                    isCurrentWeek = week == calendarWeek,
                    isDarkMode = isDarkMode,
                    hasBackground = hasBackground,
                    showGuides = showScheduleGuides,
                    weekDates = page.weekDates,
                    timeSlots = timeSlots,
                    onCourseClick = { viewModel.onCourseClick(it) },
                    onCourseLongClick = { contextMenuCourse = it },
                    isCourseActive = { viewModel.isCourseActive(it, clock) },
                    isCourseUpcoming = { viewModel.isCourseUpcoming(it, clock) },
                    getCourseProgress = { viewModel.getCourseProgress(it, clock) },
                    getMinutesUntil = { viewModel.getMinutesUntilCourse(it, clock) },
                    getRemainingMinutes = { viewModel.getCourseRemainingMinutes(it, clock) },
                    scrollState = gridScrollState,
                    entranceEpoch = entranceEpoch,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (snackbarState.visible) {
            AppSnackbar(
                message = snackbarState.message,
                type = snackbarState.type,
                onDismiss = { snackbarState = snackbarState.copy(visible = false) }
            )
        }
    }

    // Course detail 抽屉改由 MainActivity 根布局承载（遮罩需要盖住顶栏/底栏），这里不再渲染

    // Context menu on long press
    CourseContextMenu(
        course = contextMenuCourse,
        isDarkMode = isDarkMode,
        onDismiss = { contextMenuCourse = null },
        onEdit = { viewModel.onCourseClick(it) },
        onDelete = { viewModel.deleteCourse(it) },
        onCopy = { viewModel.copyCourse(it) },
        onDrag = { moveSheetCourse = it }
    )

    // Move course sheet
    CourseMoveSheet(
        course = moveSheetCourse,
        isDarkMode = isDarkMode,
        periodCount = timeSlots.size,
        onDismiss = { moveSheetCourse = null },
        onMove = { course, day, slot ->
            pendingMove = Triple(course, day, slot)
            moveSheetCourse = null
        }
    )

    // Conflict resolution dialog
    if (pendingMove != null) {
        val (course, day, slot) = pendingMove!!
        var conflicts by remember { mutableStateOf<List<Course>?>(null) }
        LaunchedEffect(pendingMove) {
            conflicts = viewModel.getConflicts(course, day, slot)
        }
        val currentConflicts = conflicts
        if (currentConflicts == null) {
            // Loading — do nothing yet
        } else if (currentConflicts.isNotEmpty()) {

            AlertDialog(
                onDismissRequest = { pendingMove = null },
                title = { Text("时间冲突") },
                text = {
                    Text("目标时段与以下课程冲突：${currentConflicts.joinToString { it.name }}。是否替换？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.moveCourseTo(course, day, slot, replaceExisting = true)
                            pendingMove = null
                        },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Text("替换")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingMove = null },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Text("取消")
                    }
                }
            )
        } else {
            // No conflict, move directly
            LaunchedEffect(currentConflicts) {
                viewModel.moveCourseTo(course, day, slot)
                pendingMove = null
            }
        }
    }
}

private data class SnackbarState(
    val message: String = "",
    val type: String = "info",
    val visible: Boolean = false
)

private data class ScheduleWeekPage(
    val week: Int,
    val coursesByDay: List<List<Course>>,
    val weekDates: List<String>
)

private fun emptyScheduleWeekPage(
    week: Int,
    weekDates: List<String>
): ScheduleWeekPage {
    return ScheduleWeekPage(
        week = week,
        coursesByDay = List(7) { emptyList() },
        weekDates = weekDates
    )
}

private fun buildScheduleWeekPages(
    maxWeek: Int,
    allCourses: List<Course>,
    getWeekDates: (Int) -> List<String>
): Map<Int, ScheduleWeekPage> {
    if (maxWeek <= 0) return emptyMap()

    val groupedByWeekAndDay = List(maxWeek + 1) {
        List(7) { mutableListOf<Course>() }
    }

    for (course in allCourses) {
        val day = course.dayOfWeek
        if (day !in 0..6) continue
        for (week in 1..maxWeek) {
            if (ScheduleViewModel.isWeekInRange(week, course.weekRange)) {
                groupedByWeekAndDay[week][day].add(course)
            }
        }
    }

    return (1..maxWeek).associateWith { week ->
        ScheduleWeekPage(
            week = week,
            coursesByDay = groupedByWeekAndDay[week].map { it.toList() },
            weekDates = getWeekDates(week)
        )
    }
}

@Composable
private fun ScheduleGrid(
    coursesByDay: List<List<Course>>,
    todayIndex: Int,
    isCurrentWeek: Boolean = true,
    isDarkMode: Boolean,
    hasBackground: Boolean = false,
    showGuides: Boolean = true,
    weekDates: List<String>,
    timeSlots: List<TimeSlot>,
    onCourseClick: (Course) -> Unit,
    onCourseLongClick: (Course) -> Unit = {},
    isCourseActive: (Course) -> Boolean = { false },
    isCourseUpcoming: (Course) -> Boolean = { false },
    getCourseProgress: (Course) -> Float = { 0f },
    getMinutesUntil: (Course) -> Int = { 0 },
    getRemainingMinutes: (Course) -> Int = { 0 },
    entranceEpoch: Int = 1,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val palette = schedulePalette(isDarkMode, hasBackground)
    val bgColor = if (hasBackground) Color.Transparent else palette.pageFallback
    val headerSurface = palette.headerBackdrop

    val periodCount = timeSlots.size

    Row(
        modifier = modifier
            .background(bgColor)
            .then(
                if (hasBackground) {
                    Modifier.background(palette.gridGlass)
                } else {
                    Modifier
                }
            )
    ) {
        // ── Time axis (sticky left) ──
        Column(
            modifier = Modifier
                .width(TIME_AXIS_WIDTH)
                .verticalScroll(scrollState)
                // 节次栏做成和星期胶囊同一套圆角卡片：顶部留 7dp 与胶囊对齐，
                // 左右各留 3dp；底部留 2dp，让最后一行时间和圆角都收得下。
                .drawBehind {
                    val insetX = 3.dp.toPx()
                    val insetTop = 7.dp.toPx()
                    val insetBottom = 2.dp.toPx()
                    drawRoundRect(
                        color = palette.headerBackdrop,
                        topLeft = Offset(insetX, insetTop),
                        size = Size(
                            width = size.width - insetX * 2f,
                            height = size.height - insetTop - insetBottom
                        ),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
        ) {
            // Header spacer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "节次",
                    fontSize = 9.sp,
                    color = palette.mutedText,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )
            }
            // Each period = one cell with start~end time
            for (i in 0 until periodCount) {
                val ts = timeSlots[i]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SLOT_HEIGHT)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${i + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.dayText,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ts.start,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.timeText,
                            letterSpacing = 0.1.sp,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = ts.end,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Thin,
                            color = palette.mutedText,
                            letterSpacing = 0.1.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        // ── Day columns ──
        Row(
            modifier = Modifier
                .weight(1f)
                .then(Modifier.verticalScroll(scrollState))
        ) {
            for (day in 0..6) {
                val dayCourses = coursesByDay.getOrElse(day) { emptyList() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    // Day header with date — glass style
                    val isToday = isCurrentWeek && day == todayIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT)
                            .padding(horizontal = 3.dp, vertical = 7.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isToday) {
                                    palette.todaySurface
                                } else {
                                    headerSurface
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayLabels[day],
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) palette.todayText else palette.headerText,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = weekDates.getOrElse(day) { "" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                color = if (isToday) palette.todayText.copy(alpha = 0.78f) else palette.mutedText,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    // Day column: each period = one cell
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SLOT_HEIGHT * periodCount)
                    ) {
                        // Subtle horizontal separators
                        if (showGuides) {
                            for (i in 1 until periodCount) {
                                Divider(
                                    modifier = Modifier.offset(y = SLOT_HEIGHT * i),
                                    color = palette.gridLine,
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        // Course cards
                        for (course in dayCourses) {
                            val cardHeight = SLOT_HEIGHT * course.slotCount - 1.dp
                            val active = isCurrentWeek && isCourseActive(course)
                            val upcoming = isCurrentWeek && isCourseUpcoming(course)

                            key(course.id, course.dayOfWeek, course.startSlot, course.name) {
                                CourseCard(
                                    course = course,
                                    isDarkMode = isDarkMode,
                                    isActive = active,
                                    isUpcoming = upcoming,
                                    progress = if (active) getCourseProgress(course) else 0f,
                                    minutesUntil = if (upcoming) getMinutesUntil(course) else 0,
                                    remainingMinutes = if (active) getRemainingMinutes(course) else 0,
                                    hasWallpaperBackground = hasBackground,
                                    entranceEpoch = entranceEpoch,
                                    onClick = { onCourseClick(course) },
                                    onLongClick = { onCourseLongClick(course) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cardHeight)
                                        .padding(
                                            horizontal = ScheduleDimensions.CourseInset,
                                            vertical = 2.dp
                                        )
                                        .offset(y = SLOT_HEIGHT * course.startSlot)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
