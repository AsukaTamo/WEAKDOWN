package com.example.courseapp.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.appColors

internal val TodoPresetTags = listOf("考试", "会议", "聚会", "旅游")
internal data class TodoTagPalette(val ink: Color, val background: Color, val border: Color)

internal fun todoTagPalette(label: String, dark: Boolean): TodoTagPalette {
    val colors = appColors(dark)
    val hue = when(label) {
        "考试" -> if(dark) Color(0xFFFFA08B) else Color(0xFF9E321A)
        "会议" -> if(dark) Color(0xFF8DC5FF) else Color(0xFF145C9E)
        "聚会" -> if(dark) Color(0xFFD6B0FF) else Color(0xFF743B9E)
        "旅游" -> colors.success
        else -> colors.warning
    }
    return TodoTagPalette(hue,lerp(colors.surfaceRaised,hue,if(dark) .12f else .08f),hue.copy(alpha=.36f))
}

@Composable
internal fun TodoTagChip(label: String, dark: Boolean, selected: Boolean = true, onClick: (() -> Unit)? = null, enabled: Boolean = true) {
    val palette = todoTagPalette(label,dark)
    if(onClick != null) FilterChip(selected=selected,onClick=onClick,enabled=enabled,
        modifier=Modifier.elasticPress(CircleShape,enabled=enabled),
        label={Text(label,maxLines=2,overflow=TextOverflow.Ellipsis,color=palette.ink)},
        colors=FilterChipDefaults.filterChipColors(containerColor=appColors(dark).surfaceRaised,
            selectedContainerColor=palette.background,labelColor=palette.ink,selectedLabelColor=palette.ink,
            disabledLabelColor=palette.ink,disabledSelectedContainerColor=palette.background),
        border=BorderStroke(if(selected) 1.5.dp else 1.dp,if(selected) palette.ink else palette.border))
    else Surface(color=palette.background,contentColor=palette.ink,shape=CircleShape,border=BorderStroke(1.dp,palette.border)) {
        Text(label,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall,
            modifier=Modifier.heightIn(min=24.dp).padding(horizontal=8.dp,vertical=4.dp),color=palette.ink)
    }
}
