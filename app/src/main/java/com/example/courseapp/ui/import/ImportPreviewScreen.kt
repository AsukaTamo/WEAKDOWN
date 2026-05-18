package com.example.courseapp.ui.import

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    viewModel: ImportViewModel,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onImportComplete: () -> Unit
) {
    val parsedCourses by viewModel.parsedCourses.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val importComplete by viewModel.importComplete.collectAsStateWithLifecycle()

    val bgColor = if (isDarkMode) BgDark else BgLight
    val cardColor = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    var showConflictDialog by remember { mutableStateOf(false) }
    var showReplaceAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(importComplete) {
        if (importComplete) onImportComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.selectAll() }) {
                        Text("全选", color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) CardDark else Color.White
                )
            )
        },
        containerColor = bgColor,
        bottomBar = {
            Surface(
                color = if (isDarkMode) CardDark else Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Full replace — primary action
                    Button(
                        onClick = { showReplaceAllDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("全部替换当前课表", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Add selected / add all
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.importSelected(replaceConflicts = false) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("全部追加")
                        }
                        OutlinedButton(
                            onClick = {
                                if (conflicts.isNotEmpty()) {
                                    showConflictDialog = true
                                } else {
                                    viewModel.importSelected(replaceConflicts = false)
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("追加选中 (${selectedIds.size})")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (parsedCourses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = subColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("未检测到课程数据", fontSize = 16.sp, color = subColor)
                            Text("请确保页面已完全加载", fontSize = 13.sp, color = subColor)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "检测到 ${parsedCourses.size} 门课程",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                itemsIndexed(parsedCourses) { index, course ->
                    val isSelected = selectedIds.contains(index.toLong())
                    val isConflict = conflicts.any { it.newCourse == course }
                    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor)
                            .clickable { viewModel.toggleSelection(index.toLong()) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSelection(index.toLong()) },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = "${days[course.dayOfWeek]} 第${course.startSlot + 1}-${course.startSlot + course.slotCount}节  ${course.weekRange}",
                                fontSize = 12.sp,
                                color = subColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (course.teacher.isNotEmpty() || course.location.isNotEmpty()) {
                                Text(
                                    text = listOfNotNull(
                                        course.teacher.ifEmpty { null },
                                        course.location.ifEmpty { null }
                                    ).joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = subColor
                                )
                            }
                            if (course.credits > 0f) {
                                Text(
                                    text = "学分: ${course.credits}",
                                    fontSize = 11.sp,
                                    color = subColor
                                )
                            }
                            if (course.notes.isNotEmpty()) {
                                Text(
                                    text = course.notes,
                                    fontSize = 11.sp,
                                    color = subColor,
                                    maxLines = 2
                                )
                            }
                        }
                        if (isConflict) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "冲突",
                                tint = Warning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Conflict resolution dialog
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("时间冲突") },
            text = {
                Text("有 ${conflicts.size} 门课程与现有课程时间冲突。是否替换现有课程？")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importSelected(replaceConflicts = true)
                    showConflictDialog = false
                }) {
                    Text("替换并导入")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.importSelected(replaceConflicts = false)
                    showConflictDialog = false
                }) {
                    Text("保留两者")
                }
            }
        )
    }

    // Replace all confirmation dialog
    if (showReplaceAllDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceAllDialog = false },
            title = { Text("全部替换") },
            text = {
                Text("将删除当前学期的所有课程，然后导入 ${selectedIds.size} 门新课程。此操作不可撤销。")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importSelected(replaceAll = true)
                    showReplaceAllDialog = false
                }) {
                    Text("确认替换", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
