package com.example.courseapp.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTimeSlotsTest {
    @Test
    fun defaultTimeSlotsMatchUniversityLargeClassSchedule() {
        val slots = ScheduleViewModel.defaultTimeSlots()

        assertEquals(12, slots.size)
        assertEquals("08:30", slots[0].start)
        assertEquals("09:15", slots[0].end)
        assertEquals("09:20", slots[1].start)
        assertEquals("10:05", slots[1].end)
        assertEquals("14:00", slots[4].start)
        assertEquals("18:30", slots[8].start)
        assertEquals("21:50", slots[11].end)
    }

    @Test
    fun legacyDefaultTimeSlotsAreUpgradedToNewSchedule() {
        val upgraded = ScheduleViewModel.normalizeLoadedTimeSlots(
            ScheduleViewModel.legacyDefaultTimeSlots()
        )

        assertEquals(12, upgraded.size)
        assertEquals("08:30", upgraded[0].start)
        assertEquals("21:50", upgraded[11].end)
    }
}
