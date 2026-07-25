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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.example.courseapp.ui.components.CourseDetailSheet
import com.example.courseapp.ui.components.CourseMoveSheet
import com.example.courseapp.ui.components.WeekSelectorDialog
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ScheduleViewModel
import com.example.courseapp.viewmodel.TimeSlot

private val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val SLOT_HEIGHT = ScheduleDimensions.SlotHeight
private val HEADER_HEIGHT = ScheduleDimensions.HeaderHeight
private val TIME_AXIS_WIDTH = ScheduleDimensions.TimeAxisWidth

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
    val backgroundUri by viewModel.backgroundUri.collectAsStateWithLifecycle()
    val scrimAlpha by viewModel.scrimAlpha.collectAsStateWithLifecycle()

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
        // Background image layer
        if (backgroundUri.isNotEmpty()) {
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
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = scrimAlpha))
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Black.copy(alpha = 0.18f),
                                        0.18f to Color.Transparent,
                                        0.72f to Color.Transparent,
                                        1.00f to Color.Black.copy(alpha = 0.18f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        val hasBackground = backgroundUri.isNotEmpty()
        Column(modifier = Modifier.fillMaxSize()) {
            val gridScrollState = rememberScrollState()
            LaunchedEffect(Unit) {
                snapshotFlow { gridScrollState.value }
                    .collect { onScrollOffsetChanged?.invoke(it.toFloat()) }
            }
            ScheduleGrid(
                courses = courses,
                todayIndex = viewModel.todayIndex,
                isDarkMode = isDarkMode,
                hasBackground = hasBackground,
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
            hasBackground = hasBackground,
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
    todayIndex: Int,
    isDarkMode: Boolean,
    hasBackground: Boolean = false,
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
    val palette = schedulePalette(isDarkMode, hasBackground)
    val bgColor = if (hasBackground) Color.Transparent else palette.pageFallback
    val headerSurface = if (hasBackground) Color.Black.copy(alpha = 0.10f) else bgColor

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
                .background(if (hasBackground) Color.Black.copy(alpha = 0.08f) else bgColor)
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
                        verticalArrangement = Arrangement.Center
                    ) {
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
                        for (i in 1 until periodCount) {
                            Divider(
                                modifier = Modifier.offset(y = SLOT_HEIGHT * i),
                                color = palette.gridLine,
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
                                hasWallpaperBackground = hasBackground,
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

private data class FabMenuAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun FabMenu(
    isOpen: Boolean,
    isDarkMode: Boolean,
    hasBackground: Boolean,
    onToggle: () -> Unit,
    onImportSchool: () -> Unit,
    onImportPdf: () -> Unit,
    onManualAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = schedulePalette(isDarkMode, hasBackground)
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) 45f else 0f,
        animationSpec = tween(
            durationMillis = ScheduleMotion.StandardMillis,
            easing = ScheduleMotion.EmphasizedEasing
        ),
        label = "fabRotation"
    )
    val actions = listOf(
        FabMenuAction(Icons.Default.Share, "学校官网导入", onImportSchool),
        FabMenuAction(Icons.Default.Star, "PDF文件导入", onImportPdf),
        FabMenuAction(Icons.Default.Edit, "手动添加课程", onManualAdd)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEachIndexed { index, action ->
                val delay = index * ScheduleMotion.FabStaggerMillis
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn(
                        animationSpec = tween(ScheduleMotion.FastMillis, delayMillis = delay)
                    ) + slideInVertically(
                        animationSpec = tween(ScheduleMotion.StandardMillis, delayMillis = delay),
                        initialOffsetY = { it / 3 }
                    ) + scaleIn(
                        animationSpec = tween(ScheduleMotion.StandardMillis, delayMillis = delay),
                        initialScale = 0.96f
                    ),
                    exit = fadeOut(animationSpec = tween(90)) + scaleOut(
                        animationSpec = tween(90),
                        targetScale = 0.98f
                    )
                ) {
                    FabMenuItem(
                        icon = action.icon,
                        label = action.label,
                        isDarkMode = isDarkMode,
                        hasBackground = hasBackground,
                        onClick = action.onClick
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (isOpen) Error else palette.fabContainer,
            contentColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.size(ScheduleDimensions.FabSize)
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
    hasBackground: Boolean,
    onClick: () -> Unit
) {
    val palette = schedulePalette(isDarkMode, hasBackground)
    val bgColor = palette.fabMenuSurface
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
            tint = if (hasBackground) Color(0xFF0F766E) else Primary,
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
