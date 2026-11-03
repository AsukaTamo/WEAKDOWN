package com.example.courseapp.ui.notes

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.AppColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 照片卡片。缩略图在卡片根部加载一次，网格/列表只改变摆放方式，
 * 图片资源不因布局切换重新解码（见 `rememberNoteBitmap` 的缓存）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhotoCard(
    photo: NotePhoto,
    isGrid: Boolean,
    colors: AppColors,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(if (isGrid) 17.dp else 15.dp)
    val bitmap = rememberNoteBitmap(
        relativePath = photo.previewRelativePath ?: photo.originalRelativePath,
        rotation = if (photo.previewRelativePath != null) 0 else photo.originalRotation,
        maxSize = 512
    )
    NoteSurface(
        colors = colors,
        modifier = Modifier
            .fillMaxSize()
            .elasticPress(shape)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
            .noteSelectionBorder(selected, colors, shape),
        shape = shape,
        elevated = true
    ) {
        if (isGrid) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(colors.surfaceSunken)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = photo.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.Image, null, tint = colors.textMuted, modifier = Modifier.align(Alignment.Center).size(24.dp))
                    }
                    if (photo.isEdited) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(colors.accent.copy(alpha = 0.86f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("已标注", color = colors.accentOn, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                    Text(
                        text = photo.title,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatNoteTime(photo.capturedAt),
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (photo.isEdited) "已标注" else "课堂记录",
                            color = colors.accentText,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(82.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSunken)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = photo.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.Image, null, tint = colors.textMuted, modifier = Modifier.align(Alignment.Center).size(20.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = photo.title,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (photo.note.isNotBlank()) {
                        Text(
                            text = photo.note,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${formatNoteTime(photo.capturedAt)} · ${if (photo.isEdited) "已标注" else "课堂记录"}",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
                if (selectionMode) {
                    SelectionMark(selected, colors)
                } else {
                    Icon(Icons.Outlined.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (selectionMode && isGrid) {
            Box(Modifier.align(Alignment.TopStart).padding(7.dp)) {
                SelectionMark(selected, colors)
            }
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, colors: AppColors) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.surfaceRaised.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = colors.accentOn, modifier = Modifier.size(15.dp))
        }
    }
}

/** 照片详情：大图 + 圆形信息提示 + 右下角可展开操作。 */
@Composable
internal fun PhotoDetailPage(
    photo: NotePhoto?,
    folderName: String,
    photos: List<NotePhoto>,
    colors: AppColors,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onEdit: (NotePhoto) -> Unit,
    onMove: (NotePhoto) -> Unit,
    onDelete: (NotePhoto) -> Unit,
    onExport: (NotePhoto) -> Unit,
    onShare: (NotePhoto) -> Unit,
    onRename: (NotePhoto) -> Unit
) {
    if (photo == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("照片不存在或已删除", color = colors.textSecondary)
                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text("返回", color = colors.accentText)
                }
            }
        }
        return
    }
    val index = photos.indexOfFirst { it.id == photo.id }
    val previous = photos.getOrNull(index - 1)
    val next = photos.getOrNull(index + 1)
    val bitmap = rememberNoteBitmap(
        relativePath = photo.displayRelativePath,
        rotation = if (photo.isEdited) 0 else photo.originalRotation,
        maxSize = 2048
    )

    var infoVisible by remember(photo.id) { mutableStateOf(false) }
    var infoRendered by remember(photo.id) { mutableStateOf(false) }
    var actionsVisible by remember(photo.id) { mutableStateOf(false) }
    val infoProgress = remember(photo.id) { Animatable(0f) }
    // 打开与收回使用同一条弹性曲线。
    val infoSpring = remember { spring<Float>(dampingRatio = 0.72f, stiffness = 430f) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (actionsVisible) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 500f),
        label = "photoActionsArrow"
    )
    val blurProgress = remember { derivedStateOf { infoProgress.value } }
    val dismissInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(infoVisible) {
        if (infoVisible) {
            infoRendered = true
            infoProgress.animateTo(1f, infoSpring)
        } else if (infoRendered) {
            infoProgress.animateTo(0f, infoSpring)
            infoRendered = false
        }
    }

    BackHandler(enabled = infoVisible || actionsVisible) {
        infoVisible = false
        actionsVisible = false
    }

    val actionItems = listOf(
        PhotoActionItem(Icons.Outlined.Edit, "重新编辑") {
            actionsVisible = false
            onEdit(photo)
        },
        PhotoActionItem(Icons.Outlined.DriveFileMove, "移动分类") {
            actionsVisible = false
            onMove(photo)
        },
        PhotoActionItem(Icons.Outlined.Share, "分享") {
            actionsVisible = false
            onShare(photo)
        },
        PhotoActionItem(Icons.Outlined.FileDownload, "导出") {
            actionsVisible = false
            onExport(photo)
        },
        PhotoActionItem(Icons.Outlined.Delete, "删除", danger = true) {
            actionsVisible = false
            onDelete(photo)
        }
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 只有 Android 12+ 有硬件模糊；低版本退化为下面的半透明遮罩。
                // 模糊层从首帧就以 0.01px 挂载，避免首次设置 RenderEffect 时闪黑。
                .infoBackdropBlur(blurProgress)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 10.dp
                )
        ) {
            NotePageHeader(
                title = photo.title,
                subtitle = folderName,
                colors = colors,
                onBack = onBack,
                actions = {
                    NoteIconButton(
                        icon = Icons.Outlined.Edit,
                        description = "编辑标题和备注",
                        onClick = { onRename(photo) },
                        colors = colors
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                ZoomableNoteImage(
                    bitmap = bitmap,
                    contentDescription = photo.title,
                    onSwipeLeft = { next?.let { onOpenPhoto(it.id) } },
                    onSwipeRight = { previous?.let { onOpenPhoto(it.id) } }
                )
                if (photos.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceRaised.copy(alpha = 0.88f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "上一张",
                            tint = if (previous != null) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(20.dp)
                                .pointerInput(previous?.id) {
                                    previous?.let { target ->
                                        detectTapGestures { onOpenPhoto(target.id) }
                                    }
                                }
                        )
                        Text(
                            "${index + 1} / ${photos.size}",
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "下一张",
                            tint = if (next != null) colors.textPrimary else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(20.dp)
                                .pointerInput(next?.id) {
                                    next?.let { target ->
                                        detectTapGestures { onOpenPhoto(target.id) }
                                    }
                                }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(48.dp)) {
                PhotoInfoHintButton(
                    colors = colors,
                    onClick = {
                        actionsVisible = false
                        infoVisible = true
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                PhotoActionToggle(
                    colors = colors,
                    expanded = actionsVisible,
                    rotation = arrowRotation,
                    onClick = {
                        infoVisible = false
                        actionsVisible = !actionsVisible
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        if (actionsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 透明点击层：只负责点击外侧收起，不改变背景亮度，避免展开时闪一下。
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null
                    ) { actionsVisible = false }
            )
        }
        // 常驻组合，让收起时也能完整播放 AnimatedVisibility 的退出动画。
        PhotoActionDrawer(
            items = actionItems,
            visible = actionsVisible,
            colors = colors,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 66.dp
                )
        )

        if (infoRendered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = infoProgress.value }
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null
                    ) { infoVisible = false }
            )
            PhotoInfoPopup(
                photo = photo,
                folderName = folderName,
                colors = colors,
                onClose = { infoVisible = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(
                        bottom = contentPadding.calculateBottomPadding() + 64.dp
                    )
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = infoProgress.value
                        alpha = progress
                        val scale = 0.86f + 0.14f * progress
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - progress) * 18.dp.toPx()
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null
                    ) { /* 吞掉卡片点击，避免穿透到遮罩 */ }
            )
        }
    }
}

// ── 照片详情：圆形提示 + 信息弹层 ─────────────────────────────────────────

private data class PhotoActionItem(
    val icon: ImageVector,
    val label: String,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 详情弹层的背景模糊。
 *
 * 始终保留一个 0.01px 的 RenderEffect，让图层从首帧就存在；
 * 半径按 0.25px 量化，避免逐帧重建 BlurEffect 导致闪烁/掉帧。
 */
private fun Modifier.infoBackdropBlur(progress: State<Float>): Modifier = graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val raw = 6.dp.toPx() * progress.value.coerceIn(0f, 1f)
        val radius = ((raw * 4f).roundToInt() / 4f).coerceAtLeast(0.01f)
        renderEffect = BlurEffect(radius, radius)
    }
}

private const val PhotoActionStaggerMillis = 67 // 60fps 下 4 帧

@Composable
private fun PhotoInfoHintButton(
    colors: AppColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(7.dp, CircleShape, clip = false)
            .elasticPress(CircleShape)
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "查看照片详情" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PhotoActionToggle(
    colors: AppColors,
    expanded: Boolean,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(9.dp, CircleShape, clip = false)
            .elasticPress(CircleShape)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (expanded) "收起照片操作" else "展开照片操作"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = null,
            tint = colors.accentOn,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun PhotoActionDrawer(
    items: List<PhotoActionItem>,
    visible: Boolean,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items.forEachIndexed { index, item ->
            // 离右下角最近的按钮先出来，逐个向上错开 4 帧。
            val delayMillis = (items.lastIndex - index) * PhotoActionStaggerMillis
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(140, delayMillis = delayMillis)) +
                    slideInVertically(
                        animationSpec = tween(
                            durationMillis = 220,
                            delayMillis = delayMillis,
                            easing = CubicBezierEasing(0.2f, 0.9f, 0.22f, 1f)
                        ),
                        initialOffsetY = { it / 2 }
                    ),
                exit = fadeOut(tween(90)) +
                    slideOutVertically(tween(120), targetOffsetY = { it / 3 })
            ) {
                PhotoActionPill(item = item, colors = colors)
            }
        }
    }
}

