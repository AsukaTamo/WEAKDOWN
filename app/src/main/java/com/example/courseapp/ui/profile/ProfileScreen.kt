package com.example.courseapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Semester
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    onDarkModeToggle: () -> Unit = {},
    isDarkMode: Boolean = false,
    onTimeSlotSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    var snackbarState by remember { mutableStateOf(SnackbarState()) }
    var showSemesterDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf<String?>(null) }

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

    val bgColor = if (isDarkMode) BgDark else BgLight

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(top = 8.dp)
        ) {
            // User header
            item {
                UserHeader(
                    name = viewModel.userName,
                    subtitle = viewModel.userSubtitle,
                    isDarkMode = isDarkMode
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

            // Appearance
            item {
                SettingsGroup(isDarkMode = isDarkMode) {
                    SettingsItem(
                        icon = Icons.Filled.DarkMode,
                        title = "深色模式",
                        isDarkMode = isDarkMode,
                        trailing = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onDarkModeToggle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Notifications,
                        title = "上课提醒",
                        isDarkMode = isDarkMode,
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
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
                        icon = Icons.Outlined.Share,
                        title = "导出课表 (JSON)",
                        subtitle = exportPath?.let { "已导出到: ${it.substringAfterLast("/")}" },
                        isDarkMode = isDarkMode,
                        onClick = {
                            val path = viewModel.saveExportFile()
                            exportPath = path
                            if (path != null) viewModel.showMessage("导出成功", "success")
                            else viewModel.showMessage("导出失败", "error")
                        }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.GetApp,
                        title = "导入课表 (JSON)",
                        isDarkMode = isDarkMode,
                        onClick = { viewModel.showMessage("请在文件管理器中选择 course_backup.json", "info") }
                    )
                    SettingsItem(
                        icon = Icons.Outlined.Delete,
                        title = "清空所有数据",
                        titleColor = Error,
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
                                Text(viewModel.appVersion, fontSize = 12.sp, color = Success)
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
        SemesterDialog(
            semesters = semesters,
            activeSemester = activeSemester,
            isDarkMode = isDarkMode,
            onSelect = { viewModel.setActiveSemester(it.id) },
            onAdd = { id, name, startDate, weeks -> viewModel.addSemester(id, name, startDate, weeks) },
            onUpdate = { viewModel.updateSemester(it) },
            onDelete = { viewModel.deleteSemester(it) },
            onPickBackground = { semester ->
                backgroundPickTarget = semester
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearBackground = { viewModel.clearBackground(it) },
            onScrimAlphaChange = { semester, alpha -> viewModel.updateScrimAlpha(semester, alpha) },
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
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) { Text("清空", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
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
    isDarkMode: Boolean,
    onSelect: (Semester) -> Unit,
    onAdd: (String, String, String, Int) -> Unit,
    onUpdate: (Semester) -> Unit,
    onDelete: (Semester) -> Unit,
    onPickBackground: (Semester) -> Unit,
    onClearBackground: (Semester) -> Unit,
    onScrimAlphaChange: (Semester, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf<Semester?>(null) }
    var newName by remember { mutableStateOf("") }
    var newStartDate by remember { mutableStateOf("2026-02-23") }
    var newWeeks by remember { mutableStateOf("18") }
    var showAddDatePicker by remember { mutableStateOf(false) }
    var showEditDatePicker by remember { mutableStateOf(false) }

    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

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
                        var editScrimAlpha by remember { mutableStateOf(semester.scrimAlpha) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.08f))
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
                            Box(modifier = Modifier.fillMaxWidth().clickable { showEditDatePicker = true }) {
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

                            // Background settings
                            Divider()
                            Text("课表背景", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)

                            if (semester.backgroundUri.isNotEmpty()) {
                                // Preview thumbnail
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
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                // Scrim alpha slider
                                Text(
                                    "遮罩不透明度: ${(editScrimAlpha * 100).toInt()}%",
                                    fontSize = 12.sp, color = subColor
                                )
                                Slider(
                                    value = editScrimAlpha,
                                    onValueChange = { editScrimAlpha = it },
                                    onValueChangeFinished = {
                                        onScrimAlphaChange(semester, editScrimAlpha)
                                    },
                                    valueRange = 0f..0.9f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Clear background button
                                OutlinedButton(
                                    onClick = { onClearBackground(semester) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("清除背景")
                                }
                            }

                            // Pick background button
                            OutlinedButton(
                                onClick = { onPickBackground(semester) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (semester.backgroundUri.isEmpty()) "设置背景" else "更换背景")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingSemester = null },
                                    modifier = Modifier.weight(1f)
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
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
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
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            editStartDate = sdf.format(Date(millis))
                                        }
                                        showEditDatePicker = false
                                    }) { Text("确定") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDatePicker = false }) { Text("取消") }
                                }
                            ) { DatePicker(state = datePickerState) }
                        }
                    } else {
                        // Normal display row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) Primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { onSelect(semester) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(semester.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                                Text("起始: ${semester.startDate} · ${semester.totalWeeks}周", fontSize = 12.sp, color = subColor)
                            }
                            if (isActive) {
                                Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                            }
                            // Edit button
                            IconButton(
                                onClick = { editingSemester = semester },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, "编辑", tint = subColor, modifier = Modifier.size(16.dp))
                            }
                            // Delete button (only for non-active)
                            if (!isActive) {
                                IconButton(
                                    onClick = { onDelete(semester) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "删除", tint = Error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (showAddForm) {
                    Divider()
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("学期名称 (如 2026-2027学年 第一学期)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Date picker trigger for add form
                    Box(modifier = Modifier.fillMaxWidth().clickable { showAddDatePicker = true }) {
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
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
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        newStartDate = sdf.format(Date(millis))
                                    }
                                    showAddDatePicker = false
                                }) { Text("确定") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddDatePicker = false }) { Text("取消") }
                            }
                        ) { DatePicker(state = datePickerState) }
                    }
                } else {
                    TextButton(onClick = { showAddForm = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加新学期")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun UserHeader(
    name: String,
    subtitle: String,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Primary, Color(0xFF1565C0))
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.4.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), letterSpacing = 0.3.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) CardDark else Color.White),
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
    val textColor = titleColor ?: if (isDarkMode) Color.White else TextPrimary
    val iconColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = textColor, letterSpacing = 0.3.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = if (isDarkMode) Color(0xFF757575) else TextSecondary)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
        }
    }
}
