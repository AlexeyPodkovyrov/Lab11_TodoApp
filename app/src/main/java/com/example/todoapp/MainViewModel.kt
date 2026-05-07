package com.example.todoapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    // Счетчик
    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    // Введенный текст
    private val _enteredText = MutableStateFlow("")
    val enteredText: StateFlow<String> = _enteredText.asStateFlow()

    // Список задач
    private val _tasks = MutableStateFlow<List<String>>(emptyList())
    val tasks: StateFlow<List<String>> = _tasks.asStateFlow()

    // Выполненные задачи
    private val _completedTasks = MutableStateFlow<Set<String>>(emptySet())
    val completedTasks: StateFlow<Set<String>> = _completedTasks.asStateFlow()

    // Добавление задачи
    fun addTask(task: String) {
        val currentList = _tasks.value.toMutableList()
        currentList.add(task)
        _tasks.value = currentList
    }

    // Обновление задачи
    fun updateTask(index: Int, newText: String) {
        val currentList = _tasks.value.toMutableList()
        if (index in currentList.indices) {
            val oldText = currentList[index]
            currentList[index] = newText
            _tasks.value = currentList

            // Обновление текста в выполненных задачах
            if (oldText in _completedTasks.value) {
                _completedTasks.value = (_completedTasks.value - oldText) + newText
            }
        }
    }

    // Удаление задачи по индексу и возвращение с состоянием до удаления
    fun deleteTask(index: Int): Pair<String, Boolean> {
        val currentList = _tasks.value.toMutableList()
        if (index in currentList.indices) {
            val deletedTask = currentList[index]
            val wasCompleted = deletedTask in _completedTasks.value  // запоминание состояния задачи
            currentList.removeAt(index)
            _tasks.value = currentList

            // Если была выполнена, удаляется
            if (wasCompleted) {
                _completedTasks.value -= deletedTask
            }
            return Pair(deletedTask, wasCompleted)  // возвращение задачи и её состояния
        }
        return Pair("", false)
    }

    // Восстановление удаленной задачи с сохранением состояния
    fun restoreTask(index: Int, task: String, wasCompleted: Boolean) {
        val currentList = _tasks.value.toMutableList()
        if (index <= currentList.size) {
            currentList.add(index, task)
            _tasks.value = currentList

            // Восстановление состояния чекбокса
            if (wasCompleted) {
                _completedTasks.value += task
            }
        }
    }

    // Чекбокс задачи
    fun toggleTaskCompletion(task: String, isCompleted: Boolean) {
        if (isCompleted) {
            _completedTasks.value += task
        } else {
            _completedTasks.value -= task
        }
    }

    // Загрузка тестовых данных
    fun loadTestData() {
        if (_tasks.value.isEmpty()) {
            _tasks.value = listOf(
                "Выполнить ЛР8",
                "Сделать отчет по работе",
                "Подключиться на пару",
                "Сдать работу"
            )
        }
    }

    // Счетчик
    fun incrementCounter() {
        _counter.value += 1
    }

    // Сброс счетчика
    fun resetCounter() {
        _counter.value = 0
    }

    // Введенный текст
    fun updateEnteredText(text: String) {
        _enteredText.value = text
    }
}