package com.example.courseapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.ui.theme.ContentPalette
import com.example.courseapp.ui.theme.contentPalette
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import kotlin.math.roundToInt

private const val WeekColumns = 5
private val CapsuleCorner = 16.dp
private val PanelCornerRadius = 28.dp
private val PanelGapBelowChip = 10.dp
private val ArrowTouchSize = 44.dp

/**
 * 教学周选择面板的流体形变层。
 *
 * 几何来源：
 * - 起点 = 顶栏「第 X 周」胶囊在根布局中的矩形（[anchor]）
 * - 终点 = 全宽面板，紧贴胶囊下方，高度等于内容的自然高度
 *
 * 位置、宽高、圆角、底色都由同一个 [progress] 插值；[progress] 由调用方持有并用高阻尼
 * spring 驱动，所以展开和收起走同一条流体曲线，胶囊同步淡出，视觉上是"胶囊长成面板"。
 */
@Composable
fun WeekMorphOverlay(
    visible: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    anchor: Rect,
    arrowAnchor: Rect,
    maxWeek: Int,
    currentWeek: Int,
    thisWeek: Int,
    chipColor: Color,
    chipContentColor: Color,
    topBarContentColor: Color,
    isDarkMode: Boolean,
    backdrop: GlassBackdrop,
    glass: Glass,
    onWeekSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val gapPx = with(density) { PanelGapBelowChip.toPx() }
    val endCornerPx = with(density) { PanelCornerRadius.toPx() }

    var selectedWeek by remember(currentWeek) { mutableIntStateOf(currentWeek) }
    // 面板自然高度：先用屏幕外的副本量一次，拿到后把面板长到这个高度
    var contentHeightPx by remember(maxWeek) { mutableFloatStateOf(0f) }

    LaunchedEffect(visible, contentHeightPx) {
        when {
            visible && contentHeightPx > 0f -> progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = 280f,
                    visibilityThreshold = 0.001f
                )
            )
            !visible && progress.value > 0f -> progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.94f,
                    stiffness = 340f,
                    visibilityThreshold = 0.001f
                )
            )
        }
    }

    val p = progress.value
    if (!visible && p <= 0.001f) return

    // 胶囊还没测量到时给一个兜底起点，避免从 (0,0) 长出来
    val start = if (anchor.width > 1f) {
        anchor
    } else {
        Rect(0f, density.density * 200f, density.density * 140f, density.density * 242f)
    }

    val targetTop = start.bottom + gapPx
    val targetHeight = if (contentHeightPx > 0f) contentHeightPx else with(density) { 360.dp.toPx() }

    val left = start.left + (0f - start.left) * p
    val top = start.top + (targetTop - start.top) * p
    val width = start.width + (screenWidthPx - start.width) * p
    val height = start.height + (targetHeight - start.height) * p
    // 起点圆角与胶囊一致（16dp），终点是面板圆角，整个形状始终是同一个圆角矩形
    val startCornerPx = with(density) { CapsuleCorner.toPx() }
    val corner = startCornerPx + (endCornerPx - startCornerPx) * p
    val cornerDp = with(density) { corner.toDp() }
    val widthDp = with(density) { width.toDp() }
    val heightDp = with(density) { height.toDp() }
    val screenWidthDp = with(density) { screenWidthPx.toDp() }
    // 几何允许 spring 过冲（弹性感），但颜色/透明度不能外推：
    // Color.lerp 走 Oklab 空间，fraction > 1 会产出越界分量直接抛异常。
    val colorFraction = p.coerceIn(0f, 1f)
    val background = lerp(chipColor, palette.surface, colorFraction)

    // 胶囊内容是同一个形状里的第一层：快速淡出；面板内容随后淡入
    val capsuleLabelAlpha = (1f - p / 0.28f).coerceIn(0f, 1f)
    val panelContentAlpha = ((p - 0.26f) / 0.74f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩同时作为面板/箭头的父层：玻璃开启后，子内容的点击先被消费，不会穿透到遮罩。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f * colorFraction))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {

        // 首帧离屏测量面板自然高度（只测量，不显示）
        if (contentHeightPx <= 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { screenWidthPx.toDp() })
                    .offset(y = 4000.dp)
            ) {
                WeekPanelContent(
                    maxWeek = maxWeek,
                    selectedWeek = selectedWeek,
                    thisWeek = thisWeek,
                    isDarkMode = isDarkMode,
                    onWeekSelected = {},
                    onConfirm = {},
                    modifier = Modifier.onSizeChanged { contentHeightPx = it.height.toFloat() }
                )
            }
        }

        // 形变容器：同一个圆角矩形从胶囊长成面板；玻璃负责模糊/折射，颜色层负责品牌色与可读性。
        // 玻璃形状跟着 spring 的圆角走，外层 clip 只负责把内容裁在形变轮廓里。
        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(widthDp, heightDp)
                .glassEffect(backdrop, glass, RoundedCornerShape(cornerDp))
                .clip(RoundedCornerShape(cornerDp))
                // 起点（p=0）保持完全不透明，避免与下面真实胶囊叠加；展开后让玻璃透出来。
                .background(background.copy(alpha = 1f - 0.24f * colorFraction))
        ) {
            // 第一层：胶囊里的「第 X 周」，形状一长大就淡出；箭头独立绘制，这里留出等宽占位保持文字位置
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(with(density) { start.height.toDp() })
                    .graphicsLayer { alpha = capsuleLabelAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第${currentWeek}周",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = chipContentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Spacer(modifier = Modifier.size(18.dp))
            }

            // 第二层：面板内容，随后淡入；按最终宽度排版（requiredWidth 不受容器约束影响），形变过程中只裁切不重排
            Box(
                modifier = Modifier
                    .requiredWidth(screenWidthDp)
                    .graphicsLayer { alpha = panelContentAlpha }
            ) {
                WeekPanelContent(
                    maxWeek = maxWeek,
                    selectedWeek = selectedWeek,
                    thisWeek = thisWeek,
                    isDarkMode = isDarkMode,
                    onWeekSelected = { selectedWeek = it },
                    onConfirm = { onWeekSelected(selectedWeek) }
                )
            }
        }

        // 箭头：从胶囊里箭头的真实位置出发，向左滑到顶部栏中间并翻转 180°；
        // 收起时沿同一条曲线滑回右侧，之后由真实胶囊接管。
        val arrowStartX = if (arrowAnchor.width > 1f) arrowAnchor.center.x else start.center.x
        val arrowStartY = if (arrowAnchor.width > 1f) arrowAnchor.center.y else start.center.y
        val arrowEndX = screenWidthPx / 2f
        val arrowEndY = start.center.y
        val arrowX = arrowStartX + (arrowEndX - arrowStartX) * p
        val arrowY = arrowStartY + (arrowEndY - arrowStartY) * p
        val arrowHalf = with(density) { ArrowTouchSize.toPx() } / 2f

        Box(
            modifier = Modifier
                .offset {
                    IntOffset((arrowX - arrowHalf).roundToInt(), (arrowY - arrowHalf).roundToInt())
                }
                .size(ArrowTouchSize)
                .elasticPress(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "收起周面板",
                // 起点在胶囊里（白），终点落在顶栏玻璃上，所以颜色要跟着从胶囊文字色过渡到顶栏墨色
                tint = lerp(chipContentColor, topBarContentColor, colorFraction),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 180f * p }
            )
        }
        }
    }
}

@Composable
private fun WeekPanelContent(
    maxWeek: Int,
    selectedWeek: Int,
    thisWeek: Int,
    isDarkMode: Boolean,
    onWeekSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = contentPalette(isDarkMode)
    val rows = (maxWeek + WeekColumns - 1) / WeekColumns

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "选择教学周",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.title
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "本周为第 $thisWeek 周",
            fontSize = 11.sp,
            color = palette.muted
        )
        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(WeekColumns) { column ->
                        val week = row * WeekColumns + column + 1
                        if (week <= maxWeek) {
                            WeekCell(
                                week = week,
                                isSelected = week == selectedWeek,
                                isThisWeek = week == thisWeek,
                                palette = palette,
                                onClick = { onWeekSelected(week) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .elasticPress(RoundedCornerShape(15.dp)),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
        ) {
            Text("确认", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WeekCell(
    week: Int,
    isSelected: Boolean,
    isThisWeek: Boolean,
    palette: ContentPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .elasticPress(shape)
            .clip(shape)
            .background(if (isSelected) palette.accent else palette.surfaceAlt)
            .then(
                if (isThisWeek && !isSelected) {
                    Modifier.border(1.2.dp, palette.accent, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = week.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected || isThisWeek) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else palette.title
        )
    }
}
