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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseContextMenu(
    course: Course?,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onEdit: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onCopy: (Course) -> Unit,
    onDrag: (Course) -> Unit
) {
    if (course == null) return

    val sheetState = rememberModalBottomSheetState()
    val sheetBg = if (isDarkMode) CardDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary

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
                .padding(bottom = 32.dp)
        ) {
            // Course info header
            Text(
                text = course.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "${course.teacher} · ${course.location}",
                fontSize = 13.sp,
                color = subColor,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Menu items
            ContextMenuItem(
                icon = Icons.Default.Edit,
                label = "编辑课程",
                description = "修改课程详细信息",
                tint = Primary,
                isDarkMode = isDarkMode,
                onClick = {
                    onDismiss()
                    onEdit(course)
                }
            )
            ContextMenuItem(
                icon = Icons.Default.ContentCopy,
                label = "复制课程",
                description = "复制到下一天相同时段",
                tint = Accent,
                isDarkMode = isDarkMode,
                onClick = {
                    onDismiss()
                    onCopy(course)
                }
            )
            ContextMenuItem(
                icon = Icons.Default.OpenWith,
                label = "拖拽移动",
                description = "长按拖拽到其他时间段",
                tint = Success,
                isDarkMode = isDarkMode,
                onClick = {
                    onDismiss()
                    onDrag(course)
                }
            )
            ContextMenuItem(
                icon = Icons.Default.Delete,
                label = "删除课程",
                description = "永久删除此课程",
                tint = Error,
                isDarkMode = isDarkMode,
                onClick = {
                    onDismiss()
                    onDelete(course)
                }
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    description: String,
    tint: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isDarkMode) BgDark else Color(0xFFF7F8FC)
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = subColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