@Composable
private fun PhotoActionPill(item: PhotoActionItem, colors: AppColors) {
    val pillShape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .widthIn(min = 148.dp)
            .height(44.dp)
            .clip(pillShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, pillShape)
            .clickable(onClick = item.onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.danger) colors.danger else colors.accentText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = item.label,
            color = if (item.danger) colors.danger else colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun PhotoInfoPopup(
    photo: NotePhoto,
    folderName: String,
    colors: AppColors,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    NoteSurface(
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        elevated = false,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "照片详情",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceSunken)
                        .clickable(onClick = onClose)
                        .semantics { contentDescription = "收起照片详情" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = photo.note.ifBlank { "暂无备注，右上角可补充标题与备注" },
                color = if (photo.note.isBlank()) colors.textMuted else colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            DetailInfoRow("所属分类", folderName, colors)
            DetailInfoRow("拍摄时间", formatNoteDateTime(photo.capturedAt), colors)
            DetailInfoRow("图片尺寸", "${photo.width} × ${photo.height}", colors)
            DetailInfoRow("编辑状态", if (photo.isEdited) "已生成标注版本（原图保留）" else "原图", colors)
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String, colors: AppColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.width(62.dp))
        Text(
            value,
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 大图：双指缩放、放大后拖动，未放大时左右滑动切换。
 * 用一个小型 pointerInput 同时处理两种手势，避免 Pager 与 Zoom 互相抢事件。
 */
@Composable
private fun ZoomableNoteImage(
    bitmap: android.graphics.Bitmap?,
    contentDescription: String,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    if (bitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, null, tint = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.size(34.dp))
        }
        return
    }
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var boxSize by remember(bitmap) { mutableStateOf(IntSize.Zero) }
    var dragAccumulator by remember(bitmap) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(bitmap) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(bitmap) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    dragAccumulator = 0f
                    var handledZoom = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            handledZoom = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val target = (scale * zoom).coerceIn(1f, 4f)
                            scale = target
                            if (target <= 1.001f) {
                                offset = Offset.Zero
                            } else {
                                offset = clampOffset(offset + pan, target, boxSize)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1) {
                            val change = pressed.first()
                            if (scale > 1.01f) {
                                handledZoom = true
                                offset = clampOffset(offset + change.positionChange(), scale, boxSize)
                                change.consume()
                            } else {
                                dragAccumulator += change.positionChange().x
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (!handledZoom && scale <= 1.01f && abs(dragAccumulator) > 90f) {
                        if (dragAccumulator < 0f) onSwipeLeft() else onSwipeRight()
                    }
                }
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

private fun clampOffset(offset: Offset, scale: Float, size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return offset
    val maxX = max(0f, size.width * (scale - 1f) / 2f)
    val maxY = max(0f, size.height * (scale - 1f) / 2f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}
