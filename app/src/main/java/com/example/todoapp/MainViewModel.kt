package com.example.todoapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.database.AppDatabase
import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val taskDao = database.taskDao()

    // Счётчик
    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    // Введённый текст
    private val _enteredText = MutableStateFlow("")
    val enteredText: StateFlow<String> = _enteredText.asStateFlow()

    // Текст поиска
    private val _searchQuery = MutableStateFlow("")

    // Результат поиска (если запрос пустой - все задачи, иначе поиск по запросу)
    val tasks: StateFlow<List<TaskEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            taskDao.getAllTasks()
        } else {
            taskDao.searchTasks(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            val task = TaskEntity(title = title)
            taskDao.insertTask(task)
        }
    }

    fun deleteTask(task: TaskEntity): TaskEntity {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
        return task
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = isCompleted)
            taskDao.updateTask(updatedTask)
        }
    }

    fun loadTestData() {
        viewModelScope.launch {
            val currentTasks = taskDao.getAllTasks().first()
            if (currentTasks.isEmpty()) {
                val testTasks = listOf(
                    TaskEntity(title = "Выполнить ЛР 10"),
                    TaskEntity(title = "Сделать отчет по работе"),
                    TaskEntity(title = "Подключиться на пару"),
                    TaskEntity(title = "Сдать работу"),
                    TaskEntity(title = "Приступить к следующей")
                )
                testTasks.forEach { taskDao.insertTask(it) }
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