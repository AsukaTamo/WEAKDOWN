package com.example.courseapp

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.courseapp.ui.splash.WeakdownSplashOverlay
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

private fun Color.withAlphaMultiplier(multiplier: Float): Color =
    copy(alpha = (alpha * multiplier).coerceIn(0f, 1f))

@Composable
private fun ThemeMorphIcon(
    isSun: Boolean,
    tint: Color,
    rotation: Float,
    iconDescription: String
) {
    val sunProgress by animateFloatAsState(
        targetValue = if (isSun) 1f else 0f,
        animationSpec = tween(560, easing = FastOutSlowInEasing),
        label = "themeIconMorph"
    )

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .size(21.dp)
            .semantics { contentDescription = iconDescription }
            .graphicsLayer {
                rotationZ = rotation
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        val center = this.center
        val moonProgress = 1f - sunProgress
        val centerRadius = size.minDimension * (0.255f + moonProgress * 0.075f)
        val rayLength = size.minDimension * (0.18f * sunProgress)
        val rayStart = centerRadius + size.minDimension * 0.12f
        val rayEnd = rayStart + rayLength
        val rayAlpha = (sunProgress * 1.15f).coerceIn(0f, 1f)

        // 光芒随太阳出现而向外展开，变成月亮时向圆心收缩。
        if (rayLength > 0.01f) {
            repeat(8) { index ->
                val angle = Math.toRadians(index * 45.0)
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                drawLine(
                    color = tint.copy(alpha = rayAlpha),
                    start = center + androidx.compose.ui.geometry.Offset(cos * rayStart, sin * rayStart),
                    end = center + androidx.compose.ui.geometry.Offset(cos * rayEnd, sin * rayEnd),
                    strokeWidth = size.minDimension * 0.075f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        drawCircle(tint, radius = centerRadius, center = center)

        // 月牙缺口从右侧逐渐收起；Offscreen + Clear 让底层背景自然透出。
        val cutoutRadius = size.minDimension * (0.30f * moonProgress)
        if (cutoutRadius > 0.01f) {
            drawCircle(
                color = Color.Transparent,
                radius = cutoutRadius,
                center = center + androidx.compose.ui.geometry.Offset(
                    size.minDimension * 0.14f,
                    -size.minDimension * 0.10f
                ),
                blendMode = BlendMode.Clear
            )
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()
            val isDarkMode by scheduleViewModel.isDarkMode.collectAsStateWithLifecycle()

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkMode
                    isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            CourseAppTheme(darkTheme = isDarkMode) {
                var showWeakdownSplash by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    CourseApp(
                        scheduleViewModel = scheduleViewModel,
                        isDarkMode = isDarkMode
                    )

                    if (showWeakdownSplash) {
                        WeakdownSplashOverlay(
                            onFinished = { showWeakdownSplash = false }
                        )
                    }
                }
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

    val visibleWeek by scheduleViewModel.visibleWeek.collectAsStateWithLifecycle()
    val activeSemester by scheduleViewModel.activeSemester.collectAsStateWithLifecycle()
    val calendarWeek by scheduleViewModel.calendarWeek.collectAsStateWithLifecycle()
    val backgroundUri by scheduleViewModel.backgroundUri.collectAsStateWithLifecycle()
    val scrimAlpha by scheduleViewModel.scrimAlpha.collectAsStateWithLifecycle()
    val showScheduleGuides by scheduleViewModel.showScheduleGuides.collectAsStateWithLifecycle()
    var showWeekDialog by remember { mutableStateOf(false) }
    var showTimeSlotSettings by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem(Screen.Schedule, "首页", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem(Screen.Manage, "添加", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
        NavItem(Screen.Profile, "我的", Icons.Filled.Person, Icons.Outlined.Person)
    )

    val topBarTitle = when (currentScreen) {
        Screen.Schedule -> activeSemester?.name ?: "未设置学期"
        Screen.Manage -> "课程管理"
        Screen.Profile -> "我的"
        Screen.WebImport -> "教务系统导入"
        Screen.ImportPreview -> "导入预览"
    }
    val showWeekSelector = currentScreen == Screen.Schedule
    val hasBackground = currentScreen == Screen.Schedule && backgroundUri.isNotEmpty()
    val palette = schedulePalette(isDarkMode, hasBackground)

    // 返回键：先关周选择弹层 / 回到首页，最后才交给系统退出。
    BackHandler(
        enabled = !showTimeSlotSettings && (showWeekDialog || currentScreen != Screen.Schedule)
    ) {
        when {
            showWeekDialog -> showWeekDialog = false
            currentScreen == Screen.ImportPreview -> currentScreen = Screen.WebImport
            else -> currentScreen = Screen.Schedule
        }
    }

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
        if (hasBackground) {
            ImmersiveScheduleBackground(
                backgroundUri = backgroundUri,
                scrimAlpha = scrimAlpha
            )
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            var themeIconRotation by remember { mutableFloatStateOf(0f) }
            val animatedThemeIconRotation by animateFloatAsState(
                targetValue = themeIconRotation,
                animationSpec = tween(520, easing = FastOutSlowInEasing),
                label = "themeIconRotation"
            )
            val weekArrowRotation by animateFloatAsState(
                targetValue = if (showWeekDialog) 180f else 0f,
                animationSpec = tween(ScheduleMotion.StandardMillis, easing = ScheduleMotion.StandardEasing),
                label = "weekArrowRotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScheduleDimensions.TopBarHeight)
                        .shadow(
                            elevation = if (hasBackground) 24.dp else 14.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black.copy(alpha = if (hasBackground) 0.24f else 0.16f),
                            spotColor = Color.Black.copy(alpha = if (hasBackground) 0.34f else 0.24f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    palette.topSurface,
                                    palette.topSurface.withAlphaMultiplier(0.82f)
                                )
                            )
                        )
                        .border(
                            width = if (hasBackground) 0.7.dp else 0.5.dp,
                            color = if (isDarkMode) {
                                Color.White.copy(alpha = if (hasBackground) 0.08f else 0.04f)
                            } else {
                                Color.White.copy(alpha = if (hasBackground) 0.22f else 0.46f)
                            },
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = {
                            // 每次累计 180°，让太阳/月亮始终沿同一方向旋转。
                            themeIconRotation += 180f
                            scheduleViewModel.toggleDarkMode()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            // 靠右一点点：与顶栏内容右边缘留 6dp，视觉上更贴近圆角外壳
                            .padding(end = 6.dp)
                            .size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(palette.weekChip.copy(alpha = if (hasBackground) 0.36f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemeMorphIcon(
                                isSun = isDarkMode,
                                tint = palette.topContent,
                                rotation = animatedThemeIconRotation,
                                iconDescription = if (isDarkMode) "切换为浅色模式" else "切换为深色模式"
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        if (showWeekSelector) {
                            val isBrowsingOtherWeek = visibleWeek != calendarWeek
                            Text(
                                text = if (isBrowsingOtherWeek) {
                                    "$topBarTitle · 本周第 ${calendarWeek} 周"
                                } else {
                                    topBarTitle
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.topMuted,
                                letterSpacing = 0.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .height(42.dp)
                                        .widthIn(min = 128.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.weekChip)
                                        .clickable { showWeekDialog = !showWeekDialog }
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "第${visibleWeek}周",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.weekChipContent,
                                        letterSpacing = 0.1.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "选择周数",
                                        tint = palette.weekChipContent.copy(alpha = 0.78f),
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(18.dp)
                                            .graphicsLayer {
                                                rotationZ = weekArrowRotation
                                            }
                                    )
                                }

                                if (isBrowsingOtherWeek) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (hasBackground || isDarkMode) {
                                                    Color.White.copy(alpha = 0.16f)
                                                } else {
                                                    Color(0xFF0F8F7B).copy(alpha = 0.12f)
                                                }
                                            )
                                            .clickable {
                                                scheduleViewModel.selectWeek(calendarWeek)
                                                showWeekDialog = false
                                            }
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "回本周",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasBackground || isDarkMode) {
                                                Color.White
                                            } else {
                                                Color(0xFF0F8F7B)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = topBarTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.topContent,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 28.dp, top = 4.dp, end = 28.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(
                            elevation = if (hasBackground) 26.dp else 18.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black.copy(alpha = if (hasBackground) 0.24f else 0.16f),
                            spotColor = Color.Black.copy(alpha = if (hasBackground) 0.34f else 0.24f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(palette.bottomSurface)
                        .border(
                            width = 0.7.dp,
                            color = if (isDarkMode) {
                                Color.White.copy(alpha = if (hasBackground) 0.08f else 0.04f)
                            } else {
                                Color.White.copy(alpha = if (hasBackground) 0.24f else 0.62f)
                            },
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentScreen == item.screen
                            val selectedScale by animateFloatAsState(
                                targetValue = if (isSelected) ScheduleMotion.SelectedScale else 1f,
                                animationSpec = tween(
                                    durationMillis = ScheduleMotion.StandardMillis,
                                    easing = ScheduleMotion.EmphasizedEasing
                                ),
                                label = "bottomNavSelectedScale"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = selectedScale
                                            scaleY = selectedScale
                                        }
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            if (isSelected) Color.Black.copy(alpha = 0.92f)
                                            else Color.Transparent
                                        )
                                        .clickable { currentScreen = item.screen }
                                        .padding(
                                            horizontal = if (isSelected) 18.dp else 10.dp,
                                            vertical = 6.dp
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier.size(22.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) palette.navSelected else palette.navInactive,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) palette.navSelected else palette.navInactive,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }
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
                        onNavigateToImport = { currentScreen = Screen.WebImport },
                        drawBackground = false
                    )
                }
            }

            if (currentScreen == Screen.Manage) {
                ManageScreen(viewModel = manageViewModel, isDarkMode = isDarkMode)
            }

            if (currentScreen == Screen.Profile) {
                ProfileScreen(
                    onDarkModeToggle = { scheduleViewModel.toggleDarkMode() },
                    onDarkModeChange = { scheduleViewModel.setDarkMode(it) },
                    isDarkMode = isDarkMode,
                    showScheduleGuides = showScheduleGuides,
                    onScheduleGuidesChange = { scheduleViewModel.setScheduleGuidesEnabled(it) },
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
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                showWeekDialog = false
                            }
                    )
                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        WeekSelectorDialog(
                            currentWeek = visibleWeek,
                            isDarkMode = isDarkMode,
                            thisWeek = calendarWeek,
                            maxWeek = activeSemester?.totalWeeks?.coerceAtLeast(1)
                                ?: ScheduleViewModel.DEFAULT_SELECTABLE_WEEK_LIMIT,
                            onWeekSelected = {
                                scheduleViewModel.selectWeek(it)
                                showWeekDialog = false
                            },
                            onDismiss = { showWeekDialog = false }
                        )
                    }
                }
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

}

@Composable
private fun ImmersiveScheduleBackground(
    backgroundUri: String,
    scrimAlpha: Float
) {
    val file = remember(backgroundUri) { java.io.File(backgroundUri) }
    if (!file.exists()) return

    val bitmap = remember(backgroundUri) {
        BitmapFactory.decodeFile(backgroundUri)?.asImageBitmap()
    } ?: return

    Image(
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
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.18f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.18f)
                    )
                )
            )
    )
}
