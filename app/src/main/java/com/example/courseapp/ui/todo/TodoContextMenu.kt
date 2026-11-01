package com.example.courseapp.ui.todo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.courseapp.data.model.Todo
import com.example.courseapp.data.model.isExamTodo
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.profile.GlassSurface
import com.example.courseapp.ui.theme.appColors
import io.github.itshedimisawi.vitro.GlassBackdrop
import kotlin.math.roundToInt

data class TodoContextRequest(val todo: Todo, val anchor: Rect, val expanded: Boolean = false)

/** A root overlay keeps the menu sharp while the entire application underneath is blurred. */
@Composable
fun TodoContextMenu(
    request: TodoContextRequest?, visible: Boolean, progress: Float,
    dark: Boolean, backdrop: GlassBackdrop,
    onDismiss: () -> Unit, onDelete: (Todo) -> Unit, onPin: (Todo, Boolean) -> Unit
) {
    val target = request ?: return
    if (!visible && progress <= .001f) return
    BackHandler { onDismiss() }
    val colors = appColors(dark)
    val density = LocalDensity.current
    val p = progress.coerceIn(0f, 1f)
    val automaticallyPinned = isExamTodo(target.todo)
    val interaction = remember { MutableInteractionSource() }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .16f * p))
            .clickable(interactionSource = interaction, indication = null, onClick = onDismiss)
    ) {
        val width = minOf(220.dp, maxWidth - 32.dp)
        val rowHeight = 52.dp * density.fontScale.coerceAtLeast(1f)
        val height = rowHeight * 2 + 16.dp
        val widthPx = with(density) { width.toPx() }
        val heightPx = with(density) { height.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }
        val screenWidth = with(density) { maxWidth.toPx() }
        val screenHeight = with(density) { maxHeight.toPx() }
        val safeTop = WindowInsets.statusBars.getTop(density) + marginPx
        val safeBottom = WindowInsets.navigationBars.getBottom(density) + marginPx
        val left = (target.anchor.right - widthPx).coerceIn(marginPx, (screenWidth - widthPx - marginPx).coerceAtLeast(marginPx))
        val top = (target.anchor.top - heightPx - with(density) { 8.dp.toPx() })
            .coerceIn(safeTop, (screenHeight - safeBottom - heightPx).coerceAtLeast(safeTop))
        // The selected card shares this sharp foreground layer with its menu.
        TodoCard(target.todo, target.expanded, false, System.currentTimeMillis(), dark, backdrop,
            onExpand = {}, onEdit = {}, onLongPress = {}, onComplete = {}, interactive = false,
            modifier = Modifier.offset { IntOffset(target.anchor.left.roundToInt(), target.anchor.top.roundToInt()) }
                .width(with(density) { target.anchor.width.toDp() }))
        GlassSurface(
            backdrop, dark,
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(width).height(height)
                .graphicsLayer {
                    alpha = p
                    scaleX = .94f + .06f * progress
                    scaleY = scaleX
                    translationY = (1f - progress) * with(density) { 8.dp.toPx() }
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(20.dp), corner = 20.dp
        ) {
            Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(rowHeight)
                        .elasticPress(RoundedCornerShape(12.dp), enabled = visible)
                        .clickable(enabled = visible) { onDelete(target.todo) }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = colors.danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("删除", color = colors.danger)
                }
                HorizontalDivider(color = colors.hairline)
                Row(
                    Modifier.fillMaxWidth().weight(1f)
                        .elasticPress(RoundedCornerShape(12.dp), enabled = visible && !automaticallyPinned)
                        .clickable(enabled = visible && !automaticallyPinned) { onPin(target.todo, !target.todo.isPinned) }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PushPin, null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(when {
                        automaticallyPinned -> "考试自动置顶"
                        target.todo.isPinned -> "取消置顶"
                        else -> "置顶"
                    }, color = colors.accentText)
                }
            }
        }
    }
}
