package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey
    val id: String, // e.g. "2025-2026-2"
    val name: String, // e.g. "2025-2026学年 第二学期"
    val startDate: String, // "2025-02-24"
    val totalWeeks: Int = 18,
    val isActive: Boolean = false
)
