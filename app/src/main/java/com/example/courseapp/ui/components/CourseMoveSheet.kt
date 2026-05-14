package com.example.courseapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.courseapp.data.model.Course
import com.example.courseapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMoveSheet(
    course: Course?,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onMove: (Course, Int, Int) -> Unit,
    onMoveAndReplace: (Course, Int, Int) -> Unit
) {
    if (course == null) return

    val sheetState = rememberModalBottomSheetState()
    val sheetBg = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary
    val inputBg = if (isDarkMode) BgDark else Color(0xFFF5F5F5)

    var selectedDay by remember(course) { mutableIntStateOf(course.dayOfWeek) }
    var selectedSlot by remember(course) { mutableIntStateOf(course.startSlot) }
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

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
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "移动课程",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "${course.name} → 选择目标位置",
                fontSize = 13.sp,
                color = subColor
            )

            // Day selector
            Text("目标星期", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEachIndexed { index, label ->
                    val selected = selectedDay == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Primary else inputBg)
                            .clickable { selectedDay = index },
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

            // Slot selector
            Text("目标节次", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0 until 10).forEach { slotIdx ->
                    val selected = selectedSlot == slotIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Primary else inputBg)
                            .clickable { selectedSlot = slotIdx },
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

            Spacer(modifier = Modifier.height(4.dp))

            // Move button
            Button(
                onClick = { onMove(course, selectedDay, selectedSlot) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("确认移动", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
