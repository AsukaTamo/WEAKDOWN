package com.example.courseapp.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.Primary
import com.example.courseapp.ui.theme.ScheduleDimensions
import com.example.courseapp.ui.theme.ScheduleMotion
import com.example.courseapp.ui.theme.TextSecondary
import com.example.courseapp.ui.theme.TextPrimary
import com.example.courseapp.ui.theme.schedulePalette

@Composable
internal fun CourseTopBar(
    title: String,
    currentWeek: Int,
    showWeekSelector: Boolean,
    hasBackground: Boolean,
    isDarkMode: Boolean,
    isWeekDialogVisible: Boolean,
    onToggleDarkMode: () -> Unit,
    onWeekSelectorClick: () -> Unit
) {
    var darkModeAnimating by remember { mutableStateOf(false) }
    val darkModeRotation by animateFloatAsState(
        targetValue = if (darkModeAnimating) 360f else 0f,
        animationSpec = tween(ScheduleMotion.EmphasizedMillis, easing = ScheduleMotion.EmphasizedEasing),
        label = "darkModeRotation",
        finishedListener = { darkModeAnimating = false }
    )
    val weekArrowRotation by animateFloatAsState(
        targetValue = if (isWeekDialogVisible) 180f else 0f,
        animationSpec = tween(ScheduleMotion.StandardMillis, easing = ScheduleMotion.StandardEasing),
        label = "weekArrowRotation"
    )
    val palette = schedulePalette(isDarkMode, hasBackground)
    val topContentColor = if (hasBackground || isDarkMode) Color.White else TextPrimary
    val topSubColor = topContentColor.copy(alpha = if (hasBackground) 0.78f else 0.64f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.topGlass,
                        palette.topGlass.copy(alpha = if (hasBackground) 0.74f else 0.92f)
                    )
                )
            )
            .border(width = 0.5.dp, color = palette.topGlassEdge)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ScheduleDimensions.TopBarHeight)
        ) {
            IconButton(
                onClick = {
                    darkModeAnimating = true
                    onToggleDarkMode()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "深色模式",
                    tint = topContentColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            rotationZ = darkModeRotation
                            scaleX = if (darkModeAnimating) {
                                val progress = darkModeRotation / 360f
                                if (progress < 0.5f) {
                                    1f - progress * 0.4f
                                } else {
                                    0.8f + (progress - 0.5f) * 0.4f
                                }
                            } else {
                                1f
                            }
                            scaleY = scaleX
                        }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (showWeekSelector) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = topSubColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onWeekSelectorClick() }
                    ) {
                        Text(
                            text = "第${currentWeek}周",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = topContentColor,
                            letterSpacing = 0.6.sp
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "选择周数",
                            tint = topSubColor,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(18.dp)
                                .graphicsLayer {
                                    rotationZ = weekArrowRotation
                                }
                        )
                    }
                } else {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = topContentColor,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun CourseBottomBar(
    items: List<BottomNavItem>,
    currentDestination: AppDestination,
    isDarkMode: Boolean,
    hasBackground: Boolean,
    onDestinationSelected: (AppDestination) -> Unit
) {
    val palette = schedulePalette(isDarkMode, hasBackground)
    val inactiveColor = if (hasBackground) Color(0xFF6B7280) else if (isDarkMode) Color(0xFF8E93A6) else TextSecondary
    val selectedColor = if (hasBackground) Color(0xFF0F766E) else Primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (hasBackground) 10.dp else 4.dp,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(palette.bottomGlass)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = if (hasBackground) 0.42f else 0.18f),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentDestination == item.destination
                val selectedScale by animateFloatAsState(
                    targetValue = if (isSelected) ScheduleMotion.SelectedScale else 1f,
                    animationSpec = tween(
                        durationMillis = ScheduleMotion.StandardMillis,
                        easing = ScheduleMotion.EmphasizedEasing
                    ),
                    label = "bottomNavSelectedScale"
                )
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onDestinationSelected(item.destination) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = selectedScale
                                    scaleY = selectedScale
                                }
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = inactiveColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = selectedColor.copy(alpha = if (hasBackground) 0.12f else 0.10f)
                    )
                )
            }
        }
    }
}
