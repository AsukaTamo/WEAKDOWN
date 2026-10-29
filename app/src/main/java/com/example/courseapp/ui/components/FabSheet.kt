package com.example.courseapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.ContentPalette
import com.example.courseapp.ui.theme.contentPalette
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import kotlin.math.min
import kotlin.math.roundToInt

/** 加号面板里的三个入口；顺序与产品定义一致。 */
enum class FabSheetAction {
    AllCourses,
    AddCourse,
    Import
}

/**
 * FAB → Sheet 的形变参数。
 *
 * 容器用同一条 cubic-bezier(.2, .9, .22, 1) 走完 480ms：开头冲得快、末段收得稳，
 * 展开和收起共用同一条曲线（收起只是把进度反向走）。容器落定后，三个选项
 * 每隔 4 帧（66ms @60Hz）交错淡入。
 */
internal object FabSheetMotion {
    const val MorphMillis = 480
    const val StaggerMillis = 66
    const val OptionFadeMillis = 200
    const val OptionFadeOutMillis = 110

    val Curve = CubicBezierEasing(0.2f, 0.9f, 0.22f, 1f)
}

private val FabSheetCorner = 28.dp
private val FabSheetRowHeight = 58.dp
private val FabSheetPadding = 10.dp

/** 面板与底栏之间留出的呼吸缝，保证底栏完整露出。 */
private val FabSheetBarGap = 10.dp

/**
 * 底部加号按钮 → 操作面板的流体形变层。
 *
 * 几何只用一个容器：起点是加号按钮在根布局中的真实矩形（圆形），终点是贴在底栏上、
 * 与底栏同宽同底的圆角卡片。位置 / 宽 / 高 / 圆角由同一个 [progress] 驱动，
 * 所以看起来是“这颗加号自己长成了面板”，而不是另开一个弹层。
 *
 * 面板内容等容器落定后才出现：每个选项按 4 帧的间隔交错淡入；关闭时选项先快速淡出，
 * 容器再沿同一条曲线缩回加号原位。
 *
 * 玻璃与遮罩沿用现有面板的父层结构：遮罩是面板的父层，子控件的点击优先于遮罩，
 * 避免 Vitro 的 sibling 命中共享把选项点击吞掉。
 */
