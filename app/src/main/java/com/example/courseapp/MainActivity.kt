package com.example.courseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.ui.components.WeekSelectorDialog
import com.example.courseapp.ui.import.ImportPreviewScreen
import com.example.courseapp.ui.import.WebImportScreen
import com.example.courseapp.ui.manage.ManageScreen
import com.example.courseapp.ui.profile.ProfileScreen
import com.example.courseapp.ui.schedule.ScheduleScreen
import com.example.courseapp.ui.settings.TimeSlotSettingsScreen
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ImportViewModel
import com.example.courseapp.viewmodel.ManageViewModel
import com.example.courseapp.viewmodel.ScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen {
    data object Schedule : Screen()
    data object Manage : Screen()
    data object Profile : Screen()
    data object WebImport : Screen()
    data object ImportPreview : Screen()
}

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            val isDarkMode by scheduleViewModel.isDarkMode.collectAsStateWithLifecycle()

            CourseAppTheme(darkTheme = isDarkMode) {
                CourseApp(
                    scheduleViewModel = scheduleViewModel,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@Composable
fun CourseApp(
    scheduleViewModel: ScheduleViewModel,
    isDarkMode: Boolean
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Schedule) }
    val manageViewModel: ManageViewModel = hiltViewModel()
    val importViewModel: ImportViewModel = hiltViewModel()

    val currentWeek by scheduleViewModel.currentWeek.collectAsStateWithLifecycle()
    val backgroundUri by scheduleViewModel.backgroundUri.collectAsStateWithLifecycle()
    var showWeekDialog by remember { mutableStateOf(false) }
    var showTimeSlotSettings by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem(Screen.Schedule, "首页", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem(Screen.Manage, "添加", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
        NavItem(Screen.Profile, "我的", Icons.Filled.Person, Icons.Outlined.Person)
    )

    val topBarTitle = when (currentScreen) {
        Screen.Schedule -> "2025-2026学年 第二学期"
        Screen.Manage -> "课程管理"
        Screen.Profile -> "我的"
        Screen.WebImport -> "教务系统导入"
        Screen.ImportPreview -> "导入预览"
    }
    val showWeekSelector = currentScreen == Screen.Schedule
    val hasBackground = currentScreen == Screen.Schedule && backgroundUri.isNotEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (hasBackground) Color.Transparent else if (isDarkMode) BgDark else BgLight,
        topBar = {
            // Dark mode toggle animation
            var darkModeAnimating by remember { mutableStateOf(false) }
            val darkModeRotation by animateFloatAsState(
                targetValue = if (darkModeAnimating) 360f else 0f,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "darkModeRotation",
                finishedListener = { darkModeAnimating = false }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (hasBackground) {
                                listOf(TopBarStart.copy(alpha = 0.6f), TopBarEnd.copy(alpha = 0.6f))
                            } else {
                                listOf(TopBarStart, TopBarEnd)
                            }
                        )
                    )
                    .statusBarsPadding()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    // Dark mode toggle
                    IconButton(
                        onClick = {
                            darkModeAnimating = true
                            scheduleViewModel.toggleDarkMode()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "深色模式",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    rotationZ = darkModeRotation
                                    scaleX = if (darkModeAnimating) {
                                        val progress = darkModeRotation / 360f
                                        if (progress < 0.5f) 1f - progress * 0.4f else 0.8f + (progress - 0.5f) * 0.4f
                                    } else 1f
                                    scaleY = scaleX
                                }
                        )
                    }

                    // Center content with strong hierarchy
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        if (showWeekSelector) {
                            Text(
                                text = topBarTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { showWeekDialog = true }
                            ) {
                                Text(
                                    text = "第${currentWeek}周",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.6.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "选择周数",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            rotationZ = if (showWeekDialog) 180f else 0f
                                        }
                                )
                            }
                        } else {
                            Text(
                                text = topBarTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val inactiveColor = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary

            // Bottom nav
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        if (isDarkMode) GlassDark else GlassWhite
                    )
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = item.screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .then(
                                            if (isSelected) Modifier.offset(y = (-1).dp)
                                            else Modifier
                                        )
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = inactiveColor,
                                unselectedTextColor = inactiveColor,
                                indicatorColor = Primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Always keep ScheduleScreen in composition to avoid removal crashes
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentScreen == Screen.Schedule) {
                    ScheduleScreen(
                        viewModel = scheduleViewModel,
                        onNavigateToAdd = {
                            currentScreen = Screen.Manage
                            manageViewModel.switchTab(1)
                        },
                        onNavigateToImport = { currentScreen = Screen.WebImport }
                    )
                }
            }

            if (currentScreen == Screen.Manage) {
                ManageScreen(viewModel = manageViewModel, isDarkMode = isDarkMode)
            }

            if (currentScreen == Screen.Profile) {
                ProfileScreen(
                    onDarkModeToggle = { scheduleViewModel.toggleDarkMode() },
                    isDarkMode = isDarkMode,
                    onTimeSlotSettings = { showTimeSlotSettings = true }
                )
            }

            if (currentScreen == Screen.WebImport) {
                WebImportScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Schedule },
                    onImportSuccess = {
                        scheduleViewModel.showMessage("检测到课表数据，正在解析...", "info")
                        currentScreen = Screen.Schedule
                    },
                    onParseHtml = { html ->
                        importViewModel.parseHtml(html, scheduleViewModel.activeSemester.value?.id ?: "2025-2026-2")
                        currentScreen = Screen.ImportPreview
                    }
                )
            }

            if (currentScreen == Screen.ImportPreview) {
                ImportPreviewScreen(
                    viewModel = importViewModel,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.WebImport },
                    onImportComplete = {
                        currentScreen = Screen.Schedule
                        importViewModel.clearState()
                    }
                )
            }

            // Week selector — slide-down frosted glass panel
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

    // Time slot settings overlay
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
