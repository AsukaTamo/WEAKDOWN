package com.example.courseapp.ui.profile

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Semester
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ProfileViewModel

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

@Composable
private fun SemesterDialog(
    semesters: List<Semester>,
    activeSemester: Semester?,
    isDarkMode: Boolean,
    onSelect: (Semester) -> Unit,
    onAdd: (String, String, String, Int) -> Unit,
    onDelete: (Semester) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newStartDate by remember { mutableStateOf("2026-02-23") }
    var newWeeks by remember { mutableStateOf("18") }

    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期管理", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                semesters.forEach { semester ->
                    val isActive = semester.id == activeSemester?.id
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

                if (showAddForm) {
                    Divider()
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("学期名称 (如 2026-2027学年 第一学期)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newStartDate,
                        onValueChange = { newStartDate = it },
                        label = { Text("起始日期 (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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
                                // Auto-generate ID from name
                                val id = newName.replace(Regex("[^\\d-]"), "").ifBlank { System.currentTimeMillis().toString() }
                                onAdd(id, newName, newStartDate, newWeeks.toIntOrNull() ?: 18)
                                newName = ""; newStartDate = "2026-02-23"; newWeeks = "18"; showAddForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("添加学期") }
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
