package com.example.courseapp.ui.notes

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** 一次布局切换的 FLIP 计划；deltas 为空表示不需要动画。 */
private data class FlipPlan(
    val deltas: Map<String, FlipDelta> = emptyMap(),
    val animate: Boolean = false
)

/** 非状态的运动学暂存：在组合阶段同步读取，避免用 State 造成每帧重组。 */
private class FlipRuntime {
    var initialized = false
    var lastModeIsGrid = true
    var lastWidth = 0f
    var progress = 1f
    var deltas: Map<String, FlipDelta> = emptyMap()
}

/**
 * 统一的网格/列表容器：**同一个 Layout、同一组以稳定 id 为 key 的 item**。
 *
 * 没有切换两个 Lazy 容器，因此布局模式变化时 item 的组合节点不会重建：
 * - 两种模式的目标矩形由同一套算法在组合阶段算好，First/Last 精确、可预测；
 * - 每项用 [graphicsLayer] 做 Invert（平移 + 以左上角为 pivot 的缩放），
 *   再由同一个 [Animatable] 统一播放；
 * - 每项按 3 帧错开，路径带轻微弧度；
 * - 新计划在**组合阶段**就生成（progress 从 0 开始），不会先画一帧最终布局再倒回去；
 * - 中途切换时按上一轮的真实可视矩形重新衔接，不会叠加两次变换。
 *
 * 离屏项不参与虚拟化：课堂笔记单分类数量有限，这里选择“节点连续复用 + 图片只解码一次”，
 * 而不是为了省内存牺牲 FLIP 的空间连续性。
 */
