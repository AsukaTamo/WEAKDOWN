package com.example.courseapp.ui.notes

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 一次 FLIP 需要的几何信息（像素，容器坐标）。 */
data class FlipBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

/** First → Last 的逆变换：先平移与缩放，再动画回单位变换。 */
data class FlipDelta(
    val dx: Float,
    val dy: Float,
    val scaleX: Float,
    val scaleY: Float
)

data class FlipTransform(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float
)

/**
 * 网格/列表 FLIP 的运动学。
 *
 * 时间轴：每项 620ms，三次贝塞尔 `(0.22, 1.0, 0.36, 1.0)`；
 * 相邻项错开 3 帧（60fps ≈ 50ms），最多错开 5 项，避免长列表尾部拖太久。
 * 空间路径：二次贝塞尔，控制点在位移中垂线方向偏移，形成轻微弧线。
 *
 * 这些函数保持纯函数，便于单元测试；UI 层只负责测量与读取动画进度。
 */
object FlipMotion {

    const val DurationMillis = 620

    /** 60fps 下 3 帧 ≈ 50ms。 */
    const val StaggerMillis = 50
    const val FrameMillis = 1000f / 60f

    /** 超过 5 个之后的项目不再继续顺延，保证整体动画不会无限变长。 */
    const val MaxStaggerStep = 5

    const val ArcBaseDp = 8f
    const val ArcMaxDp = 28f

    val Easing: Easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

    fun staggerMillis(index: Int): Int = index.coerceIn(0, MaxStaggerStep) * StaggerMillis

    fun totalMillis(itemCount: Int): Int =
        DurationMillis + if (itemCount <= 1) 0 else staggerMillis(itemCount - 1)

    /** 单项在整段时间轴上的原始进度（0..1，未缓动）。 */
    fun localProgress(rawProgress: Float, index: Int, totalMillis: Int): Float {
        if (totalMillis <= 0) return 1f
        val elapsed = rawProgress.coerceIn(0f, 1f) * totalMillis - staggerMillis(index)
        return (elapsed / DurationMillis).coerceIn(0f, 1f)
    }

    fun easedProgress(rawProgress: Float, index: Int, totalMillis: Int): Float =
        Easing.transform(localProgress(rawProgress, index, totalMillis))

    fun deltaBetween(first: FlipBounds, last: FlipBounds): FlipDelta? {
        if (last.width <= 0.01f || last.height <= 0.01f) return null
        return FlipDelta(
            dx = first.left - last.left,
            dy = first.top - last.top,
            scaleX = first.width / last.width,
            scaleY = first.height / last.height
        )
    }

    /** 弧线控制点偏移量：位移越大弧越明显，但有上限。 */
    fun arcMagnitude(dx: Float, basePx: Float, maxPx: Float): Float =
        min(maxPx, basePx + abs(dx) * 0.06f)

    /**
     * 在进度 [q] 处计算变换。
     *
     * 平移沿二次贝塞尔路径（两端分别是逆变换位移和 0），缩放线性回到 1。
     * pivot 固定为左上角，因此平移量就是 First / Last 左上角的差值。
     */
    fun transformAt(
        delta: FlipDelta,
        q: Float,
        arcPx: Float,
        index: Int
    ): FlipTransform {
        val t = q.coerceIn(0f, 1f)
        val length = kotlin.math.hypot(delta.dx.toDouble(), delta.dy.toDouble()).toFloat()
        val direction = if (index % 2 == 0) 1f else -1f
        val perpX: Float
        val perpY: Float
        if (length < 0.001f) {
            perpX = 0f
            perpY = -1f
        } else {
            perpX = -delta.dy / length
            perpY = delta.dx / length
        }
        val controlX = delta.dx * 0.5f + perpX * arcPx * direction
        val controlY = delta.dy * 0.5f + perpY * arcPx * direction
        val oneMinus = 1f - t
        val x = oneMinus * oneMinus * delta.dx + 2f * oneMinus * t * controlX
        val y = oneMinus * oneMinus * delta.dy + 2f * oneMinus * t * controlY
        return FlipTransform(
            translationX = x,
            translationY = y,
            scaleX = delta.scaleX + (1f - delta.scaleX) * t,
            scaleY = delta.scaleY + (1f - delta.scaleY) * t
        )
    }

    /** 动画被中途打断时，用当前变换把 Last 矩形还原成“屏幕上真实的 First 矩形”。 */
    fun visualBounds(last: FlipBounds, transform: FlipTransform): FlipBounds = FlipBounds(
        left = last.left + transform.translationX,
        top = last.top + transform.translationY,
        width = max(0.01f, last.width * transform.scaleX),
        height = max(0.01f, last.height * transform.scaleY)
    )
}
