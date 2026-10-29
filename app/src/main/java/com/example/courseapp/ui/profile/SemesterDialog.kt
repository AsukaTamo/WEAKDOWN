package com.example.courseapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.courseapp.data.model.Semester
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.Error
import com.example.courseapp.ui.theme.contentPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterDialog(
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
