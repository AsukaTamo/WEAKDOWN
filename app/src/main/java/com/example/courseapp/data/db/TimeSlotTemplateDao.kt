package com.example.courseapp.data.db

import androidx.room.*
import com.example.courseapp.data.model.TimeSlotTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotTemplateDao {
    @Query("SELECT * FROM time_slot_templates ORDER BY id DESC")
    fun getAllTemplates(): Flow<List<TimeSlotTemplate>>

    @Query("SELECT * FROM time_slot_templates WHERE isActive = 1 LIMIT 1")
    fun getActiveTemplate(): Flow<TimeSlotTemplate?>

    @Query("SELECT * FROM time_slot_templates WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTemplateSync(): TimeSlotTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TimeSlotTemplate): Long

    @Update
    suspend fun updateTemplate(template: TimeSlotTemplate)

    @Delete
    suspend fun deleteTemplate(template: TimeSlotTemplate)

    @Query("UPDATE time_slot_templates SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE time_slot_templates SET isActive = 1 WHERE id = :id")
    suspend fun activateTemplate(id: Long)
}
