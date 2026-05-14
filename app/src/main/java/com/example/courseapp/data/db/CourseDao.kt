package com.example.courseapp.data.db

import androidx.room.*
import com.example.courseapp.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE semester = :semester ORDER BY dayOfWeek, startSlot")
    fun getCoursesBySemester(semester: String): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semester = :semester AND dayOfWeek = :day ORDER BY startSlot")
    fun getCoursesByDay(semester: String, day: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startSlot")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE id IN (:ids)")
    suspend fun deleteCoursesByIds(ids: List<Long>)

    @Query("DELETE FROM courses WHERE semester = :semester")
    suspend fun deleteAllBySemester(semester: String)

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Query("SELECT COUNT(*) FROM courses WHERE semester = :semester")
    suspend fun getCourseCount(semester: String): Int

    @Query("SELECT * FROM courses WHERE semester = :semester AND dayOfWeek = :day AND startSlot < :endSlot AND (startSlot + slotCount) > :startSlot")
    suspend fun getConflictingCourses(semester: String, day: Int, startSlot: Int, endSlot: Int): List<Course>
}
