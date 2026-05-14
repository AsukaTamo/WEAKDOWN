package com.example.courseapp.ui.manage

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.components.AppSnackbar
import com.example.courseapp.ui.components.ColorPickerRow
import com.example.courseapp.ui.theme.*
import com.example.courseapp.viewmodel.ManageViewModel

@Composable
fun ManageScreen(
    viewModel: ManageViewModel = hiltViewModel(),
    isDarkMode: Boolean = false
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val snackbarFlow = viewModel.snackbarMessage.collectAsStateWithLifecycle(initialValue = "" to "")

    var snackbarState by remember { mutableStateOf(SnackbarState()) }

    LaunchedEffect(snackbarFlow.value.first) {
        val (msg, type) = snackbarFlow.value
        if (msg.isNotEmpty()) {
            snackbarState = SnackbarState(msg, type, true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab bar
        TabBar(
            selectedTab = viewModel.selectedTab,
            onTabSelected = { viewModel.switchTab(it) },
            isDarkMode = isDarkMode
        )

        // Content
        AnimatedContent(
            targetState = viewModel.selectedTab,
            label = "tabContent",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> AllCoursesPanel(
                    courses = courses,
                    isDarkMode = isDarkMode,
                    isBatchMode = viewModel.isBatchMode,
                    selectedIds = viewModel.selectedIds,
                    onToggleBatch = { viewModel.toggleBatchMode() },
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onSelectAll = { viewModel.selectAll() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                    onDeleteCourse = { viewModel.deleteCourse(it) },
                    onDuplicateCourse = { viewModel.duplicateCourse(it) }
                )
                1 -> AddCoursePanel(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode
                )
            }
        }
    }

    // Snackbar
    if (snackbarState.visible) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AppSnackbar(
                message = snackbarState.message,
                type = snackbarState.type,
                onDismiss = { snackbarState = snackbarState.copy(visible = false) }
            )
        }
    }
}

private data class SnackbarState(
    val message: String = "",
    val type: String = "info",
    val visible: Boolean = false
)

