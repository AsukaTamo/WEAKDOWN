package com.example.courseapp.ui.schedule

import kotlin.math.floor

/**
 * 长按课表空白格后正在拖选的草稿：
 * - [dayOfWeek] 由手指落在哪一列决定，拖选过程中不变；
 * - [startSlot]/[slotCount] 随手指上下移动实时变化。
 */
data class CourseDraftSelection(
    val dayOfWeek: Int,
    val startSlot: Int,
    val slotCount: Int
) {
    val endSlot: Int get() = startSlot + slotCount - 1
}

/**
 * 把「长按起点」和「手指当前所在节次」换算成段落式选区。
 * 手指越过起点往下拖就向下延长，往上拖就向上延长，起点始终在选区内。
 */
fun courseDraftRange(anchorSlot: Int, targetSlot: Int, periodCount: Int): Pair<Int, Int> {
    if (periodCount <= 0) return 0 to 1
    val anchor = anchorSlot.coerceIn(0, periodCount - 1)
    val target = targetSlot.coerceIn(0, periodCount - 1)
    val start = minOf(anchor, target)
    val end = maxOf(anchor, target)
    return start to (end - start + 1)
}

/** 把手指在列内的 y 偏移换算成节次下标；超出上下边界时收敛到首/末节。 */
fun slotIndexAtOffset(offsetY: Float, slotHeightPx: Float, periodCount: Int): Int {
    if (periodCount <= 0) return 0
    if (slotHeightPx <= 0f) return 0
    return floor(offsetY / slotHeightPx).toInt().coerceIn(0, periodCount - 1)
}
