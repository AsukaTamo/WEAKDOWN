package com.example.courseapp.data.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TodoTest {
    @Test fun completedDaysCountElapsedFullDaysAndClampFutureTimes() {
        assertEquals(0L, completedTodoDays(1000, 1000))
        assertEquals(0L, completedTodoDays(1000, 1000 + 86_400_000 - 1))
        assertEquals(1L, completedTodoDays(1000, 1000 + 86_400_000))
        assertEquals(0L, completedTodoDays(2000, 1000))
    }
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-01T04:00:00Z").toEpochMilli()
    private fun todo(id: String, due: Long?, created: Long = now - 1000) =
        Todo(id = id, title = id, dueAt = due, createdAt = created)

    @Test fun groupsUseLocalDateAndDeadlineBoundary() {
        val items = listOf(
            todo("next", Instant.parse("2026-11-01T16:00:00Z").toEpochMilli()),
            todo("today", now + 1000), todo("late", now - 1), todo("due", now),
            todo("no-date-old", null, now - 2000), todo("no-date-new", null),
            todo("done", now).copy(completedAt = now)
        )
        val groups = groupTodos(items, now, zone)
        assertEquals(TodoGroup.entries.filter { it != TodoGroup.PINNED }, groups.keys.toList())
        assertEquals(listOf("late", "due"), groups[TodoGroup.OVERDUE]!!.map { it.id })
        assertEquals(listOf("today"), groups[TodoGroup.TODAY]!!.map { it.id })
        assertEquals(listOf("next"), groups[TodoGroup.UPCOMING]!!.map { it.id })
        assertEquals(listOf("no-date-new", "no-date-old"), groups[TodoGroup.NO_DATE]!!.map { it.id })
    }

    @Test fun emptyGroupsAreOmitted() {
        assertTrue(groupTodos(emptyList(), now, zone).isEmpty())
    }

    @Test fun progressIsTimeConsumptionAndClamped() {
        val item = todo("x", now + 1000)
        assertEquals(0.5f, todoTimeProgress(item, now)!!, 0.0001f)
        assertEquals(0f, todoTimeProgress(item, now - 5000)!!, 0f)
        assertEquals(1f, todoTimeProgress(item, now + 5000)!!, 0f)
        assertNull(todoTimeProgress(item.copy(dueAt = null), now))
    }

    @Test fun invalidOrZeroDurationIsAlreadyDue() {
        assertEquals(1f, todoTimeProgress(todo("x", now, now), now)!!, 0f)
        assertEquals(1f, todoTimeProgress(todo("x", now - 1, now), now)!!, 0f)
    }

    @Test fun progressAvoidsLongOverflow() {
        val item = todo("x", Long.MAX_VALUE, Long.MIN_VALUE)
        assertEquals(0.5f, todoTimeProgress(item, 0)!!, 0.0001f)
    }
    @Test fun tagsAreTrimmedDistinctAndExamIsAnExactTag() {
        assertEquals(listOf("学习", "考试", "英语"), parseTodoTags(" 学习,考试，学习\n英语\r\n考试；英语; "))
        assertTrue(isExamTodo(todo("exam", null).copy(tags = "学习， 考试 \n英语")))
        assertFalse(isExamTodo(todo("not-exam", null).copy(tags = "期末考试,考试准备")))
        assertFalse(isExamTodo(todo("none", null)))
    }

    @Test fun pinsCrossTimeGroupsAndExamsHavePriority() {
        val examNoDate = todo("exam-no-date", null).copy(tags = "考试")
        val examDate = todo("exam-date", now + 10000).copy(tags = "考试")
        val manualLate = todo("pin-late", now - 10000).copy(isPinned = true)
        val manualNoDateOld = todo("pin-no-date-old", null, now - 5000).copy(isPinned = true)
        val manualNoDateNew = todo("pin-no-date-new", null).copy(isPinned = true)
        val normal = todo("late", now - 20000)
        val groups = groupTodos(listOf(manualNoDateOld, normal, examNoDate, manualLate, examDate, manualNoDateNew), now, zone)
        assertEquals(listOf(TodoGroup.PINNED, TodoGroup.OVERDUE), groups.keys.toList())
        assertEquals(listOf("exam-date", "exam-no-date", "pin-late", "pin-no-date-new", "pin-no-date-old"), groups[TodoGroup.PINNED]!!.map { it.id })
        assertEquals(listOf("late"), groups[TodoGroup.OVERDUE]!!.map { it.id })
    }

    @Test fun manualAndAutomaticPinsAreIndependent() {
        val manual = todo("manual", now + 1000).copy(isPinned = true)
        assertTrue(isTodoPinned(manual))
        assertFalse(isTodoPinned(manual.copy(isPinned = false)))
        assertEquals(TodoGroup.TODAY, groupTodos(listOf(manual.copy(isPinned = false)), now, zone).keys.single())
        val exam = manual.copy(tags = "考试")
        assertTrue(isTodoPinned(exam.copy(isPinned = false)))
        assertTrue(isTodoPinned(exam.copy(tags = "")))
        assertFalse(isTodoPinned(exam.copy(tags = "", isPinned = false)))
    }

    @Test fun completedSortsExamThenManualThenNewestCompletion() {
        val items = listOf(
            todo("normal-new", null).copy(completedAt = now),
            todo("manual", null).copy(completedAt = now - 10000, isPinned = true),
            todo("exam-old", null).copy(completedAt = now - 20000, tags = "考试"),
            todo("exam-new", null).copy(completedAt = now - 10000, tags = "考试"),
            todo("normal-old", null).copy(completedAt = now - 1000),
            todo("unfinished", null).copy(tags = "考试")
        )
        assertEquals(listOf("exam-new", "exam-old", "manual", "normal-new", "normal-old"), sortCompletedTodos(items).map { it.id })
    }

    @Test fun migrationAddsPinDefaultWithoutRecreatingTables() {
        val statements = mutableListOf<String>()
        val db = java.lang.reflect.Proxy.newProxyInstance(
            androidx.sqlite.db.SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") { statements += args!![0] as String; null }
            else throw UnsupportedOperationException(method.name)
        } as androidx.sqlite.db.SupportSQLiteDatabase
        com.example.courseapp.data.db.AppDatabase.MIGRATION_6_7.migrate(db)
        assertEquals(listOf("ALTER TABLE todos ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"), statements)
        assertFalse(Todo(title = "旧构造默认不置顶").isPinned)
        assertFalse(Todo("id", "title", now, null, "", "", null, null).isPinned)
    }
}
