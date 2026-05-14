package com.example.courseapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.repository.CourseRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: CourseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userName = "张同学"
    val userSubtitle = "计算机科学与技术 · 2023级"
    val appVersion = "v2.4.1"

    val courses: StateFlow<List<Course>> = repository.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<Semester>> = repository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSemester: StateFlow<Semester?> = repository.getActiveSemester()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<Pair<String, String>>()
    val snackbarMessage: SharedFlow<Pair<String, String>> = _snackbarMessage.asSharedFlow()

    private val gson = Gson()

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    fun showMessage(message: String, type: String = "info") {
        viewModelScope.launch {
            _snackbarMessage.emit(message to type)
        }
    }

    // ── Semester management ──

    fun addSemester(id: String, name: String, startDate: String, totalWeeks: Int) {
        viewModelScope.launch {
            val semester = Semester(id = id, name = name, startDate = startDate, totalWeeks = totalWeeks)
            repository.insertSemester(semester)
            _snackbarMessage.emit("学期「$name」已添加" to "success")
        }
    }

    fun setActiveSemester(id: String) {
        viewModelScope.launch {
            repository.setActiveSemester(id)
            _snackbarMessage.emit("已切换学期" to "success")
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch {
            repository.deleteSemester(semester)
            _snackbarMessage.emit("已删除学期「${semester.name}」" to "success")
        }
    }

    // ── JSON export/import ──

    fun exportToJson(): String {
        val data = mapOf(
            "courses" to courses.value,
            "semesters" to semesters.value,
            "exportTime" to System.currentTimeMillis()
        )
        return gson.toJson(data)
    }

    fun saveExportFile(): String? {
        return try {
            val json = exportToJson()
            val file = File(context.getExternalFilesDir(null), "course_backup.json")
            file.writeText(json)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun importFromJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, type)
            val coursesJson = gson.toJson(data["courses"])
            val courseListType = object : TypeToken<List<Course>>() {}.type
            val importedCourses: List<Course> = gson.fromJson(coursesJson, courseListType)
            viewModelScope.launch {
                importedCourses.forEach { course ->
                    repository.insertCourse(course.copy(id = 0))
                }
                _snackbarMessage.emit("成功导入 ${importedCourses.size} 门课程" to "success")
            }
            true
        } catch (e: Exception) {
            viewModelScope.launch {
                _snackbarMessage.emit("导入失败：文件格式错误" to "error")
            }
            false
        }
    }

    fun importFromFile(file: File): Boolean {
        return try {
            val json = file.readText()
            importFromJson(json)
        } catch (e: Exception) {
            viewModelScope.launch {
                _snackbarMessage.emit("读取文件失败" to "error")
            }
            false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            courses.value.forEach { repository.deleteCourse(it) }
            _snackbarMessage.emit("已清空所有课程数据" to "success")
        }
    }
}
