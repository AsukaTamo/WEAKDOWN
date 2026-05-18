package com.example.courseapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.importer.CourseHtmlParser
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportConflict(
    val newCourse: Course,
    val existingCourse: Course
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    private val _parsedCourses = MutableStateFlow<List<Course>>(emptyList())
    val parsedCourses: StateFlow<List<Course>> = _parsedCourses.asStateFlow()

    private val _conflicts = MutableStateFlow<List<ImportConflict>>(emptyList())
    val conflicts: StateFlow<List<ImportConflict>> = _conflicts.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<Pair<String, String>>()
    val snackbarMessage: SharedFlow<Pair<String, String>> = _snackbarMessage.asSharedFlow()

    private val _importComplete = MutableStateFlow(false)
    val importComplete: StateFlow<Boolean> = _importComplete.asStateFlow()

    fun parseHtml(html: String, semester: String) {
        viewModelScope.launch {
            try {
                Log.d("ImportVM", "parseHtml called, html length: ${html.length}, semester: $semester")
                val parsed = CourseHtmlParser.parse(html)
                Log.d("ImportVM", "Parsed ${parsed.size} courses from HTML")
                for (p in parsed) {
                    Log.d("ImportVM", "  Course: ${p.name}, day=${p.dayOfWeek}, slot=${p.startSlot}, count=${p.slotCount}, weeks=${p.weekRange}")
                }
                val courses = CourseHtmlParser.toCourses(parsed, semester)
                _parsedCourses.value = courses
                _selectedIds.value = courses.indices.map { it.toLong() }.toSet()
                checkConflicts(courses)
            } catch (e: Exception) {
                Log.e("ImportVM", "Parse error", e)
                _snackbarMessage.emit("解析失败：${e.message}" to "error")
            }
        }
    }

    private suspend fun checkConflicts(newCourses: List<Course>) {
        val existingCourses = repository.getAllCourses().first()
        val conflictList = mutableListOf<ImportConflict>()

        for (newCourse in newCourses) {
            val conflicting = existingCourses.filter { existing ->
                existing.dayOfWeek == newCourse.dayOfWeek &&
                existing.semester == newCourse.semester &&
                existing.startSlot < newCourse.startSlot + newCourse.slotCount &&
                existing.startSlot + existing.slotCount > newCourse.startSlot &&
                weekRangesOverlap(existing.weekRange, newCourse.weekRange)
            }
            for (existing in conflicting) {
                conflictList.add(ImportConflict(newCourse, existing))
            }
        }

        _conflicts.value = conflictList
    }

    private fun weekRangesOverlap(range1: String, range2: String): Boolean {
        for (week in 1..30) {
            if (ScheduleViewModel.isWeekInRange(week, range1) &&
                ScheduleViewModel.isWeekInRange(week, range2)) {
                return true
            }
        }
        return false
    }

    fun toggleSelection(index: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(index)) remove(index) else add(index)
        }
    }

    fun selectAll() {
        _selectedIds.value = _parsedCourses.value.indices.map { it.toLong() }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun importSelected(replaceConflicts: Boolean = false, replaceAll: Boolean = false) {
        viewModelScope.launch {
            val courses = _parsedCourses.value
            val selected = _selectedIds.value.map { courses[it.toInt()] }
            val semester = selected.firstOrNull()?.semester ?: ""

            if (replaceAll && semester.isNotEmpty()) {
                // Delete all existing courses for this semester, then insert all selected
                repository.deleteAllBySemester(semester)
            } else if (replaceConflicts) {
                // Delete only conflicting existing courses
                val conflicts = _conflicts.value
                for (conflict in conflicts) {
                    if (selected.contains(conflict.newCourse)) {
                        repository.deleteCourse(conflict.existingCourse)
                    }
                }
            }

            // Insert selected courses
            for (course in selected) {
                repository.insertCourse(course)
            }

            _importComplete.value = true
            _snackbarMessage.emit("已导入 ${selected.size} 门课程" to "success")
        }
    }

    fun clearState() {
        _parsedCourses.value = emptyList()
        _conflicts.value = emptyList()
        _selectedIds.value = emptySet()
        _importComplete.value = false
    }
}
