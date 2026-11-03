package com.example.courseapp.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FLIP 运动学验收：620ms、3 帧错开、三次贝塞尔、轻微弧线、无变换叠加。
 */
class FlipMotionTest {

    @Test
    fun `duration and stagger match the handbook baseline`() {
        assertEquals(620, FlipMotion.DurationMillis)
        assertEquals(50, FlipMotion.StaggerMillis)
        assertEquals(3f * FlipMotion.FrameMillis, 50f, 0.2f)
    }

    @Test
    fun `total duration grows with stagger but is capped`() {
        assertEquals(620, FlipMotion.totalMillis(1))
        assertEquals(670, FlipMotion.totalMillis(2))
        assertEquals(620 + 5 * 50, FlipMotion.totalMillis(100))
        assertEquals(FlipMotion.MaxStaggerStep * FlipMotion.StaggerMillis, FlipMotion.staggerMillis(50))
    }

    @Test
    fun `stagger delays the second item by three frames`() {
        val total = FlipMotion.totalMillis(2)
        val first = FlipMotion.localProgress(0.5f, 0, total)
        val second = FlipMotion.localProgress(0.5f, 1, total)
        assertEquals(50f / 620f, first - second, 0.0001f)
        assertEquals(335f / 620f, first, 0.0001f)
        assertEquals(1f, FlipMotion.localProgress(1f, 9, FlipMotion.totalMillis(10)), 0.0001f)
        assertEquals(0f, FlipMotion.localProgress(0f, 9, FlipMotion.totalMillis(10)), 0.0001f)
    }

    @Test
    fun `cubic bezier easing is monotonic with unit endpoints`() {
        assertEquals(0f, FlipMotion.easedProgress(0f, 0, 620), 0.0001f)
        assertEquals(1f, FlipMotion.easedProgress(1f, 0, 620), 0.0001f)
        var previous = 0f
        for (step in 0..20) {
            val value = FlipMotion.easedProgress(step / 20f, 0, 620)
            assertTrue("easing should be monotonic at $step", value >= previous - 0.0001f)
            previous = value
        }
    }

    @Test
    fun `invert starts at first bounds and ends at identity`() {
        val first = FlipBounds(0f, 0f, 120f, 80f)
        val last = FlipBounds(240f, 400f, 360f, 240f)
        val delta = FlipMotion.deltaBetween(first, last)
        assertNotNull(delta)
        delta!!
        val start = FlipMotion.transformAt(delta, 0f, 20f, 0)
        assertEquals(first.left - last.left, start.translationX, 0.001f)
        assertEquals(first.top - last.top, start.translationY, 0.001f)
        assertEquals(first.width / last.width, start.scaleX, 0.001f)
        val end = FlipMotion.transformAt(delta, 1f, 20f, 0)
        assertEquals(0f, end.translationX, 0.001f)
        assertEquals(0f, end.translationY, 0.001f)
        assertEquals(1f, end.scaleX, 0.001f)
        assertEquals(1f, end.scaleY, 0.001f)
    }

    @Test
    fun `motion path curves perpendicular to the displacement`() {
        val delta = FlipDelta(dx = 200f, dy = 0f, scaleX = 1f, scaleY = 1f)
        val even = FlipMotion.transformAt(delta, 0.5f, arcPx = 20f, index = 0)
        val odd = FlipMotion.transformAt(delta, 0.5f, arcPx = 20f, index = 1)
        assertEquals(100f, even.translationX, 0.001f)
        assertEquals(10f, even.translationY, 0.001f)
        assertEquals(-10f, odd.translationY, 0.001f)
        assertTrue("path must not be a straight line", kotlin.math.abs(even.translationY) > 1f)
        assertEquals(0f, FlipMotion.transformAt(delta, 0f, 20f, 0).translationY, 0.001f)
        assertEquals(0f, FlipMotion.transformAt(delta, 1f, 20f, 0).translationY, 0.001f)
    }

    @Test
    fun `interrupted animation resumes from the current visual bounds`() {
        val previousTarget = FlipBounds(240f, 400f, 360f, 240f)
        val delta = FlipDelta(dx = -240f, dy = -400f, scaleX = 0.5f, scaleY = 0.5f)
        val transform = FlipMotion.transformAt(delta, 0.4f, arcPx = 12f, index = 0)
        val visual = FlipMotion.visualBounds(previousTarget, transform)
        // 还原后的可视矩形不应再叠加一次完整逆变换。
        assertTrue(visual.left > previousTarget.left + delta.dx)
        assertTrue(visual.width > previousTarget.width * delta.scaleX)
    }

    @Test
    fun `arc magnitude is capped`() {
        assertEquals(28f, FlipMotion.arcMagnitude(1000f, basePx = 8f, maxPx = 28f), 0.001f)
        assertEquals(8f, FlipMotion.arcMagnitude(0f, basePx = 8f, maxPx = 28f), 0.001f)
    }
}
