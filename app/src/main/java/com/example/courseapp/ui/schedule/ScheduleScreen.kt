package com.example.courseapp.ui.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.CourseCard
import com.example.courseapp.ui.components.CourseContextMenu
import com.example.courseapp.ui.components.CourseDetailSheet
import com.example.courseapp.ui.components.CourseMoveSheet
import com.example.courseapp.ui.components.WeekSelectorDialog
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ScheduleViewModel
import com.example.courseapp.viewmodel.TimeSlot

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val SLOT_HEIGHT = 64.dp
private val HEADER_HEIGHT = 50.dp
private val TIME_AXIS_WIDTH = 58.dp

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    onScrollOffsetChanged: ((Float) -> Unit)? = null
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val currentWeek by viewModel.currentWeek.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isFabOpen by viewModel.isFabOpen.collectAsStateWithLifecycle()
    val selectedCourse by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val timeSlots by viewModel.timeSlots.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    var showWeekDialog by remember { mutableStateOf(false) }
    var snackbarState by remember { mutableStateOf(SnackbarState()) }
    var contextMenuCourse by remember { mutableStateOf<Course?>(null) }
    var moveSheetCourse by remember { mutableStateOf<Course?>(null) }
    var pendingMove by remember { mutableStateOf<Triple<Course, Int, Int>?>(null) }

    val weekDates = remember(currentWeek) { viewModel.getWeekDates(currentWeek) }
    val timeSlotLabels = remember(timeSlots) { viewModel.timeSlotLabels() }

    LaunchedEffect(snackbarFlow.value.first) {
        val (msg, type) = snackbarFlow.value
        if (msg.isNotEmpty()) {
            snackbarState = SnackbarState(msg, type, true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val gridScrollState = rememberScrollState()
            LaunchedEffect(Unit) {
                snapshotFlow { gridScrollState.value }
                    .collect { onScrollOffsetChanged?.invoke(it.toFloat()) }
            }
            ScheduleGrid(
                courses = courses,
                currentWeek = currentWeek,
                todayIndex = viewModel.todayIndex,
                isDarkMode = isDarkMode,
                weekDates = weekDates,
                timeSlots = timeSlots,
                onCourseClick = { viewModel.onCourseClick(it) },
                onCourseLongClick = { contextMenuCourse = it },
                isCourseActive = { viewModel.isCourseActive(it) },
                isCourseUpcoming = { viewModel.isCourseUpcoming(it) },
                getCourseProgress = { viewModel.getCourseProgress(it) },
                getMinutesUntil = { viewModel.getMinutesUntilCourse(it) },
                scrollState = gridScrollState,
                modifier = Modifier.weight(1f)
            )
        }

        // FAB
        FabMenu(
            isOpen = isFabOpen,
            isDarkMode = isDarkMode,
            onToggle = { viewModel.toggleFab() },
            onImportSchool = {
                viewModel.closeFab()
                onNavigateToImport()
            },
            onImportPdf = {
                viewModel.closeFab()
                viewModel.showMessage("请选择PDF文件...", "info")
            },
            onManualAdd = {
                viewModel.closeFab()
                onNavigateToAdd()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        )

        if (showWeekDialog) {
            WeekSelectorDialog(
                currentWeek = currentWeek,
                isDarkMode = isDarkMode,
                onWeekSelected = {
                    viewModel.selectWeek(it)
                    showWeekDialog = false
                },
                onDismiss = { showWeekDialog = false }
            )
        }

        if (snackbarState.visible) {
            AppSnackbar(
                message = snackbarState.message,
                type = snackbarState.type,
                onDismiss = { snackbarState = snackbarState.copy(visible = false) }
            )
        }
    }

    // Course detail bottom sheet
    CourseDetailSheet(
        course = selectedCourse,
        isDarkMode = isDarkMode,
        timeSlotLabels = timeSlotLabels,
        onDismiss = { viewModel.dismissCourseDetail() },
        onSave = { viewModel.updateCourse(it) },
        onDelete = { viewModel.deleteCourse(it) },
        onCopy = { viewModel.copyCourse(it) }
    )

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
        onDismiss = { moveSheetCourse = null },
        onMove = { course, day, slot ->
            pendingMove = Triple(course, day, slot)
            moveSheetCourse = null
        },
        onMoveAndReplace = { course, day, slot ->
            viewModel.moveCourseTo(course, day, slot, replaceExisting = true)
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
                    TextButton(onClick = {
                        viewModel.moveCourseTo(course, day, slot, replaceExisting = true)
                        pendingMove = null
                    }) {
                        Text("替换")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingMove = null }) {
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

@Composable
private fun ScheduleGrid(
    courses: List<Course>,
    currentWeek: Int,
    todayIndex: Int,
    isDarkMode: Boolean,
    weekDates: List<String>,
    timeSlots: List<TimeSlot>,
    onCourseClick: (Course) -> Unit,
    onCourseLongClick: (Course) -> Unit = {},
    isCourseActive: (Course) -> Boolean = { false },
    isCourseUpcoming: (Course) -> Boolean = { false },
    getCourseProgress: (Course) -> Float = { 0f },
    getMinutesUntil: (Course) -> Int = { 0 },
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkMode) BgDark else BgLight
    val timeLabelColor = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary
    val separatorColor = if (isDarkMode) Color(0xFF2A2D38) else Color(0xFFECEEF4)

    val periodCount = timeSlots.size

    Row(modifier = modifier.background(bgColor)) {
        // ── Time axis (sticky left) ──
        Column(
            modifier = Modifier
                .width(TIME_AXIS_WIDTH)
                .verticalScroll(scrollState)
                .background(bgColor)
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
                    color = if (isDarkMode) Color(0xFF6B7080) else Color(0xFFB0B4C0),
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
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = ts.start,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            color = timeLabelColor,
                            letterSpacing = 0.1.sp,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = ts.end,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Thin,
                            color = timeLabelColor.copy(alpha = 0.55f),
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
                .horizontalScroll(rememberScrollState())
                .then(Modifier.verticalScroll(scrollState))
        ) {
            for (day in 0..6) {
                val dayCourses = courses.filter { it.dayOfWeek == day }
                Column(
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .weight(1f, fill = false)
                ) {
                    // Day header with date — glass style
                    val isToday = day == todayIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT)
                            .background(
                                if (isToday) {
                                    if (isDarkMode) TodayHeaderBgDark else TodayHeaderBg
                                } else bgColor
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayLabels[day],
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) {
                                    if (isDarkMode) PrimaryLight else Primary
                                } else if (isDarkMode) Color(0xFF8E93A6) else TextSecondary,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = weekDates.getOrElse(day) { "" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                color = if (isToday) {
                                    if (isDarkMode) PrimaryLight.copy(alpha = 0.7f) else Primary.copy(alpha = 0.7f)
                                } else if (isDarkMode) Color(0xFF6B7080) else Color(0xFFB0B4C0),
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
                        for (i in 1 until periodCount) {
                            Divider(
                                modifier = Modifier.offset(y = SLOT_HEIGHT * i),
                                color = separatorColor,
                                thickness = 0.5.dp
                            )
                        }

                        // Course cards
                        for (course in dayCourses) {
                            val cardHeight = SLOT_HEIGHT * course.slotCount - 3.dp
                            val active = isCourseActive(course)
                            val upcoming = isCourseUpcoming(course)

                            CourseCard(
                                course = course,
                                isDarkMode = isDarkMode,
                                isActive = active,
                                isUpcoming = upcoming,
                                progress = if (active) getCourseProgress(course) else 0f,
                                minutesUntil = if (upcoming) getMinutesUntil(course) else 0,
                                onClick = { onCourseClick(course) },
                                onLongClick = { onCourseLongClick(course) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cardHeight)
                                    .padding(horizontal = 2.dp, vertical = 1.5.dp)
                                    .offset(y = SLOT_HEIGHT * course.startSlot)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FabMenu(
    isOpen: Boolean,
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    onImportSchool: () -> Unit,
    onImportPdf: () -> Unit,
    onManualAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "fabRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FabMenuItem(
                    icon = Icons.Default.Language,
                    label = "学校官网导入",
                    isDarkMode = isDarkMode,
                    onClick = onImportSchool
                )
                FabMenuItem(
                    icon = Icons.Default.InsertDriveFile,
                    label = "PDF文件导入",
                    isDarkMode = isDarkMode,
                    onClick = onImportPdf
                )
                FabMenuItem(
                    icon = Icons.Default.Edit,
                    label = "手动添加课程",
                    isDarkMode = isDarkMode,
                    onClick = onManualAdd
                )
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (isOpen) Error else Accent,
            contentColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isOpen) "关闭" else "添加",
                modifier = Modifier.size(24.dp).rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isDarkMode) CardDark else CardLight
    val textColor = if (isDarkMode) Color.White else TextPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}
