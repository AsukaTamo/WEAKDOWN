package com.example.courseapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 给卡片加"跟随手指的 3D 透视倾斜 + 径向反射流光"。
 *
 * 和 [elasticPress] 分工不同：按压缩放仍由 elasticPress 负责，这里只做：
 * - 触摸位置映射成 ±maxTiltDegrees 的 rotationX / rotationY，卡片朝手指方向压下去；
 * - 手指处叠一团径向高光，模拟玻璃表面的反射；
 * - 松手（或滚动抢走手势）时用欠阻尼弹簧回正，带一次轻微过冲。
 *
 * 用法：放在尺寸之后、shadow / 裁剪之前，shape 传卡片最终外观形状：
 * ```
 * Modifier
 *     .fillMaxWidth()
 *     .height(122.dp)
 *     .tiltPress(RoundedCornerShape(24.dp))
 *     .shadow(...)
 * ```
 *
 * 实现要点：
 * - 旋转和流光都放在 graphicsLayer / draw 阶段读取动画值，手势过程不触发重组；
 * - 指针监听不消费事件，clickable / 竖向滚动仍然按原样工作；
 * - graphicsLayer 在外层，阴影会一起旋转；流光自己按 shape 裁剪，不依赖外层的 clip。
 */
@Composable
fun Modifier.tiltPress(
    shape: Shape,
    enabled: Boolean = true,
    maxTiltDegrees: Float = 8.5f
): Modifier {
    val scope = rememberCoroutineScope()
    val state = remember { TiltPressState() }

    // 控件被禁用或指针监听重启时，保证从倾斜态弹回原位。
    LaunchedEffect(enabled) {
        if (!enabled) state.settle(scope)
    }

    return this
        .pointerInput(enabled, maxTiltDegrees) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                state.press(scope, normalize(down.position))
                var moved = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        // 越过 touchSlop 后若事件被竖向滚动消费，就让卡片回正，
                        // 避免滚动时卡片还保持倾斜。
                        if (!moved &&
                            (change.position - down.position).getDistance() >
                            viewConfiguration.touchSlop
                        ) {
                            moved = true
                        }
                        if (moved && change.isConsumed) break

                        state.follow(scope, normalize(change.position))
                    }
                } finally {
                    state.release(scope)
                }
            }
        }
        .graphicsLayer {
            rotationX = -state.tilt.value.y * maxTiltDegrees
            rotationY = state.tilt.value.x * maxTiltDegrees
            // 默认 8x density 透视过强，卡片边缘会明显被拉斜；14x 保留立体感又不过分。
            cameraDistance = 14f * density
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val shapePath = Path().apply { addOutline(outline) }
            val broadRadius = size.maxDimension * 0.72f
            val coreRadius = size.maxDimension * 0.13f

            onDrawWithContent {
                drawContent()

                val glow = state.glow.value.coerceIn(0f, 1f)
                if (glow <= 0.005f) return@onDrawWithContent

                val center = Offset(
                    x = state.sheen.x * size.width,
                    y = state.sheen.y * size.height
                )
                clipPath(shapePath) {
                    // 大范围柔光负责"照亮"，小光斑负责"反光点"，两层都是 Screen，
                    // 叠在蓝色卡面上会提亮而不会盖住文字。
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.22f * glow),
                                0.18f to Color(0xFFDDF4FF).copy(alpha = 0.13f * glow),
                                0.52f to Color(0xFF8FD6FF).copy(alpha = 0.05f * glow),
                                1f to Color.Transparent
                            ),
                            center = center,
                            radius = broadRadius
                        ),
                        radius = broadRadius,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.28f * glow),
                                0.55f to Color(0xFFEAF9FF).copy(alpha = 0.08f * glow),
                                1f to Color.Transparent
                            ),
                            center = center,
                            radius = coreRadius
                        ),
                        radius = coreRadius,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                }
            }
        }
}

/** 把指针位置归一化到 [-1, 1]，中心为 0；越界时夹紧，避免手指滑出卡片后角度继续变大。 */
private fun PointerInputScope.normalize(position: Offset): Offset {
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    if (width <= 0f || height <= 0f) return Offset.Zero
    return Offset(
        x = ((position.x / width) * 2f - 1f).coerceIn(-1f, 1f),
        y = ((position.y / height) * 2f - 1f).coerceIn(-1f, 1f)
    )
}

/**
 * 一次按压的动画状态。
 *
 * [tilt] 用 Offset 同时表示 x / y 两个方向；按下时 `snapTo` 直接跟手，
 * 松手后 `animateTo(Offset.Zero)` 走欠阻尼弹簧，回到中心时会向反方向轻微过冲。
 */
@Stable
private class TiltPressState {
    val tilt = Animatable(Offset.Zero, Offset.VectorConverter)
    val glow = Animatable(0f)

    /** 流光的归一化中心；松手后保持在最后触摸点淡出，不跟着旋转动画滑回卡片中心。 */
    var sheen by mutableStateOf(Offset(0.5f, 0.5f))
        private set

    private var settleJob: Job? = null

    fun press(scope: CoroutineScope, normalized: Offset) {
        follow(scope, normalized)
        scope.launch { glow.animateTo(1f, PressGlowSpring) }
    }

    fun follow(scope: CoroutineScope, normalized: Offset) {
        sheen = Offset(normalized.x * 0.5f + 0.5f, normalized.y * 0.5f + 0.5f)
        settleJob?.cancel()
        settleJob = scope.launch { tilt.snapTo(normalized) }
    }

    fun release(scope: CoroutineScope) {
        settleJob?.cancel()
        settleJob = scope.launch { tilt.animateTo(Offset.Zero, ReturnSpring) }
        scope.launch { glow.animateTo(0f, ReleaseGlowSpring) }
    }

    fun settle(scope: CoroutineScope) {
        settleJob?.cancel()
        settleJob = scope.launch {
            tilt.snapTo(Offset.Zero)
            glow.snapTo(0f)
        }
    }
}

// 按下时流光快速亮起；松手后欠阻尼收掉，和卡片回弹的节奏一致。
private val PressGlowSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 1400f)
private val ReleaseGlowSpring = spring<Float>(dampingRatio = 0.34f, stiffness = 420f)

// 0.4 / 300 与仓库里玻璃回弹、按压释放同一条弹簧语言，回到中心时约 25% 过冲。
private val ReturnSpring = spring<Offset>(dampingRatio = 0.4f, stiffness = 300f)
