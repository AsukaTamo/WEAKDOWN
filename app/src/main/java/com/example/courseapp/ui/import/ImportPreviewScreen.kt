package com.example.courseapp.ui.import

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.components.elasticPress
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
    val currentSemesterCourseCount by viewModel.currentSemesterCourseCount.collectAsStateWithLifecycle()
    val importComplete by viewModel.importComplete.collectAsStateWithLifecycle()

    val palette = contentPalette(isDarkMode)
    val bgColor = Color.Transparent
    val subColor = palette.muted

    var showOverwriteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(importComplete) {
        if (importComplete) onImportComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.elasticPress(CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = palette.title,
                    navigationIconContentColor = palette.title
                )
            )
        },
        containerColor = bgColor,
        bottomBar = {
            Surface(
                color = palette.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (viewModel.currentImportAction() == com.example.courseapp.viewmodel.ImportOverwriteAction.ConfirmOverwrite) {
                                showOverwriteDialog = true
                            } else {
                                viewModel.importCurrentSemester(overwriteExisting = currentSemesterCourseCount > 0)
                            }
                        },
                        enabled = parsedCourses.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .elasticPress(
                                shape = RoundedCornerShape(12.dp),
                                enabled = parsedCourses.isNotEmpty()
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("导入当前课表", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

                items(parsedCourses) { course ->
                    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentCard(palette, radius = 14.dp)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.title
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
                    }
                }
            }
        }
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("覆盖当前课表") },
            text = {
                Text("当前学期已经有课程。继续后将直接覆盖当前课表，不进行冲突检测。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importCurrentSemester(overwriteExisting = true)
                        showOverwriteDialog = false
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) {
                    Text("继续覆盖")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverwriteDialog = false
                    },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) {
                    Text("再想想")
                }
            }
        )
    }
}
