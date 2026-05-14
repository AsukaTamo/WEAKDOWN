package com.example.courseapp.data.repository

import com.example.courseapp.data.db.CourseDao
import com.example.courseapp.data.db.SemesterDao
import com.example.courseapp.data.db.TimeSlotTemplateDao
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.TimeSlotTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao,
    private val semesterDao: SemesterDao,
    private val timeSlotTemplateDao: TimeSlotTemplateDao
) {
    // ── Courses ──

    fun getCoursesBySemester(semester: String): Flow<List<Course>> =
        courseDao.getCoursesBySemester(semester)

    fun getCoursesByDay(semester: String, day: Int): Flow<List<Course>> =
        courseDao.getCoursesByDay(semester, day)

    fun getAllCourses(): Flow<List<Course>> =
        courseDao.getAllCourses()

    suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course)

    suspend fun insertCourses(courses: List<Course>) =
        courseDao.insertCourses(courses)

    suspend fun updateCourse(course: Course) =
        courseDao.updateCourse(course)

    suspend fun deleteCourse(course: Course) =
        courseDao.deleteCourse(course)

    suspend fun deleteCoursesByIds(ids: List<Long>) =
        courseDao.deleteCoursesByIds(ids)

    suspend fun getCourseById(id: Long): Course? =
        courseDao.getCourseById(id)

    suspend fun getCourseCount(semester: String): Int =
        courseDao.getCourseCount(semester)

    suspend fun getConflictingCourses(semester: String, day: Int, startSlot: Int, endSlot: Int): List<Course> =
        courseDao.getConflictingCourses(semester, day, startSlot, endSlot)

    // ── Semesters ──

    fun getAllSemesters(): Flow<List<Semester>> =
        semesterDao.getAllSemesters()

    fun getActiveSemester(): Flow<Semester?> =
        semesterDao.getActiveSemester()

    suspend fun getActiveSemesterSync(): Semester? =
        semesterDao.getActiveSemesterSync()

    suspend fun insertSemester(semester: Semester) =
        semesterDao.insertSemester(semester)

    suspend fun updateSemester(semester: Semester) =
        semesterDao.updateSemester(semester)

    suspend fun deleteSemester(semester: Semester) =
        semesterDao.deleteSemester(semester)

    suspend fun setActiveSemester(id: String) {
        semesterDao.deactivateAll()
        semesterDao.activateSemester(id)
    }

    // ── Time Slot Templates ──

    fun getAllTimeSlotTemplates(): Flow<List<TimeSlotTemplate>> =
        timeSlotTemplateDao.getAllTemplates()

    fun getActiveTimeSlotTemplate(): Flow<TimeSlotTemplate?> =
        timeSlotTemplateDao.getActiveTemplate()

    suspend fun getActiveTimeSlotTemplateSync(): TimeSlotTemplate? =
        timeSlotTemplateDao.getActiveTemplateSync()

    suspend fun insertTimeSlotTemplate(template: TimeSlotTemplate): Long =
        timeSlotTemplateDao.insertTemplate(template)

    suspend fun updateTimeSlotTemplate(template: TimeSlotTemplate) =
        timeSlotTemplateDao.updateTemplate(template)

    suspend fun deleteTimeSlotTemplate(template: TimeSlotTemplate) =
        timeSlotTemplateDao.deleteTemplate(template)

    suspend fun setActiveTimeSlotTemplate(id: Long) {
        timeSlotTemplateDao.deactivateAll()
        timeSlotTemplateDao.activateTemplate(id)
    }
}
