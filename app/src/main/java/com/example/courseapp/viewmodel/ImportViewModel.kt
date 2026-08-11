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

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    private val _parsedCourses = MutableStateFlow<List<Course>>(emptyList())
    val parsedCourses: StateFlow<List<Course>> = _parsedCourses.asStateFlow()

    private val _currentSemesterCourseCount = MutableStateFlow(0)
    val currentSemesterCourseCount: StateFlow<Int> = _currentSemesterCourseCount.asStateFlow()

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
                _currentSemesterCourseCount.value = repository.getCourseCount(semester)
            } catch (e: Exception) {
                Log.e("ImportVM", "Parse error", e)
                _snackbarMessage.emit("解析失败：${e.message}" to "error")
            }
        }
    }

    fun importCurrentSemester(overwriteExisting: Boolean) {
        viewModelScope.launch {
            val courses = _parsedCourses.value

            if (courses.isEmpty()) {
                _snackbarMessage.emit("没有可导入的课程" to "error")
                return@launch
            }

            val semester = courses.first().semester

            if (overwriteExisting) {
                repository.deleteAllBySemester(semester)
            }

            repository.insertCourses(courses)

            _importComplete.value = true
            _snackbarMessage.emit("已导入 ${courses.size} 门课程" to "success")
        }
    }

    fun currentImportAction(): ImportOverwriteAction {
        return ImportOverwritePolicy.actionFor(
            currentSemesterCourseCount = _currentSemesterCourseCount.value,
            selectedCourseCount = _parsedCourses.value.size
        )
    }

    fun clearState() {
        _parsedCourses.value = emptyList()
        _currentSemesterCourseCount.value = 0
        _importComplete.value = false
    }
}
