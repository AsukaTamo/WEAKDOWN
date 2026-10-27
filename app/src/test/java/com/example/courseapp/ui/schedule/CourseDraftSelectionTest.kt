package com.example.courseapp.ui.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseDraftSelectionTest {

    @Test
    fun `drag down extends from anchor`() {
        assertEquals(2 to 4, courseDraftRange(anchorSlot = 2, targetSlot = 5, periodCount = 12))
    }

    @Test
    fun `drag up extends above anchor`() {
        assertEquals(1 to 4, courseDraftRange(anchorSlot = 4, targetSlot = 1, periodCount = 12))
    }

    @Test
    fun `release without moving keeps a single period`() {
        assertEquals(3 to 1, courseDraftRange(anchorSlot = 3, targetSlot = 3, periodCount = 12))
    }

    @Test
    fun `target beyond the last period is clamped`() {
        assertEquals(10 to 2, courseDraftRange(anchorSlot = 10, targetSlot = 99, periodCount = 12))
    }

    @Test
    fun `anchor beyond the last period is clamped`() {
        assertEquals(8 to 4, courseDraftRange(anchorSlot = 99, targetSlot = 8, periodCount = 12))
    }

    @Test
    fun `empty period count still returns a safe range`() {
        assertEquals(0 to 1, courseDraftRange(anchorSlot = 3, targetSlot = 1, periodCount = 0))
    }

    @Test
    fun `offset maps to the slot under the finger`() {
        assertEquals(0, slotIndexAtOffset(offsetY = 0f, slotHeightPx = 100f, periodCount = 12))
        assertEquals(2, slotIndexAtOffset(offsetY = 250f, slotHeightPx = 100f, periodCount = 12))
        assertEquals(11, slotIndexAtOffset(offsetY = 5000f, slotHeightPx = 100f, periodCount = 12))
        assertEquals(0, slotIndexAtOffset(offsetY = -20f, slotHeightPx = 100f, periodCount = 12))
        assertEquals(0, slotIndexAtOffset(offsetY = 50f, slotHeightPx = 0f, periodCount = 12))
    }
}
