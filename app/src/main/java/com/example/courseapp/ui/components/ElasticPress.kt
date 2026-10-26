package com.example.courseapp.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.courseapp.ui.theme.ScheduleMotion
import kotlinx.coroutines.launch

/**
 * 全局按压触感参数。
 *
 * 按下与释放用两条不同的弹簧曲线：
 * - 按下：高阻尼 + 高刚度，手指落下去就立刻压到 0.96，几乎看不到抖动；
 * - 释放：欠阻尼弹簧，回到 1.0 时轻微超调（约 1.2%），形成“弹回来”的物理回弹。
 */
object ElasticPress {
    /**
     * 按压动画的收敛阈值必须远小于 0.01：
     * Compose 对 Float 的默认 visibilityThreshold 是 0.01，用在 scale 上会停在 0.99，
     * 按钮看起来会一直小一号。
     */
    private const val ScaleVisibilityThreshold = 0.0005f

    /** 按下：快速压缩，几乎不过冲。 */
    val PressSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 1400f,
        visibilityThreshold = ScaleVisibilityThreshold
    )

    /** 释放：欠阻尼弹簧，回弹时轻微超调。 */
    val ReleaseSpring = spring<Float>(
        dampingRatio = 0.34f,
        stiffness = 420f,
        visibilityThreshold = ScaleVisibilityThreshold
    )
}

/**
 * 给任意可按控件加“弹性微缩 + 深度内阴影 + 触觉反馈”。
 *
 * 用法：把本 Modifier 放在尺寸/裁剪/背景之前，shape 传控件最终的外观形状：
 * ```
 * Box(
 *     Modifier
 *         .fillMaxWidth()
 *         .elasticPress(RoundedCornerShape(12.dp))
 *         .clip(RoundedCornerShape(12.dp))
 *         .background(...)
 *         .clickable { ... }
 * )
 * ```
 *
 * 实现要点：
 * - 指针监听放在最外层，不消费任何事件，因此不会抢走子级 clickable / 长按 / 滑动手势；
 * - 缩放和内阴影都放在 graphicsLayer / draw 阶段读取动画值，按压过程不触发重组；
 * - 内阴影只在按下时出现，透明度跟随压缩进度，松开后随回弹一起消失。
 */
@Composable
fun Modifier.elasticPress(
    shape: Shape,
    enabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    pressedScale: Float = ScheduleMotion.PressedScale
): Modifier {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val pressRange = (1f - pressedScale).coerceAtLeast(0.01f)

    // 控件中途被禁用时，确保从按压态弹回原位。
    LaunchedEffect(enabled) {
        if (!enabled) {
            scale.animateTo(1f, ElasticPress.ReleaseSpring)
        }
    }

    return this
        .pointerInput(enabled, hapticsEnabled, pressedScale) {
            if (!enabled) return@pointerInput
            try {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (hapticsEnabled) {
                        // 按键级别的轻触反馈，不忽略用户全局触感开关。
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    scope.launch {
                        scale.animateTo(pressedScale, ElasticPress.PressSpring)
                    }
                    // 子级 clickable 消费事件或滑动取消时都会返回，统一视为松手。
                    waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(1f, ElasticPress.ReleaseSpring)
                    }
                }
            } finally {
                // 指针监听被重启（例如 enabled 变化）或页面离开时，避免卡在压缩态。
                scope.launch {
                    scale.animateTo(1f, ElasticPress.ReleaseSpring)
                }
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val shapePath = Path().apply { addOutline(outline) }
            onDrawWithContent {
                drawContent()
                if (enabled) {
                    val depth = ((1f - scale.value) / pressRange).coerceIn(0f, 1f)
                    if (depth > 0.01f) {
                        drawPressedDepth(shapePath, depth)
                    }
                }
            }
        }
}

/**
 * 按压深度内阴影：
 * 1. 外圈用模糊的 DstOut 挖洞，留下贴边的一圈暗部（真实内阴影）；
 * 2. 顶部纵向压深 + 底部内侧高光，让控件看起来被按进面板里。
 *
 * API 28 以下硬件绘制不支持 BlurMaskFilter，直接用内描边兜底。
 */
private fun DrawScope.drawPressedDepth(shapePath: Path, depth: Float) {
    // 小控件（色块、图标按钮）用更小的模糊半径，避免内阴影糊成一团。
    val minSide = size.minDimension
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val blurPx = (minSide * 0.16f).coerceIn(2.dp.toPx(), 8.dp.toPx())
        val offsetY = (minSide * 0.06f).coerceIn(1.dp.toPx(), 2.5.dp.toPx())
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(shapePath)
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            canvas.drawRect(
                Rect(Offset.Zero, size),
                Paint().apply { color = Color.Black.copy(alpha = 0.46f * depth) }
            )
            val hole = Paint().apply {
                color = Color.Black
                blendMode = BlendMode.DstOut
            }
            hole.asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = android.graphics.BlurMaskFilter(
                    blurPx,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            canvas.translate(0f, offsetY)
            canvas.drawPath(shapePath, hole)
            canvas.restore()
            canvas.restore()
        }
    }

    clipPath(shapePath) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.20f * depth),
                0.45f to Color.Black.copy(alpha = 0.06f * depth),
                1f to Color.Transparent
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                0.70f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.10f * depth)
            )
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            drawPath(
                path = shapePath,
                color = Color.Black.copy(alpha = 0.24f * depth),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }
    }
}
