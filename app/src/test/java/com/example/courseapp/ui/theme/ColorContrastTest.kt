package com.example.courseapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 把配色约束写成测试：以后改色只要踩线，构建就会红。
 * 阈值按 WCAG 2.1：正文 4.5:1，大号/次要文字 3:1，这里统一按 4.5 卡。
 */
class ColorContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertContrast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Double
    ) {
        val ratio = contrast(foreground, background)
        assertTrue("$label 对比度 ${"%.2f".format(ratio)}:1，低于要求 ${minimum}:1", ratio >= minimum)
    }

    private val light = LightAppColors
    private val dark = DarkAppColors

    @Test
    fun `正文与标题在两种模式下都满足 AA`() {
        for ((name, c) in listOf("浅色" to light, "深色" to dark)) {
            assertContrast("$name 主文字/面板", c.textPrimary, c.surface, 7.0)
            assertContrast("$name 次文字/面板", c.textSecondary, c.surface, 4.5)
            assertContrast("$name 弱文字/画布", c.textMuted, c.canvas, 4.5)
            assertContrast("$name 弱文字/面板", c.textMuted, c.surface, 4.5)
        }
    }

    @Test
    fun `强调色既能当文字也能当按钮底色`() {
        for ((name, c) in listOf("浅色" to light, "深色" to dark)) {
            // 当文字用
            assertContrast("$name 强调文字/面板", c.accentText, c.surface, 4.5)
            assertContrast("$name 强调文字/画布", c.accentText, c.canvas, 4.5)
            // 当底色用，按钮上的文字要能看清
            assertContrast("$name 按钮文字/强调底", c.accentOn, c.accent, 4.5)
        }
    }

    @Test
    fun `语义色在两种模式下都能读`() {
        for ((name, c) in listOf("浅色" to light, "深色" to dark)) {
            assertContrast("$name 成功色/面板", c.success, c.surface, 4.5)
            assertContrast("$name 危险色/面板", c.danger, c.surface, 4.5)
        }
    }

    @Test
    fun `选中态必须保住 AA，且不能明显比未选中更暗`() {
        // 浅色用颜色区分选中，深色必须比未选中更亮（原生 bug 是深色选中 #0F8F7B 比未选中还暗）。
        for ((name, c) in listOf("浅色" to light, "深色" to dark)) {
            val selected = contrast(c.accentText, c.surface)
            val idle = contrast(c.textMuted, c.surface)
            assertTrue("$name 选中态 ${"%.2f".format(selected)}:1 低于 AA", selected >= 4.5)
            assertTrue(
                "$name 选中态 ${"%.2f".format(selected)}:1 明显暗于未选中 ${"%.2f".format(idle)}:1",
                selected >= idle * 0.85
            )
        }
    }

    @Test
    fun `课程卡白字在四种类型上都满足 AA`() {
        val lightPairs = listOf(
            "必修" to (CourseRequiredStart to CourseRequiredEnd),
            "选修" to (CourseElectiveStart to CourseElectiveEnd),
            "实验" to (CourseLabStart to CourseLabEnd),
            "自定义" to (CourseCustomStart to CourseCustomEnd)
        )
        val darkPairs = listOf(
            "必修" to (CourseRequiredStartDark to CourseRequiredEndDark),
            "选修" to (CourseElectiveStartDark to CourseElectiveEndDark),
            "实验" to (CourseLabStartDark to CourseLabEndDark),
            "自定义" to (CourseCustomStartDark to CourseCustomEndDark)
        )
        for ((mode, pairs) in listOf("浅色" to lightPairs, "深色" to darkPairs)) {
            for ((type, gradient) in pairs) {
                assertContrast("$mode $type 起点白字", Color.White, gradient.first, 4.5)
                assertContrast("$mode $type 终点白字", Color.White, gradient.second, 4.5)
            }
        }
    }
}
