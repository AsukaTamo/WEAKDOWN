package com.example.courseapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.courseapp.data.model.Todo
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.TimeSlotTemplate
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteEditDocumentEntity
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.model.UNCATEGORIZED_CATEGORY_ID
import com.example.courseapp.data.model.uncategorizedCategory

@Database(
    entities = [
        Course::class,
        Semester::class,
        TimeSlotTemplate::class,
        Todo::class,
        NoteCategory::class,
        NotePhoto::class,
        NoteEditDocumentEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun courseDao(): CourseDao
    abstract fun semesterDao(): SemesterDao
    abstract fun timeSlotTemplateDao(): TimeSlotTemplateDao
    abstract fun noteDao(): NoteDao

    companion object {
        /**
         * 随手记三张表。分类保持独立，照片只存 categoryId；
         * 系统「未归类」在迁移时初始化，后续由 Repository 的 ensureUncategorized 兜底。
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        iconKey TEXT NOT NULL,
                        colorValue INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        isSystem INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_photos (
                        id TEXT NOT NULL PRIMARY KEY,
                        categoryId TEXT NOT NULL,
                        originalFileName TEXT NOT NULL,
                        originalRelativePath TEXT NOT NULL,
                        editedRelativePath TEXT,
                        previewRelativePath TEXT,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        capturedAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        mimeType TEXT NOT NULL,
                        originalRotation INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_photos_categoryId ON note_photos(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_photos_capturedAt ON note_photos(capturedAt)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_edit_documents (
                        photoId TEXT NOT NULL PRIMARY KEY,
                        cropLeft REAL NOT NULL,
                        cropTop REAL NOT NULL,
                        cropRight REAL NOT NULL,
                        cropBottom REAL NOT NULL,
                        rotation INTEGER NOT NULL,
                        textLayersJson TEXT NOT NULL,
                        strokesJson TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                val now = System.currentTimeMillis()
                val system = uncategorizedCategory(now)
                db.execSQL(
                    "INSERT OR IGNORE INTO note_categories " +
                        "(id, name, iconKey, colorValue, createdAt, updatedAt, sortOrder, isSystem) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        UNCATEGORIZED_CATEGORY_ID,
                        system.name,
                        system.iconKey,
                        system.colorValue,
                        now,
                        now,
                        system.sortOrder,
                        1
                    )
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        dueAt INTEGER,
                        notes TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        completedAt INTEGER,
                        imagePath TEXT
                    )
                """.trimIndent())
            }
        }
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
                val defaultSlots = """[{"start":"08:30","end":"09:15"},{"start":"09:20","end":"10:05"},{"start":"10:15","end":"11:00"},{"start":"11:05","end":"11:50"},{"start":"14:00","end":"14:45"},{"start":"14:50","end":"15:35"},{"start":"15:45","end":"16:30"},{"start":"16:35","end":"17:20"},{"start":"18:30","end":"19:15"},{"start":"19:20","end":"20:05"},{"start":"20:15","end":"21:00"},{"start":"21:05","end":"21:50"}]"""
                db.execSQL("INSERT INTO time_slot_templates (name, slotsJson, isActive) VALUES ('默认作息表', '$defaultSlots', 1)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE semesters ADD COLUMN backgroundUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE semesters ADD COLUMN scrimAlpha REAL NOT NULL DEFAULT 0.4")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val defaultSlots = """[{"start":"08:30","end":"09:15"},{"start":"09:20","end":"10:05"},{"start":"10:15","end":"11:00"},{"start":"11:05","end":"11:50"},{"start":"14:00","end":"14:45"},{"start":"14:50","end":"15:35"},{"start":"15:45","end":"16:30"},{"start":"16:35","end":"17:20"},{"start":"18:30","end":"19:15"},{"start":"19:20","end":"20:05"},{"start":"20:15","end":"21:00"},{"start":"21:05","end":"21:50"}]"""
                db.execSQL(
                    """
                        UPDATE time_slot_templates
                        SET slotsJson = '$defaultSlots'
                        WHERE name = '默认作息表'
                    """.trimIndent()
                )
            }
        }
    }
}
