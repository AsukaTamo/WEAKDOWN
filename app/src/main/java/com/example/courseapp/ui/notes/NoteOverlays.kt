package com.example.courseapp.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteCategoryColors
import com.example.courseapp.data.model.NoteCategoryDraft
import com.example.courseapp.data.model.NoteCategoryIconKeys
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.model.UNCATEGORIZED_CATEGORY_ID
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.AppColors

// ── 分类选择 Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteCategoryPickerSheet(
    request: NotePickerRequest,
    categories: List<NoteCategory>,
    selectedCategoryId: String?,
    photoCounts: Map<String, Int>,
    colors: AppColors,
    onSelect: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = when (request) {
        is NotePickerRequest.NewPhoto -> "保存到分类"
        is NotePickerRequest.Batch -> "保存 ${request.drafts.size} 张照片"
        is NotePickerRequest.Move -> "移动到分类"
    }
    val subtitle = when (request) {
        is NotePickerRequest.Move -> "选择目标学习文件夹，照片的原图不会重复复制"
        else -> "选择一个学习分类，方便以后按课程查找"
    }
    val confirmLabel = when (request) {
        is NotePickerRequest.NewPhoto -> "保存到此分类"
        is NotePickerRequest.Batch -> "保存到此分类"
        is NotePickerRequest.Move -> "确认移动"
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceRaised,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = colors.textMuted, fontSize = 11.sp)
                }
                NoteIconButton(
                    icon = Icons.Outlined.Close,
                    description = "关闭",
                    onClick = onDismiss,
                    colors = colors,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                categories.forEach { category ->
                    PickerCategoryRow(
                        category = category,
                        count = photoCounts[category.id] ?: 0,
                        selected = selectedCategoryId == category.id,
                        colors = colors,
                        onClick = { onSelect(category.id) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(onClick = onCreateCategory)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, null, tint = colors.accentText, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("新建分类", color = colors.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (request is NotePickerRequest.NewPhoto || request is NotePickerRequest.Batch) {
                    NoteSecondaryButton(
                        text = "暂不分类",
                        onClick = onSkip,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }
                NotePrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    colors = colors,
                    modifier = Modifier.weight(1.35f),
                    enabled = selectedCategoryId != null
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "「暂不分类」的照片会进入系统「未归类」文件夹，之后可随时移动。",
                color = colors.textMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun PickerCategoryRow(
    category: NoteCategory,
    count: Int,
    selected: Boolean,
    colors: AppColors,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.surface)
            .border(
                width = if (selected) 1.4.dp else 0.8.dp,
                color = if (selected) colors.accent else colors.hairline,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category, colors, 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = category.name,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("$count 张照片", color = colors.textMuted, fontSize = 10.sp)
        }
        if (selected) {
            Icon(Icons.Outlined.Check, null, tint = colors.accentText, modifier = Modifier.size(19.dp))
        } else {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.4.dp, colors.outline, CircleShape)
            )
        }
    }
}

@Composable
private fun CategoryBadge(category: NoteCategory, colors: AppColors, size: androidx.compose.ui.unit.Dp) {
    val tint = noteColor(category.colorValue)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(noteIconFor(category.iconKey), null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

// ── 新建/编辑分类 ─────────────────────────────────────────────────────────

@Composable
internal fun NoteCategoryEditorDialog(
    initial: NoteCategory?,
    colors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (NoteCategoryDraft) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var iconKey by remember(initial) { mutableStateOf(initial?.iconKey ?: NoteCategoryIconKeys.first()) }
    var colorValue by remember(initial) { mutableStateOf(initial?.colorValue ?: NoteCategoryColors.first()) }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        NoteSurface(colors = colors, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (initial == null) "新建分类" else "编辑分类",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    NoteIconButton(Icons.Outlined.Close, "关闭", onDismiss, colors, Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(18)
                        error = null
                    },
                    label = { Text("分类名称") },
                    placeholder = { Text("例如：高等数学", fontSize = 13.sp) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) Text(error!!, color = colors.danger, fontSize = 11.sp)
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = noteTextFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("选择图标", color = colors.textMuted, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    NoteCategoryIconKeys.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                val selected = key == iconKey
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) colors.accentSoft else colors.surfaceSunken)
                                        .border(
                                            width = if (selected) 1.4.dp else 0.dp,
                                            color = if (selected) colors.accent else colors.hairline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { iconKey = key }
                                        .semantics { contentDescription = "图标 $key" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        noteIconFor(key),
                                        null,
                                        tint = if (selected) colors.accentText else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("主题颜色", color = colors.textMuted, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteCategoryColors.forEach { color ->
                        val selected = color == colorValue
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(noteColor(color))
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) colors.textPrimary else colors.surfaceRaised,
                                    shape = CircleShape
                                )
                                .clickable { colorValue = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Outlined.Check, null, tint = colors.accentOn, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    NoteSecondaryButton("取消", onDismiss, colors, Modifier.weight(1f))
                    NotePrimaryButton(
                        text = if (initial == null) "创建文件夹" else "保存修改",
                        onClick = {
                            val draft = NoteCategoryDraft(name, iconKey, colorValue)
                            if (!draft.isValid) error = "请填写分类名称" else onConfirm(draft)
                        },
                        colors = colors,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun noteTextFieldColors(colors: AppColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.textPrimary,
    unfocusedTextColor = colors.textPrimary,
    focusedContainerColor = colors.surfaceSunken,
    unfocusedContainerColor = colors.surfaceSunken,
    focusedBorderColor = colors.accent,
    unfocusedBorderColor = colors.outline,
    cursorColor = colors.accent,
    focusedLabelColor = colors.accentText,
    unfocusedLabelColor = colors.textMuted,
    errorBorderColor = colors.danger,
    errorLabelColor = colors.danger
)

// ── 分类操作 / 删除确认 ───────────────────────────────────────────────────

@Composable
internal fun NoteCategoryActionsDialog(
    category: NoteCategory,
    photoCount: Int,
    colors: AppColors,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NoteSurface(colors = colors, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category, colors, 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("$photoCount 张照片", color = colors.textMuted, fontSize = 11.sp)
                    }
                    NoteIconButton(Icons.Outlined.Close, "关闭", onDismiss, colors, Modifier.size(32.dp))
                }
                Spacer(Modifier.height(18.dp))
                DialogActionRow(
                    icon = Icons.Outlined.Edit,
                    title = "重命名 / 修改图标颜色",
                    subtitle = "分类名称、图标和主题色",
                    colors = colors,
                    onClick = onRename
                )
                Spacer(Modifier.height(9.dp))
                DialogActionRow(
                    icon = Icons.Outlined.Delete,
                    title = if (category.isSystem) "系统分类不能删除" else "删除分类",
                    subtitle = if (category.isSystem) "「未归类」用于承接暂不分类的照片" else "照片不会被静默删除，可选择迁移",
                    colors = colors,
                    danger = true,
                    enabled = !category.isSystem,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun DialogActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    colors: AppColors,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceSunken)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = when {
                !enabled -> colors.textMuted.copy(alpha = 0.5f)
                danger -> colors.danger
                else -> colors.accentText
            },
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                title,
                color = if (danger && enabled) colors.danger else colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(subtitle, color = colors.textMuted, fontSize = 9.sp)
        }
    }
}

@Composable
internal fun NoteDeleteCategoryDialog(
    category: NoteCategory,
    photoCount: Int,
    colors: AppColors,
    onDismiss: () -> Unit,
    onMigrate: () -> Unit,
    onDeleteAll: () -> Unit
) {
    var confirmDeletePhotos by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        NoteSurface(colors = colors, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = if (confirmDeletePhotos) "确定删除 $photoCount 张照片？" else "删除「${category.name}」？",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        photoCount == 0 -> "这个分类还没有照片，删除后不可恢复。"
                        confirmDeletePhotos -> "照片和编辑记录会一并删除，无法恢复。原图不会保留。"
                        else -> "这个分类里有 $photoCount 张照片。默认把照片迁移到「未归类」，不会删除照片内容。"
                    },
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(18.dp))
                if (confirmDeletePhotos) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        NoteSecondaryButton("取消", { confirmDeletePhotos = false }, colors, Modifier.weight(1f))
                        NotePrimaryButton("确认删除照片", onDeleteAll, colors, Modifier.weight(1.4f))
                    }
                } else if (photoCount == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        NoteSecondaryButton("取消", onDismiss, colors, Modifier.weight(1f))
                        NotePrimaryButton("删除分类", onMigrate, colors, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        NotePrimaryButton(
                            "迁移到未归类并删除",
                            onMigrate,
                            colors,
                            Modifier.fillMaxWidth()
                        )
                        NoteSecondaryButton(
                            "删除分类和 $photoCount 张照片",
                            { confirmDeletePhotos = true },
                            colors,
                            Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("取消", color = colors.textMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── 照片标题/备注 ─────────────────────────────────────────────────────────

@Composable
internal fun NotePhotoMetaDialog(
    photo: NotePhoto,
    colors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String) -> Unit
) {
    var title by remember(photo.id) { mutableStateOf(photo.title) }
    var note by remember(photo.id) { mutableStateOf(photo.note) }
    Dialog(onDismissRequest = onDismiss) {
        NoteSurface(colors = colors, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "编辑标题与备注",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    NoteIconButton(Icons.Outlined.Close, "关闭", onDismiss, colors, Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(40) },
                    label = { Text("标题") },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    colors = noteTextFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("备注") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(13.dp),
                    colors = noteTextFieldColors(colors),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    NoteSecondaryButton("取消", onDismiss, colors, Modifier.weight(1f))
                    NotePrimaryButton("保存", { onConfirm(title, note) }, colors, Modifier.weight(1f))
                }
            }
        }
    }
}

// ── 权限说明 ──────────────────────────────────────────────────────────────

@Composable
internal fun NotePermissionDialog(
    colors: AppColors,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NoteSurface(colors = colors, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    NoteIconButton(Icons.Outlined.Close, "关闭", onDismiss, colors, Modifier.size(32.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(message, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(18.dp))
                NotePrimaryButton(primaryLabel, onPrimary, colors, Modifier.fillMaxWidth())
                if (secondaryLabel != null && onSecondary != null) {
                    Spacer(Modifier.height(8.dp))
                    NoteSecondaryButton(secondaryLabel, onSecondary, colors, Modifier.fillMaxWidth())
                }
                if (tertiaryLabel != null && onTertiary != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onTertiary, modifier = Modifier.fillMaxWidth()) {
                        Text(tertiaryLabel, color = colors.accentText, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "照片只保存在应用私有目录；只有你主动导出时才会写入系统相册。",
                    color = colors.textMuted,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

/** 未归类在分类列表中的展示名，供测试与 UI 复用。 */
internal fun NoteCategory.displayName(): String =
    if (id == UNCATEGORIZED_CATEGORY_ID) "未归类" else name
