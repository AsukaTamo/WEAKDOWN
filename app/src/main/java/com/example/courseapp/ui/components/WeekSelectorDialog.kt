package com.example.courseapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.*

@Composable
fun WeekSelectorDialog(
    currentWeek: Int,
    onWeekSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean = false
) {
    var selectedWeek by remember { mutableIntStateOf(currentWeek) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                if (isDarkMode) GlassDark else GlassWhite
            )
    ) {
        // Frosted glass overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(
                    if (isDarkMode) Color.White.copy(alpha = 0.03f)
                    else Color.White.copy(alpha = 0.5f)
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle bar
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

            Text(
                text = "选择教学周",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else TextPrimary,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Week grid - 5 columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..4) {
                            val week = row * 5 + col + 1
                            if (week <= 20) {
                                WeekOption(
                                    week = week,
                                    isSelected = week == selectedWeek,
                                    isDarkMode = isDarkMode,
                                    onClick = { selectedWeek = week },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm button
            Button(
                onClick = { onWeekSelected(selectedWeek) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "确认",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
private fun WeekOption(
    week: Int,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (isSelected) Primary
                else if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFAFAFA)
            )
            .then(
                if (!isSelected) Modifier.shadow(0.dp) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = week.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White
                   else if (isDarkMode) Color.White else TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
