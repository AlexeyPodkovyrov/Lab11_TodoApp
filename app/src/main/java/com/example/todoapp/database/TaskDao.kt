package com.example.todoapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Все задачи с сортировкой
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdTime DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Поиск задачи по названию
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' ORDER BY isCompleted ASC, createdTime DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

}