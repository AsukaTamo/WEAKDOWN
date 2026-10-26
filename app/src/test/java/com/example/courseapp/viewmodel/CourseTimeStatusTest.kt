package com.example.courseapp.viewmodel

import com.example.courseapp.data.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTimeStatusTest {

    private val slots = listOf(
        TimeSlot("08:30", "09:15"), // 0
        TimeSlot("09:20", "10:05"), // 1
        TimeSlot("14:00", "14:45"), // 2
        TimeSlot("14:50", "15:35") // 3
    )

    private fun course(day: Int = 0, startSlot: Int = 0, slotCount: Int = 2) = Course(
        name = "操作系统",
        dayOfWeek = day,
        startSlot = startSlot,
        slotCount = slotCount
    )

    private fun at(hour: Int, minute: Int) = hour * 60 + minute

    @Test
    fun parsesClockTextLenientlyAndRejectsGarbage() {
        assertEquals(510, CourseTimeStatus.parseClockMinutes("08:30"))
        assertEquals(510, CourseTimeStatus.parseClockMinutes("8:30"))
        assertEquals(545, CourseTimeStatus.parseClockMinutes(" 09:05 "))

        assertNull(CourseTimeStatus.parseClockMinutes(""))
        assertNull(CourseTimeStatus.parseClockMinutes("830"))
        assertNull(CourseTimeStatus.parseClockMinutes("24:00"))
        assertNull(CourseTimeStatus.parseClockMinutes("08:60"))
        assertNull(CourseTimeStatus.parseClockMinutes("abc"))
    }

    @Test
    fun multiSlotCourseIsActiveUntilLastPeriodEnds() {
        val course = course(startSlot = 0, slotCount = 2) // 08:30 - 10:05

        assertTrue(CourseTimeStatus.isActive(course, slots, 0, at(9, 0)))
        assertTrue(CourseTimeStatus.isActive(course, slots, 0, at(10, 5)))
        assertFalse(CourseTimeStatus.isActive(course, slots, 0, at(8, 29)))
        assertFalse(CourseTimeStatus.isActive(course, slots, 0, at(10, 6)))
    }

    @Test
    fun courseIsNotActiveOnOtherWeekdays() {
        val course = course(day = 1, startSlot = 0)

        assertFalse(CourseTimeStatus.isActive(course, slots, dayIndex = 0, minuteOfDay = at(9, 0)))
        assertEquals(0, CourseTimeStatus.minutesUntil(course, slots, dayIndex = 0, minuteOfDay = at(8, 0)))
    }

    @Test
    fun progressRunsFromZeroAtStartToOneAtEnd() {
        val course = course(startSlot = 0, slotCount = 2)

        assertEquals(0f, CourseTimeStatus.progress(course, slots, at(8, 30)), 0.001f)
        assertEquals(1f, CourseTimeStatus.progress(course, slots, at(10, 5)), 0.001f)
        assertEquals(0.3157f, CourseTimeStatus.progress(course, slots, at(9, 0)), 0.001f)
    }

    @Test
    fun remainingMinutesCountsDownToLastPeriodEnd() {
        val course = course(startSlot = 0, slotCount = 2)

        assertEquals(65, CourseTimeStatus.minutesRemaining(course, slots, at(9, 0)))
        assertEquals(0, CourseTimeStatus.minutesRemaining(course, slots, at(10, 6)))
    }

    @Test
    fun upcomingOnlyWithinTheLookaheadWindow() {
        val course = course(startSlot = 0) // 08:30

        assertTrue(CourseTimeStatus.isUpcoming(course, slots, 0, at(7, 30)))
        assertEquals(60, CourseTimeStatus.minutesUntil(course, slots, 0, at(7, 30)))

        assertFalse(CourseTimeStatus.isUpcoming(course, slots, 0, at(7, 29)))
        assertFalse(CourseTimeStatus.isUpcoming(course, slots, 0, at(8, 30)))
    }

    @Test
    fun malformedOrMissingTimeSlotsNeverThrow() {
        val brokenSlots = listOf(TimeSlot("bad", "worse"))
        val course = course(startSlot = 0, slotCount = 2)

        assertNull(CourseTimeStatus.startMinute(course, brokenSlots))
        assertFalse(CourseTimeStatus.isActive(course, brokenSlots, 0, at(9, 0)))
        assertEquals(0f, CourseTimeStatus.progress(course, brokenSlots, at(9, 0)), 0.001f)
        assertEquals(0, CourseTimeStatus.minutesRemaining(course, brokenSlots, at(9, 0)))
    }

    @Test
    fun startSlotBeyondTimetableIsIgnored() {
        val course = course(startSlot = 9, slotCount = 2)

        assertFalse(CourseTimeStatus.isActive(course, slots, 0, at(9, 0)))
        assertEquals(0, CourseTimeStatus.minutesUntil(course, slots, 0, at(8, 0)))
        assertEquals(0f, CourseTimeStatus.progress(course, slots, at(9, 0)), 0.001f)
    }

    @Test
    fun slotCountBeyondTimetableClampsToLastPeriod() {
        val course = course(startSlot = 3, slotCount = 4) // 只有 4 节，越界的部分收敛到最后一节

        assertEquals(at(15, 35), CourseTimeStatus.endMinute(course, slots))
        assertTrue(CourseTimeStatus.isActive(course, slots, 0, at(15, 30)))
    }
}
