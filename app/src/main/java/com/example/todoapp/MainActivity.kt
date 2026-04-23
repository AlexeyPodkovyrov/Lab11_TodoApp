package com.example.todoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private var counter = 0
    private val tasks = mutableListOf<String>()
    private var enteredText = ""

    private lateinit var adapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textTaskCount: TextView

    // Регистрация ActivityResultLauncher для получения результата из DetailActivity
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            data?.let {
                if (it.hasExtra("edited_text")) {
                    //  Редактирование задачи
                    val position = it.getIntExtra("task_position", -1)
                    val newText = it.getStringExtra("edited_text")
                    if (position != -1 && newText != null) {
                        tasks[position] = newText
                        adapter.updateTask(position, newText)
                        updateTaskCount()
                        Toast.makeText(this, "Задача обновлена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Блок 1: Счётчик
        val textCounter = findViewById<TextView>(R.id.textCounter)
        val buttonIncrement = findViewById<Button>(R.id.buttonIncrement)
        val buttonResetCounter = findViewById<Button>(R.id.buttonResetCounter)

        // Блок 2: Поле ввода
        val editTextInput = findViewById<EditText>(R.id.editTextInput)
        val buttonShow = findViewById<Button>(R.id.buttonShow)
        val textEntered = findViewById<TextView>(R.id.textEntered)

        // Блок 3: ToDo список
        val editTextTask = findViewById<EditText>(R.id.editTextTask)
        val buttonAddTask = findViewById<Button>(R.id.buttonAddTask)
        val buttonDeleteLast = findViewById<Button>(R.id.buttonDeleteLast)
        recyclerView = findViewById(R.id.recyclerViewTasks)
        textTaskCount = findViewById(R.id.textTaskCount)

        // Настройка RecyclerView
        setupRecyclerView()

        // Восстановление состояния
        savedInstanceState?.let {
            counter = it.getInt("counter", 0)
            enteredText = it.getString("enteredText", "")
            it.getStringArrayList("tasks")?.let { list ->
                tasks.clear()
                tasks.addAll(list)
            }
        }

        // Обновление интерфейса
        updateCounterDisplay(textCounter)

        if (enteredText.isNotEmpty()) {
            textEntered.text = "${getString(R.string.label_entered)} $enteredText"
        }

        updateTasksDisplay()

        // Обработчики
        buttonIncrement.setOnClickListener {
            counter++
            updateCounterDisplay(textCounter)
        }

        buttonResetCounter.setOnClickListener {
            counter = 0
            updateCounterDisplay(textCounter)
        }

        buttonShow.setOnClickListener {
            val inputText = editTextInput.text.toString()
            enteredText = inputText
            textEntered.text = "${getString(R.string.label_entered)} $inputText"
        }

        buttonAddTask.setOnClickListener {
            val task = editTextTask.text.toString().trim()
            if (task.isNotBlank()) {
                tasks.add(task)
                adapter.notifyItemInserted(tasks.size - 1)
                updateTaskCount()
                editTextTask.text.clear()
                recyclerView.scrollToPosition(tasks.size - 1)
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.toast_empty_task, Toast.LENGTH_SHORT).show()
            }
        }

        buttonDeleteLast.setOnClickListener {
            if (tasks.isNotEmpty()) {
                val lastIndex = tasks.size - 1
                val deletedTask = tasks[lastIndex]
                tasks.removeAt(lastIndex)
                adapter.notifyItemRemoved(lastIndex)
                updateTaskCount()
                Toast.makeText(this, "Последняя задача удалена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Список задач пуст", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = tasks,
            onTaskCheckedChange = { position, isChecked ->
                updateTaskCount()
            },
            onItemClick = { position ->
                // Открытие DetailActivity для редактирования
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("task_text", tasks[position])
                intent.putExtra("task_position", position)
                editTaskLauncher.launch(intent)
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
                    val deletedTask = tasks[position]
                    tasks.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateTaskCount()

                    Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                        .setAction("Отмена") {
                            tasks.add(position, deletedTask)
                            adapter.notifyItemInserted(position)
                            updateTaskCount()
                        }
                        .show()
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun updateCounterDisplay(textView: TextView) {
        textView.text = getString(R.string.counter_text, counter)
    }

    private fun updateTasksDisplay() {
        updateTaskCount()
        adapter.notifyDataSetChanged()
    }

    private fun updateTaskCount() {
        textTaskCount.text = getString(R.string.label_task_count, tasks.size)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("counter", counter)
        outState.putString("enteredText", enteredText)
        outState.putStringArrayList("tasks", ArrayList(tasks))
    }
}