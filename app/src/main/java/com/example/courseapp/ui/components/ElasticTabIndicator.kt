package com.example.courseapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 底栏 Tab 指示器的“液态拉伸”参数。
 *
 * 一条指示器由左右两条边沿各自驱动：
 * - 前沿 0ms 就起步，按 cubic-bezier(.2, .9, .22, 1) 先一步扑向目标，190ms 到位；
 * - 后沿延后 6 帧（100ms @60Hz）才起步，前段故意压慢，宽度因此被拉开，
 *   后段加速追赶，末端再多追 5% 后弹回，形成一次轻微超调；整个过程 280ms 收完。
 *
 * 指示器用 left + width 驱动，不碰 scaleX：横向缩放会把圆角压扁。
 */
internal object ElasticTabMotion {
    /** 前沿从起步到抵达目标的时长。 */
    const val LeadMillis = 190

    /** 后沿延迟：6 帧 @60Hz。 */
    const val TrailDelayMillis = 100

    /** 后沿追赶时长；后沿总在 280ms 收完。 */
    const val TrailMillis = 180

    /** 从前沿起步到后沿收完的总时长。 */
    const val TotalMillis = TrailDelayMillis + TrailMillis

    /** 前沿曲线：起步极快，视觉上“先冲出去”。 */
    val LeadCurve: Easing = CubicBezierEasing(0.2f, 0.9f, 0.22f, 1f)

    /** 后沿基础曲线：慢起步、快收尾，宽度才会在中段被拉开。 */
    private val TrailBaseCurve: Easing = CubicBezierEasing(0.55f, 0.02f, 0.25f, 1f)

    /** 超调从后沿行程的 62% 处开始，到终点时正好回到 0。 */
    private const val OvershootStart = 0.62f

    /** 后沿最多越过目标 5% 的行程（相邻 Tab 约 4.6dp）。 */
    private const val OvershootAmount = 0.05f

    /**
     * 后沿曲线：在基础追赶曲线上叠一个 sin² 单峰。
     * 峰值在 81% 处，末端归零，所以只在收尾时出现一次轻微超调。
     */
    val TrailCurve: Easing = Easing { raw ->
        val t = raw.coerceIn(0f, 1f)
        val u = ((t - OvershootStart) / (1f - OvershootStart)).coerceIn(0f, 1f)
        val bump = sin(PI * u).pow(2).toFloat()
        TrailBaseCurve.transform(t) + OvershootAmount * bump
    }

    /** 测试/文档用：前沿在 [elapsedMillis] 时刻的行程比例（0..1）。 */
    fun leadProgressAt(elapsedMillis: Float): Float =
        LeadCurve.transform((elapsedMillis / LeadMillis).coerceIn(0f, 1f))

    /** 测试/文档用：后沿在 [elapsedMillis] 时刻的行程比例（末端可短暂大于 1）。 */
    fun trailProgressAt(elapsedMillis: Float): Float {
        val t = ((elapsedMillis - TrailDelayMillis) / TrailMillis).coerceIn(0f, 1f)
        return TrailCurve.transform(t)
    }
}

/** 指示器横线尺寸：短横线，拉伸时宽度会明显翻倍。 */
private val TabLineWidth = 30.dp
private val TabLineHeight = 3.dp

/**
 * 底栏液态拉伸指示器（按钮底部横线）。
 *
 * 左侧位置与宽度分别由两条边沿的动画值驱动：
 * - 前沿（运动方向上的那条边）先走，后沿（被甩在后面的那条边）晚 6 帧追；
 * - 两条边之间距离就是宽度，因此中途会自然拉长到 2 倍以上，再随着后沿追上来收回；
 * - 后沿带一次轻微超调，收尾时横线会比目标宽度稍窄一点再弹回。
 *
 * 横线本身保持固定的 3dp 高度和圆形端头，只用 left/width 变形，
 * 不用 scaleX：横向缩放会把圆角和线宽一起压扁。
 *
 * 位移放在 offset 的 lambda 里、宽度放在 layout 的 measure 里读取动画值，
 * 每一帧只失效放置/布局，不触发重组。
 *
 * @param selectedIndex 当前选中的 Tab；-1 表示当前页面不属于底栏（导入/课程管理页），此时不绘制。
 * @param tabBounds 每个 Tab 按钮相对底栏的矩形（px）。中间加号与未知项用 [Rect.Zero]。
 */
@Composable
internal fun ElasticTabIndicator(
    selectedIndex: Int,
    tabBounds: List<Rect>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val tab = tabBounds.getOrNull(selectedIndex) ?: return
    if (tab.width <= 0f || tab.height <= 0f) return

    val density = LocalDensity.current
    val lineWidthPx = with(density) { TabLineWidth.toPx() }
    val lineHeightPx = with(density) { TabLineHeight.toPx() }

    // 横线贴在按钮底边、水平居中；左右沿的行程都基于这个矩形计算。
    fun lineRect(bounds: Rect) = Rect(
        left = bounds.center.x - lineWidthPx / 2f,
        top = bounds.bottom - lineHeightPx,
        right = bounds.center.x + lineWidthPx / 2f,
        bottom = bounds.bottom
    )

    val initial = lineRect(tab)

    // 两条边沿各自一条 Animatable：中断时从当前可见位置续接，不会跳回起点。
    val leftEdge = remember { Animatable(initial.left) }
    val rightEdge = remember { Animatable(initial.right) }
    var lastIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        val nextTab = tabBounds.getOrNull(selectedIndex) ?: return@LaunchedEffect
        if (selectedIndex == lastIndex) return@LaunchedEffect
        lastIndex = selectedIndex
        val next = lineRect(nextTab)

        // 以“眼睛看到的位置”判断方向：快速连点时也能正确换向。
        val visualCenter = (leftEdge.value + rightEdge.value) / 2f
        val toRight = next.center.x > visualCenter

        val lead = if (toRight) rightEdge else leftEdge
        val trail = if (toRight) leftEdge else rightEdge
        val leadTarget = if (toRight) next.right else next.left
        val trailTarget = if (toRight) next.left else next.right

        coroutineScope {
            // 前沿立刻出发。
            launch {
                lead.animateTo(
                    targetValue = leadTarget,
                    animationSpec = tween(
                        durationMillis = ElasticTabMotion.LeadMillis,
                        easing = ElasticTabMotion.LeadCurve
                    )
                )
            }
            // 后沿晚 6 帧起步，慢起快收，末端带一次轻微超调。
            launch {
                trail.animateTo(
                    targetValue = trailTarget,
                    animationSpec = tween(
                        durationMillis = ElasticTabMotion.TrailMillis,
                        delayMillis = ElasticTabMotion.TrailDelayMillis,
                        easing = ElasticTabMotion.TrailCurve
                    )
                )
            }
        }
    }

    val heightPx = lineHeightPx.roundToInt()
    val topPx = initial.top

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = leftEdge.value.roundToInt(),
                    y = topPx.roundToInt()
                )
            }
            .layout { measurable, _ ->
                val widthPx = (rightEdge.value - leftEdge.value).roundToInt().coerceAtLeast(0)
                if (widthPx <= 0) {
                    layout(0, 0) {}
                } else {
                    val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
                    layout(widthPx, heightPx) { placeable.place(0, 0) }
                }
            }
            .drawBehind {
                if (size.width <= 1f) return@drawBehind
                // 两端保持半圆；宽度被拉伸时线宽不变，圆角不会被压扁。
                val radius = size.height / 2f
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
    )
}
