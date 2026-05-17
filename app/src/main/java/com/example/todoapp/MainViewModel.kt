package com.example.todoapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    // Счётчик
    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    // Введённый текст
    private val _enteredText = MutableStateFlow("")
    val enteredText: StateFlow<String> = _enteredText.asStateFlow()

    // Текст поиска
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Получение задач через репозиторий
    val tasks: StateFlow<List<TaskEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllTasks()
        } else {
            repository.searchTasks(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.addTask(title)
        }
    }

    fun deleteTask(task: TaskEntity): TaskEntity {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
        return task
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.restoreTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task, isCompleted)
        }
    }

    fun loadTestData() {
        viewModelScope.launch {
            val currentTasks = repository.getAllTasks().first()
            if (currentTasks.isEmpty()) {
                val testTasks = listOf(
                    "Приступить к следующей",
                    "Сдать работу",
                    "Подключиться на пару",
                    "Сделать отчет по работе",
                    "Выполнить ЛР 11"
                )
                testTasks.forEach { repository.addTask(it) }
            }
        }
    }

    fun incrementCounter() {
        _counter.value += 1
    }

    fun resetCounter() {
        _counter.value = 0
    }

    fun updateEnteredText(text: String) {
        _enteredText.value = text
    }
}