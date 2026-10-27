package com.example.courseapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.ui.components.CourseDetailSheet
import com.example.courseapp.ui.components.ElasticTabIndicator
import com.example.courseapp.ui.components.QuickAddCourseSheet
import com.example.courseapp.ui.components.QuickAddDraft
import com.example.courseapp.ui.components.WeekMorphOverlay
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.components.edgeBlur
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
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassAppearance
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import io.github.itshedimisawi.vitro.rememberGlassBackdrop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
        animationSpec = tween(ThemeTransitionMillis, easing = FastOutSlowInEasing),
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
                // Vitro 的玻璃默认跟系统深色；这里显式喂给它 App 自己的主题开关
                CompositionLocalProvider(LocalGlassDarkTheme provides isDarkMode) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CourseApp(
                            scheduleViewModel = scheduleViewModel,
                            isDarkMode = isDarkMode,
                            window = window,
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
}

@Composable
fun CourseApp(
    scheduleViewModel: ScheduleViewModel,
    isDarkMode: Boolean,
    window: Window,
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
    // 首页空白格长按拖选后待填写的快捷添加草稿
    var quickAddDraft by remember { mutableStateOf<QuickAddDraft?>(null) }
    // 周选择胶囊 → 面板的形变进度（0=胶囊，1=面板），胶囊与面板共用同一条曲线
    val weekMorphProgress = remember { Animatable(0f) }
    var weekChipBounds by remember { mutableStateOf(Rect.Zero) }
    var weekArrowBounds by remember { mutableStateOf(Rect.Zero) }
    // 主题切换：图标缩放 + 切换前快照淡出。
    // 快照把旧主题整屏拍下来，换主题后按 alpha 淡出——每张卡片/按钮都是逐像素颜色渐变，
    // 但只有一次 GPU 混合，不需要每帧重算整棵树，能跑满高刷。
    val themeIconScale = remember { Animatable(1f) }
    val themeScope = rememberCoroutineScope()
    var themeSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var themeSnapshotAlpha by remember { mutableFloatStateOf(1f) }
    var themeCapturing by remember { mutableStateOf(false) }
    // 主题按钮在根布局中的位置：快照淡出时在这里挖一个软边孔，
    // 保证太阳/月亮动画不会被快照盖住。
    var themeButtonCenter by remember { mutableStateOf(Offset.Zero) }

    val navItems = listOf(
        NavItem(Screen.Schedule, "首页", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem(Screen.Manage, "添加", Icons.Filled.AddCircle, Icons.Outlined.AddCircle),
        NavItem(Screen.Profile, "我的", Icons.Filled.Person, Icons.Outlined.Person)
    )

    // 底栏指示器：先记录每个 Tab 胶囊在窗口中的矩形，再换算成底栏内部的相对坐标。
    // 用窗口坐标做基线，底栏上下移动（导航栏 inset 变化）时也不会错位。
    val bottomTabBoundsInWindow = remember {
        mutableStateListOf<Rect>().apply { repeat(navItems.size) { add(Rect.Zero) } }
    }
    var bottomBarOrigin by remember { mutableStateOf<Offset?>(null) }

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
    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    var measuredTopBarPx by remember { mutableIntStateOf(0) }
    var measuredBottomBarPx by remember { mutableIntStateOf(0) }
    val fallbackTopBar = topInset + 16.dp + ScheduleDimensions.TopBarHeight
    val fallbackBottomBar = bottomInset + 14.dp + 72.dp
    val topBarContentPadding = maxOf(
        fallbackTopBar,
        with(density) { measuredTopBarPx.toDp() }
    )
    val bottomBarContentPadding = maxOf(
        fallbackBottomBar,
        with(density) { measuredBottomBarPx.toDp() }
    )
    // The bars remain floating overlays. Their occupied area is fed back to page content as
    // scrollable padding, so course rows can pass behind the glass while the first/last rows stay
    // reachable. This mirrors Scaffold's measured insets without putting the schedule in a
    // clipped content slot.
    val pagePadding = remember(topInset, bottomInset, measuredTopBarPx, measuredBottomBarPx) {
        PaddingValues(
            top = topBarContentPadding,
            bottom = bottomBarContentPadding
        )
    }
    // 主页面视口整体下移到顶栏外壳的上沿（状态栏 + 顶栏外边距），并相应扣掉顶部留白：
    // 滚动内容仍然可以从顶栏玻璃后面经过，但不会画到状态栏图标底下，也不会在玻璃上方露出被裁断的卡片。
    // 初始布局位置与之前完全一致：视口偏移 + (顶部留白 - 视口偏移)。
    val pageViewportTop = topInset + TopBarTopGap
    val mainPagePadding = remember(pagePadding, pageViewportTop) {
        PaddingValues(
            top = (pagePadding.calculateTopPadding() - pageViewportTop).coerceAtLeast(0.dp),
            bottom = pagePadding.calculateBottomPadding()
        )
    }

    // ── 液态玻璃（Vitro）：背景层标成 backdrop，顶栏/底栏都以它为折射源 ──
    val glassBackdrop = rememberGlassBackdrop()
    // 主题过渡期间按同一个"深色程度"插值玻璃材质，避免顶/底栏在切换时硬跳。
    // 玻璃着色器对材质变化很敏感，按 1/4 档更新即可，既不闪也不至于每帧重建。
    val themeDarkFraction = ThemeFade.fractionFor(isDarkMode)
    val glassStep = (themeDarkFraction * 4f).roundToInt() / 4f
    val glassMaterial = remember(glassStep) { themeGlass(glassStep) }
    // 玻璃底上的次级文字（学期名 / 未选中的 Tab）：muted 灰在亮玻璃上只有 2.6:1，
    // 换成同色系压深一档的 textSecondary，暗玻璃继续用原来的 muted
    val barMutedInk = lerpColor(
        appColors(false).textSecondary,
        palette.topMuted,
        themeDarkFraction
    )
    val navInactiveInk = lerpColor(
        appColors(false).textSecondary,
        palette.navInactive,
        themeDarkFraction
    )

    // 彩色描边：在玻璃外圈叠一层极淡的 sweepGradient，近似原型图的「色彩溢出 / Color Bleed」。
    val colorBleed = remember(themeDarkFraction) { colorBleedBrush(themeDarkFraction) }
    // 玻璃面：顶部提亮、底部压暗的极淡渐变，配合内侧倒角高光/阴影，避免像一块平的半透明板。
    val glassFace = remember(themeDarkFraction) { glassFaceBrush(themeDarkFraction) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景层：底色 + 壁纸 + 遮罩。玻璃从这一层采样，所以它必须画在栏之前、
        // 并且单独成为一个 backdrop（玻璃自己不在这一层里）。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBackdrop(glassBackdrop)
        ) {
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
            )
            if (hasBackground) {
                ImmersiveScheduleBackground(
                    backgroundUri = backgroundUri,
                    scrimAlpha = scrimAlpha,
                    isDarkMode = isDarkMode
                )
            }

            // Page content belongs to the backdrop recording. The bars below are siblings, so
            // Vitro samples the actual scrolling course grid instead of a static wallpaper layer.
            // The viewport starts at the floating bar's outer top edge (below the status bar), so
            // scrolled content never collides with the system status icons; it still passes behind
            // the glass bar and is sampled by Vitro.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = pageViewportTop)
                    // 顶部按顶栏同款圆角裁切：内容上滑时在两侧以圆角消失，而不是一条直边。
                    .clip(
                        RoundedCornerShape(
                            topStart = PageTopCorner,
                            topEnd = PageTopCorner
                        )
                    )
            ) {
                listOf(Screen.Schedule, Screen.Manage, Screen.Profile).forEach { screen ->
                    key(screen) {
                        val visible = currentScreen == screen
                        val screenDarkMode = isDarkMode
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
                                    onQuickAdd = { dayOfWeek, startSlot, slotCount ->
                                        quickAddDraft = QuickAddDraft(
                                            dayOfWeek = dayOfWeek,
                                            startSlot = startSlot,
                                            slotCount = slotCount,
                                            week = visibleWeek,
                                            semesterId = activeSemester?.id
                                                ?: ScheduleViewModel.DEFAULT_SEMESTER_ID
                                        )
                                    },
                                    drawBackground = false,
                                    contentPadding = mainPagePadding,
                                    entranceEpoch = homeEntranceEpoch
                                )
                                Screen.Manage -> ManageScreen(
                                    viewModel = manageViewModel,
                                    isDarkMode = screenDarkMode,
                                    onNavigateToImport = { currentScreen = Screen.WebImport },
                                    contentPadding = mainPagePadding
                                )
                                Screen.Profile -> ProfileScreen(
                                    isDarkMode = screenDarkMode,
                                    showScheduleGuides = showScheduleGuides,
                                    onScheduleGuidesChange = { scheduleViewModel.setScheduleGuidesEnabled(it) },
                                    onTimeSlotSettings = { showTimeSlotSettings = true },
                                    contentPadding = mainPagePadding
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }

            if (currentScreen == Screen.WebImport) {
                Box(Modifier.fillMaxSize().padding(pagePadding)) {
                    WebImportScreen(
                        isDarkMode = isDarkMode,
                        onBack = { currentScreen = Screen.Schedule },
                        onParseHtml = { html ->
                            importViewModel.parseHtml(html, scheduleViewModel.activeSemester.value?.id ?: "2025-2026-2")
                            currentScreen = Screen.ImportPreview
                        }
                    )
                }
            }

            if (currentScreen == Screen.ImportPreview) {
                Box(Modifier.fillMaxSize().padding(pagePadding)) {
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

            // 底部高斯模糊渐变：只模糊底栏附近的一条带状区域，顶部保持干净（避免和系统状态栏颜色冲突）。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .edgeBlur(glassBackdrop, radius = 20.dp, edgeHeight = 200.dp)
            )

            // 顶栏 / 底栏不再放进 Material3 Scaffold：Scaffold 内部的 Surface 是全屏的，并且自带一个
            // pointerInput 节点，会在命中测试里赢得整屏，导致 backdrop 里的页面永远收不到触摸
            // （只有 Surface 内部的栏按钮能响应）。直接作为根 Box 的子节点摆放，位置与原来一致，
            // 页面重新拿到命中测试。
            var themeIconRotation by remember { mutableFloatStateOf(0f) }
            val animatedThemeIconRotation by animateFloatAsState(
                targetValue = themeIconRotation,
                animationSpec = tween(ThemeTransitionMillis, easing = FastOutSlowInEasing),
                label = "themeIconRotation"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, top = TopBarTopGap, end = 14.dp, bottom = 8.dp)
                    .onGloballyPositioned { measuredTopBarPx = it.size.height }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScheduleDimensions.TopBarHeight)
                        // 悬浮投影：让玻璃块从背景上“浮”起来，而不是贴在上面
                        .shadow(
                            elevation = 12.dp,
                            shape = GlassBarShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.10f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        )
                        // 液态玻璃：折射 / 色散 / 高光 / 阴影全部由 Vitro 的着色器负责
                        .glassEffect(
                            backdrop = glassBackdrop,
                            glass = glassMaterial,
                            shape = GlassBarShape
                        )
                        // 玻璃面 → 彩色边 → 内侧倒角：三层一起才有“玻璃块”的厚度
                        .background(glassFace, GlassBarShape)
                        .border(1.dp, colorBleed, GlassBarShape)
                        .glassBevel(themeDarkFraction)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = {
                            // 切换/淡出期间忽略重复点击，避免两段动画互相覆盖
                            if (themeSnapshot != null || themeCapturing) return@IconButton
                            // 先把当前画面拍成快照，再换主题、让快照淡出：
                            // 每张卡片/按钮都经历真实的旧→新颜色渐变，但过渡只花一次 GPU 混合。
                            themeCapturing = true
                            captureWindowSnapshot(window) { bitmap ->
                                themeCapturing = false
                                // 图标旋转/缩放在主题真正开始切换的这一刻启动，
                                // 和 380ms 的整体渐变同速同相（太阳/月亮的形变也是 380ms）。
                                themeIconRotation += 180f
                                themeScope.launch {
                                    themeIconScale.animateTo(0f, tween(ThemeTransitionMillis / 2, easing = FastOutSlowInEasing))
                                    themeIconScale.animateTo(1f, tween(ThemeTransitionMillis / 2, easing = FastOutSlowInEasing))
                                }
                                if (bitmap != null) {
                                    // 先同步把遮罩置为不透明，新主题那一帧不会闪出来
                                    themeSnapshotAlpha = 1f
                                    themeSnapshot = bitmap.asImageBitmap()
                                    scheduleViewModel.toggleDarkMode()
                                    themeScope.launch {
                                        // 等新主题完成一次组合，再开始淡出
                                        withFrameNanos { }
                                        animate(
                                            initialValue = 1f,
                                            targetValue = 0f,
                                            animationSpec = tween(
                                                durationMillis = ThemeTransitionMillis,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) { value, _ -> themeSnapshotAlpha = value }
                                        themeSnapshot = null
                                    }
                                } else {
                                    // 极少数设备 PixelCopy 失败：直接换主题
                                    scheduleViewModel.toggleDarkMode()
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            // 靠右一点点：与顶栏内容右边缘留 6dp，视觉上更贴近圆角外壳
                            .padding(end = 6.dp)
                            .size(40.dp)
                            .elasticPress(CircleShape)
                            .onGloballyPositioned { themeButtonCenter = it.boundsInRoot().center }
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
                                color = barMutedInk,
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

            // 底栏
            // 指示器矩形换到底栏内部坐标：原点未知（首帧）或 Tab 未测量时先给 Rect.Zero，
            // 组件会先不绘制，等测量完成后再落位，避免第一帧闪一个错误位置。
            val tabBounds = remember(bottomTabBoundsInWindow.toList(), bottomBarOrigin) {
                val origin = bottomBarOrigin
                if (origin == null) {
                    List(navItems.size) { Rect.Zero }
                } else {
                    bottomTabBoundsInWindow.map { rect ->
                        if (rect.width <= 0f) {
                            Rect.Zero
                        } else {
                            rect.translate(-origin.x, -origin.y)
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 28.dp, top = 4.dp, end = 28.dp, bottom = 10.dp)
                    .onGloballyPositioned { measuredBottomBarPx = it.size.height }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = GlassBarShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.10f),
                            spotColor = Color.Black.copy(alpha = 0.20f)
                        )
                        .glassEffect(
                            backdrop = glassBackdrop,
                            glass = glassMaterial,
                            shape = GlassBarShape
                        )
                        .background(glassFace, GlassBarShape)
                        .border(1.dp, colorBleed, GlassBarShape)
                        .glassBevel(themeDarkFraction)
                        .onGloballyPositioned { coords ->
                            val origin = coords.positionInWindow()
                            if (bottomBarOrigin != origin) bottomBarOrigin = origin
                        }
                ) {
                    // 液态拉伸指示器放在内容下方：图标/文字始终压在胶囊之上。
                    ElasticTabIndicator(
                        selectedIndex = navItems.indexOfFirst { it.screen == currentScreen },
                        tabBounds = tabBounds,
                        color = palette.weekChip
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = currentScreen == item.screen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        // 记录胶囊真实矩形，供底栏指示器定位；放在按压缩放外层，
                                        // 按下时 0.96 的缩放不会让指示器跟着抖。
                                        .onGloballyPositioned { coords ->
                                            val rect = coords.boundsInRoot()
                                            if (bottomTabBoundsInWindow[index] != rect) {
                                                bottomTabBoundsInWindow[index] = rect
                                            }
                                        }
                                        .elasticPress(RoundedCornerShape(22.dp))
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable {
                                            currentScreen = item.screen
                                            // 底部「+」直接进添加表单，补上首页 FAB 移除后的添加入口
                                            if (item.screen == Screen.Manage) {
                                                manageViewModel.switchTab(1)
                                            }
                                        }
                                        // 选中态不再修改内边距：胶囊宽度保持稳定，
                                        // 指示器只需要处理位置/宽度，不会因为选中而二次撑开。
                                        .padding(horizontal = 18.dp, vertical = 6.dp),
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
                                            tint = if (isSelected) palette.weekChipContent else navInactiveInk,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) palette.weekChipContent else navInactiveInk,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

    // 主题切换快照：盖在整屏最上层，只改一个 alpha，不拦截触摸，也不触发重组。
    // 在主题按钮处挖一个软边透明孔，按钮的变色/缩放/日月形变动画始终露在最上面。
    themeSnapshot?.let { snapshot ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = themeSnapshotAlpha }
        ) {
            drawIntoCanvas {
                it.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
            }
            drawImage(
                image = snapshot,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
            if (themeButtonCenter != Offset.Zero) {
                // 孔半径贴着按钮自己的 38dp 圆底：只让按钮这一块（图标 + 圆底）露出来，
                // 不把顶栏的新旧底色差带进来。
                val holeRadius = 20.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.76f to Color.Black,
                            1f to Color.Transparent
                        ),
                        center = themeButtonCenter,
                        radius = holeRadius
                    ),
                    radius = holeRadius,
                    center = themeButtonCenter,
                    blendMode = BlendMode.DstOut
                )
            }
            drawIntoCanvas { it.restore() }
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
        backdrop = glassBackdrop,
        glass = glassMaterial,
        onWeekSelected = {
            scheduleViewModel.selectWeek(it)
            showWeekDialog = false
        },
        onDismiss = { showWeekDialog = false }
    )

    // 课程详情抽屉：放在根布局，遮罩才能覆盖顶栏与底部导航
    CourseDetailSheet(
        course = selectedCourse,
        isDarkMode = isDarkMode,
        timeSlotLabels = remember(timeSlots) { timeSlots.flatMap { listOf(it.start, it.end) } },
        backdrop = glassBackdrop,
        glass = glassMaterial,
        onDismiss = { scheduleViewModel.dismissCourseDetail() },
        onSave = { scheduleViewModel.updateCourse(it) },
        onDelete = { scheduleViewModel.deleteCourse(it) },
        onCopy = { scheduleViewModel.copyCourse(it) }
    )

    // 首页空白格长按 → 拖选节次 → 快捷添加面板（同样挂在根布局，玻璃和遮罩才完整）
    QuickAddCourseSheet(
        draft = quickAddDraft,
        isDarkMode = isDarkMode,
        timeSlotLabels = remember(timeSlots) { timeSlots.flatMap { listOf(it.start, it.end) } },
        backdrop = glassBackdrop,
        glass = glassMaterial,
        onDismiss = { quickAddDraft = null },
        onCheckConflicts = { scheduleViewModel.findConflictsForNewCourse(it) },
        onSave = { course, replaceExisting ->
            scheduleViewModel.addCourseQuickly(course, replaceExisting)
        }
    )
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
    // 深色模式下壁纸整体再压暗一点，避免亮色照片把深色主题顶得发白
    val darkFraction = ThemeFade.fractionFor(isDarkMode)
    if (darkFraction > 0.001f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f * darkFraction))
        )
    }
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

/**
 * 把当前窗口表面抓成一张位图（含液态玻璃等 GPU 效果）。
 * 用于主题切换的逐像素交叉淡化：比每帧重算整棵树便宜得多。
 */
private fun captureWindowSnapshot(window: Window, onCaptured: (Bitmap?) -> Unit) {
    val decor = window.decorView
    val width = decor.width
    val height = decor.height
    if (width <= 0 || height <= 0) {
        onCaptured(null)
        return
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    PixelCopy.request(
        window,
        bitmap,
        { result ->
            if (result == PixelCopy.SUCCESS) {
                onCaptured(bitmap)
            } else {
                bitmap.recycle()
                onCaptured(null)
            }
        },
        Handler(Looper.getMainLooper())
    )
}

/** 主题切换的整体节拍：渐变、太阳/月亮形变、旋转、缩放全部跟着它走。 */
private const val ThemeTransitionMillis = 460

/** 顶栏玻璃外壳与状态栏、屏幕左右边缘之间的留白；页面视口也从这个上沿开始裁断。 */
private val TopBarTopGap = 8.dp

/** 页面内容顶部裁切的圆角，与顶栏玻璃的 28dp 圆角呼应。 */
private val PageTopCorner = 28.dp

private val GlassBarShape = RoundedCornerShape(28.dp)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(start, stop, fraction.coerceIn(0f, 1f))

/**
 * 深浅两套玻璃材质按同一进度插值。
 * 浅色：乳白玻璃，带一点色散；深色：烟熏玻璃，用深色染色压下去保证白字对比度。
 */
private fun themeGlass(darkFraction: Float): Glass {
    val t = darkFraction.coerceIn(0f, 1f)
    return Glass.Regular.copy(
        // appearance 只能二选一，取过半那一档；tint / brightness 连续变化，过渡中看不出硬切
        appearance = if (t >= 0.5f) GlassAppearance.Dark else GlassAppearance.Light,
        tint = lerpColor(Color.Transparent, Color(0xFF0B0B10).copy(alpha = 0.72f), t),
        brightness = lerpFloat(0.30f, 0.55f, t),
        dispersion = 0.06f,
        blurRadius = 7.dp,
        refractionHeight = 18.dp,
        refractionAmount = 34.dp,
        magnification = 1.02f,
        saturation = 1.15f,
        highlightIntensity = lerpFloat(1.25f, 1.3f, t),
        shadowRadius = 34.dp,
        shadowOffset = 12.dp,
        shadowOpacity = lerpFloat(0.20f, 0.22f, t)
    )
}

/** 玻璃面渐变：顶部受光、底部略暗，给平整的半透明面一点体积感。 */
private fun glassFaceBrush(darkFraction: Float): Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = lerpFloat(0.18f, 0.14f, darkFraction)),
    0.45f to Color.Transparent,
    1f to Color.Black.copy(alpha = lerpFloat(0.07f, 0.24f, darkFraction))
)

/**
 * 内侧倒角：沿外轮廓再画一圈 1.8dp 的竖向渐变描边。
 * 顶边亮、底边先暗再带一道细高光，模拟玻璃边缘的厚度，而不是只有一圈细边框。
 */
private fun Modifier.glassBevel(darkFraction: Float): Modifier = drawWithContent {
    drawContent()
    // 内侧柔光带：顶部微亮、底部微暗，先铺一层 5dp 的软过渡，让倒角不是只有一条硬线
    val soft = 5.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = lerpFloat(0.08f, 0.07f, darkFraction)),
            0.35f to Color.Transparent,
            0.70f to Color.Transparent,
            1f to Color.Black.copy(alpha = lerpFloat(0.07f, 0.14f, darkFraction))
        ),
        topLeft = Offset(soft / 2f, soft / 2f),
        size = Size(size.width - soft, size.height - soft),
        cornerRadius = CornerRadius((28.dp.toPx() - soft / 2f).coerceAtLeast(0f)),
        style = Stroke(soft)
    )
    // 清晰的倒角线：顶边亮、底边先暗再带一道细高光
    val stroke = 1.8.dp.toPx()
    val inset = stroke / 2f
    val radius = (28.dp.toPx() - inset).coerceAtLeast(0f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = lerpFloat(0.55f, 0.45f, darkFraction)),
            0.22f to Color.White.copy(alpha = 0.05f),
            0.55f to Color.Transparent,
            0.88f to Color.Black.copy(alpha = lerpFloat(0.12f, 0.26f, darkFraction)),
            1f to Color.White.copy(alpha = lerpFloat(0.32f, 0.30f, darkFraction))
        ),
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(stroke)
    )
}

/**
 * 原型图的「色彩溢出」：玻璃外圈很淡的一圈光谱。Vitro 的 dispersion 是物理 RGB 分离，
 * 在平整背景上几乎看不见，所以额外用 sweepGradient 描边做视觉近似。
 */
private fun colorBleedBrush(darkFraction: Float): Brush {
    val alpha = lerpFloat(0.50f, 0.60f, darkFraction)
    return Brush.sweepGradient(
        listOf(
            Color(0xFF7BD8FF).copy(alpha = alpha),
            Color(0xFF9C8CFF).copy(alpha = alpha * 0.9f),
            Color(0xFF7CFFC4).copy(alpha = alpha * 0.75f),
            Color(0xFFFFB1D9).copy(alpha = alpha * 0.9f),
            Color(0xFFFF8CC8).copy(alpha = alpha),
            Color(0xFF7BD8FF).copy(alpha = alpha)
        )
    )
}
