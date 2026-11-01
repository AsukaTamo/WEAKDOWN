package com.example.courseapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val notes: String = "",
    val tags: String = "",
    val completedAt: Long? = null,
    val imagePath: String? = null,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false
)

fun parseTodoTags(tags: String): List<String> = tags.split(',', '，', ';', '；', '\n', '\r')
    .map { it.trim() }.filter { it.isNotEmpty() }.distinct()

fun isExamTodo(todo: Todo): Boolean = "考试" in parseTodoTags(todo.tags)

fun isTodoPinned(todo: Todo): Boolean = todo.isPinned || isExamTodo(todo)

enum class TodoGroup(val label: String) {
    PINNED("置顶"), OVERDUE("已逾期"), TODAY("今天"), UPCOMING("接下来"), NO_DATE("无截止日期")
}

private val deadlineOrder = Comparator<Todo> { first, second ->
    when {
        first.dueAt == null && second.dueAt == null -> second.createdAt.compareTo(first.createdAt)
        first.dueAt == null -> 1
        second.dueAt == null -> -1
        else -> first.dueAt.compareTo(second.dueAt).takeIf { it != 0 }
            ?: first.createdAt.compareTo(second.createdAt)
    }
}

fun groupTodos(
    todos: List<Todo>,
    now: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Map<TodoGroup, List<Todo>> {
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
    val groups = todos.filter { it.completedAt == null }.groupBy { todo ->
        val due = todo.dueAt
        when {
            isTodoPinned(todo) -> TodoGroup.PINNED
            due == null -> TodoGroup.NO_DATE
            due <= now -> TodoGroup.OVERDUE
            Instant.ofEpochMilli(due).atZone(zoneId).toLocalDate() == today -> TodoGroup.TODAY
            else -> TodoGroup.UPCOMING
        }
    }
    return TodoGroup.entries.mapNotNull { group ->
        groups[group]?.let { list ->
            group to if (group == TodoGroup.PINNED) list.sortedWith(
                compareByDescending<Todo> { isExamTodo(it) }.then(deadlineOrder)
            ) else list.sortedWith(deadlineOrder)
        }
    }.toMap()
}

fun sortCompletedTodos(todos: List<Todo>): List<Todo> = todos.filter { it.completedAt != null }
    .sortedWith(compareByDescending<Todo> { isExamTodo(it) }
        .thenByDescending { isTodoPinned(it) }.thenByDescending { it.completedAt })

fun todoTimeProgress(todo: Todo, now: Long): Float? {
    val due = todo.dueAt ?: return null
    if (due <= todo.createdAt) return 1f
    return ((now.toDouble() - todo.createdAt.toDouble()) /
        (due.toDouble() - todo.createdAt.toDouble())).toFloat().coerceIn(0f, 1f)
}