@Composable
fun FlipGridList(
    items: List<String>,
    isGrid: Boolean,
    modifier: Modifier = Modifier,
    gridColumns: Int = 2,
    autoColumns: Boolean = true,
    gridAspect: Float = 0.92f,
    listRowHeight: Dp = 92.dp,
    horizontalGap: Dp = 11.dp,
    verticalGap: Dp = 12.dp,
    content: @Composable (id: String, isGrid: Boolean) -> Unit
) {
    if (items.isEmpty()) return
    val density = LocalDensity.current
    val reducedMotion = !ValueAnimator.areAnimatorsEnabled()
    val hGapPx = with(density) { horizontalGap.toPx() }
    val vGapPx = with(density) { verticalGap.toPx() }
    val rowHeightPx = with(density) { listRowHeight.toPx() }
    val arcBasePx = with(density) { FlipMotion.ArcBaseDp.dp.toPx() }
    val arcMaxPx = with(density) { FlipMotion.ArcMaxDp.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val columns = when {
            !autoColumns -> gridColumns
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        val gridBounds = remember(items, widthPx, columns, hGapPx, vGapPx, gridAspect) {
            buildGridBounds(items, widthPx, columns, hGapPx, vGapPx, gridAspect)
        }
        val listBounds = remember(items, widthPx, rowHeightPx, vGapPx) {
            buildListBounds(items, widthPx, rowHeightPx, vGapPx)
        }
        val targetBounds = if (isGrid) gridBounds else listBounds
        val otherBounds = if (isGrid) listBounds else gridBounds
        val totalMillis = FlipMotion.totalMillis(items.size)
        val indexById = remember(items) { items.withIndex().associate { (index, id) -> id to index } }

        val runtime = remember { FlipRuntime() }

        // 组合阶段就地生成计划：模式变更发生在第一次绘制之前，progress 从 0 开始。
        val plan = remember(isGrid, widthPx, items, reducedMotion, totalMillis) {
            val modeChanged = runtime.initialized &&
                isGrid != runtime.lastModeIsGrid &&
                widthPx == runtime.lastWidth
            val previousDeltas = runtime.deltas
            val previousProgress = runtime.progress
            runtime.initialized = true
            runtime.lastModeIsGrid = isGrid
            runtime.lastWidth = widthPx
            if (!modeChanged || reducedMotion) {
                runtime.deltas = emptyMap()
                FlipPlan(deltas = emptyMap(), animate = false)
            } else {
                val settled = previousProgress >= 0.999f || previousDeltas.isEmpty()
                // 上一轮的目标就是“另一种模式”；打断时用上一轮变换还原真实可视矩形。
                val firstBounds = if (settled) {
                    otherBounds
                } else {
                    otherBounds.mapValues { (id, bounds) ->
                        val delta = previousDeltas[id] ?: return@mapValues bounds
                        val index = indexById[id] ?: 0
                        val q = FlipMotion.easedProgress(previousProgress, index, totalMillis)
                        val arc = FlipMotion.arcMagnitude(delta.dx, arcBasePx, arcMaxPx)
                        FlipMotion.visualBounds(bounds, FlipMotion.transformAt(delta, q, arc, index))
                    }
                }
                val next = buildMap {
                    firstBounds.forEach { (id, first) ->
                        val last = targetBounds[id] ?: return@forEach
                        FlipMotion.deltaBetween(first, last)?.let { put(id, it) }
                    }
                }
                runtime.deltas = next
                FlipPlan(deltas = next, animate = next.isNotEmpty())
            }
        }

        val progress = remember(plan) {
            Animatable(if (plan.animate) 0f else 1f)
        }
        LaunchedEffect(plan, progress) {
            if (plan.animate) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = totalMillis, easing = LinearEasing)
                )
            }
        }
        // 用 snapshotFlow 跟踪真实进度，供下一次打断计算使用；不触发重组。
        LaunchedEffect(progress) {
            runtime.progress = progress.value
            snapshotFlow { progress.value }.collect { runtime.progress = it }
        }

        Layout(
            content = {
                items.forEachIndexed { index, id ->
                    key(id) {
                        val delta = plan.deltas[id]
                        Box(
                            modifier = Modifier
                                .zIndex(if (delta != null) 1f else 0f)
                                .graphicsLayer {
                                    if (delta != null) {
                                        val q = FlipMotion.easedProgress(progress.value, index, totalMillis)
                                        val arc = FlipMotion.arcMagnitude(delta.dx, arcBasePx, arcMaxPx)
                                        val transform = FlipMotion.transformAt(delta, q, arc, index)
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        translationX = transform.translationX
                                        translationY = transform.translationY
                                        scaleX = transform.scaleX
                                        scaleY = transform.scaleY
                                    }
                                },
                            contentAlignment = Alignment.TopStart
                        ) {
                            content(id, isGrid)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.mapIndexed { index, measurable ->
                val bounds = targetBounds[items[index]]
                if (bounds == null) {
                    measurable.measure(Constraints())
                } else {
                    measurable.measure(
                        Constraints.fixed(
                            width = bounds.width.roundToInt().coerceAtLeast(1),
                            height = bounds.height.roundToInt().coerceAtLeast(1)
                        )
                    )
                }
            }
            val totalHeight = (targetBounds.values.maxOfOrNull { it.bottom } ?: 0f)
                .roundToInt()
                .coerceAtLeast(0)
            layout(constraints.maxWidth, totalHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val bounds = targetBounds[items[index]] ?: return@forEachIndexed
                    placeable.place(bounds.left.roundToInt(), bounds.top.roundToInt())
                }
            }
        }
    }
}

/** 网格目标矩形：等宽单元格，高度 = 宽度 × [aspect]。 */
internal fun buildGridBounds(
    ids: List<String>,
    containerWidth: Float,
    columns: Int,
    horizontalGap: Float,
    verticalGap: Float,
    aspect: Float
): Map<String, FlipBounds> {
    if (ids.isEmpty() || containerWidth <= 0f) return emptyMap()
    val safeColumns = columns.coerceAtLeast(1)
    val cellWidth = ((containerWidth - horizontalGap * (safeColumns - 1)) / safeColumns)
        .coerceAtLeast(1f)
    val cellHeight = (cellWidth * aspect).coerceAtLeast(1f)
    return ids.withIndex().associate { (index, id) ->
        val column = index % safeColumns
        val row = index / safeColumns
        id to FlipBounds(
            left = column * (cellWidth + horizontalGap),
            top = row * (cellHeight + verticalGap),
            width = cellWidth,
            height = cellHeight
        )
    }
}

/** 列表目标矩形：整行宽、固定行高。 */
internal fun buildListBounds(
    ids: List<String>,
    containerWidth: Float,
    rowHeight: Float,
    verticalGap: Float
): Map<String, FlipBounds> = ids.withIndex().associate { (index, id) ->
    id to FlipBounds(
        left = 0f,
        top = index * (rowHeight + verticalGap),
        width = containerWidth.coerceAtLeast(1f),
        height = rowHeight.coerceAtLeast(1f)
    )
}
