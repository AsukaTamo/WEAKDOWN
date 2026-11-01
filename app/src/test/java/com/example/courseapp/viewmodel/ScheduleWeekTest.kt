package com.example.courseapp.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ScheduleWeekTest {

    private val dayMillis = 24L * 60 * 60 * 1000

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis

    private fun weekOf(nowMillis: Long, startMillis: Long, totalWeeks: Int = 25): Int =
        ScheduleViewModel.weekOfSemester(
            startDateMillis = startMillis,
            nowMillis = nowMillis,
            totalWeeks = totalWeeks
        )

    @Test
    fun mondayStartCountsWeekOneFromOpeningDay() {
        val start = date(2026, Calendar.AUGUST, 31) // 周一
        assertEquals(1, weekOf(start, start))
        assertEquals(1, weekOf(start + 6 * dayMillis, start)) // 周日仍属于第 1 周
        assertEquals(2, weekOf(start + 7 * dayMillis, start))
        assertEquals(10, weekOf(date(2026, Calendar.NOVEMBER, 2), start))
    }

    @Test
    fun midWeekStartUsesTheMondayOfItsWeek() {
        val start = date(2026, Calendar.SEPTEMBER, 2) // 周三
        assertEquals(date(2026, Calendar.AUGUST, 31), ScheduleViewModel.weekStartMillis(start, 1))
        assertEquals(1, weekOf(start, start))
    }

    @Test
    fun sundayStartAlignsToTheMondayOfThatWeek() {
        // 回归用例：开学日填周日时，旧实现会让日期列和"周一…周日"列头整体错 6 天。
        val start = date(2026, Calendar.AUGUST, 30) // 周日
        assertEquals(date(2026, Calendar.AUGUST, 24), ScheduleViewModel.firstMondayMillis(start))
        assertEquals(date(2026, Calendar.OCTOBER, 26), ScheduleViewModel.weekStartMillis(start, 10))
        assertEquals(10, weekOf(date(2026, Calendar.NOVEMBER, 1), start))
    }

    @Test
    fun neverExceedsTotalWeeks() {
        val start = date(2026, Calendar.AUGUST, 31)
        assertEquals(18, weekOf(start + 18 * 7 * dayMillis, start, totalWeeks = 18))
        assertEquals(18, weekOf(start + 40 * 7 * dayMillis, start, totalWeeks = 18))
    }

    @Test
    fun beforeSemesterStartClampsToFirstWeek() {
        val start = date(2026, Calendar.AUGUST, 31)
        assertEquals(1, weekOf(start - 3 * dayMillis, start))
        assertEquals(1, weekOf(start - 40 * dayMillis, start))
    }

    @Test
    fun zeroTotalWeeksFallsBackToFirstWeek() {
        val start = date(2026, Calendar.AUGUST, 31)
        assertEquals(1, weekOf(start, start, totalWeeks = 0))
        assertEquals(1, weekOf(start + 30 * dayMillis, start, totalWeeks = 0))
    }
}
