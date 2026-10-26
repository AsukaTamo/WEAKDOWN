package com.example.courseapp.viewmodel

import com.example.courseapp.data.model.Course

/**
 * 课表当前的"此刻"：星期几 + 当天第几分钟。
 * 由 ScheduleViewModel 定时刷新，供课表 UI 做进行中/即将开始判断，避免各处重复读 Calendar。
 */
data class ScheduleClock(
    val dayIndex: Int, // 0=周一 .. 6=周日
    val minuteOfDay: Int // 0..1439
) {
    companion object {
        fun now(): ScheduleClock {
            val cal = java.util.Calendar.getInstance()
            val dayIndex = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            val minuteOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                cal.get(java.util.Calendar.MINUTE)
            return ScheduleClock(dayIndex, minuteOfDay)
        }
    }
}

/**
 * 课表时间相关的纯计算：不持有状态、不读系统时间，全部参数显式传入，便于单测。
 */
object CourseTimeStatus {

    /** 只有 60 分钟内开始的课程才算"即将开始"，避免早上把全天的课都标成即将开始。 */
    const val UPCOMING_WINDOW_MINUTES = 60

    /** 解析 "HH:mm" / "H:mm"；非法输入返回 null，避免脏作息数据让课表崩溃。 */
    fun parseClockMinutes(text: String): Int? {
        val parts = text.trim().split(":", limit = 2)
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /** 课程开始分钟数；节次越界或时间格式非法时返回 null。 */
    fun startMinute(course: Course, timeSlots: List<TimeSlot>): Int? {
        if (course.startSlot !in timeSlots.indices) return null
        return parseClockMinutes(timeSlots[course.startSlot].start)
    }

    /** 课程结束分钟数；按最后一节的结束时间计算，超出作息表时收敛到最后一节。 */
    fun endMinute(course: Course, timeSlots: List<TimeSlot>): Int? {
        if (course.startSlot !in timeSlots.indices) return null
        val lastSlot = (course.startSlot + course.slotCount - 1).coerceIn(0, timeSlots.lastIndex)
        return parseClockMinutes(timeSlots[lastSlot].end)
    }

    /** 是否正在进行；结束分钟当刻仍算进行中，与原实现保持一致。 */
    fun isActive(
        course: Course,
        timeSlots: List<TimeSlot>,
        dayIndex: Int,
        minuteOfDay: Int
    ): Boolean {
        if (course.dayOfWeek != dayIndex) return false
        val start = startMinute(course, timeSlots) ?: return false
        val end = endMinute(course, timeSlots) ?: return false
        if (end <= start) return false
        return minuteOfDay in start..end
    }

    /** 进行中课程的完成度 0..1；不在时间段内时收敛到 0 或 1。 */
    fun progress(course: Course, timeSlots: List<TimeSlot>, minuteOfDay: Int): Float {
        val start = startMinute(course, timeSlots) ?: return 0f
        val end = endMinute(course, timeSlots) ?: return 0f
        if (end <= start) return 0f
        return ((minuteOfDay - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }

    /** 是否即将开始（仅当天、且在下个 [UPCOMING_WINDOW_MINUTES] 分钟内）。 */
    fun isUpcoming(
        course: Course,
        timeSlots: List<TimeSlot>,
        dayIndex: Int,
        minuteOfDay: Int
    ): Boolean {
        if (course.dayOfWeek != dayIndex) return false
        val start = startMinute(course, timeSlots) ?: return false
        return (start - minuteOfDay) in 1..UPCOMING_WINDOW_MINUTES
    }

    /** 距离开始还有几分钟；非当天或已开始返回 0。 */
    fun minutesUntil(
        course: Course,
        timeSlots: List<TimeSlot>,
        dayIndex: Int,
        minuteOfDay: Int
    ): Int {
        if (course.dayOfWeek != dayIndex) return 0
        val start = startMinute(course, timeSlots) ?: return 0
        return (start - minuteOfDay).coerceAtLeast(0)
    }

    /** 进行中课程还剩几分钟；非进行中返回 0。 */
    fun minutesRemaining(course: Course, timeSlots: List<TimeSlot>, minuteOfDay: Int): Int {
        val start = startMinute(course, timeSlots) ?: return 0
        val end = endMinute(course, timeSlots) ?: return 0
        if (end <= start) return 0
        return (end - minuteOfDay).coerceAtLeast(0)
    }
}
