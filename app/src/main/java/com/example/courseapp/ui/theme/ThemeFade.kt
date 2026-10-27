package com.example.courseapp.ui.theme

/**
 * 主题色档位。
 *
 * 深浅切换的视觉过渡由切换瞬间抓取的整屏快照交叉淡化完成（见 MainActivity），
 * 不再逐帧插值整棵树的颜色；这里只提供当前目标档位，给课程卡、玻璃、开关等
 * 固定色提供统一的取色入口。
 */
object ThemeFade {
    /** 0 = 浅色，1 = 深色。 */
    fun fractionFor(isDarkMode: Boolean): Float = if (isDarkMode) 1f else 0f
}
