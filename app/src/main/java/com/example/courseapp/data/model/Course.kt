package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int, // 0=Mon .. 6=Sun
    val startSlot: Int, // 0-based start period
    val slotCount: Int = 2,
    val type: CourseType = CourseType.REQUIRED,
    val weekRange: String = "1-18周",
    val colorIndex: Int = 0,
    val semester: String = "2025-2026-2",
    // New fields
    val credits: Float = 0f,
    val notes: String = "",
    val examDate: String = "",
    val customColor: String = "" // hex color override, empty = use type default
)

enum class CourseType(val displayName: String, val colorHex: String) {
    REQUIRED("必修课", "#2196F3"),
    ELECTIVE("选修课", "#9C27B0"),
    LAB("实验课", "#4CAF50"),
    CUSTOM("自定义", "#FF9800")
}
