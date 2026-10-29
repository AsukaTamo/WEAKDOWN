package com.example.courseapp.ui.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.R
import com.example.courseapp.data.model.School
import com.example.courseapp.data.model.Schools

/**
 * 学校 → 校徽资源。目前只内置中国地质大学（武汉）；
 * 其他学校在 [SchoolBadge] 里退化成"校名缩写圆形徽章"，不会出现错 logo。
 */
@DrawableRes
fun schoolEmblemRes(school: School): Int? = when (school.id) {
    Schools.CUG.id -> R.drawable.school_cug_emblem
    else -> null
}

/**
 * 「我的」页背景：固定在右下角的学校水印 + 一团很淡的冷色光。
 *
 * 它画在 backdrop 里、页面滚动层之外，所以列表怎么滑它都不动；
 * 上面的磨砂卡片用 Vitro 采样这一层，透出来的就是这团模糊的校徽。
 */
@Composable
fun SchoolBackdrop(
    school: School,
    isDarkMode: Boolean
) {
    val accent = if (isDarkMode) Color(0xFF7FB8FF) else Color(0xFF2E7FD1)
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 右下角一大片冷色光：卡片采样时会透出一点点冷调，
            // 同时把校徽所在的角落和奶油底分开，页面不会一片死平。
            val washCenter = Offset(size.width * 0.86f, size.height * 0.72f)
            val washRadius = size.width * 0.98f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(alpha = if (isDarkMode) 0.24f else 0.15f),
                        0.55f to accent.copy(alpha = if (isDarkMode) 0.10f else 0.055f),
                        1f to Color.Transparent
                    ),
                    center = washCenter,
                    radius = washRadius
                ),
                radius = washRadius,
                center = washCenter
            )

            // 校徽背后再压一团更亮的光，让水印从背景里"浮"出来
            val center = Offset(size.width * 0.90f, size.height * 0.86f)
            val radius = size.width * 0.46f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to accent.copy(alpha = if (isDarkMode) 0.22f else 0.12f),
                        1f to Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        SchoolBadge(
            school = school,
            size = 264.dp,
            alpha = if (isDarkMode) 0.21f else 0.19f,
            isDarkMode = isDarkMode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 58.dp, y = -18.dp)
        )
    }
}

@Composable
private fun SchoolBadge(
    school: School,
    size: Dp,
    alpha: Float,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        val emblem = schoolEmblemRes(school)
        if (emblem != null) {
            Image(
                painter = painterResource(emblem),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            val ink = if (isDarkMode) Color(0xFF9CC8F0) else Color(0xFF2E6FA8)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.06f))
                    .border(2.dp, ink.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = school.shortName,
                    color = ink,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