@Composable
private fun TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isDarkMode: Boolean
) {
    val tabs = listOf("全部课程", "添加课程")
    val bgColor = if (isDarkMode) BgDark else BgLight
    val borderColor = if (isDarkMode) DividerDark else Divider
    val inactiveColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bgColor)
            .border(1.dp, borderColor)
    ) {
        tabs.forEachIndexed { index, title ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedTab == index) Primary else inactiveColor,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (selectedTab == index) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .background(Primary, RoundedCornerShape(2.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    }
}

@Composable
private fun AllCoursesPanel(
    courses: List<Course>,
    isDarkMode: Boolean,
    isBatchMode: Boolean,
    selectedIds: List<Long>,
    onToggleBatch: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onDuplicateCourse: (Course) -> Unit
) {
    val bgColor = if (isDarkMode) BgDark else BgLight
    var showDeleteDialog by remember { mutableStateOf<Course?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBatchMode) "已选 ${selectedIds.size} 项" else "共 ${courses.size} 门课程",
                fontSize = 13.sp,
                color = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isBatchMode) {
                    TextButton(onClick = onSelectAll) {
                        Text("全选", fontSize = 13.sp, color = Primary)
                    }
                    TextButton(onClick = onDeleteSelected) {
                        Text("删除", fontSize = 13.sp, color = Error)
                    }
                }
                TextButton(onClick = onToggleBatch) {
                    Text(
                        if (isBatchMode) "取消" else "批量管理",
                        fontSize = 13.sp,
                        color = Primary
                    )
                }
            }
        }

        if (courses.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isDarkMode) Color(0xFF3A3D48) else Color(0xFFD0D4E0)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无课程",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) Color(0xFF6B7080) else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击「添加课程」标签页开始添加",
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color(0xFF555555) else Color(0xFFBDBDBD)
                    )
                }
            }
        } else {
            // Group courses by name
            val grouped = courses.groupBy { it.name }
            val expandedGroups = remember { mutableStateListOf<String>() }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (name, groupCourses) ->
                    val isExpanded = expandedGroups.contains(name)
                    item(key = "header_$name") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (expandedGroups.contains(name)) expandedGroups.remove(name)
                                    else expandedGroups.add(name)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${groupCourses.size}节",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary
                            )
                        }
                    }

                    if (isExpanded) {
                        itemsIndexed(groupCourses, key = { _, c -> c.id }) { _, course ->
                            val isSelected = selectedIds.contains(course.id)
                            CourseListItem(
                                course = course,
                                isDarkMode = isDarkMode,
                                isBatchMode = isBatchMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isBatchMode) onToggleSelection(course.id)
                                },
                                onDelete = { showDeleteDialog = course },
                                onDuplicate = { onDuplicateCourse(course) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { course ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${course.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCourse(course)
                    showDeleteDialog = null
                }) { Text("删除", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CourseListItem(
    course: Course,
    isDarkMode: Boolean,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val itemBg = if (isDarkMode) CardDark else Color.White
    val borderColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFF0F0F0)
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val detailColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    val indicatorColor: Color = if (course.customColor.isNotEmpty()) {
        try { Color(android.graphics.Color.parseColor(course.customColor)) }
        catch (_: Exception) { CourseRequired }
    } else when (course.type) {
        CourseType.REQUIRED -> CourseRequired
        CourseType.ELECTIVE -> CourseElective
        CourseType.LAB -> CourseLab
        CourseType.CUSTOM -> CourseCustom
    }

    val dayText = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[course.dayOfWeek]
    val slotLabels = listOf("第1节", "第2节", "第3节", "第4节", "第5节", "第6节", "第7节", "第8节", "第9节", "第10节")
    val startLabel = slotLabels.getOrElse(course.startSlot) { "第${course.startSlot + 1}节" }
    val endLabel = slotLabels.getOrElse(course.startSlot + course.slotCount - 1) { "第${course.startSlot + course.slotCount}节" }

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(itemBg)
            .border(0.5.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Batch checkbox or color indicator
        if (isBatchMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(indicatorColor, RoundedCornerShape(2.dp))
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = course.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                letterSpacing = 0.3.sp
            )
            Text(
                text = "$dayText $startLabel-$endLabel",
                fontSize = 12.sp,
                color = detailColor,
                letterSpacing = 0.2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course.location.isNotEmpty()) {
                    Text(
                        text = course.location,
                        fontSize = 11.sp,
                        color = detailColor.copy(alpha = 0.8f)
                    )
                }
                if (course.teacher.isNotEmpty()) {
                    Text(
                        text = course.teacher,
                        fontSize = 11.sp,
                        color = detailColor.copy(alpha = 0.8f)
                    )
                }
            }
            Text(
                text = "${course.weekRange} · ${course.type.displayName}",
                fontSize = 11.sp,
                color = detailColor.copy(alpha = 0.6f)
            )
        }

        // Context menu
        if (!isBatchMode) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("复制课程") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                        onClick = { onDuplicate(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = Error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Error, modifier = Modifier.size(18.dp)) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCoursePanel(
    viewModel: ManageViewModel,
    isDarkMode: Boolean
) {
    val bgColor = if (isDarkMode) BgDark else BgLight
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val labelColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val types = CourseType.entries.toList()
    val slotOptions = (1..10).map { "第${it}节" }
    val countOptions = (1..6).map { "${it}节" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FormField("课程名称 *", viewModel.courseName, { viewModel.updateCourseName(it) }, "请输入课程名称", isDarkMode)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("授课教师", viewModel.teacher, { viewModel.updateTeacher(it) }, "教师姓名", isDarkMode, Modifier.weight(1f))
                FormField("上课地点", viewModel.location, { viewModel.updateLocation(it) }, "教室", isDarkMode, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("学分", viewModel.credits, { viewModel.updateCredits(it) }, "0.0", isDarkMode, Modifier.weight(1f))
                FormField("周次范围", viewModel.weekRange, { viewModel.updateWeekRange(it) }, "1-18周", isDarkMode, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormDropdown("上课星期", days, viewModel.selectedDay, { viewModel.updateDay(it) }, isDarkMode, Modifier.weight(1f))
                FormDropdown("课程类型", types.map { it.displayName }, types.indexOf(viewModel.selectedType), { viewModel.updateType(types[it]) }, isDarkMode, Modifier.weight(1f))
            }
        }
        item {
            ColorPickerRow(
                selectedColor = viewModel.selectedColor,
                onColorSelected = { viewModel.updateColor(it) },
                labelColor = labelColor
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormDropdown("开始节次", slotOptions, viewModel.selectedStartSlot, { viewModel.updateStartSlot(it) }, isDarkMode, Modifier.weight(1f))
                FormDropdown("持续节数", countOptions, (viewModel.selectedSlotCount - 1).coerceIn(0, 5), { viewModel.updateSlotCount(it + 1) }, isDarkMode, Modifier.weight(1f))
            }
        }
        item {
            FormField("备注", viewModel.notes, { viewModel.updateNotes(it) }, "可选备注信息", isDarkMode)
        }
        item {
            Button(
                onClick = { viewModel.saveCourse() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("保存课程", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val inputBg = if (isDarkMode) CardDark else Color.White
    val inputBorder = if (isDarkMode) DividerDark else Divider
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val labelColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFFBDBDBD), fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = inputBorder,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
        )
    }
}

@Composable
private fun FormDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val inputBg = if (isDarkMode) CardDark else Color.White
    val inputBorder = if (isDarkMode) DividerDark else Divider
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val labelColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box {
            OutlinedTextField(
                value = options.getOrElse(selectedIndex) { options.first() },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clickable { expanded = true },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = inputBorder,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    disabledBorderColor = inputBorder,
                    disabledContainerColor = inputBg,
                    disabledTextColor = textColor
                ),
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, null, tint = labelColor)
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(inputBg)
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, color = textColor, fontSize = 13.sp) },
                        onClick = { onSelected(index); expanded = false }
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
        }
    }
}
