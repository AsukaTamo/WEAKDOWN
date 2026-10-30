package com.example.courseapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.UserProfile
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.components.tiltPress
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ProfileViewModel
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffectContainer

/**
 * 「我的」页。
 *
 * 页面本身不画背景：学校水印在 MainActivity 的 backdrop 层里，固定不动；
 * 这里的卡片逐个用 Vitro 玻璃采样那层背景，所以滚动时水印不会跟着走，
 * 卡片里透出来的始终是同一块模糊底色。
 */
@Composable
fun ProfileScreen(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean = false,
    showScheduleGuides: Boolean = true,
    onScheduleGuidesChange: (Boolean) -> Unit = {},
    onTimeSlotSettings: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    var snackbarState by remember { mutableStateOf(SnackbarState()) }
    var showSemesterDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPersonalInfo by remember { mutableStateOf(false) }
    var showBackgroundSettings by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }

    // 背景图选择
    var backgroundPickTarget by remember { mutableStateOf<Semester?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val semester = backgroundPickTarget
        if (uri != null && semester != null) {
            viewModel.setBackground(semester, uri)
        }
        backgroundPickTarget = null
    }

    LaunchedEffect(snackbarFlow.value.first) {
        val (msg, type) = snackbarFlow.value
        if (msg.isNotEmpty()) {
            snackbarState = SnackbarState(msg, type, true)
        }
    }

    val palette = contentPalette(isDarkMode)
    val contentTop = contentPadding.calculateTopPadding() + 8.dp
    val contentBottom = contentPadding.calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().glassEffectContainer(spacing = 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentTop,
                    bottom = contentBottom + 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileHero(
                profile = profile,
                isDarkMode = isDarkMode,
                onClick = { showPersonalInfo = true }
            )

            QuickActions(
                backdrop = backdrop,
                isDarkMode = isDarkMode,
                semesterLabel = activeSemester?.name?.let(::shortSemesterName) ?: "未设置",
                hasBackground = activeSemester?.backgroundUri?.isNotEmpty() == true,
                onSemester = { showSemesterDialog = true },
                onBackground = { showBackgroundSettings = true },
                onSupport = { showSupport = true }
            )

            PreferenceCapsule(
                backdrop = backdrop,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled,
                onNotificationsChange = { viewModel.toggleNotifications() },
                onTimeSlotSettings = onTimeSlotSettings
            )

            SettingsGlassCard(
                backdrop = backdrop,
                isDarkMode = isDarkMode,
                appVersion = viewModel.appVersion,
                onCheckUpdate = {
                    viewModel.showMessage("已检查更新，当前为最新版本", "success")
                },
                onAbout = { showAbout = true },
                onFeedback = { showFeedback = true }
            )

            // 「清空数据」排在页面最下方、靠右：做成玻璃胶囊，压在背景校徽上时
            // 能透出模糊的蓝色，也是整页"卡片都是玻璃"的最后一块。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GlassSurface(
                    backdrop = backdrop,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    corner = 22.dp,
                    onClick = { showClearDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = palette.destructive.copy(alpha = 0.9f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "清空数据",
                            fontSize = 12.sp,
                            color = palette.destructive.copy(alpha = 0.9f),
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }

        if (snackbarState.visible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AppSnackbar(
                    message = snackbarState.message,
                    type = snackbarState.type,
                    bottomPadding = contentBottom + 58.dp,
                    onDismiss = { snackbarState = snackbarState.copy(visible = false) }
                )
            }
        }
    }

    // ── 学期管理 ──
    if (showSemesterDialog) {
        val courseCounts = remember(courses) { courses.groupingBy { it.semester }.eachCount() }
        SemesterDialog(
            semesters = semesters,
            activeSemester = activeSemester,
            courseCounts = courseCounts,
            isDarkMode = isDarkMode,
            onSelect = { viewModel.setActiveSemester(it.id) },
            onAdd = { id, name, startDate, weeks -> viewModel.addSemester(id, name, startDate, weeks) },
            onUpdate = { viewModel.updateSemester(it) },
            onDelete = { viewModel.deleteSemester(it) },
            onDismiss = { showSemesterDialog = false }
        )
    }

    // ── 个人信息 ──
    if (showPersonalInfo) {
        PersonalInfoDialog(
            profile = profile,
            isDarkMode = isDarkMode,
            onDismiss = { showPersonalInfo = false },
            onSave = { name, schoolId, major, grade, avatarUri ->
                viewModel.saveProfile(name, schoolId, major, grade, avatarUri)
                showPersonalInfo = false
            }
        )
    }

    // ── 课表背景详情（遮罩 / 辅助线也收在这里） ──
    if (showBackgroundSettings) {
        BackgroundSettingsDialog(
            semester = activeSemester,
            isDarkMode = isDarkMode,
            showScheduleGuides = showScheduleGuides,
            onScheduleGuidesChange = onScheduleGuidesChange,
            onPickImage = {
                val semester = activeSemester
                if (semester == null) {
                    viewModel.showMessage("请先选择或创建学期", "info")
                } else {
                    backgroundPickTarget = semester
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            onClearImage = { activeSemester?.let { viewModel.clearBackground(it) } },
            onScrimCommit = { alpha ->
                activeSemester?.let { viewModel.updateScrimAlpha(it, alpha) }
            },
            onCreateSemester = {
                showBackgroundSettings = false
                showSemesterDialog = true
            },
            onDismiss = { showBackgroundSettings = false }
        )
    }

    if (showSupport) {
        SupportDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showSupport = false }
        )
    }

    if (showAbout) {
        AboutDialog(
            version = viewModel.appVersion,
            isDarkMode = isDarkMode,
            onDismiss = { showAbout = false }
        )
    }

    if (showFeedback) {
        FeedbackDialog(
            isDarkMode = isDarkMode,
            appVersion = viewModel.appVersion,
            onDismiss = { showFeedback = false },
            onSendResult = { message, type ->
                viewModel.showMessage(message, type)
                showFeedback = false
            }
        )
    }

    // ── 清空数据确认 ──
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有数据") },
            text = { Text("此操作不可撤销，确定要清空所有课程数据吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("清空", color = Error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("取消") }
            }
        )
    }
}

