package com.example.courseapp.viewmodel

import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleCourseFilterTest {
    @Test
    fun filtersCoursesToActiveSemester() {
        val active = Semester(
            id = "2026-2027-1",
            name = "2026-2027学年 第一学期",
            startDate = "2026-09-01",
            isActive = true
        )
        val activeCourse = course("新学期课程", semester = "2026-2027-1")
        val otherCourse = course("旧学期课程", semester = "2025-2026-2")

        val filtered = ScheduleCourseFilter.activeSemesterCourses(
            courses = listOf(activeCourse, otherCourse),
            activeSemester = active,
            defaultSemesterId = "2025-2026-2"
        )

        assertEquals(listOf(activeCourse), filtered)
    }

    @Test
    fun fallsBackToDefaultSemesterWhenNoActiveSemesterExists() {
        val defaultCourse = course("默认学期课程", semester = "2025-2026-2")
        val otherCourse = course("其他学期课程", semester = "2026-2027-1")

        val filtered = ScheduleCourseFilter.activeSemesterCourses(
            courses = listOf(defaultCourse, otherCourse),
            activeSemester = null,
            defaultSemesterId = "2025-2026-2"
        )

        assertEquals(listOf(defaultCourse), filtered)
    }

    private fun course(name: String, semester: String): Course {
        return Course(
            name = name,
            dayOfWeek = 0,
            startSlot = 0,
            semester = semester
        )
    }
}
