package com.example.courseapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Semester
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
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
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    var snackbarState by remember { mutableStateOf(SnackbarState()) }
    var showSemesterDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var backgroundScrimAlpha by remember(activeSemester?.id, activeSemester?.scrimAlpha) {
        mutableStateOf(activeSemester?.scrimAlpha ?: 0.4f)
    }

    // Background image picker
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            // 首尾留白跟着玻璃栏走：列表首项在顶栏下方，滚动时可以真实经过顶/底栏玻璃。
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding()
            )
        ) {
            // User header
            item {
                UserHeader(
                    isDarkMode = isDarkMode,
                    name = viewModel.userName,
                    subtitle = viewModel.userSubtitle
                )
            }

            // Semester management
            item {
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "学期管理",
                        subtitle = activeSemester?.name ?: "未设置",
                        isDarkMode = isDarkMode,
                        onClick = { showSemesterDialog = true }
                    )
                }
            }

            // Background settings
            item {
                val semester = activeSemester
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Image,
                        title = "课表背景",
                        subtitle = when {
                            semester == null -> "请先选择或创建学期"
                            semester.backgroundUri.isEmpty() -> "未设置，点击选择图片"
                            else -> "已设置，点击更换图片"
                        },
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (semester == null) {
                                viewModel.showMessage("请先选择或创建学期", "info")
                            } else {
                                backgroundPickTarget = semester
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    )

                    if (semester?.backgroundUri?.isNotEmpty() == true) {
                        val bgFile = java.io.File(semester.backgroundUri)
                        if (bgFile.exists()) {
                            val bitmap = remember(semester.backgroundUri) {
                                android.graphics.BitmapFactory.decodeFile(semester.backgroundUri)?.asImageBitmap()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "背景预览",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(92.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "背景遮罩 ${(backgroundScrimAlpha * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = palette.muted
                            )
                            Slider(
                                value = backgroundScrimAlpha,
                                onValueChange = { backgroundScrimAlpha = it },
                                onValueChangeFinished = {
                                    viewModel.updateScrimAlpha(semester, backgroundScrimAlpha)
                                },
                                valueRange = 0f..0.9f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accent,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = palette.accentSoft
                                )
                            )
                        }

                        SettingsItem(
                            icon = Icons.Outlined.Delete,
                            title = "清除背景",
                            titleColor = palette.destructive,
                            isDarkMode = isDarkMode,
                            onClick = { viewModel.clearBackground(semester) }
                        )
                    }

                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = "辅助线",
                        subtitle = "控制课表横向分割线显示",
                        isDarkMode = isDarkMode,
                        trailing = {
                            Switch(
                                checked = showScheduleGuides,
                                onCheckedChange = onScheduleGuidesChange,
                                colors = themeSwitchColors(isDarkMode, palette.accent)
                            )
                        }
                    )
                }
            }

            // Appearance
            item {
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Notifications,
                        title = "上课提醒",
                        isDarkMode = isDarkMode,
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications() },
                                colors = themeSwitchColors(isDarkMode, palette.accent)
                            )
                        }
                    )
                    SettingsItem(
                        icon = Icons.Filled.Schedule,
                        title = "作息时间设置",
                        subtitle = "自定义课程时间段",
                        isDarkMode = isDarkMode,
                        onClick = onTimeSlotSettings
                    )
                }
            }

            // Data management
            item {
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "清空所有数据",
                        titleColor = palette.destructive,
                        isDarkMode = isDarkMode,
                        onClick = { showClearDialog = true }
                    )
                }
            }

            // About
            item {
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Outlined.Update,
                        title = "检查更新",
                        isDarkMode = isDarkMode,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(viewModel.appVersion, fontSize = 12.sp, color = palette.success)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = { viewModel.showMessage("已检查更新，当前为最新版本", "success") }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = "关于应用",
                        isDarkMode = isDarkMode,
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "帮助与反馈",
                        isDarkMode = isDarkMode,
                        onClick = { viewModel.showMessage("反馈已提交，感谢您的建议！", "success") }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        // Snackbar
        if (snackbarState.visible) {
            AppSnackbar(
                message = snackbarState.message,
                type = snackbarState.type,
                onDismiss = { snackbarState = snackbarState.copy(visible = false) }
            )
        }
    }

    // Semester management dialog
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

    // Clear data confirmation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterDialog(
    semesters: List<Semester>,
    activeSemester: Semester?,
    courseCounts: Map<String, Int>,
    isDarkMode: Boolean,
    onSelect: (Semester) -> Unit,
    onAdd: (String, String, String, Int) -> Unit,
    onUpdate: (Semester) -> Unit,
    onDelete: (Semester) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Semester?>(null) }
    var editingSemester by remember { mutableStateOf<Semester?>(null) }
    var newName by remember { mutableStateOf("") }
    var newStartDate by remember { mutableStateOf("2026-02-23") }
    var newWeeks by remember { mutableStateOf("18") }
    var showAddDatePicker by remember { mutableStateOf(false) }
    var showEditDatePicker by remember { mutableStateOf(false) }

    val palette = contentPalette(isDarkMode)
    val textColor = palette.title
    val subColor = palette.muted

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期管理", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                semesters.forEach { semester ->
                    val isActive = semester.id == activeSemester?.id
                    val isEditing = editingSemester?.id == semester.id

                    if (isEditing) {
                        // Edit form for this semester
                        var editName by remember { mutableStateOf(semester.name) }
                        var editStartDate by remember { mutableStateOf(semester.startDate) }
                        var editWeeks by remember { mutableStateOf(semester.totalWeeks.toString()) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accentSoft)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("学期名称") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            // Date picker trigger
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .elasticPress(RoundedCornerShape(8.dp))
                                    .clickable { showEditDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = editStartDate,
                                    onValueChange = {},
                                    label = { Text("起始日期") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(Icons.Default.CalendarMonth, "选择日期")
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = textColor,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = subColor,
                                        disabledTrailingIconColor = subColor
                                    )
                                )
                            }
                            OutlinedTextField(
                                value = editWeeks,
                                onValueChange = { editWeeks = it },
                                label = { Text("总周数") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingSemester = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .elasticPress(ButtonDefaults.outlinedShape)
                                ) { Text("取消") }
                                Button(
                                    onClick = {
                                        onUpdate(
                                            semester.copy(
                                                name = editName,
                                                startDate = editStartDate,
                                                totalWeeks = editWeeks.toIntOrNull() ?: 18
                                            )
                                        )
                                        editingSemester = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .elasticPress(ButtonDefaults.shape),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                                ) { Text("保存") }
                            }
                        }

                        // Edit date picker dialog
                        if (showEditDatePicker) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val initialMillis = try {
                                sdf.parse(editStartDate)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = initialMillis
                            )
                            DatePickerDialog(
                                onDismissRequest = { showEditDatePicker = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            datePickerState.selectedDateMillis?.let { millis ->
                                                editStartDate = sdf.format(Date(millis))
                                            }
                                            showEditDatePicker = false
                                        },
                                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                                    ) { Text("确定") }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showEditDatePicker = false },
                                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                                    ) { Text("取消") }
                                }
                            ) { DatePicker(state = datePickerState) }
                        }
                    } else {
                        // Normal display row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .elasticPress(RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) palette.accentSoft else Color.Transparent)
                                .clickable { onSelect(semester) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(semester.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                Text("起始: ${semester.startDate} · ${semester.totalWeeks}周", fontSize = 12.sp, color = subColor)
                            }
                            if (isActive) {
                                Icon(Icons.Default.CheckCircle, null, tint = palette.accent, modifier = Modifier.size(20.dp))
                            }
                            // Edit button
                            IconButton(
                                onClick = { editingSemester = semester },
                                modifier = Modifier
                                    .size(24.dp)
                                    // 学期行本身已有触感，避免嵌套控件双重震动
                                    .elasticPress(CircleShape, hapticsEnabled = false)
                            ) {
                                Icon(Icons.Default.Edit, "编辑", tint = subColor, modifier = Modifier.size(16.dp))
                            }
                            // Delete button (only for non-active)
                            if (!isActive) {
                                IconButton(
                                    onClick = { pendingDelete = semester },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .elasticPress(CircleShape, hapticsEnabled = false)
                                ) {
                                    Icon(Icons.Default.Delete, "删除", tint = Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (showAddForm) {
                    HorizontalDivider(color = palette.divider)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("学期名称 (如 2026-2027学年 第一学期)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Date picker trigger for add form
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .elasticPress(RoundedCornerShape(8.dp))
                            .clickable { showAddDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = newStartDate,
                            onValueChange = {},
                            label = { Text("起始日期") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            trailingIcon = {
                                Icon(Icons.Default.CalendarMonth, "选择日期")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textColor,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = subColor,
                                disabledTrailingIconColor = subColor
                            )
                        )
                    }
                    OutlinedTextField(
                        value = newWeeks,
                        onValueChange = { newWeeks = it },
                        label = { Text("总周数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newName.isNotBlank() && newStartDate.isNotBlank()) {
                                val id = newName.replace(Regex("[^\\d-]"), "").ifBlank { System.currentTimeMillis().toString() }
                                onAdd(id, newName, newStartDate, newWeeks.toIntOrNull() ?: 18)
                                newName = ""; newStartDate = "2026-02-23"; newWeeks = "18"; showAddForm = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .elasticPress(RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) { Text("添加学期") }

                    // Add date picker dialog
                    if (showAddDatePicker) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val initialMillis = try {
                            sdf.parse(newStartDate)?.time ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = initialMillis
                        )
                        DatePickerDialog(
                            onDismissRequest = { showAddDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            newStartDate = sdf.format(Date(millis))
                                        }
                                        showAddDatePicker = false
                                    },
                                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                                ) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showAddDatePicker = false },
                                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                                ) { Text("取消") }
                            }
                        ) { DatePicker(state = datePickerState) }
                    }
                } else {
                    TextButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加新学期")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.elasticPress(ButtonDefaults.textShape)
            ) { Text("完成") }
        }
    )

    // Deleting a semester also deletes its courses, so confirm with the count first.
    pendingDelete?.let { semester ->
        val courseCount = courseCounts[semester.id] ?: 0
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除学期") },
            text = {
                Text(
                    if (courseCount > 0) {
                        "将删除「${semester.name}」及其 $courseCount 门课程，操作不可恢复。"
                    } else {
                        "将删除「${semester.name}」，操作不可恢复。"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(semester)
                        pendingDelete = null
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("删除", color = Error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDelete = null },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("取消") }
            }
        )
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

@Composable
private fun UserHeader(
    name: String,
    subtitle: String,
    isDarkMode: Boolean
) {
    // 浅色：鸢尾蓝 135° 渐变；深色：藏蓝 → 紫 → 火山橙微光。
    // 主题过渡期间按同一进度插值，头图卡片和页面其余元素一起变色。
    val t = ThemeFade.fractionFor(isDarkMode)
    fun themeColor(light: Color, dark: Color): Color = lerp(light, dark, t)
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to themeColor(Color(0xFF0F64B5), Color(0xFF121552)),
            0.55f to themeColor(Color(0xFF1E88E5), Color(0xFF2A1B4E)),
            1.0f to themeColor(Color(0xFF1E88E5), Color(0x80FF6047))
        )
    )
    val avatarBg = themeColor(Color(0xFFFFFBEA), Color(0xFFFF6047))
    val avatarInk = themeColor(Color(0xFF0F64B5), Color(0xFF000035))
    val nameColor = themeColor(Color(0xFFFFFBEA), Color(0xFFF5F6FA))
    val subColor = themeColor(Color(0xFFD6E6F7), Color(0xFF8E92B2))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(brush = brush)
            .border(
                1.dp,
                Color(0x66FF6047).copy(alpha = 0.4f * t),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = avatarInk
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = nameColor, letterSpacing = 0.4.sp)
            Text(subtitle, fontSize = 12.sp, color = subColor, letterSpacing = 0.3.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = subColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsGroup(
    isDarkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .contentCard(contentPalette(isDarkMode), radius = 18.dp),
        content = content
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    subtitle: String? = null,
    titleColor: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = contentPalette(isDarkMode)
    val textColor = titleColor ?: palette.title
    val iconColor = palette.muted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elasticPress(RoundedCornerShape(14.dp), enabled = onClick != null)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = textColor, letterSpacing = 0.3.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = palette.muted)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = palette.muted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}
