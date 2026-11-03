package com.example.courseapp.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import com.example.courseapp.ui.theme.AppColors
import kotlinx.coroutines.delay

/** 搜索框形变统一曲线：cubic-bezier(.2,.9,.22,1)，宽度与圆角同一条曲线。 */
private val SearchMorphEasing = CubicBezierEasing(0.2f, 0.9f, 0.22f, 1f)
private const val SearchMorphMillis = 360

/**
 * 圆形搜索按钮 ↔ 搜索输入框的连续形变。
 *
 * 始终是同一个容器、同一个 BasicTextField、同一个图标：
 * - 宽度从 56dp 插值到整行宽度；
 * - 圆角从 28dp（胶囊/圆形）插值到 16dp；
 * - 图标从圆心位置平移到 16dp 内边距；
 * - 占位文字与光标的透明度在宽度接近落定后才开始淡入（280ms 后，100ms 淡入）；
 * - 全程没有替换成另一个搜索组件，也没有用淡入淡出冒充宽度变化。
 */
@Composable
fun MorphingSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: AppColors,
    modifier: Modifier = Modifier,
    collapsedWidth: Dp = 56.dp,
    expandedRadius: Dp = 16.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fullWidth = maxWidth
        var expanded by rememberSaveable { mutableStateOf(value.isNotEmpty()) }
        var focusedOnce by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current

        // 外部清空/填值（例如“清除搜索”或从照片详情返回）时同步开合状态。
        LaunchedEffect(value) {
            if (value.isNotEmpty() && !expanded) expanded = true
        }
        LaunchedEffect(expanded) {
            if (expanded) {
                delay(280)
                focusedOnce = true
                focusRequester.requestFocus()
                keyboard?.show()
            } else {
                focusedOnce = false
                keyboard?.hide()
            }
        }

        BackHandler(enabled = expanded) {
            if (value.isNotEmpty()) onValueChange("") else expanded = false
        }

        val width by animateDpAsState(
            targetValue = if (expanded) fullWidth else collapsedWidth,
            animationSpec = tween(SearchMorphMillis, easing = SearchMorphEasing),
            label = "searchMorphWidth"
        )
        val radius by animateDpAsState(
            targetValue = if (expanded) expandedRadius else collapsedWidth / 2,
            animationSpec = tween(SearchMorphMillis, easing = SearchMorphEasing),
            label = "searchMorphRadius"
        )
        val iconStart by animateDpAsState(
            targetValue = if (expanded) 16.dp else (collapsedWidth - 20.dp) / 2,
            animationSpec = tween(SearchMorphMillis, easing = SearchMorphEasing),
            label = "searchMorphIcon"
        )
        val contentAlpha by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = if (expanded) 280 else 0
            ),
            label = "searchMorphContent"
        )
        val closeAlpha by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(140),
            label = "searchMorphClose"
        )
        val shape = RoundedCornerShape(radius)

        Box(
            modifier = Modifier
                .width(width)
                .height(56.dp)
                .clip(shape)
                .background(colors.surfaceSunken)
                .border(1.dp, colors.hairline, shape)
                .clickable {
                    expanded = true
                }
                .semantics { contentDescription = if (expanded) "搜索输入框" else "展开搜索" }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = iconStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (expanded) 44.dp else 0.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = expanded,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    if (value.isEmpty() && expanded && focusedOnce) {
                        Text(
                            text = placeholder,
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 关闭只占一层，不参与 Row 的宽度分配，避免展开过程中图标/文字被挤动。
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(30.dp)
                    .graphicsLayer { alpha = closeAlpha }
                    .clip(CircleShape)
                    .then(
                        if (expanded) {
                            Modifier.clickable {
                                if (value.isNotEmpty()) {
                                    onValueChange("")
                                } else {
                                    expanded = false
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = if (value.isNotEmpty()) "清除搜索" else "收起搜索",
                    tint = colors.textMuted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
