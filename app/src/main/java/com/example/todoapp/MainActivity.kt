package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var adapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textTaskCount: TextView

    // Для получения результата из DetailActivity
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            data?.let {
                if (it.hasExtra("edited_text")) {
                    val position = it.getIntExtra("task_position", -1)
                    val newText = it.getStringExtra("edited_text")
                    if (position != -1 && newText != null) {
                        viewModel.updateTask(position, newText)
                        Toast.makeText(this, "Задача обновлена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Блок 1: Счётчик и другие элементы
        val textCounter = findViewById<TextView>(R.id.textCounter)
        val buttonIncrement = findViewById<Button>(R.id.buttonIncrement)
        val buttonResetCounter = findViewById<Button>(R.id.buttonResetCounter)
        val editTextInput = findViewById<EditText>(R.id.editTextInput)
        val buttonShow = findViewById<Button>(R.id.buttonShow)
        val textEntered = findViewById<TextView>(R.id.textEntered)

        // Блок 2: ToDo список
        val editTextTask = findViewById<EditText>(R.id.editTextTask)
        val buttonAddTask = findViewById<Button>(R.id.buttonAddTask)
        val buttonDeleteLast = findViewById<Button>(R.id.buttonDeleteLast)
        recyclerView = findViewById(R.id.recyclerViewTasks)
        textTaskCount = findViewById(R.id.textTaskCount)

        // Настройка RecyclerView
        setupRecyclerView()

        // Подписка на счетчик
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.counter.collect { count ->
                    textCounter.text = getString(R.string.counter_text, count)
                }
            }
        }

        // Подписка на введенный текст
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.enteredText.collect { text ->
                    if (text.isNotEmpty()) {
                        textEntered.text = "${getString(R.string.label_entered)} $text"
                    } else {
                        textEntered.text = getString(R.string.label_entered)
                    }
                }
            }
        }

        // Подписка на изменения списка задач
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collect { tasks ->
                    val completed = viewModel.completedTasks.value
                    adapter.updateData(tasks, completed)
                    updateTaskCount(tasks.size)
                }
            }
        }

        // Подписка на изменения выполненных задач (чекбоксы)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedTasks.collect { completed ->
                    val tasks = viewModel.tasks.value
                    adapter.updateData(tasks, completed)
                }
            }
        }

        // Загрузка тестовых данных
        viewModel.loadTestData()

        // Обработчики кнопок
        buttonIncrement.setOnClickListener {
            viewModel.incrementCounter()
        }

        buttonResetCounter.setOnClickListener {
            viewModel.resetCounter()
        }

        buttonShow.setOnClickListener {
            val inputText = editTextInput.text.toString()
            viewModel.updateEnteredText(inputText)
        }

        buttonAddTask.setOnClickListener {
            val task = editTextTask.text.toString().trim()
            if (task.isNotBlank()) {
                viewModel.addTask(task)
                editTextTask.text.clear()
                recyclerView.scrollToPosition(viewModel.tasks.value.size - 1)
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.toast_empty_task, Toast.LENGTH_SHORT).show()
            }
        }

        // Удаление последней задачи
        buttonDeleteLast.setOnClickListener {
            val currentTasks = viewModel.tasks.value
            if (currentTasks.isNotEmpty()) {
                val lastIndex = currentTasks.size - 1
                val (deletedTask, wasCompleted) = viewModel.deleteTask(lastIndex)

                // Snackbar для отмены
                Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        viewModel.restoreTask(lastIndex, deletedTask, wasCompleted)
                    }
                    .show()
            } else {
                Toast.makeText(this, "Список задач пуст", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = emptyList(),
            onItemClick = { position ->
                val taskText = viewModel.tasks.value[position]
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("task_text", taskText)
                intent.putExtra("task_position", position)
                intent.putExtra("task_number", position + 1)
                editTaskLauncher.launch(intent)
            },
            onItemLongClick = { position ->

                // Удаление и получение задачи и её состояния
                val (deletedTask, wasCompleted) = viewModel.deleteTask(position)

                Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {

                        // Восстановление с сохранением состояния
                        viewModel.restoreTask(position, deletedTask, wasCompleted)
                    }
                    .show()
            },
            onTaskCheckedChange = { task, isChecked ->
                viewModel.toggleTaskCompletion(task, isChecked)
            }
        )

        recyclerView.adapter = adapter

        // Удаление свайпом
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val (deletedTask, wasCompleted) = viewModel.deleteTask(position)

                    Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                        .setAction("Отмена") {
                            viewModel.restoreTask(position, deletedTask, wasCompleted)
                        }
                        .show()
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun updateTaskCount(count: Int) {
        textTaskCount.text = getString(R.string.label_task_count, count)
    }
}