package com.example.courseapp.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.contentPalette

/** 毛玻璃浮层的节拍：面板浮起与背景模糊共用 1s，内容从顶部开始晚 4 帧。 */
internal object FabModalMotion {
    const val OpenMillis = 1000
    const val CloseMillis = 280

    /** 4 帧 @60Hz ≈ 66ms，换算成 1s 进度里的比例。 */
    const val ContentDelayFraction = 4f / 60f

    val Curve = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
}

/** 浮层打开时背景页的最大模糊半径。 */
internal val FabModalPageBlur = 26.dp

private val FabModalCorner = 28.dp
private val FabModalWidthFraction = 0.92f
private val FabModalHeightFraction = 0.86f
private val FabModalMaxWidth = 560.dp
private val FabModalMaxHeight = 760.dp
private val FabModalFloatDistance = 28.dp

/**
 * 三个入口共用的毛玻璃浮层（Frosted Glass Sheet）。
 *
 * 打开时：
 * - 背景页 blur 0 → 26dp；
 * - 顶栏 / 底栏 / 加号面板快速隐去，避免它们的玻璃高光和橙色色块透过面板形成亮线；
 * - 面板从 0.94 放大到 1、向上浮 28dp，同时淡入；
 * - 面板内容（列表 / 表单 / 导入说明）从顶部向下填充，起步比面板晚 4 帧。
 * 整个过程 1s，曲线 cubic-bezier(.2, .7, .2, 1)。
 *
 * [content] 在浮层打开期间一直保持组合；关闭动画结束后才卸载。
 */
@Composable
fun FabActionModalHost(
    action: FabSheetAction?,
    progress: Animatable<Float, AnimationVector1D>,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    content: @Composable (FabSheetAction) -> Unit
) {
    var displayedAction by remember { mutableStateOf<FabSheetAction?>(null) }
    var mounted by remember { mutableStateOf(false) }

    LaunchedEffect(action) {
        if (action != null) {
            displayedAction = action
            mounted = true
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FabModalMotion.OpenMillis,
                    easing = FabModalMotion.Curve
                )
            )
        } else if (mounted) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = FabModalMotion.CloseMillis,
                    easing = FastOutLinearInEasing
                )
            )
            mounted = false
            displayedAction = null
        }
    }

    BackHandler(enabled = mounted) { onDismiss() }

    val shown = displayedAction ?: return
    val palette = contentPalette(isDarkMode)
    val shape = RoundedCornerShape(FabModalCorner)
    // 参考值是 rgba(255,255,255,42)，但 42% 会让底页课表格子的横线从面板里透出来。
    // 面板底色因此几乎做实色：只留 2%~4% 的透色让边缘的 26dp backdrop 模糊透一点，
    // 底页的横线不会再形成可见分界。
    val panelTint = if (isDarkMode) {
        Color(0xFF141419).copy(alpha = 0.98f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }
    val panelSheen = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDarkMode) 0.07f else 0.35f),
        0.45f to Color.Transparent,
        1f to Color.Black.copy(alpha = if (isDarkMode) 0.16f else 0.03f)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // 表单弹键盘时整张浮层跟着抬起来，不会被键盘压住。
            .imePadding()
            .drawBehind {
                drawRect(Color.Black.copy(alpha = 0.18f * progress.value))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        val cardWidth = minOf(maxWidth * FabModalWidthFraction, FabModalMaxWidth)
        val cardHeight = minOf(maxHeight * FabModalHeightFraction, FabModalMaxHeight)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(cardWidth, cardHeight)
                .graphicsLayer {
                    val p = progress.value
                    alpha = p
                    val scale = 0.94f + 0.06f * p
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - p) * FabModalFloatDistance.toPx()
                }
                // 毛玻璃底色直接铺在已经糊掉的背景页上：42% 白 + 顶部受光，不再叠第二层玻璃着色器。
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.24f)
                )
                .clip(shape)
                .background(panelTint, shape)
                .background(panelSheen, shape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isDarkMode) 0.16f else 0.45f),
                    shape = shape
                )
                // 吃掉卡片内部的空白点击，避免点在标题/行距上把整个浮层关掉；
                // 子控件（关闭、输入框、列表）仍然先命中。
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modalTitle(shown),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.title,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .elasticPress(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = palette.muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.7.dp,
                    color = palette.divider
                )
                // 内容从顶部按进度填充：面板先浮起来，列表 / 表单再从上往下出现。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .drawWithContent {
                            val p = progress.value
                            val reveal = ((p - FabModalMotion.ContentDelayFraction) /
                                (1f - FabModalMotion.ContentDelayFraction)).coerceIn(0f, 1f)
                            when {
                                reveal >= 1f -> drawContent()
                                reveal > 0f -> clipRect(
                                    left = 0f,
                                    top = 0f,
                                    right = size.width,
                                    bottom = size.height * reveal
                                ) {
                                    this@drawWithContent.drawContent()
                                }
                                // 面板已出现但内容还没轮到：保持完全透明，避免先闪出满屏内容。
                                else -> Unit
                            }
                        }
                ) {
                    content(shown)
                }
            }
        }
    }
}

private fun modalTitle(action: FabSheetAction): String = when (action) {
    FabSheetAction.AllCourses -> "本学期课程"
    FabSheetAction.AddCourse -> "添加课程"
    FabSheetAction.Import -> "教务系统导入"
}

/**
 * 浮层打开时把背景页从 0 糊到 26dp。
 *
 * 用 RenderEffect 直接作用在 backdrop 内容层上：页面本身和底部玻璃采样到的纹理都会一起变糊，
 * 而不是只盖一层半透明遮罩。Android 12 以下没有硬件模糊，直接跳过。
 *
 * [progress] 由调用方按档位派生（见 MainActivity）：每帧重建 RenderEffect 重糊整页太贵，
 * 只有跨过一档才更新一次模糊半径。
 */
internal fun Modifier.fabModalPageBlur(progress: State<Float>): Modifier =
    this.graphicsLayer {
        val p = progress.value
        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && p > 0.001f) {
            val radius = FabModalPageBlur.toPx() * p
            BlurEffect(radius, radius)
        } else {
            null
        }
    }
