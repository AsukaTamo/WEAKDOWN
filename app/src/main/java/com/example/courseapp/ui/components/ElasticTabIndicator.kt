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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
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
 * 指示器用 left + width 驱动，不碰 scaleX：横向缩放会把圆角和液体轮廓一起压扁。
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

/**
 * 底栏液态拉伸指示器。
 *
 * 左侧位置与宽度分别由两条边沿的动画值驱动：
 * - 前沿（运动方向上的那条边）先走，后沿（被甩在后面的那条边）晚 6 帧追；
 * - 两条边之间距离就是宽度，因此中途会自然拉长到 2 倍以上，再随着后沿追上来收回；
 * - 后沿带一次轻微超调，收尾时指示器会比目标宽度稍窄一点再弹回。
 *
 * 轮廓不是固定的圆角矩形：宽度被拉开时，前沿一侧鼓成圆钝的“水滴头”，
 * 后沿一侧收细成拖尾，上下边用三次贝塞尔连接；宽度回到目标时形变归零，
 * 恢复成标准胶囊。这样看起来是一团液体被拉出去，而不是一块圆角矩形平移。
 *
 * 位移放在 offset 的 lambda 里、宽度放在 layout 的 measure 里读取动画值，
 * 每一帧只失效放置/布局，不触发重组。
 *
 * @param selectedIndex 当前选中的 Tab；-1 表示当前页面不属于底栏（如导入页），此时不绘制。
 * @param tabBounds 每个 Tab 胶囊相对本组件父容器的矩形（px）。未知项用 [Rect.Zero]。
 */
@Composable
internal fun ElasticTabIndicator(
    selectedIndex: Int,
    tabBounds: List<Rect>,
    color: Color,
    modifier: Modifier = Modifier,
    sheenColor: Color = Color.White
) {
    val target = tabBounds.getOrNull(selectedIndex) ?: return
    if (target.width <= 0f || target.height <= 0f) return

    // 两条边沿各自一条 Animatable：中断时从当前可见位置续接，不会跳回起点。
    val leftEdge = remember { Animatable(target.left) }
    val rightEdge = remember { Animatable(target.right) }
    var lastIndex by remember { mutableIntStateOf(selectedIndex) }
    // +1 = 向右（头在右），-1 = 向左（头在左）。只影响轮廓朝向，不改位置。
    var direction by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(selectedIndex) {
        val next = tabBounds.getOrNull(selectedIndex) ?: return@LaunchedEffect
        if (selectedIndex == lastIndex) return@LaunchedEffect
        lastIndex = selectedIndex

        // 以“眼睛看到的位置”判断方向：快速连点时也能正确换向。
        val visualCenter = (leftEdge.value + rightEdge.value) / 2f
        val toRight = next.center.x > visualCenter
        direction = if (toRight) 1f else -1f

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

    val heightPx = target.height.roundToInt()
    val topPx = target.top

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
                val widthPx = size.width
                val height = size.height
                if (widthPx <= 1f || height <= 1f) return@drawBehind

                // 拉伸量决定液体形变的幅度：宽度回到目标宽度时，形变归零 → 标准胶囊。
                val fluid = ((widthPx - target.width) / (height * 1.15f)).coerceIn(0f, 1f)
                val path = buildFluidTabPath(widthPx, height, fluid)

                withTransform({
                    // 向左移动时把轮廓水平镜像，水滴头始终朝着运动方向。
                    if (direction < 0f) {
                        translate(widthPx, 0f)
                        scale(-1f, 1f, pivot = Offset.Zero)
                    }
                }) {
                    drawPath(path, color)
                    clipPath(path) {
                        // 顶部一层薄高光：拉伸时仍能看出液面的体积。
                        val sheenTop = -height * 0.16f
                        val sheenHeight = height * 1.22f
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to sheenColor.copy(alpha = 0.24f),
                                0.5f to sheenColor.copy(alpha = 0.03f),
                                1f to sheenColor.copy(alpha = 0f),
                                startY = sheenTop,
                                endY = sheenTop + sheenHeight
                            ),
                            topLeft = Offset(0f, sheenTop),
                            size = Size(widthPx, sheenHeight)
                        )
                        // 湿润的“头部”高光：只在被拉长时出现，强化液体冲出去的方向感。
                        if (fluid > 0.01f) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        sheenColor.copy(alpha = 0.18f * fluid),
                                        sheenColor.copy(alpha = 0f)
                                    ),
                                    center = Offset(widthPx - height * 0.32f, height / 2f),
                                    radius = height * 0.85f
                                )
                            )
                        }
                    }
                }
            }
    )
}

private const val LiquidCircleK = 0.5522848f

/**
 * 液态胶囊轮廓：头部（右侧）比基础高度鼓 7%，尾部收细 38%，中间用三次贝塞尔过渡。
 * 尾部还会从圆头逐渐收成更尖的拖尾，避免收细后仍像一个圆瓣。
 * `fluid = 0` 时头尾等高、端头为正圆，退化成标准胶囊；`fluid = 1` 时是最夸张的水滴形。
 */
private fun buildFluidTabPath(width: Float, height: Float, fluid: Float): Path {
    val cy = height / 2f
    val headRadius = height * (1f + 0.07f * fluid) / 2f
    val tailRadius = height * (1f - 0.62f * fluid) / 2f
    val headTop = cy - headRadius
    val headBottom = cy + headRadius
    val tailTop = cy - tailRadius
    val tailBottom = cy + tailRadius
    val span = (width - headRadius - tailRadius).coerceAtLeast(0f)
    val k = LiquidCircleK
    // 尾端控制点从正圆（k）逐渐收向尖角，0.8 是保留一点圆润的折中值。
    val tailSharp = 0.8f * fluid
    val kx = k + (0.16f - k) * tailSharp
    val ky = k + (0.38f - k) * tailSharp

    return Path().apply {
        moveTo(tailRadius, tailTop)
        // 上边：尾 → 头
        cubicTo(
            tailRadius + span * 0.38f, tailTop,
            tailRadius + span * 0.76f, headTop,
            width - headRadius, headTop
        )
        // 水滴头：右半圆
        cubicTo(
            width - headRadius + k * headRadius, headTop,
            width, cy - k * headRadius,
            width, cy
        )
        cubicTo(
            width, cy + k * headRadius,
            width - headRadius + k * headRadius, headBottom,
            width - headRadius, headBottom
        )
        // 下边：头 → 尾
        cubicTo(
            tailRadius + span * 0.76f, headBottom,
            tailRadius + span * 0.38f, tailBottom,
            tailRadius, tailBottom
        )
        // 拖尾：从圆头逐渐收尖
        cubicTo(
            tailRadius - kx * tailRadius, tailBottom,
            0f, cy + ky * tailRadius,
            0f, cy
        )
        cubicTo(
            0f, cy - ky * tailRadius,
            tailRadius - kx * tailRadius, tailTop,
            tailRadius, tailTop
        )
        close()
    }
}
