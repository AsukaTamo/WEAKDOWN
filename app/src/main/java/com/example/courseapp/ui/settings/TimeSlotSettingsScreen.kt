package com.example.courseapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.TimeSlotTemplate
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ScheduleViewModel
import com.example.courseapp.viewmodel.TimeSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotSettingsScreen(
    viewModel: ScheduleViewModel,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val timeSlots by viewModel.timeSlots.collectAsState()
    var editingSlots by remember { mutableStateOf(timeSlots.toList()) }
    var templateName by remember { mutableStateOf("自定义作息表") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val bgColor = if (isDarkMode) BgDark else BgLight
    val cardColor = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary
    val inputBg = if (isDarkMode) BgDark else Color(0xFFF5F5F5)

    LaunchedEffect(timeSlots) {
        editingSlots = timeSlots.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("作息时间设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showSaveDialog = true }) {
                        Text("保存", color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) CardDark else Color.White
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "当前作息表（共 ${editingSlots.size} 节课）",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = subColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(editingSlots.size) { index ->
                val slot = editingSlots[index]
                TimeSlotEditItem(
                    index = index,
                    slot = slot,
                    isDarkMode = isDarkMode,
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
                        val lastEnd = editingSlots.lastOrNull()?.end ?: "08:00"
                        val newStart = lastEnd
                        // Calculate new end as start + 45 min
                        val parts = newStart.split(":")
                        val totalMin = parts[0].toInt() * 60 + parts[1].toInt() + 45
                        val newEnd = "${(totalMin / 60).toString().padStart(2, '0')}:${(totalMin % 60).toString().padStart(2, '0')}"
                        editingSlots = editingSlots + TimeSlot(newStart, newEnd)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("添加时段")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { editingSlots = ScheduleViewModel.defaultTimeSlots() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("恢复默认")
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存作息表") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("作息表名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveTimeSlotTemplate(templateName, editingSlots)
                    showSaveDialog = false
                    onBack()
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TimeSlotEditItem(
    index: Int,
    slot: TimeSlot,
    isDarkMode: Boolean,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Period number
        Text(
            text = "第${index + 1}节",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.width(50.dp)
        )

        // Start time
        Text(
            text = slot.start,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        Text("~", fontSize = 14.sp, color = subColor, modifier = Modifier.padding(horizontal = 4.dp))

        // End time
        Text(
            text = slot.end,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "删除",
                tint = Error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
