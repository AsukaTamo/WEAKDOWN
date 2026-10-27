package com.example.courseapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 底栏液态指示器的曲线验收：
 * - 前沿立刻起步，后沿延迟 6 帧（100ms @60Hz）；
 * - 中途宽度被拉到 2 倍以上再收回；
 * - 末端只有一次轻微超调，最终精确落回目标宽度。
 */
class ElasticTabMotionTest {

    /** 真机相邻 Tab 的近似几何：胶囊宽 71.5，相邻中心间距 92。 */
    private val tabWidth = 71.5f
    private val centerGap = 92f

    /** 向右移动时：前沿是右沿、后沿是左沿，宽度 = 基础宽 + 两沿行程差 × 中心间距。 */
    private fun widthAt(elapsedMillis: Float): Float {
        val lead = ElasticTabMotion.leadProgressAt(elapsedMillis)
        val trail = ElasticTabMotion.trailProgressAt(elapsedMillis)
        return tabWidth + (lead - trail) * centerGap
    }

    @Test
    fun `front edge starts immediately and arrives before total duration`() {
        assertEquals(0f, ElasticTabMotion.leadProgressAt(0f), 1e-4f)
        // 6 帧（100ms）时前沿已经走完大半，视觉上「先冲出去」。
        assertTrue(ElasticTabMotion.leadProgressAt(100f) > 0.8f)
        assertEquals(1f, ElasticTabMotion.leadProgressAt(ElasticTabMotion.LeadMillis.toFloat()), 1e-4f)
    }

    @Test
    fun `back edge waits six frames at 60hz before moving`() {
        assertEquals(100, ElasticTabMotion.TrailDelayMillis)
        assertEquals(0f, ElasticTabMotion.trailProgressAt(0f), 1e-4f)
        assertEquals(0f, ElasticTabMotion.trailProgressAt(99f), 1e-4f)
        // 延迟结束后才开始追赶。
        assertTrue(ElasticTabMotion.trailProgressAt(140f) > 0f)
        assertEquals(1f, ElasticTabMotion.trailProgressAt(ElasticTabMotion.TotalMillis.toFloat()), 1e-3f)
    }

    @Test
    fun `width stretches past double mid flight then contracts`() {
        var maxWidth = 0f
        var maxAt = 0
        for (ms in 0..ElasticTabMotion.TotalMillis) {
            val width = widthAt(ms.toFloat())
            if (width > maxWidth) {
                maxWidth = width
                maxAt = ms
            }
        }
        assertTrue(
            "峰值 ${maxWidth / tabWidth}× 出现在 ${maxAt}ms",
            maxWidth > tabWidth * 2f
        )
        // 收尾必须回到目标宽度。
        assertEquals(tabWidth, widthAt(ElasticTabMotion.TotalMillis.toFloat()), 0.2f)
    }

    @Test
    fun `back edge overshoots once at the end, compressing the pill slightly`() {
        var maxTrail = 0f
        var minWidth = Float.MAX_VALUE
        var minWidthAt = 0
        for (ms in 0..ElasticTabMotion.TotalMillis) {
            maxTrail = maxOf(maxTrail, ElasticTabMotion.trailProgressAt(ms.toFloat()))
            val width = widthAt(ms.toFloat())
            if (width < minWidth) {
                minWidth = width
                minWidthAt = ms
            }
        }
        // 后沿越过目标 5% 以内：一次轻微超调，不是弹簧来回抖。
        assertTrue("后沿最大行程 $maxTrail", maxTrail > 1.02f && maxTrail < 1.08f)
        // 超调表现为收尾时胶囊比目标宽度再窄一点，然后弹回。
        assertTrue("最窄 ${minWidth}px 出现在 ${minWidthAt}ms", minWidth < tabWidth - 1f)
        assertTrue(minWidth > tabWidth * 0.9f)
    }

    @Test
    fun `back edge never overtakes the front edge while front is still travelling`() {
        for (ms in ElasticTabMotion.TrailDelayMillis..ElasticTabMotion.TotalMillis) {
            val lead = ElasticTabMotion.leadProgressAt(ms.toFloat())
            val trail = ElasticTabMotion.trailProgressAt(ms.toFloat())
            if (lead < 1f) {
                assertTrue("${ms}ms: lead=$lead trail=$trail", trail <= lead + 1e-4f)
            }
        }
    }
}
