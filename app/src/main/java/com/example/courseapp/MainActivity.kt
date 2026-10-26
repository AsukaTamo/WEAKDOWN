package com.example.courseapp

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.ui.components.CourseDetailSheet
import com.example.courseapp.ui.components.WeekMorphOverlay
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.components.keepPlacedWhen
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
import kotlinx.coroutines.launch
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
    iconDescription: String,
    scale: Float = 1f
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
                scaleX = scale
                scaleY = scale
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
    // 冷启动跟随系统；用户点了太阳/月亮按钮后，本次进程内以手动选择为准
    val systemDarkMode = isSystemInDarkTheme()
    LaunchedEffect(systemDarkMode) {
        scheduleViewModel.updateSystemDarkMode(systemDarkMode)
    }

            var showWeakdownSplash by rememberSaveable { mutableStateOf(true) }
            // 0 = 启动页还在，1 = 首次进入课表；首页卡片只在这个 0 -> 1 的瞬间做一次入场
            var homeEntranceEpoch by rememberSaveable { mutableIntStateOf(0) }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    // 奶油色启动页期间强制深色系统栏图标；退场后再按主题恢复
                    val lightSystemBars = showWeakdownSplash || !isDarkMode
                    isAppearanceLightStatusBars = lightSystemBars
                    isAppearanceLightNavigationBars = lightSystemBars
                }
            }

            CourseAppTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CourseApp(
                        scheduleViewModel = scheduleViewModel,
                        isDarkMode = isDarkMode,
                        homeEntranceEpoch = homeEntranceEpoch
                    )

                    if (showWeakdownSplash) {
                        WeakdownSplashOverlay(
                            onFinished = {
                                showWeakdownSplash = false
                                if (homeEntranceEpoch == 0) homeEntranceEpoch = 1
                            },
                            // 启动页开始淡出前就放行首页，让卡片入场和遮罩退场重叠
                            onRevealStart = {
                                if (homeEntranceEpoch == 0) homeEntranceEpoch = 1
                            }
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
    isDarkMode: Boolean,
    homeEntranceEpoch: Int = 1
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Schedule) }
    val manageViewModel: ManageViewModel = hiltViewModel()
    val importViewModel: ImportViewModel = hiltViewModel()

    val visibleWeek by scheduleViewModel.visibleWeek.collectAsStateWithLifecycle()
    val activeSemester by scheduleViewModel.activeSemester.collectAsStateWithLifecycle()
    val calendarWeek by scheduleViewModel.calendarWeek.collectAsStateWithLifecycle()
    val selectedCourse by scheduleViewModel.selectedCourse.collectAsStateWithLifecycle()
    val timeSlots by scheduleViewModel.timeSlots.collectAsStateWithLifecycle()
    val backgroundUri by scheduleViewModel.backgroundUri.collectAsStateWithLifecycle()
    val scrimAlpha by scheduleViewModel.scrimAlpha.collectAsStateWithLifecycle()
    val showScheduleGuides by scheduleViewModel.showScheduleGuides.collectAsStateWithLifecycle()
    var showWeekDialog by remember { mutableStateOf(false) }
    var showTimeSlotSettings by remember { mutableStateOf(false) }
    // 周选择胶囊 → 面板的形变进度（0=胶囊，1=面板），胶囊与面板共用同一条曲线
    val weekMorphProgress = remember { Animatable(0f) }
    var weekChipBounds by remember { mutableStateOf(Rect.Zero) }
    var weekArrowBounds by remember { mutableStateOf(Rect.Zero) }
    // 主题切换：圆形揭示 + 图标缩放
    val themeReveal = remember { Animatable(1f) }
    // 只有开始/结束各变一次，避免在 composition 里每帧读动画值
    var themeRevealActive by remember { mutableStateOf(false) }
    // 覆盖层自身的淡入：从 0 到 1 用 90ms，画面先柔和地化到底色，再扩散圆洞
    val themeCoverAlpha = remember { Animatable(1f) }
    // 隐藏页面的主题色延迟到揭示结束后再切：主题切换那一帧只重组当前可见页
    var delayedDarkMode by remember { mutableStateOf(isDarkMode) }
    LaunchedEffect(isDarkMode) {
        if (delayedDarkMode == isDarkMode) return@LaunchedEffect
        kotlinx.coroutines.delay(ThemeRevealDelayMillis)
        delayedDarkMode = isDarkMode
    }
    val themeIconScale = remember { Animatable(1f) }
    var themeRevealCenter by remember { mutableStateOf(Offset.Zero) }
    var themeRevealColor by remember { mutableStateOf(Color.Transparent) }
    val themeScope = rememberCoroutineScope()

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
            // 大色块做颜色过渡，换主题时顶栏/底栏是"化"过去的，不是直接跳
            val animatedTopSurface by animateColorAsState(
                targetValue = palette.topSurface,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "topSurfaceColor"
            )
            val animatedBottomSurface by animateColorAsState(
                targetValue = palette.bottomSurface,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "bottomSurfaceColor"
            )

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
                scrimAlpha = scrimAlpha,
                isDarkMode = isDarkMode
            )
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            var themeIconRotation by remember { mutableFloatStateOf(0f) }
            val animatedThemeIconRotation by animateFloatAsState(
                targetValue = themeIconRotation,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "themeIconRotation"
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
                        // 近似液态玻璃：磨砂玻璃底 + 顶部高光 + 彩虹描边
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedTopSurface.copy(alpha = 0.72f),
                                    animatedTopSurface.copy(alpha = 0.52f)
                                )
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDarkMode) 0.10f else 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 0.9.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color(0x66FF6047),
                                    Color(0x6600D2D3),
                                    Color(0x66A29BFE),
                                    Color.White.copy(alpha = 0.55f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
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
                            themeRevealColor = appColors(isDarkMode).canvas
                            themeScope.launch {
                                themeIconScale.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
                                themeIconScale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
                            }
                            themeScope.launch {
                                themeRevealActive = true
                                themeReveal.snapTo(0f)
                                // 旧界面先用 90ms 柔和地化到纯底色，再在纯色下面换主题：
                                // 既没有整屏硬切，也把整树换色那一帧藏住了
                                themeCoverAlpha.snapTo(0f)
                                themeCoverAlpha.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(90, easing = FastOutSlowInEasing)
                                )
                                scheduleViewModel.toggleDarkMode()
                                // 让主题重组先落地，再启动卡片错峰，避免挤在同一帧
                                kotlinx.coroutines.delay(96)
                                ThemeTransition.trigger()
                                // 与周数面板展开同一条高阻尼 spring 曲线
                                themeReveal.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.97f,
                                        stiffness = 170f,
                                        visibilityThreshold = 0.001f
                                    )
                                )
                                themeReveal.snapTo(1f)
                                themeRevealActive = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            // 靠右一点点：与顶栏内容右边缘留 6dp，视觉上更贴近圆角外壳
                            .padding(end = 6.dp)
                            .size(40.dp)
                            .elasticPress(CircleShape)
                            .onGloballyPositioned { themeRevealCenter = it.boundsInRoot().center }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(palette.topContent.copy(alpha = if (hasBackground) 0.16f else 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemeMorphIcon(
                                isSun = isDarkMode,
                                tint = palette.topContent,
                                rotation = animatedThemeIconRotation,
                                scale = themeIconScale.value,
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
                                        .onGloballyPositioned { weekChipBounds = it.boundsInRoot() }
                                        // alpha 必须放在 background/clip 之前，否则药丸底和水波纹不受影响，
                                        // 展开后会在原位留下一个空的小圆角框
                                        .graphicsLayer {
                                            alpha = if (weekMorphProgress.value > 0.001f) 0f else 1f
                                        }
                                        .elasticPress(RoundedCornerShape(16.dp))
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
                                            .onGloballyPositioned { weekArrowBounds = it.boundsInRoot() }
                                    )
                                }

                                if (isBrowsingOtherWeek) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(42.dp)
                                            .elasticPress(RoundedCornerShape(16.dp))
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedBottomSurface.copy(alpha = 0.74f),
                                    animatedBottomSurface.copy(alpha = 0.56f)
                                )
                            )
                        )
                        .border(
                            width = 0.9.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.50f),
                                    Color(0x55FF6047),
                                    Color(0x5500D2D3),
                                    Color(0x55A29BFE),
                                    Color.White.copy(alpha = 0.50f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
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
                                        .elasticPress(RoundedCornerShape(22.dp))
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            if (isSelected) palette.weekChip else Color.Transparent
                                        )
                                        .clickable {
                                            currentScreen = item.screen
                                            // 底部「+」直接进添加表单，补上首页 FAB 移除后的添加入口
                                            if (item.screen == Screen.Manage) {
                                                manageViewModel.switchTab(1)
                                            }
                                        }
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
                                            tint = if (isSelected) palette.weekChipContent else palette.navInactive,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) palette.weekChipContent else palette.navInactive,
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
            // 三个主页面常驻组合：切 Tab 只切可见性，不再整屏重建（重建会有 ~200ms 首帧尖峰）。
            // 隐藏页只 measure 不 place，所以既不绘制也不参与命中测试。
            listOf(Screen.Schedule, Screen.Manage, Screen.Profile).forEach { screen ->
                key(screen) {
                    val visible = currentScreen == screen
                    // 可见页立刻跟主题，隐藏页用延迟值，避免一次切换让三整屏同时重组
                    val screenDarkMode = if (visible) isDarkMode else delayedDarkMode
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .keepPlacedWhen(visible)
                    ) {
                        when (screen) {
                            Screen.Schedule -> ScheduleScreen(
                                viewModel = scheduleViewModel,
                                onNavigateToAdd = {
                                    currentScreen = Screen.Manage
                                    manageViewModel.switchTab(1)
                                },
                                onNavigateToImport = { currentScreen = Screen.WebImport },
                                drawBackground = false,
                                entranceEpoch = homeEntranceEpoch
                            )
                            Screen.Manage -> ManageScreen(
                                viewModel = manageViewModel,
                                isDarkMode = screenDarkMode,
                                onNavigateToImport = { currentScreen = Screen.WebImport }
                            )
                            Screen.Profile -> ProfileScreen(
                                onDarkModeToggle = { scheduleViewModel.toggleDarkMode() },
                                onDarkModeChange = { scheduleViewModel.setDarkMode(it) },
                                isDarkMode = screenDarkMode,
                                showScheduleGuides = showScheduleGuides,
                                onScheduleGuidesChange = { scheduleViewModel.setScheduleGuidesEnabled(it) },
                                onTimeSlotSettings = { showTimeSlotSettings = true }
                            )
                            else -> Unit
                        }
                    }
                }
            }

            if (currentScreen == Screen.WebImport) {
                WebImportScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Schedule },
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

    // 周选择：胶囊 ↔ 面板的流体形变（放在根布局，几何用胶囊在根布局中的真实矩形）
    WeekMorphOverlay(
        visible = showWeekDialog,
        progress = weekMorphProgress,
        anchor = weekChipBounds,
        arrowAnchor = weekArrowBounds,
        maxWeek = activeSemester?.totalWeeks?.coerceAtLeast(1)
            ?: ScheduleViewModel.DEFAULT_SELECTABLE_WEEK_LIMIT,
        currentWeek = visibleWeek,
        thisWeek = calendarWeek,
        chipColor = palette.weekChip,
        chipContentColor = palette.weekChipContent,
        topBarContentColor = palette.topContent,
        isDarkMode = isDarkMode,
        onWeekSelected = {
            scheduleViewModel.selectWeek(it)
            showWeekDialog = false
        },
        onDismiss = { showWeekDialog = false }
    )

    // 主题切换的圆形揭示：用旧底色盖住全屏，只挖出从按钮扩散的圆洞。
    // 进度在 DrawScope 里读，只失效绘制，不触发重组。
    ThemeRevealOverlay(
        active = themeRevealActive,
        progress = themeReveal,
        coverAlpha = themeCoverAlpha,
        center = themeRevealCenter,
        color = themeRevealColor
    )

    // 课程详情抽屉：放在根布局，遮罩才能覆盖顶栏与底部导航
    CourseDetailSheet(
        course = selectedCourse,
        isDarkMode = isDarkMode,
        timeSlotLabels = remember(timeSlots) { timeSlots.flatMap { listOf(it.start, it.end) } },
        onDismiss = { scheduleViewModel.dismissCourseDetail() },
        onSave = { scheduleViewModel.updateCourse(it) },
        onDelete = { scheduleViewModel.deleteCourse(it) },
        onCopy = { scheduleViewModel.copyCourse(it) }
    )
}

}

