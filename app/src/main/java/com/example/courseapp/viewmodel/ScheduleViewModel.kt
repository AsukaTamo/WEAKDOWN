package com.example.courseapp.viewmodel

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TimeSlot(val start: String, val end: String)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: CourseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val preferences = context.getSharedPreferences("schedule_settings", Context.MODE_PRIVATE)

    private val _allStoredCourses = MutableStateFlow<List<Course>>(emptyList())
    private val _allCourses = MutableStateFlow<List<Course>>(emptyList())
    val allCourses: StateFlow<List<Course>> = _allCourses.asStateFlow()

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _visibleWeek = MutableStateFlow(1)
    val visibleWeek: StateFlow<Int> = _visibleWeek.asStateFlow()

    private val _isDarkMode = MutableStateFlow(preferences.getBoolean(KEY_DARK_MODE, false))
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

    private val _backgroundUri = MutableStateFlow("")
    val backgroundUri: StateFlow<String> = _backgroundUri.asStateFlow()

    private val _scrimAlpha = MutableStateFlow(0.4f)
    val scrimAlpha: StateFlow<Float> = _scrimAlpha.asStateFlow()

    private val _showScheduleGuides = MutableStateFlow(
        preferences.getBoolean(KEY_SHOW_SCHEDULE_GUIDES, true)
    )
    val showScheduleGuides: StateFlow<Boolean> = _showScheduleGuides.asStateFlow()

    /** 学期第一天的 0 点；必须声明在使用它的 StateFlow 之前，否则构造期读到 null。 */
    private var semesterStartDate = Calendar.getInstance().apply {
        set(2025, Calendar.FEBRUARY, 24, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /**
     * 课表的"此刻"。每 [CLOCK_TICK_MILLIS] 刷新一次，页面订阅时才会跑，离开首页自动停止。
     * 所有进行中/即将开始/进度判断都以它为准，保证 UI 不依赖其它状态变化才会更新。
     */
    val clock: StateFlow<ScheduleClock> = flow {
        while (true) {
            emit(ScheduleClock.now())
            delay(CLOCK_TICK_MILLIS)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(CLOCK_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = ScheduleClock.now()
    )

    /**
     * 学期里【今天真正属于】的那一周，只跟日期走，不随翻页变化。
     * 进行中/即将开始的标记只在"正在查看的周 == 日历周"时显示，否则同一节课在每一周都会被点亮。
     */
    val calendarWeek: StateFlow<Int> = combine(clock, _activeSemester) { _, _ ->
        calculateCalendarWeek()
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(CLOCK_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = calculateCalendarWeek()
    )

    private fun calculateCalendarWeek(): Int = weekOfSemester(
        startDateMillis = semesterStartDate.timeInMillis,
        nowMillis = System.currentTimeMillis(),
        totalWeeks = _activeSemester.value?.totalWeeks ?: DEFAULT_SELECTABLE_WEEK_LIMIT
    )

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
                _backgroundUri.value = semester?.backgroundUri ?: ""
                _scrimAlpha.value = semester?.scrimAlpha ?: 0.4f
                if (semester != null) {
                    try {
                        val parts = semester.startDate.split("-")
                        semesterStartDate = Calendar.getInstance().apply {
                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        // 首次进入停留在"今天属于的那一周"
                        val calculatedWeek = calculateCalendarWeek()
                        _currentWeek.value = calculatedWeek
                        _visibleWeek.value = calculatedWeek
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            repository.getAllCourses().collect { list ->
                _allStoredCourses.value = list
            }
        }
        viewModelScope.launch {
            combine(_allStoredCourses, _activeSemester) { list, semester ->
                ScheduleCourseFilter.activeSemesterCourses(
                    courses = list,
                    activeSemester = semester,
                    defaultSemesterId = DEFAULT_SEMESTER_ID
                )
            }.collect { activeSemesterCourses ->
                _allCourses.value = activeSemesterCourses
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
                        val normalized = normalizeLoadedTimeSlots(slots)
                        if (normalized.isNotEmpty()) {
                            if (normalized != slots) {
                                repository.updateTimeSlotTemplate(
                                    template.copy(slotsJson = Gson().toJson(normalized))
                                )
                            }
                            _timeSlots.value = normalized
                        }
                    } catch (_: Exception) {}
                } else {
                    _timeSlots.value = defaultTimeSlots()
                }
            }
        }
    }

    fun saveTimeSlotTemplate(name: String, slots: List<TimeSlot>) {
        if (slots.isEmpty()) {
            viewModelScope.launch { _snackbarMessage.emit("至少保留一个时段" to "error") }
            return
        }
        viewModelScope.launch {
            val json = Gson().toJson(slots)
            // 新模板必须顶掉旧模板的激活状态，否则重启后可能加载到旧作息。
            repository.replaceActiveTimeSlotTemplate(name, json)
            _timeSlots.value = slots
            _snackbarMessage.emit("已保存作息表「$name」" to "success")
        }
    }

    /** 已保存的作息表列表（含当前激活项），供作息设置页切换。 */
    val timeSlotTemplates: StateFlow<List<TimeSlotTemplate>> =
        repository.getAllTimeSlotTemplates()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(CLOCK_SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    fun selectTimeSlotTemplate(template: TimeSlotTemplate) {
        viewModelScope.launch {
            repository.setActiveTimeSlotTemplate(template.id)
            _snackbarMessage.emit("已切换到「${template.name}」" to "success")
        }
    }

    /** 把当前编辑内容写回正在使用的作息表，不新建记录。 */
    fun updateActiveTimeSlotTemplate(slots: List<TimeSlot>) {
        if (slots.isEmpty()) {
            viewModelScope.launch { _snackbarMessage.emit("至少保留一个时段" to "error") }
            return
        }
        viewModelScope.launch {
            val active = repository.getActiveTimeSlotTemplateSync()
            if (active == null) {
                _snackbarMessage.emit("请先另存为一个作息表" to "error")
                return@launch
            }
            repository.updateTimeSlotTemplate(active.copy(slotsJson = Gson().toJson(slots)))
            _timeSlots.value = normalizeLoadedTimeSlots(slots)
            _snackbarMessage.emit("已保存到「${active.name}」" to "success")
        }
    }

    fun deleteTimeSlotTemplate(template: TimeSlotTemplate) {
        viewModelScope.launch {
            repository.deleteTimeSlotTemplate(template)
            _snackbarMessage.emit("已删除作息表「${template.name}」" to "success")
        }
    }

    private fun initSampleData() {
        // 示例数据只播种一次：清空数据后重启不应该再"复活"示范课程。
        if (preferences.getBoolean(KEY_SAMPLE_SEEDED, false)) return
        viewModelScope.launch {
            if (repository.getAllCourses().first().isEmpty()) {
                val semesterId = repository.getActiveSemesterSync()?.id
                    ?: DEFAULT_SEMESTER_ID
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
                ).map { it.copy(semester = semesterId) }
                samples.forEach { repository.insertCourse(it) }
            }
            // 无论本次是否真的插入，都把"示例数据已处理"记下来，避免清空数据后再次播种。
            preferences.edit().putBoolean(KEY_SAMPLE_SEEDED, true).apply()
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
        val maxWeek = _activeSemester.value?.totalWeeks?.coerceAtLeast(1)
            ?: DEFAULT_SELECTABLE_WEEK_LIMIT
        val boundedWeek = week.coerceIn(1, maxWeek)
        _currentWeek.value = boundedWeek
        _visibleWeek.value = boundedWeek
    }

    fun previewWeek(week: Int) {
        val maxWeek = _activeSemester.value?.totalWeeks?.coerceAtLeast(1)
            ?: DEFAULT_SELECTABLE_WEEK_LIMIT
        _visibleWeek.value = week.coerceIn(1, maxWeek)
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setDarkMode(enabled: Boolean) {
        if (_isDarkMode.value == enabled) return
        _isDarkMode.value = enabled
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
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

    fun setScheduleGuidesEnabled(enabled: Boolean) {
        _showScheduleGuides.value = enabled
        preferences.edit().putBoolean(KEY_SHOW_SCHEDULE_GUIDES, enabled).apply()
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
            _snackbarMessage.emit("已移动「${course.name}」" to "success")
        }
    }

    fun isCourseActive(course: Course, clock: ScheduleClock): Boolean =
        CourseTimeStatus.isActive(course, _timeSlots.value, clock.dayIndex, clock.minuteOfDay)

    fun getCourseProgress(course: Course, clock: ScheduleClock): Float =
        CourseTimeStatus.progress(course, _timeSlots.value, clock.minuteOfDay)

    fun isCourseUpcoming(course: Course, clock: ScheduleClock): Boolean =
        CourseTimeStatus.isUpcoming(course, _timeSlots.value, clock.dayIndex, clock.minuteOfDay)

    fun getMinutesUntilCourse(course: Course, clock: ScheduleClock): Int =
        CourseTimeStatus.minutesUntil(course, _timeSlots.value, clock.dayIndex, clock.minuteOfDay)

    fun getCourseRemainingMinutes(course: Course, clock: ScheduleClock): Int =
        CourseTimeStatus.minutesRemaining(course, _timeSlots.value, clock.minuteOfDay)

    companion object {
        const val DEFAULT_SEMESTER_ID = "2025-2026-2"
        const val DEFAULT_SELECTABLE_WEEK_LIMIT = 30
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SHOW_SCHEDULE_GUIDES = "show_schedule_guides"
        private const val KEY_SAMPLE_SEEDED = "sample_data_seeded"

        /** 课表"此刻"刷新间隔：15 秒足够让进度条和倒计时看起来是连续的，又不至于频繁重组。 */
        private const val CLOCK_TICK_MILLIS = 15_000L
        private const val CLOCK_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
        private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

        /** 开学日期到 now 属于第几周（1 基），早于开学按第 1 周、超出总周数按最后一周收敛。 */
        fun weekOfSemester(startDateMillis: Long, nowMillis: Long, totalWeeks: Int): Int {
            val diffWeeks = ((nowMillis - startDateMillis) / WEEK_MILLIS).toInt() + 1
            return diffWeeks.coerceIn(1, totalWeeks.coerceAtLeast(1))
        }

        fun defaultTimeSlots() = listOf(
            TimeSlot("08:30", "09:15"),
            TimeSlot("09:20", "10:05"),
            TimeSlot("10:15", "11:00"),
            TimeSlot("11:05", "11:50"),
            TimeSlot("14:00", "14:45"),
            TimeSlot("14:50", "15:35"),
            TimeSlot("15:45", "16:30"),
            TimeSlot("16:35", "17:20"),
            TimeSlot("18:30", "19:15"),
            TimeSlot("19:20", "20:05"),
            TimeSlot("20:15", "21:00"),
            TimeSlot("21:05", "21:50"),
        )

        fun legacyDefaultTimeSlots() = listOf(
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

        fun normalizeLoadedTimeSlots(slots: List<TimeSlot>): List<TimeSlot> {
            if (slots.isEmpty()) return defaultTimeSlots()
            return if (slots == legacyDefaultTimeSlots()) defaultTimeSlots() else slots
        }

        /**
         * Check if a given week number falls within a weekRange string.
         * Supports formats: "1-18周", "1-5,7-9周", "1-5(单),7-9周", "7-8,9-11(单),12-14,15-17周"
         */
        fun isWeekInRange(week: Int, weekRange: String): Boolean {
            if (weekRange.isEmpty()) return true
            val cleaned = weekRange.removeSuffix("周").trim()
            if (cleaned.isEmpty()) return true

            val segments = cleaned.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            for (segment in segments) {
                val m = Regex("(\\d+)(?:\\s*-\\s*(\\d+))?\\s*(?:\\((单|双)\\))?").find(segment)
                if (m != null) {
                    val start = m.groupValues[1].toIntOrNull() ?: continue
                    val end = m.groupValues[2].toIntOrNull() ?: start
                    val parity = m.groupValues[3]

                    if (week in start..end) {
                        when (parity) {
                            "单" -> if (week % 2 == 1) return true
                            "双" -> if (week % 2 == 0) return true
                            else -> return true
                        }
                    }
                }
            }
            return false
        }
    }
}
