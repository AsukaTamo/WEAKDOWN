package com.example.courseapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.Success
import com.example.courseapp.ui.theme.Error
import kotlinx.coroutines.delay

@Composable
fun AppSnackbar(
    message: String,
    type: String = "info",
    visible: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val bgColor = when (type) {
        "success" -> Success
        "error" -> Error
        else -> Color(0xFF323232)
    }
    val icon: ImageVector = when (type) {
        "success" -> Icons.Default.CheckCircle
        "error" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    var show by remember { mutableStateOf(false) }

    LaunchedEffect(message, visible) {
        if (visible && message.isNotEmpty()) {
            show = true
            delay(2800)
            show = false
            delay(300)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .background(bgColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