@Composable
private fun ThemeRevealOverlay(
    active: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    coverAlpha: Animatable<Float, AnimationVector1D>,
    center: Offset,
    color: Color
) {
    if (!active) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        // DrawScope 里读 Animatable：只让这一层重绘，不带着整棵树重组
        val raw = progress.value
        val fraction = raw.coerceIn(0f, 1f)
        if (fraction >= 1f) return@Canvas
        val maxRadius = kotlin.math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            addOval(Rect(center = center, radius = maxRadius * fraction))
        }
        drawPath(path, color.copy(alpha = color.alpha * coverAlpha.value.coerceIn(0f, 1f)))
    }
}

@Composable
private fun ImmersiveScheduleBackground(
    backgroundUri: String,
    scrimAlpha: Float,
    isDarkMode: Boolean
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
    // 遮罩跟着主题走：浅色用流光白、深色用藏蓝，不再一律压黑
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
                        0f to maskColor.copy(alpha = 0.12f),
                        0.18f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to maskColor.copy(alpha = 0.18f)
                    )
                )
            )
    )
}

/**
 * 常驻组合、条件摆放：visible 为 false 时只 measure 不 place，
 * 这样页面状态得以保留，又不会占用绘制与触摸。
 */
private fun Modifier.keepPlacedWhen(visible: Boolean): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        if (visible) {
            placeable.place(0, 0)
        }
    }
}

/** 揭示动画时长（spring 收尾），隐藏页在这之后才跟着换主题。 */
private const val ThemeRevealDelayMillis = 520L
