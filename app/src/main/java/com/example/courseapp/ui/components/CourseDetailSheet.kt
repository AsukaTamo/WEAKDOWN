package com.example.courseapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(
    course: Course?,
    isDarkMode: Boolean,
    timeSlotLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onCopy: (Course) -> Unit = {}
) {
    if (course == null) return

    val sheetState = rememberModalBottomSheetState()

    // Editable fields
    var editName by remember(course) { mutableStateOf(course.name) }
    var editTeacher by remember(course) { mutableStateOf(course.teacher) }
    var editLocation by remember(course) { mutableStateOf(course.location) }
    var editWeekRange by remember(course) { mutableStateOf(course.weekRange) }
    var editDay by remember(course) { mutableIntStateOf(course.dayOfWeek) }
    var editType by remember(course) { mutableStateOf(course.type) }
    var editStartSlot by remember(course) { mutableIntStateOf(course.startSlot) }
    var editSlotCount by remember(course) { mutableIntStateOf(course.slotCount) }
    var editCredits by remember(course) { mutableStateOf(course.credits.toString()) }
    var editNotes by remember(course) { mutableStateOf(course.notes) }
    var isEditing by remember { mutableStateOf(false) }

    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val types = CourseType.entries

    val cardColor = when (course.type) {
        CourseType.REQUIRED -> if (isDarkMode) CourseRequiredDark else CourseRequired
        CourseType.ELECTIVE -> if (isDarkMode) CourseElectiveDark else CourseElective
        CourseType.LAB -> if (isDarkMode) CourseLabDark else CourseLab
        CourseType.CUSTOM -> if (isDarkMode) CourseCustomDark else CourseCustom
    }

    val sheetBg = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary
    val inputBg = if (isDarkMode) BgDark else Color(0xFFF5F5F5)
    val inputBorder = if (isDarkMode) DividerDark else Divider

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            if (isDarkMode) Color(0xFF555555) else Color(0xFFD0D0D0),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color bar + title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(40.dp)
                        .background(cardColor, RoundedCornerShape(3.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${days[course.dayOfWeek]} · ${course.type.displayName}",
                        fontSize = 13.sp,
                        color = subColor
                    )
                }
                // Edit / Close buttons
                IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (isEditing) "取消" else "编辑",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(color = inputBorder)

            if (!isEditing) {
                // ---- View mode ----
                DetailRow("授课教师", course.teacher, subColor, textColor)
                DetailRow("上课地点", course.location, subColor, textColor)
                DetailRow(
                    "上课时间",
                    "${days[course.dayOfWeek]} ${timeSlotLabelFor(timeSlotLabels, course.startSlot, course.slotCount)}",
                    subColor, textColor
                )
                DetailRow("课程类型", course.type.displayName, subColor, textColor)
                DetailRow("周数范围", course.weekRange, subColor, textColor)
                DetailRow(
                    "节次信息",
                    "第${course.startSlot + 1}节 - 第${course.startSlot + course.slotCount}节",
                    subColor, textColor
                )
                if (course.credits > 0f) {
                    DetailRow("学分", course.credits.toString(), subColor, textColor)
                }
                if (course.notes.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("备注信息", fontSize = 14.sp, color = subColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(course.notes, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Copy + Delete buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopy(course) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制课程", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    OutlinedButton(
                        onClick = { onDelete(course) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                        border = BorderStroke(1.dp, Error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("删除课程", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // ---- Edit mode ----
                EditField("课程名称", editName, { editName = it }, inputBg, textColor, subColor)
                EditField("授课教师", editTeacher, { editTeacher = it }, inputBg, textColor, subColor)
                EditField("上课地点", editLocation, { editLocation = it }, inputBg, textColor, subColor)
                EditField("周数范围", editWeekRange, { editWeekRange = it }, inputBg, textColor, subColor)
                EditField("学分", editCredits, { editCredits = it }, inputBg, textColor, subColor)
                EditField("备注", editNotes, { editNotes = it }, inputBg, textColor, subColor)

                // Day selector
                Text("上课星期", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEachIndexed { index, label ->
                        val selected = editDay == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Primary else inputBg)
                                .clickable { editDay = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else textColor
                            )
                        }
                    }
                }

                // Type selector
                Text("课程类型", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { type ->
                        val selected = editType == type
                        val tColor = when (type) {
                            CourseType.REQUIRED -> CourseRequired
                            CourseType.ELECTIVE -> CourseElective
                            CourseType.LAB -> CourseLab
                            CourseType.CUSTOM -> CourseCustom
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) tColor else inputBg)
                                .clickable { editType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else textColor
                            )
                        }
                    }
                }

                // Start slot selector
                Text("开始节次", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val periodCount = timeSlotLabels.size / 2 // 10 periods
                    (0 until periodCount).forEach { slotIdx ->
                        val selected = editStartSlot == slotIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Primary else inputBg)
                                .clickable { editStartSlot = slotIdx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${slotIdx + 1}",
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else textColor
                            )
                        }
                    }
                }

                // Slot count selector
                Text("持续节数", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (2..4).forEach { count ->
                        val selected = editSlotCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Primary else inputBg)
                                .clickable { editSlotCount = count },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${count}节",
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = {
                        val updated = course.copy(
                            name = editName,
                            teacher = editTeacher,
                            location = editLocation,
                            weekRange = editWeekRange,
                            dayOfWeek = editDay,
                            type = editType,
                            startSlot = editStartSlot,
                            slotCount = editSlotCount,
                            credits = editCredits.toFloatOrNull() ?: 0f,
                            notes = editNotes
                        )
                        onSave(updated)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存修改", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = labelColor)
        Text(text = value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    bgColor: Color,
    textColor: Color,
    labelColor: Color
) {
    Column {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = labelColor)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

private fun timeSlotLabelFor(labels: List<String>, startSlot: Int, slotCount: Int): String {
    // labels = [start0, end0, start1, end1, ...] — 2 entries per period
    val start = labels.getOrElse(startSlot * 2) { "??:??" }
    val endIdx = (startSlot + slotCount - 1) * 2 + 1
    val end = labels.getOrElse(endIdx) { "??:??" }
    return "$start - $end"
}
