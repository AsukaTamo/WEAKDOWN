package com.example.courseapp.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.ui.components.WeekSelectorDialog
import com.example.courseapp.ui.import.ImportPreviewScreen
import com.example.courseapp.ui.import.WebImportScreen
import com.example.courseapp.ui.manage.ManageScreen
import com.example.courseapp.ui.profile.ProfileScreen
import com.example.courseapp.ui.schedule.ScheduleScreen
import com.example.courseapp.ui.settings.TimeSlotSettingsScreen
import com.example.courseapp.ui.theme.BgDark
import com.example.courseapp.ui.theme.BgLight
import com.example.courseapp.viewmodel.ImportViewModel
import com.example.courseapp.viewmodel.ManageViewModel
import com.example.courseapp.viewmodel.ScheduleViewModel

@Composable
fun CourseAppScaffold(
    scheduleViewModel: ScheduleViewModel,
    isDarkMode: Boolean
) {
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Schedule) }
    val manageViewModel: ManageViewModel = hiltViewModel()
    val importViewModel: ImportViewModel = hiltViewModel()

    val currentWeek by scheduleViewModel.currentWeek.collectAsStateWithLifecycle()
    val backgroundUri by scheduleViewModel.backgroundUri.collectAsStateWithLifecycle()
    var showWeekDialog by remember { mutableStateOf(false) }
    var showTimeSlotSettings by remember { mutableStateOf(false) }

    val navItems = remember {
        listOf(
            BottomNavItem(
                AppDestination.Schedule,
                "首页",
                Icons.Filled.CalendarMonth,
                Icons.Outlined.CalendarMonth
            ),
            BottomNavItem(
                AppDestination.Manage,
                "添加",
                Icons.Filled.AddCircle,
                Icons.Outlined.AddCircle
            ),
            BottomNavItem(
                AppDestination.Profile,
                "我的",
                Icons.Filled.Person,
                Icons.Outlined.Person
            )
        )
    }

    val topBarTitle = when (currentDestination) {
        AppDestination.Schedule -> "2025-2026学年 第二学期"
        AppDestination.Manage -> "课程管理"
        AppDestination.Profile -> "我的"
        AppDestination.WebImport -> "教务系统导入"
        AppDestination.ImportPreview -> "导入预览"
    }
    val showWeekSelector = currentDestination == AppDestination.Schedule
    val hasBackground = currentDestination == AppDestination.Schedule && backgroundUri.isNotEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (hasBackground) {
            Color.Transparent
        } else if (isDarkMode) {
            BgDark
        } else {
            BgLight
        },
        topBar = {
            CourseTopBar(
                title = topBarTitle,
                currentWeek = currentWeek,
                showWeekSelector = showWeekSelector,
                hasBackground = hasBackground,
                isDarkMode = isDarkMode,
                isWeekDialogVisible = showWeekDialog,
                onToggleDarkMode = { scheduleViewModel.toggleDarkMode() },
                onWeekSelectorClick = { showWeekDialog = true }
            )
        },
        bottomBar = {
            CourseBottomBar(
                items = navItems,
                currentDestination = currentDestination,
                isDarkMode = isDarkMode,
                hasBackground = hasBackground,
                onDestinationSelected = { currentDestination = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentDestination == AppDestination.Schedule) {
                    ScheduleScreen(
                        viewModel = scheduleViewModel,
                        onNavigateToAdd = {
                            currentDestination = AppDestination.Manage
                            manageViewModel.switchTab(1)
                        },
                        onNavigateToImport = { currentDestination = AppDestination.WebImport }
                    )
                }
            }

            if (currentDestination == AppDestination.Manage) {
                ManageScreen(viewModel = manageViewModel, isDarkMode = isDarkMode)
            }

            if (currentDestination == AppDestination.Profile) {
                ProfileScreen(
                    onDarkModeToggle = { scheduleViewModel.toggleDarkMode() },
                    isDarkMode = isDarkMode,
                    onTimeSlotSettings = { showTimeSlotSettings = true }
                )
            }

            if (currentDestination == AppDestination.WebImport) {
                WebImportScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentDestination = AppDestination.Schedule },
                    onImportSuccess = {
                        scheduleViewModel.showMessage("检测到课表数据，正在解析...", "info")
                        currentDestination = AppDestination.Schedule
                    },
                    onParseHtml = { html ->
                        importViewModel.parseHtml(
                            html,
                            scheduleViewModel.activeSemester.value?.id ?: "2025-2026-2"
                        )
                        currentDestination = AppDestination.ImportPreview
                    }
                )
            }

            if (currentDestination == AppDestination.ImportPreview) {
                ImportPreviewScreen(
                    viewModel = importViewModel,
                    isDarkMode = isDarkMode,
                    onBack = { currentDestination = AppDestination.WebImport },
                    onImportComplete = {
                        currentDestination = AppDestination.Schedule
                        importViewModel.clearState()
                    }
                )
            }

            AnimatedVisibility(
                visible = showWeekDialog,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                WeekSelectorDialog(
                    currentWeek = currentWeek,
                    isDarkMode = isDarkMode,
                    onWeekSelected = {
                        scheduleViewModel.selectWeek(it)
                        showWeekDialog = false
                    },
                    onDismiss = { showWeekDialog = false }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showTimeSlotSettings,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        TimeSlotSettingsScreen(
            viewModel = scheduleViewModel,
            isDarkMode = isDarkMode,
            onBack = { showTimeSlotSettings = false }
        )
    }
}