@Composable
fun FabSheetOverlay(
    visible: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    anchor: Rect,
    barBounds: Rect,
    fabColor: Color,
    fabContentColor: Color,
    isDarkMode: Boolean,
    backdrop: GlassBackdrop,
    glass: Glass,
    onDismiss: () -> Unit,
    onAction: (FabSheetAction) -> Unit
) {
    val palette = contentPalette(isDarkMode)
    val density = LocalDensity.current

    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(visible, contentHeightPx) {
        if (visible && contentHeightPx > 0f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FabSheetMotion.MorphMillis,
                    easing = FabSheetMotion.Curve
                )
            )
            // 容器落定之后再放选项，淡入节奏才不会和形变打架。
            revealed = true
        } else if (!visible && progress.value > 0.001f) {
            revealed = false
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = FabSheetMotion.MorphMillis,
                    easing = FabSheetMotion.Curve
                )
            )
        }
    }

    BackHandler(enabled = visible) { onDismiss() }

    val p = progress.value
    if (visible || p > 0.001f) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            val fallbackSizePx = with(density) { 44.dp.toPx() }
            val fallbackMarginPx = with(density) { 16.dp.toPx() }

            val start = if (anchor.width > 1f && anchor.height > 1f) {
                anchor
            } else {
                Rect(
                    left = screenWidthPx / 2f - fallbackSizePx / 2f,
                    top = screenHeightPx - with(density) { 120.dp.toPx() } - fallbackSizePx,
                    right = screenWidthPx / 2f + fallbackSizePx / 2f,
                    bottom = screenHeightPx - with(density) { 120.dp.toPx() }
                )
            }
            val bar = if (barBounds.width > 1f) {
                barBounds
            } else {
                Rect(
                    left = fallbackMarginPx,
                    top = screenHeightPx - with(density) { 96.dp.toPx() },
                    right = screenWidthPx - fallbackMarginPx,
                    bottom = screenHeightPx - with(density) { 24.dp.toPx() }
                )
            }

            val panelWidthPx = bar.width
            val panelHeightPx = if (contentHeightPx > 0f) {
                contentHeightPx
            } else {
                with(density) { 220.dp.toPx() }
            }
            // 面板整体停在底栏上方，底栏保持完整可见（也保持可点）。
            val barGapPx = with(density) { FabSheetBarGap.toPx() }
            val end = Rect(
                left = bar.left,
                top = bar.top - barGapPx - panelHeightPx,
                right = bar.right,
                bottom = bar.top - barGapPx
            )

            // 颜色/透明度不能跟着越界外推；几何本身用 p 原值。
            val colorFraction = p.coerceIn(0f, 1f)
            val left = start.left + (end.left - start.left) * p
            val top = start.top + (end.top - start.top) * p
            val width = start.width + (end.width - start.width) * p
            val height = start.height + (end.height - start.height) * p

            val startCornerPx = min(start.width, start.height) / 2f
            val endCornerPx = with(density) { FabSheetCorner.toPx() }
            val cornerPx = startCornerPx + (endCornerPx - startCornerPx) * p
            val cornerDp = with(density) { cornerPx.toDp() }
            val widthDp = with(density) { width.toDp() }
            val heightDp = with(density) { height.toDp() }
            val panelWidthDp = with(density) { panelWidthPx.toDp() }
            val panelHeightDp = with(density) { panelHeightPx.toDp() }
            val shape = RoundedCornerShape(cornerDp)

            // 起点是实心强调色圆，终点是玻璃卡片；中间由同一进度插值。
            val panelBackground = lerp(
                fabColor,
                palette.surface.copy(alpha = 0.90f),
                colorFraction
            )
            // 加号本身在形变刚开始时就退场；面板选项要等容器落定后单独淡入。
            val plusAlpha = (1f - colorFraction / 0.18f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 遮罩与点击区只到玻璃底栏的上沿：底栏完整露出，并且仍然可以切换页面。
                    // 底栏占用的是屏幕底部 screenHeight - bar.top 这一段，所以底部 padding 取它。
                    .padding(
                        bottom = with(density) {
                            (screenHeightPx - bar.top).coerceAtLeast(0f).toDp()
                        }
                    )
                    .background(Color.Black.copy(alpha = 0.32f * colorFraction))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            ) {
                // 首帧离屏测量内容自然高度；拿到后再开始长，避免先跳到兜底高度。
                if (contentHeightPx <= 0f) {
                    Box(
                        modifier = Modifier
                            .width(panelWidthDp)
                            .offset(y = 4000.dp)
                    ) {
                        FabSheetContent(
                            palette = palette,
                            revealed = false,
                            onAction = {},
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
                        .background(panelBackground)
                        .border(
                            width = 1.dp,
                            color = palette.surfaceBorder.copy(alpha = colorFraction),
                            shape = shape
                        )
                ) {
                    // 加号：始终居中在当前容器里，随容器长大快速淡出。
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp)
                            .graphicsLayer { alpha = plusAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = fabContentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // 面板内容按最终宽高排版，形变期间只裁切、不重排。
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .requiredSize(panelWidthDp, panelHeightDp)
                    ) {
                        FabSheetContent(
                            palette = palette,
                            revealed = revealed,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FabSheetContent(
    palette: ContentPalette,
    revealed: Boolean,
    onAction: (FabSheetAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FabSheetPadding)
    ) {
        FabSheetOption(
            index = 0,
            revealed = revealed,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "本学期课程",
            subtitle = "查看和批量管理本学期全部课程",
            palette = palette,
            onClick = { onAction(FabSheetAction.AllCourses) }
        )
        OptionDivider(palette = palette, revealed = revealed, index = 1)
        FabSheetOption(
            index = 1,
            revealed = revealed,
            icon = Icons.Default.Add,
            title = "手动添加",
            subtitle = "填写课程信息，保存到当前学期",
            palette = palette,
            onClick = { onAction(FabSheetAction.AddCourse) }
        )
        OptionDivider(palette = palette, revealed = revealed, index = 2)
        FabSheetOption(
            index = 2,
            revealed = revealed,
            icon = Icons.Default.CloudDownload,
            title = "从教务系统导入",
            subtitle = "打开教务页面，自动解析整张课表",
            palette = palette,
            onClick = { onAction(FabSheetAction.Import) }
        )
    }
}

@Composable
private fun OptionDivider(
    palette: ContentPalette,
    revealed: Boolean,
    index: Int
) {
    // 分隔线和它下面那一项同步出现，避免容器还在长大时先露出两条横线。
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (revealed) {
            tween(
                durationMillis = FabSheetMotion.OptionFadeMillis,
                delayMillis = index * FabSheetMotion.StaggerMillis
            )
        } else {
            tween(durationMillis = FabSheetMotion.OptionFadeOutMillis)
        },
        label = "fabSheetDividerAlpha$index"
    )
    HorizontalDivider(
        modifier = Modifier
            .padding(start = 66.dp, end = 18.dp)
            .graphicsLayer { this.alpha = alpha },
        thickness = 0.7.dp,
        color = palette.divider
    )
}

@Composable
private fun FabSheetOption(
    index: Int,
    revealed: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: ContentPalette,
    onClick: () -> Unit
) {
    // 容器落定后每项延迟 4 帧出现；收起时不排队，直接快速淡出。
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (revealed) {
            tween(
                durationMillis = FabSheetMotion.OptionFadeMillis,
                delayMillis = index * FabSheetMotion.StaggerMillis
            )
        } else {
            tween(durationMillis = FabSheetMotion.OptionFadeOutMillis)
        },
        label = "fabSheetOptionAlpha$index"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FabSheetRowHeight)
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = revealed, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.title,
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.muted.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}
