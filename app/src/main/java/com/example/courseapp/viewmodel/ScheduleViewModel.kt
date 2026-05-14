package com.example.courseapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.TimeSlotTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.courseapp.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TimeSlot(val start: String, val end: String)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<Pair<String, String>>()
    val snackbarMessage: SharedFlow<Pair<String, String>> = _snackbarMessage.asSharedFlow()

    private val _isFabOpen = MutableStateFlow(false)
    val isFabOpen: StateFlow<Boolean> = _isFabOpen.asStateFlow()

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _timeSlots = MutableStateFlow(defaultTimeSlots())
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    private val _activeSemester = MutableStateFlow<Semester?>(null)
    val activeSemester: StateFlow<Semester?> = _activeSemester.asStateFlow()

    private val _draggedCourse = MutableStateFlow<Course?>(null)
    val draggedCourse: StateFlow<Course?> = _draggedCourse.asStateFlow()

    val todayIndex: Int
        get() {
            val cal = Calendar.getInstance()
            return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        }

    val semesterText: String
        get() = _activeSemester.value?.name ?: "2025-2026学年 第二学期"

    private var semesterStartDate = Calendar.getInstance().apply {
        set(2025, Calendar.FEBRUARY, 24, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    init {
        loadSemester()
        loadCourses()
        loadTimeSlotsFromTemplate()
        initSampleData()
    }

    private fun loadSemester() {
        viewModelScope.launch {
            repository.getActiveSemester().collect { semester ->
                _activeSemester.value = semester
                if (semester != null) {
                    try {
                        val parts = semester.startDate.split("-")
                        semesterStartDate = Calendar.getInstance().apply {
                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        // Auto-calculate current week
                        val now = Calendar.getInstance()
                        val diffMs = now.timeInMillis - semesterStartDate.timeInMillis
                        val diffWeeks = (diffMs / (7 * 24 * 60 * 60 * 1000L)).toInt() + 1
                        _currentWeek.value = diffWeeks.coerceIn(1, semester.totalWeeks)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            repository.getAllCourses().collect { list ->
                _courses.value = list
            }
        }
    }

    private fun loadTimeSlotsFromTemplate() {
        viewModelScope.launch {
            repository.getActiveTimeSlotTemplate().collect { template ->
                if (template != null) {
                    try {
                        val type = object : TypeToken<List<TimeSlot>>() {}.type
                        val slots: List<TimeSlot> = Gson().fromJson(template.slotsJson, type)
                        if (slots.isNotEmpty()) {
                            _timeSlots.value = slots
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun saveTimeSlotTemplate(name: String, slots: List<TimeSlot>) {
        viewModelScope.launch {
            val json = Gson().toJson(slots)
            val template = TimeSlotTemplate(name = name, slotsJson = json, isActive = true)
            repository.insertTimeSlotTemplate(template)
            _timeSlots.value = slots
            _snackbarMessage.emit("已保存作息表「$name」" to "success")
        }
    }

    private fun initSampleData() {
        viewModelScope.launch {
            if (repository.getAllCourses().first().isEmpty()) {
                val samples = listOf(
                    Course(name = "操作系统", teacher = "张教授", location = "教三楼301", dayOfWeek = 0, startSlot = 0, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "计算机网络", teacher = "李教授", location = "教三楼301", dayOfWeek = 0, startSlot = 2, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "数据库原理", teacher = "王教授", location = "教五楼201", dayOfWeek = 0, startSlot = 4, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "算法设计", teacher = "刘教授", location = "教二楼105", dayOfWeek = 1, startSlot = 0, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "软件工程", teacher = "陈教授", location = "教五楼502", dayOfWeek = 1, startSlot = 2, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "操作系统实验", teacher = "张教授", location = "实验楼A201", dayOfWeek = 2, startSlot = 0, slotCount = 2, type = CourseType.LAB, weekRange = "3-16周"),
                    Course(name = "网络实验", teacher = "李教授", location = "实验楼B102", dayOfWeek = 2, startSlot = 2, slotCount = 2, type = CourseType.LAB, weekRange = "5-18周"),
                    Course(name = "数据库原理", teacher = "王教授", location = "教五楼201", dayOfWeek = 3, startSlot = 0, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "人工智能导论", teacher = "王教授", location = "教五楼502", dayOfWeek = 3, startSlot = 2, slotCount = 2, type = CourseType.ELECTIVE, weekRange = "1-16周"),
                    Course(name = "编译原理", teacher = "赵教授", location = "教三楼205", dayOfWeek = 4, startSlot = 0, slotCount = 2, type = CourseType.REQUIRED, weekRange = "1-18周"),
                    Course(name = "职业规划", teacher = "陈老师", location = "教一楼101", dayOfWeek = 4, startSlot = 4, slotCount = 2, type = CourseType.CUSTOM, weekRange = "6-14周"),
                )
                samples.forEach { repository.insertCourse(it) }
            }
        }
    }

    fun getWeekDates(week: Int): List<String> {
        val sdf = SimpleDateFormat("M/d", Locale.getDefault())
        val cal = semesterStartDate.clone() as Calendar
        cal.add(Calendar.WEEK_OF_YEAR, week - 1)
        return (0..6).map { day ->
            val d = cal.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, day)
            sdf.format(d.time)
        }
    }

    fun selectWeek(week: Int) {
        _currentWeek.value = week
        viewModelScope.launch {
            _snackbarMessage.emit("已切换到第${week}周" to "success")
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        viewModelScope.launch {
            _snackbarMessage.emit(
                (if (_isDarkMode.value) "深色模式已开启" else "浅色模式已开启") to "info"
            )
        }
    }

    fun toggleFab() {
        _isFabOpen.value = !_isFabOpen.value
    }

    fun closeFab() {
        _isFabOpen.value = false
    }

    fun showMessage(message: String, type: String = "info") {
        viewModelScope.launch {
            _snackbarMessage.emit(message to type)
        }
    }

    fun onCourseClick(course: Course) {
        _selectedCourse.value = course
    }

    fun dismissCourseDetail() {
        _selectedCourse.value = null
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
            _selectedCourse.value = null
            _snackbarMessage.emit("课程已更新" to "success")
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            _selectedCourse.value = null
            _snackbarMessage.emit("已删除「${course.name}」" to "success")
        }
    }

    fun copyCourse(course: Course) {
        viewModelScope.launch {
            val copy = course.copy(id = 0, dayOfWeek = (course.dayOfWeek + 1) % 7)
            repository.insertCourse(copy)
            _selectedCourse.value = null
            _snackbarMessage.emit("已复制「${course.name}」" to "success")
        }
    }

    fun setDraggedCourse(course: Course) {
        _draggedCourse.value = course
    }

    fun clearDraggedCourse() {
        _draggedCourse.value = null
    }

    suspend fun getConflicts(course: Course, newDay: Int, newStartSlot: Int): List<Course> {
        val endSlot = newStartSlot + course.slotCount
        return repository.getConflictingCourses(course.semester, newDay, newStartSlot, endSlot)
            .filter { it.id != course.id }
    }

    fun moveCourseTo(course: Course, newDay: Int, newStartSlot: Int, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            val conflicts = getConflicts(course, newDay, newStartSlot)
            if (conflicts.isNotEmpty() && replaceExisting) {
                conflicts.forEach { repository.deleteCourse(it) }
            }
            val moved = course.copy(dayOfWeek = newDay, startSlot = newStartSlot)
            repository.updateCourse(moved)
            _draggedCourse.value = null
            _snackbarMessage.emit("已移动「${course.name}」" to "success")
        }
    }

    fun updateTimeSlots(slots: List<TimeSlot>) {
        _timeSlots.value = slots
    }

    fun timeSlotLabels(): List<String> {
        return _timeSlots.value.flatMap { listOf(it.start, it.end) }
    }

    fun isCourseActive(course: Course): Boolean {
        val now = Calendar.getInstance()
        val today = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (course.dayOfWeek != today) return false
        val slots = _timeSlots.value
        if (course.startSlot >= slots.size) return false
        val startParts = slots[course.startSlot].start.split(":")
        val endSlot = (course.startSlot + course.slotCount - 1).coerceAtMost(slots.size - 1)
        val endParts = slots[endSlot].end.split(":")
        val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endMin = endParts[0].toInt() * 60 + endParts[1].toInt()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return nowMin in startMin..endMin
    }

    fun getCourseProgress(course: Course): Float {
        val slots = _timeSlots.value
        if (course.startSlot >= slots.size) return 0f
        val startParts = slots[course.startSlot].start.split(":")
        val endSlot = (course.startSlot + course.slotCount - 1).coerceAtMost(slots.size - 1)
        val endParts = slots[endSlot].end.split(":")
        val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
        val endMin = endParts[0].toInt() * 60 + endParts[1].toInt()
        val now = Calendar.getInstance()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return ((nowMin - startMin).toFloat() / (endMin - startMin)).coerceIn(0f, 1f)
    }

    fun isCourseUpcoming(course: Course): Boolean {
        val now = Calendar.getInstance()
        val today = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
        if (course.dayOfWeek != today) return false
        val slots = _timeSlots.value
        if (course.startSlot >= slots.size) return false
        val startParts = slots[course.startSlot].start.split(":")
        val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return nowMin < startMin
    }

    fun getMinutesUntilCourse(course: Course): Int {
        val slots = _timeSlots.value
        if (course.startSlot >= slots.size) return 0
        val startParts = slots[course.startSlot].start.split(":")
        val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
        val now = Calendar.getInstance()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return (startMin - nowMin).coerceAtLeast(0)
    }

    companion object {
        fun defaultTimeSlots() = listOf(
            TimeSlot("08:00", "08:45"),
            TimeSlot("08:55", "09:40"),
            TimeSlot("10:00", "10:45"),
            TimeSlot("10:55", "11:40"),
            TimeSlot("14:00", "14:45"),
            TimeSlot("14:55", "15:40"),
            TimeSlot("16:00", "16:45"),
            TimeSlot("16:55", "17:40"),
            TimeSlot("19:00", "19:45"),
            TimeSlot("19:55", "20:40"),
        )
    }
}
