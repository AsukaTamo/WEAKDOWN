package com.example.courseapp.data.repository

import com.example.courseapp.data.db.TodoDao
import com.example.courseapp.data.model.Todo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor(private val dao: TodoDao) {
    val todos = dao.observeAll()
    suspend fun save(todo: Todo) {
        require(todo.title.isNotBlank()) { "请输入待办标题" }
        dao.save(todo.copy(title = todo.title.trim()))
    }
    suspend fun delete(todo: Todo) = dao.delete(todo)
    suspend fun setPinned(todo: Todo, pinned: Boolean) = dao.setPinned(todo.id, pinned)
    suspend fun setCompleted(todo: Todo, completed: Boolean) =
        dao.setCompleted(todo.id, if (completed) System.currentTimeMillis() else null)
}
