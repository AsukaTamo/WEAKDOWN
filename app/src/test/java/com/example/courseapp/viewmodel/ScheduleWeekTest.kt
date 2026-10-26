package com.example.courseapp.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleWeekTest {

    private val dayMillis = 24L * 60 * 60 * 1000
    private val semesterStart = 1_700_000_000_000L

    private fun weekAfter(days: Long, totalWeeks: Int = 18): Int =
        ScheduleViewModel.weekOfSemester(
            startDateMillis = semesterStart,
            nowMillis = semesterStart + days * dayMillis,
            totalWeeks = totalWeeks
        )

    @Test
    fun openingDayIsWeekOne() {
        assertEquals(1, weekAfter(0))
    }

    @Test
    fun lastMomentOfFirstWeekIsStillWeekOne() {
        // 6 天 23 小时
        val almostSevenDays = semesterStart + 7 * dayMillis - 60_000L
        assertEquals(
            1,
            ScheduleViewModel.weekOfSemester(semesterStart, almostSevenDays, 18)
        )
    }

    @Test
    fun seventhDayStartsWeekTwo() {
        assertEquals(2, weekAfter(7))
    }

    @Test
    fun weeksAdvanceEverySevenDays() {
        assertEquals(3, weekAfter(14))
        assertEquals(9, weekAfter(56))
    }

    @Test
    fun neverExceedsTotalWeeks() {
        assertEquals(18, weekAfter(18 * 7))
        assertEquals(18, weekAfter(40 * 7))
    }

    @Test
    fun beforeSemesterStartClampsToFirstWeek() {
        assertEquals(1, weekAfter(-3))
    }

    @Test
    fun zeroTotalWeeksFallsBackToFirstWeek() {
        assertEquals(1, weekAfter(0, totalWeeks = 0))
        assertEquals(1, weekAfter(30, totalWeeks = 0))
    }
}
