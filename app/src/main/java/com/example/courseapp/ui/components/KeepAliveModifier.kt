package com.example.courseapp.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * 常驻组合、条件摆放：visible 为 false 时只 measure 不 place。
 * 页面/标签状态得以保留，又不占绘制与触摸命中。
 */
internal fun Modifier.keepPlacedWhen(visible: Boolean): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        if (visible) {
            placeable.place(0, 0)
        }
    }
}
