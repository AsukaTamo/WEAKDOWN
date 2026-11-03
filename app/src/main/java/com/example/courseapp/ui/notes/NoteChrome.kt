package com.example.courseapp.ui.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.AppColors

/**
 * 随手记页面与全局顶栏共享的布局状态。
 * 首页/分类详情显示顶栏左侧的网格/列表按钮时，点按只改这里，由 FLIP 容器消费。
 */
@Stable
class NotesChromeState {
    var isGrid by mutableStateOf(true)
    var showLayoutToggle by mutableStateOf(false)
    var layoutToggleDescription by mutableStateOf("切换为列表")

    fun toggleLayout() {
        isGrid = !isGrid
        layoutToggleDescription = if (isGrid) "切换为列表" else "切换为网格"
    }

    fun syncDescription() {
        layoutToggleDescription = if (isGrid) "切换为列表" else "切换为网格"
    }
}

fun noteIconFor(key: String): ImageVector = when (key) {
    "code" -> Icons.Outlined.Code
    "language" -> Icons.Outlined.Translate
    "math" -> Icons.Outlined.Functions
    "science" -> Icons.Outlined.Science
    "star" -> Icons.Outlined.Star
    "note" -> Icons.Outlined.EditNote
    "palette" -> Icons.Outlined.Palette
    "inbox" -> Icons.Outlined.Inbox
    else -> Icons.Outlined.MenuBook
}

fun noteColor(value: Int): Color = Color(value)

/** 奶油白卡片：投影 + 细描边 + 极轻的顶部受光，延续原型图的柔和轻拟物。 */
@Composable
fun NoteSurface(
    colors: AppColors,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    elevated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 9.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.13f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceRaised,
                        colors.surface
                    )
                )
            )
            .border(1.dp, colors.hairline, shape),
        content = content
    )
}

@Composable
fun NoteIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val background by animateColorAsState(
        if (selected) colors.accentSoft else colors.surfaceSunken,
        tween(160),
        label = "noteIconBg"
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .elasticPress(CircleShape, enabled = enabled)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.accentText else colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 顶部布局切换器：一个胶囊里放网格/列表两个小按钮。 */
@Composable
fun NoteLayoutSwitch(
    isGrid: Boolean,
    onToggle: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceSunken)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(true to (Icons.Outlined.GridView to "网格布局"), false to (Icons.Outlined.ViewList to "列表布局"))
            .forEach { (grid, pair) ->
                val selected = grid == isGrid
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 28.dp)
                        .then(if (selected) Modifier else Modifier.clickable(onClick = onToggle))
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) colors.accent else Color.Transparent)
                        .semantics { contentDescription = pair.second },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pair.first,
                        contentDescription = null,
                        tint = if (selected) colors.accentOn else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
    }
}

@Composable
fun NoteChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: AppColors,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val container by animateColorAsState(
        if (selected) colors.accent else colors.surfaceSunken,
        tween(180),
        label = "noteChip"
    )
    Row(
        modifier = modifier
            .elasticPress(shape)
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 34.dp)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) colors.accentOn else colors.accentText,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            color = if (selected) colors.accentOn else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** 页面内主按钮。 */
@Composable
fun NotePrimaryButton(
    text: String,
    onClick: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .elasticPress(shape, enabled = enabled)
            .clip(shape)
            .background(if (enabled) colors.accent else colors.surfaceSunken)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) colors.accentOn else colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = if (enabled) colors.accentOn else colors.textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NoteSecondaryButton(
    text: String,
    onClick: () -> Unit,
    colors: AppColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .elasticPress(shape, enabled = enabled)
            .clip(shape)
            .background(colors.surfaceSunken)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 页面标题行：返回 + 标题 + 说明 + 右侧操作。 */
@Composable
fun NotePageHeader(
    title: String,
    subtitle: String?,
    colors: AppColors,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            NoteIconButton(
                icon = Icons.Outlined.ArrowBack,
                description = "返回",
                onClick = onBack,
                colors = colors
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        actions()
    }
}

@Composable
fun NoteEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    colors: AppColors,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = colors.accentText, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = message,
            color = colors.textMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NotePrimaryButton(primaryLabel, onPrimary, colors)
                if (secondaryLabel != null && onSecondary != null) {
                    NoteSecondaryButton(secondaryLabel, onSecondary, colors)
                }
            }
        } else if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(18.dp))
            NoteSecondaryButton(secondaryLabel, onSecondary, colors)
        }
    }
}

@Composable
fun NoteSectionHeader(
    title: String,
    subtitle: String? = null,
    colors: AppColors,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = colors.textMuted, fontSize = 11.sp)
            }
        }
        action?.invoke()
    }
}

/** 保存/渲染阻塞态：半透明遮罩 + 进度，防止重复提交。 */
@Composable
fun NoteBusyOverlay(visible: Boolean, colors: AppColors, label: String = "正在处理…") {
    if (!visible) return
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            // 消费点击，避免保存中重复触发底层按钮。
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceRaised)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.accent,
                strokeWidth = 2.dp
            )
            Text(label, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** 空列表占位的小图标 + 文案，用于卡片里的“暂无照片”。 */
@Composable
fun NoteInlineEmpty(text: String, colors: AppColors, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = colors.textMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun NoteCheckMark(selected: Boolean, colors: AppColors) {
    if (selected) {
        Icon(Icons.Outlined.Check, null, tint = colors.accentText, modifier = Modifier.size(18.dp))
    }
}

/** 供手感统一：选中态卡片描边。 */
fun Modifier.noteSelectionBorder(selected: Boolean, colors: AppColors, shape: RoundedCornerShape): Modifier =
    if (selected) border(BorderStroke(1.6.dp, colors.accent), shape) else this

@Composable
fun NoteDockShape(colors: AppColors, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceRaised.copy(alpha = 0.97f))
            .border(1.dp, colors.hairline, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}
