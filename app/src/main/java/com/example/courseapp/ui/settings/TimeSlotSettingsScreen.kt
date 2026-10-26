package com.example.courseapp.ui.settings

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.TimeSlotTemplate
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.ContentPalette
import com.example.courseapp.ui.theme.contentCard
import com.example.courseapp.ui.theme.contentPalette
import com.example.courseapp.viewmodel.ScheduleViewModel
import com.example.courseapp.viewmodel.TimeSlot
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotSettingsScreen(
    viewModel: ScheduleViewModel,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val timeSlots by viewModel.timeSlots.collectAsStateWithLifecycle()
    val templates by viewModel.timeSlotTemplates.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    val palette = contentPalette(isDarkMode)
    val activeTemplate = templates.firstOrNull { it.isActive }

    var editingSlots by remember { mutableStateOf(timeSlots.toList()) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<TimeSlotTemplate?>(null) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf("info") }

    // 这是覆盖在主壳之上的全屏面板，没有导航栈；不拦返回键会直接退出 App。
    BackHandler { onBack() }

    LaunchedEffect(timeSlots) {
        editingSlots = timeSlots.toList()
    }
    LaunchedEffect(snackbarFlow.value.first) {
        val (msg, type) = snackbarFlow.value
        if (msg.isNotEmpty()) {
            snackbarMessage = msg
            snackbarType = type
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(palette.canvasStart, palette.canvasEnd))
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("作息时间设置", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.elasticPress(CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (activeTemplate == null) {
                                    newTemplateName = defaultTemplateName(templates.size)
                                    showSaveAsDialog = true
                                } else {
                                    viewModel.updateActiveTimeSlotTemplate(editingSlots)
                                }
                            },
                            modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                        ) {
                            Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = palette.title,
                        navigationIconContentColor = palette.title
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    SectionTitle("我的作息表", palette)
                }

                items(templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        isActive = template.isActive,
                        palette = palette,
                        onSelect = { viewModel.selectTimeSlotTemplate(template) },
                        onDelete = { pendingDelete = template }
                    )
                }

                item {
                    TextButton(
                        onClick = {
                            newTemplateName = defaultTemplateName(templates.size)
                            showSaveAsDialog = true
                        },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("另存为新作息表", color = palette.accent, fontSize = 13.sp)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = palette.divider, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "当前编辑：${activeTemplate?.name ?: "未命名"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.title
                    )
                    Text(
                        text = "共 ${editingSlots.size} 节 · 改完点右上角「保存」",
                        fontSize = 12.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                items(editingSlots.size) { index ->
                    TimeSlotEditItem(
                        index = index,
                        slot = editingSlots[index],
                        palette = palette,
                        onStartTimeChange = { newStart ->
                            editingSlots = editingSlots.toMutableList().also {
                                it[index] = it[index].copy(start = newStart)
                            }
                        },
                        onEndTimeChange = { newEnd ->
                            editingSlots = editingSlots.toMutableList().also {
                                it[index] = it[index].copy(end = newEnd)
                            }
                        },
                        onDelete = {
                            editingSlots = editingSlots.toMutableList().also {
                                it.removeAt(index)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val lastEnd = editingSlots.lastOrNull()?.end ?: "08:30"
                            val parts = lastEnd.split(":")
                            val totalMin = parts[0].toInt() * 60 + parts[1].toInt() + 45
                            val newEnd = "${(totalMin / 60).toString().padStart(2, '0')}:${(totalMin % 60).toString().padStart(2, '0')}"
                            editingSlots = editingSlots + TimeSlot(lastEnd, newEnd)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.surfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("添加时段", fontSize = 14.sp)
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { editingSlots = ScheduleViewModel.defaultTimeSlots() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.surfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.body)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("恢复默认时间", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (snackbarMessage.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AppSnackbar(
                    message = snackbarMessage,
                    type = snackbarType,
                    onDismiss = { snackbarMessage = "" }
                )
            }
        }
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("另存为作息表") },
            text = {
                Column {
                    Text(
                        text = "当前 ${editingSlots.size} 节会保存成一张新作息表，并立即切换使用。",
                        fontSize = 13.sp,
                        color = palette.muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTemplateName,
                        onValueChange = { newTemplateName = it },
                        label = { Text("作息表名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newTemplateName.trim()
                        if (name.isNotEmpty()) {
                            viewModel.saveTimeSlotTemplate(name, editingSlots)
                            showSaveAsDialog = false
                        }
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("保存", color = palette.accent) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveAsDialog = false },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("取消") }
            }
        )
    }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除作息表") },
            text = { Text("确定删除「${template.name}」吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTimeSlotTemplate(template)
                        pendingDelete = null
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("删除", color = palette.destructive) }
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

private fun defaultTemplateName(existingCount: Int): String = "作息表 ${existingCount + 1}"

@Composable
private fun SectionTitle(text: String, palette: ContentPalette) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = palette.muted,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun TemplateRow(
    template: TimeSlotTemplate,
    isActive: Boolean,
    palette: ContentPalette,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elasticPress(shape, enabled = !isActive)
            .clip(shape)
            .background(if (isActive) palette.accentSoft else palette.surface)
            .border(0.8.dp, if (isActive) palette.accent.copy(alpha = 0.45f) else palette.surfaceBorder, shape)
            .clickable(enabled = !isActive, onClick = onSelect)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (isActive) palette.accent else palette.muted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = palette.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${template.periodCount()} 节 · ${if (isActive) "使用中" else "点击切换"}",
                fontSize = 11.sp,
                color = if (isActive) palette.accent else palette.muted,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (!isActive) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    // 作息表行本身已有触感，避免嵌套控件双重震动
                    .elasticPress(CircleShape, hapticsEnabled = false)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除作息表",
                    tint = palette.destructive,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun TimeSlotTemplate.periodCount(): Int = runCatching {
    Gson().fromJson(slotsJson, Array<TimeSlot>::class.java)?.size ?: 0
}.getOrDefault(0)

private fun parseTimeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

private fun minutesToTime(totalMin: Int): String {
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSlotEditItem(
    index: Int,
    slot: TimeSlot,
    palette: ContentPalette,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDurationMenu by remember { mutableStateOf(false) }

    val durationMin = parseTimeToMinutes(slot.end) - parseTimeToMinutes(slot.start)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .contentCard(palette)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "第${index + 1}节",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            modifier = Modifier.width(48.dp)
        )

        Text(
            text = slot.start,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.title,
            modifier = Modifier
                .elasticPress(RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable { showStartPicker = true }
                .background(palette.inputSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )

        Text("~", fontSize = 14.sp, color = palette.muted, modifier = Modifier.padding(horizontal = 6.dp))

        Text(
            text = slot.end,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.title,
            modifier = Modifier
                .elasticPress(RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable { showEndPicker = true }
                .background(palette.inputSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            Text(
                text = "${durationMin}分钟",
                fontSize = 11.sp,
                color = palette.accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .elasticPress(RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showDurationMenu = true }
                    .background(palette.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            DropdownMenu(
                expanded = showDurationMenu,
                onDismissRequest = { showDurationMenu = false }
            ) {
                listOf(45, 50, 90, 100, 120).forEach { dur ->
                    DropdownMenuItem(
                        text = { Text("${dur}分钟") },
                        modifier = Modifier.elasticPress(RoundedCornerShape(8.dp)),
                        onClick = {
                            val startMin = parseTimeToMinutes(slot.start)
                            onEndTimeChange(minutesToTime(startMin + dur))
                            showDurationMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(30.dp)
                .elasticPress(CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除",
                tint = palette.destructive,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showStartPicker) {
        val parts = slot.start.split(":")
        val state = rememberTimePickerState(
            initialHour = parts[0].toInt(),
            initialMinute = parts[1].toInt(),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartTimeChange(
                            "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                        )
                        showStartPicker = false
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("确定", color = palette.accent) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartPicker = false },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("取消") }
            },
            text = { TimePicker(state = state) }
        )
    }

    if (showEndPicker) {
        val parts = slot.end.split(":")
        val state = rememberTimePickerState(
            initialHour = parts[0].toInt(),
            initialMinute = parts[1].toInt(),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEndTimeChange(
                            "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                        )
                        showEndPicker = false
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("确定", color = palette.accent) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndPicker = false },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) { Text("取消") }
            },
            text = { TimePicker(state = state) }
        )
    }
}
