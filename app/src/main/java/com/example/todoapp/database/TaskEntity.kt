package com.example.todoapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks") // Имя таблицы в БД
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) // Автоматическая генерация ID
    val id: Long = 0,
    val title: String,          // Текст задачи
    val isCompleted: Boolean = false, // Статус выполнения
    val createdTime: Long = System.currentTimeMillis() // Время создания для сортировки
)