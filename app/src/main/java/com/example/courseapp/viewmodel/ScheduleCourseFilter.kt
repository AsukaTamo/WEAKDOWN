package com.example.courseapp.viewmodel

import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester

object ScheduleCourseFilter {
    fun activeSemesterCourses(
        courses: List<Course>,
        activeSemester: Semester?,
        defaultSemesterId: String
    ): List<Course> {
        val semesterId = activeSemester?.id ?: defaultSemesterId
        return courses.filter { it.semester == semesterId }
    }
}
