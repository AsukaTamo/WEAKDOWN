package com.example.courseapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.ContentPalette
import com.example.courseapp.ui.theme.contentPalette
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val FieldCorner = 10.dp
private val PanelCorner = 28.dp
private val PanelSideMargin = 16.dp
private val PanelGapAbove = 24.dp
private val PanelGapBelow = 10.dp
private val PanelMaxWidth = 560.dp

/**
 * 表单里的一个"点击后弹出选择框"的请求。
 *
 * [anchor] 是字段在根布局中的矩形；[onSelected] 直接写回 ViewModel。
 * 这里刻意不持有页面状态：面板关闭后字段值已经在 ViewModel 里更新，
 * 下一次打开会用新的 selectedIndex。
 */
data class FieldPickerRequest(
    val key: String,
    val label: String,
    val options: List<String>,
    val selectedIndex: Int,
    val columns: Int,
    val anchor: Rect,
    val onSelected: (Int) -> Unit
)

/**
 * 表单选择面板的流体形变层。
 *
 * 和顶栏周数胶囊同一套几何逻辑：起点是字段在根布局中的矩形，终点是宽度更大、
 * 圆角更圆的面板；位置 / 宽 / 高 / 圆角全部由同一个 [Animatable] 驱动。
 * 用高阻尼 spring（0.90 / 280）展开，收起稍快（0.94 / 340），让尺寸和圆角
 * 走同一条连续曲线，而不是"先长高再变圆"。
 *
 * 位置策略：优先放在字段上方；上方放不下时才翻到下方（"位置不够就下滑"）。
 * 面板和遮罩都在 bar 之上，因此会盖住顶/底栏；面板本身用 Vitro 采样 backdrop，
 * 和主页周面板保持同一材质。
 */
