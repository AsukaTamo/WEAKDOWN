package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_slot_templates")
data class TimeSlotTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val slotsJson: String,  // JSON array of {"start":"08:00","end":"08:45"}
    val isActive: Boolean = false
)
