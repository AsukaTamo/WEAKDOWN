package com.example.courseapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.model.Todo
import com.example.courseapp.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(private val repository: TodoRepository) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val loadVersion = MutableStateFlow(0)
    val todos: StateFlow<List<Todo>> = loadVersion.flatMapLatest {
        repository.todos
        .catch { cause ->
            if (cause is CancellationException) throw cause
            _error.value = "待办加载失败，请重试"
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retry() {
        clearError()
        loadVersion.value += 1
    }

    fun clearError() { _error.value = null }

    fun save(todo: Todo, onSaved: () -> Unit = {}) = perform("保存失败，请重试") {
        repository.save(todo)
        onSaved()
    }

    fun delete(todo: Todo, onDeleted: () -> Unit = {}) = perform("删除失败，请重试") {
        repository.delete(todo)
        onDeleted()
    }

    fun setPinned(todo: Todo, pinned: Boolean, onUpdated: () -> Unit = {}) = perform("更新失败，请重试") {
        repository.setPinned(todo, pinned)
        onUpdated()
    }

    fun setCompleted(todo: Todo, completed: Boolean, onUpdated: () -> Unit = {}) = perform("更新失败，请重试") {
        repository.setCompleted(todo, completed)
        onUpdated()
    }

    private fun perform(message: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _error.value = if (cause is IllegalArgumentException) cause.message ?: message else message
            }
        }
    }
}