@Composable
fun FieldMorphOverlay(
    request: FieldPickerRequest?,
    backdrop: GlassBackdrop,
    glass: Glass,
    isDarkMode: Boolean,
    topSafePx: Float = 0f,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var current by remember { mutableStateOf<FieldPickerRequest?>(null) }
    var visible by remember { mutableStateOf(false) }
    var contentHeightPx by remember(current) { mutableFloatStateOf(0f) }

    LaunchedEffect(request) {
        if (request != null) {
            // 同一块字段重复点击时不重播；换字段则从 0 重新长出来。
            if (current?.key != request.key) {
                progress.snapTo(0f)
                contentHeightPx = 0f
            }
            current = request
            visible = true
        } else {
            visible = false
        }
    }

    LaunchedEffect(visible, contentHeightPx, current) {
        if (current == null) return@LaunchedEffect
        if (visible && contentHeightPx > 0f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.90f,
                    stiffness = 280f,
                    visibilityThreshold = 0.001f
                )
            )
        } else if (!visible && progress.value > 0.001f) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.94f,
                    stiffness = 340f,
                    visibilityThreshold = 0.001f
                )
            )
            current = null
        } else if (!visible) {
            current = null
        }
    }

    BackHandler(enabled = current != null) { onDismiss() }

    val spec = current ?: return
    val palette = contentPalette(isDarkMode)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val sidePx = with(density) { PanelSideMargin.toPx() }
        val panelWidthPx = min(
            screenWidthPx - sidePx * 2f,
            with(density) { PanelMaxWidth.toPx() }
        )
        val panelWidthDp = with(density) { panelWidthPx.toDp() }
        val fallbackHeightPx = with(density) { 300.dp.toPx() }
        val panelHeightPx = if (contentHeightPx > 0f) contentHeightPx else fallbackHeightPx

        val start = spec.anchor
        val gapAbovePx = with(density) { PanelGapAbove.toPx() }
        val gapBelowPx = with(density) { PanelGapBelow.toPx() }
        val statusTopPx = WindowInsets.statusBars.getTop(density) + with(density) { 8.dp.toPx() }
        // 页面内容上沿（顶栏 + 管理页 Tab 栏）；面板不要压住这两层，只允许覆盖表单本身。
        val safeTopPx = max(statusTopPx, topSafePx)
        val safeBottomPx = screenHeightPx -
            WindowInsets.navigationBars.getBottom(density) -
            with(density) { 8.dp.toPx() }

        val aboveTop = start.top - gapAbovePx - panelHeightPx
        val belowTop = start.bottom + gapBelowPx
        val canAbove = aboveTop >= safeTopPx
        val canBelow = belowTop + panelHeightPx <= safeBottomPx
        // 上方优先；上方不够就翻到下方（"位置不够就下滑"）。两者都不够时夹在安全区内。
        var targetTop = if (canAbove) aboveTop else belowTop
        targetTop = targetTop.coerceIn(
            safeTopPx,
            max(safeTopPx, safeBottomPx - panelHeightPx)
        )

        // 竖向列表（开始节次）会很长：优先占用字段上方的空间，把面板高度限制在
        // 上方可用高度内，列表本身可下滑；只有上方连一个可用高度都没有时才翻到下方。
        val aboveSpacePx = start.top - gapAbovePx - safeTopPx
        val belowSpacePx = safeBottomPx - start.bottom - gapBelowPx
        val minUsableVerticalPx = with(density) { 160.dp.toPx() }
        val preferAbove = aboveSpacePx >= minUsableVerticalPx || aboveSpacePx >= belowSpacePx
        val maxVerticalHeightPx = (if (preferAbove) aboveSpacePx else belowSpacePx)
            .coerceAtLeast(with(density) { 80.dp.toPx() })
        val maxContentHeight = if (spec.columns == 1) {
            with(density) { maxVerticalHeightPx.toDp() }
        } else {
            Dp.Unspecified
        }
        val targetLeft = (screenWidthPx - panelWidthPx) / 2f

        val p = progress.value
        val colorFraction = p.coerceIn(0f, 1f)
        val left = start.left + (targetLeft - start.left) * p
        val top = start.top + (targetTop - start.top) * p
        val width = start.width + (panelWidthPx - start.width) * p
        val height = start.height + (panelHeightPx - start.height) * p
        val startCornerPx = with(density) { FieldCorner.toPx() }
        val endCornerPx = with(density) { PanelCorner.toPx() }
        val cornerPx = startCornerPx + (endCornerPx - startCornerPx) * p
        val cornerDp = with(density) { cornerPx.toDp() }
        val widthDp = with(density) { width.toDp() }
        val heightDp = with(density) { height.toDp() }
        val shape = RoundedCornerShape(cornerDp)
        val startLabelAlpha = (1f - p / 0.28f).coerceIn(0f, 1f)
        val panelContentAlpha = ((p - 0.22f) / 0.78f).coerceIn(0f, 1f)
        val panelBackground = lerp(palette.inputSurface, palette.surface, colorFraction)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f * colorFraction))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            // 首帧离屏测量，拿到内容自然高度后再开始形变；否则面板会先长到兜底高度再跳。
            if (contentHeightPx <= 0f) {
                Box(
                    modifier = Modifier
                        .width(panelWidthDp)
                        .offset(y = 4000.dp)
                ) {
                    FieldPickerContent(
                        spec = spec,
                        palette = palette,
                        selectedIndex = spec.selectedIndex,
                        onSelect = {},
                        maxHeight = maxContentHeight,
                        modifier = Modifier.onSizeChanged {
                            contentHeightPx = it.height.toFloat()
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(widthDp, heightDp)
                    .glassEffect(backdrop, glass, shape)
                    .clip(shape)
                    .background(panelBackground.copy(alpha = 1f - 0.18f * colorFraction))
                    .border(
                        width = 1.dp,
                        color = palette.surfaceBorder.copy(alpha = (1f - colorFraction) * 0.9f),
                        shape = shape
                    )
            ) {
                // 起点：字段自己的值 + 下拉箭头，长出面板时快速淡出。
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(with(density) { start.height.toDp() })
                        .padding(horizontal = 14.dp)
                        .graphicsLayer { alpha = startLabelAlpha },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spec.options.getOrElse(spec.selectedIndex) { spec.options.firstOrNull().orEmpty() },
                        fontSize = 13.sp,
                        color = palette.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 终点：按最终宽度排版，形变过程中只裁切不重排。
                Box(
                    modifier = Modifier
                        .requiredWidth(panelWidthDp)
                        .graphicsLayer { alpha = panelContentAlpha }
                ) {
                    FieldPickerContent(
                        spec = spec,
                        palette = palette,
                        selectedIndex = spec.selectedIndex,
                        maxHeight = maxContentHeight,
                        onSelect = { index ->
                            spec.onSelected(index)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldPickerContent(
    spec: FieldPickerRequest,
    palette: ContentPalette,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    maxHeight: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier
) {
    if (spec.columns <= 1) {
        VerticalPickerContent(
            spec = spec,
            palette = palette,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            maxHeight = maxHeight,
            modifier = modifier
        )
    } else {
        GridPickerContent(
            spec = spec,
            palette = palette,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

/**
 * 开始节次的竖向列表。
 *
 * 12 节全部列出来会超过一屏，所以 [maxHeight] 取"字段上方/下方更大的一侧"：
 * 面板先落在空间更大的一侧，列表自身可下滑；这样既保持竖向排列，又不会
 * 因为 1~12 节太长而顶出屏幕。
 */
@Composable
private fun VerticalPickerContent(
    spec: FieldPickerRequest,
    palette: ContentPalette,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        // 打开时把当前选中的节次带进可视区域（前面留一项作为上下文）
        initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
    )
    val cappedModifier = if (!maxHeight.value.isNaN()) {
        Modifier.heightIn(max = maxHeight)
    } else {
        Modifier
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .then(cappedModifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text(
                    text = "选择${spec.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.title
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        itemsIndexed(spec.options) { index, option ->
            val selected = index == selectedIndex
            val rowShape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .elasticPress(rowShape)
                    .clip(rowShape)
                    .background(if (selected) palette.accent else palette.surfaceAlt)
                    .then(
                        if (selected) {
                            Modifier
                        } else {
                            Modifier.border(1.dp, palette.surfaceBorder, rowShape)
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) palette.onAccent else palette.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.onAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridPickerContent(
    spec: FieldPickerRequest,
    palette: ContentPalette,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = ceil(spec.options.size.toFloat() / spec.columns.toFloat()).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 16.dp)
    ) {
        Text(
            text = "选择${spec.label}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.title
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(spec.columns) { column ->
                        val index = row * spec.columns + column
                        if (index < spec.options.size) {
                            val selected = index == selectedIndex
                            val chipShape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .elasticPress(chipShape)
                                    .clip(chipShape)
                                    .background(if (selected) palette.accent else palette.surfaceAlt)
                                    .then(
                                        if (selected) {
                                            Modifier
                                        } else {
                                            Modifier.border(1.dp, palette.surfaceBorder, chipShape)
                                        }
                                    )
                                    .clickable { onSelect(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = spec.options[index],
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) palette.onAccent else palette.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
