package com.example.todoapp.data.repository

import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

// Репозиторий хранит данные в памяти, не в БД
class InMemoryTaskRepository : TaskRepository {

    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllTasks(): Flow<List<TaskEntity>> = _tasks.asStateFlow()

    override suspend fun addTask(title: String) {
        val newTask = TaskEntity(
            id = nextId++,
            title = title,
            isCompleted = false,
            createdTime = System.currentTimeMillis()
        )
        _tasks.value = _tasks.value + newTask
    }

    override suspend fun deleteTask(task: TaskEntity) {
        _tasks.value = _tasks.value.filter { it.id != task.id }
    }

    override suspend fun updateTask(task: TaskEntity) {
        _tasks.value = _tasks.value.map {
            if (it.id == task.id) task else it
        }
    }

    override suspend fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        updateTask(task.copy(isCompleted = isCompleted))
    }

    override suspend fun deleteAllTasks() {
        _tasks.value = emptyList()
    }

    override suspend fun restoreTask(task: TaskEntity) {
        _tasks.value = _tasks.value + task
    }

    override suspend fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return _tasks.asStateFlow().map { tasks ->
            if (query.isBlank()) {
                tasks
            } else {
                tasks.filter { it.title.contains(query, ignoreCase = true) }
            }
        }
    }
}