private data class SnackbarState(
    val message: String = "",
    val type: String = "info",
    val visible: Boolean = false
)

private fun shortSemesterName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.length <= 7) return trimmed
    return trimmed.take(7) + "…"
}

// ───────────────────────── 顶部身份卡 ─────────────────────────

@Composable
private fun ProfileHero(
    profile: UserProfile,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val t = ThemeFade.fractionFor(isDarkMode)
    fun themeColor(light: Color, dark: Color): Color = lerp(light, dark, t)
    val shape = RoundedCornerShape(24.dp)
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to themeColor(Color(0xFF0B4C93), Color(0xFF0A1A30)),
            0.55f to themeColor(Color(0xFF156CC2), Color(0xFF123C66)),
            1f to themeColor(Color(0xFF3390E2), Color(0xFF1D5B94))
        ),
        start = Offset.Zero,
        end = Offset(1100f, 900f)
    )
    val avatarBg = themeColor(Color(0xFFFFF9E9), Color(0xFFF4EBD6))
    val avatarInk = themeColor(Color(0xFF0D4A86), Color(0xFF0B3D70))
    val nameColor = Color(0xFFFFFCF3)
    val schoolColor = nameColor.copy(alpha = 0.80f)
    val detailColor = nameColor.copy(alpha = 0.62f)
    val avatarBitmap = remember(profile.avatarUri) {
        ProfileAvatarStore.decodeFile(profile.avatarUri, 256)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            // 3D 倾斜放在 shadow 外层：卡片转动时阴影跟着一起偏，不会只剩内容在动。
            .tiltPress(shape)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color(0xFF0B3D70).copy(alpha = 0.16f),
                spotColor = Color(0xFF0B3D70).copy(alpha = 0.22f)
            )
            .elasticPress(shape)
            .clip(shape)
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick)
    ) {
        // 右上角一团柔光 + 左下角一层淡色，避免大块纯色显得平
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.02f),
                    radius = size.width * 0.62f
                ),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.92f, size.height * 0.02f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.04f, size.height * 1.08f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(avatarBg)
                    .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = profile.avatarText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = avatarInk
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (profile.isSet) {
                    Text(
                        text = profile.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nameColor,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = profile.school?.name ?: "未选择学校",
                        fontSize = 12.5.sp,
                        color = schoolColor,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.detailLine.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = profile.detailLine,
                            fontSize = 11.5.sp,
                            color = detailColor,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "登录 / 完善个人信息",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nameColor,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "填写姓名、学校与专业，展示在这里",
                        fontSize = 12.sp,
                        color = schoolColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "填写学校后显示校徽背景",
                        fontSize = 10.5.sp,
                        color = detailColor,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ───────────────────────── 三个快捷卡片 ─────────────────────────

@Composable
private fun QuickActions(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean,
    semesterLabel: String,
    hasBackground: Boolean,
    onSemester: () -> Unit,
    onBackground: () -> Unit,
    onSupport: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickTile(
            backdrop = backdrop,
            isDarkMode = isDarkMode,
            icon = Icons.Outlined.CalendarMonth,
            label = "学期",
            caption = semesterLabel,
            accent = palette.accent,
            onClick = onSemester,
            modifier = Modifier.weight(1f)
        )
        QuickTile(
            backdrop = backdrop,
            isDarkMode = isDarkMode,
            icon = Icons.Outlined.Image,
            label = "课表背景",
            caption = if (hasBackground) "已设置" else "默认",
            accent = palette.accent,
            onClick = onBackground,
            modifier = Modifier.weight(1f)
        )
        QuickTile(
            backdrop = backdrop,
            isDarkMode = isDarkMode,
            icon = Icons.Filled.Favorite,
            label = "支持",
            caption = "喜欢就分享",
            accent = palette.accent,
            onClick = onSupport,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickTile(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean,
    icon: ImageVector,
    label: String,
    caption: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = contentPalette(isDarkMode)
    GlassSurface(
        backdrop = backdrop,
        isDarkMode = isDarkMode,
        modifier = modifier.height(104.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (isDarkMode) 0.16f else 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.title,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                fontSize = 10.sp,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ───────────────────────── 作息 / 提醒胶囊 ─────────────────────────

@Composable
private fun PreferenceCapsule(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onTimeSlotSettings: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    val leftShape = RoundedCornerShape(topStart = 29.dp, bottomStart = 29.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    val rightShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 29.dp, bottomEnd = 29.dp)

    GlassSurface(
        backdrop = backdrop,
        isDarkMode = isDarkMode,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(29.dp),
        corner = 29.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .elasticPress(leftShape)
                    .clip(leftShape)
                    .clickable(onClick = onTimeSlotSettings)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "作息时间",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.title,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.muted.copy(alpha = 0.55f),
                    modifier = Modifier.size(15.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(palette.divider)
            )

            Row(
                modifier = Modifier
                    .clip(rightShape)
                    .fillMaxHeight()
                    .elasticPress(rightShape)
                    .clickable { onNotificationsChange(!notificationsEnabled) }
                    .padding(start = 13.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = palette.muted,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "上课提醒",
                    fontSize = 12.sp,
                    color = palette.title,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = null,
                    colors = themeSwitchColors(isDarkMode, palette.accent)
                )
            }
        }
    }
}

// ───────────────────────── 设置列表 ─────────────────────────

@Composable
private fun SettingsGlassCard(
    backdrop: GlassBackdrop,
    isDarkMode: Boolean,
    appVersion: String,
    onCheckUpdate: () -> Unit,
    onAbout: () -> Unit,
    onFeedback: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    GlassSurface(
        backdrop = backdrop,
        isDarkMode = isDarkMode,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Column {
            SettingsRow(
                icon = Icons.Outlined.Update,
                title = "检查更新",
                isDarkMode = isDarkMode,
                onClick = onCheckUpdate,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appVersion,
                            fontSize = 11.sp,
                            color = palette.success
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = palette.muted.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
            SettingsDivider(isDarkMode)
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "关于应用",
                isDarkMode = isDarkMode,
                onClick = onAbout
            )
            SettingsDivider(isDarkMode)
            SettingsRow(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "帮助与反馈",
                isDarkMode = isDarkMode,
                onClick = onFeedback
            )
        }
    }
}

@Composable
private fun SettingsDivider(isDarkMode: Boolean) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp, end = 16.dp),
        thickness = 0.7.dp,
        color = contentPalette(isDarkMode).divider
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = contentPalette(isDarkMode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 6.dp)
            .elasticPress(RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.muted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = palette.title,
            letterSpacing = 0.25.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = palette.muted.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 开关配色也走主题过渡进度，避免页面在渐变时开关硬跳色。 */
@Composable
private fun themeSwitchColors(isDarkMode: Boolean, accent: Color): SwitchColors {
    val t = ThemeFade.fractionFor(isDarkMode)
    return SwitchDefaults.colors(
        checkedThumbColor = lerp(Color.White, Color(0xFF000035), t),
        checkedTrackColor = accent,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = lerp(Color.White, Color(0xFF8E92B2), t),
        uncheckedTrackColor = lerp(Color(0xFFE0E0E0), Color(0xFF161852), t),
        uncheckedBorderColor = lerp(Color.Transparent, Color(0xFF3A3F7A), t)
    )
}
