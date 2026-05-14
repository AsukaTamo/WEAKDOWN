package com.example.courseapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    val courses: StateFlow<List<Course>> = repository.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedTab by mutableIntStateOf(0)
        private set

    // Batch selection
    private val _selectedIds = mutableStateListOf<Long>()
    val selectedIds: List<Long> get() = _selectedIds
    var isBatchMode by mutableStateOf(false)
        private set

    // Add course form state
    var courseName by mutableStateOf("")
        private set
    var teacher by mutableStateOf("")
        private set
    var location by mutableStateOf("")
        private set
    var credits by mutableStateOf("")
        private set
    var weekRange by mutableStateOf("1-18周")
        private set
    var notes by mutableStateOf("")
        private set
    var selectedDay by mutableIntStateOf(0)
        private set
    var selectedType by mutableStateOf(CourseType.REQUIRED)
        private set
    var selectedStartSlot by mutableIntStateOf(0)
        private set
    var selectedSlotCount by mutableIntStateOf(2)
        private set
    var selectedColor by mutableStateOf<Color?>(null)
        private set

    private val _snackbarMessage = MutableSharedFlow<Pair<String, String>>()
    val snackbarMessage: SharedFlow<Pair<String, String>> = _snackbarMessage.asSharedFlow()

    fun switchTab(index: Int) {
        selectedTab = index
    }

    fun updateCourseName(value: String) { courseName = value }
    fun updateTeacher(value: String) { teacher = value }
    fun updateLocation(value: String) { location = value }
    fun updateCredits(value: String) { credits = value }
    fun updateWeekRange(value: String) { weekRange = value }
    fun updateNotes(value: String) { notes = value }
    fun updateDay(value: Int) { selectedDay = value }
    fun updateType(value: CourseType) { selectedType = value }
    fun updateStartSlot(value: Int) { selectedStartSlot = value }
    fun updateSlotCount(value: Int) { selectedSlotCount = value }
    fun updateColor(value: Color?) { selectedColor = value }

    // Batch operations
    fun toggleBatchMode() {
        isBatchMode = !isBatchMode
        if (!isBatchMode) _selectedIds.clear()
    }

    fun toggleSelection(id: Long) {
        if (_selectedIds.contains(id)) _selectedIds.remove(id) else _selectedIds.add(id)
    }

    fun selectAll() {
        _selectedIds.clear()
        _selectedIds.addAll(courses.value.map { it.id })
    }

    fun deleteSelected() {
        if (_selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.deleteCoursesByIds(_selectedIds.toList())
            _snackbarMessage.emit("已删除 ${_selectedIds.size} 门课程" to "success")
            _selectedIds.clear()
            isBatchMode = false
        }
    }

    fun saveCourse() {
        if (courseName.isBlank()) {
            viewModelScope.launch { _snackbarMessage.emit("请输入课程名称" to "error") }
            return
        }
        viewModelScope.launch {
            val course = Course(
                name = courseName,
                teacher = teacher,
                location = location,
                dayOfWeek = selectedDay,
                startSlot = selectedStartSlot,
                slotCount = selectedSlotCount,
                type = selectedType,
                weekRange = weekRange,
                credits = credits.toFloatOrNull() ?: 0f,
                notes = notes,
                customColor = selectedColor?.let { color ->
                    String.format("#%06X", 0xFFFFFF and color.toArgb())
                } ?: ""
            )
            repository.insertCourse(course)
            clearForm()
            _snackbarMessage.emit("课程保存成功！" to "success")
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            _snackbarMessage.emit("已删除「${course.name}」" to "success")
        }
    }

    fun duplicateCourse(course: Course) {
        viewModelScope.launch {
            val copy = course.copy(id = 0, dayOfWeek = (course.dayOfWeek + 1) % 7)
            repository.insertCourse(copy)
            _snackbarMessage.emit("已复制「${course.name}」到下一天" to "success")
        }
    }

    private fun clearForm() {
        courseName = ""
        teacher = ""
        location = ""
        credits = ""
        weekRange = "1-18周"
        notes = ""
        selectedDay = 0
        selectedType = CourseType.REQUIRED
        selectedStartSlot = 0
        selectedSlotCount = 2
        selectedColor = null
    }
}
