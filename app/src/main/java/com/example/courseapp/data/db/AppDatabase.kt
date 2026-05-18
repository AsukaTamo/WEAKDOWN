package com.example.courseapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.TimeSlotTemplate

@Database(entities = [Course::class, Semester::class, TimeSlotTemplate::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun semesterDao(): SemesterDao
    abstract fun timeSlotTemplateDao(): TimeSlotTemplateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN credits REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE courses ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE courses ADD COLUMN examDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE courses ADD COLUMN customColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS semesters (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        totalWeeks INTEGER NOT NULL DEFAULT 18,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO semesters (id, name, startDate, totalWeeks, isActive) VALUES ('2025-2026-2', '2025-2026学年 第二学期', '2025-02-24', 18, 1)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_slot_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        slotsJson TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // Insert default template
                val defaultSlots = """[{"start":"08:00","end":"08:45"},{"start":"08:55","end":"09:40"},{"start":"10:00","end":"10:45"},{"start":"10:55","end":"11:40"},{"start":"14:00","end":"14:45"},{"start":"14:55","end":"15:40"},{"start":"16:00","end":"16:45"},{"start":"16:55","end":"17:40"},{"start":"19:00","end":"19:45"},{"start":"19:55","end":"20:40"}]"""
                db.execSQL("INSERT INTO time_slot_templates (name, slotsJson, isActive) VALUES ('默认作息表', '$defaultSlots', 1)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE semesters ADD COLUMN backgroundUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE semesters ADD COLUMN scrimAlpha REAL NOT NULL DEFAULT 0.4")
            }
        }
    }
}
