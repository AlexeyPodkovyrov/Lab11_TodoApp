<p align="center">
  МИНИСТЕРСТВО НАУКИ И ВЫСШЕГО ОБРАЗОВАНИЯ РОССИЙСКОЙ ФЕДЕРАЦИИ ФЕДЕРАЛЬНОЕ<br>
  ГОСУДАРСТВЕННОЕ БЮДЖЕТНОЕ ОБРАЗОВАТЕЛЬНОЕ УЧРЕЖДЕНИЕ ВЫСШЕГО ОБРАЗОВАНИЯ<br>
  «САХАЛИНСКИЙ ГОСУДАРСТВЕННЫЙ УНИВЕРСИТЕТ»
</p>

<br><br><br>

<p align="center">
  Институт естественных наук и техносферной безопасности<br>
  Кафедра информатики<br>
  Подковыров Алексей Игоревич
</p>

<br><br><br>

<p align="center">
  Лабораторная работа №8<br>
  «Перенос логики списка задач из Activity в ViewModel. Использование StateFlow для хранения состояния».<br>
  01.03.02 Прикладная математика и информатика
</p>

<br><br><br><br><br><br><br><br><br><br>

<p align="right">
  Научный руководитель<br>
  Соболев Евгений Игоревич
</p>

<br><br><br><br>

<p align="center">
  г. Южно-Сахалинск<br>
  2026 г.
</p>

---

## Цель работы

Изучить архитектурный компонент ViewModel, научиться выносить логику и состояние UI из Activity, использовать StateFlow для реактивного обновления данных, обеспечить сохранение состояния при изменении конфигурации.

---

## Листинг класса `MainViewModel.kt`

```kotlin
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
```

---

## Листинг обновленного `MainActivity.kt`

```kotlin
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
```

---

## Листинг обновленного `TaskAdapter.kt`

```kotlin
package com.example.todoapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<String>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit,
    private val onTaskCheckedChange: (String, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Состояние чекбоксов
    private var completedTasks = emptySet<String>()

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val textTask: TextView = itemView.findViewById(R.id.textTask)
        val checkTask: CheckBox = itemView.findViewById(R.id.checkTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isChecked = task in completedTasks

        holder.textTask.text = task

        // Цвет карточки по четности
        if (position % 2 == 0) {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
        } else {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.light_gray)
            )
        }

        // Перечеркивание текста
        if (isChecked) {
            holder.textTask.paintFlags = holder.textTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.textTask.alpha = 0.6f
        } else {
            holder.textTask.paintFlags = holder.textTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.textTask.alpha = 1.0f
        }

        // Чекбокс
        holder.checkTask.setOnCheckedChangeListener(null)
        holder.checkTask.isChecked = isChecked
        holder.checkTask.setOnCheckedChangeListener { _, isCheckedNow ->

            // Обновление локального состояния
            completedTasks = if (isCheckedNow) {
                completedTasks + task
            } else {
                completedTasks - task
            }

            // Уведомление ViewModel
            onTaskCheckedChange(task, isCheckedNow)

            // Обновление перечеркивания
            if (isCheckedNow) {
                holder.textTask.paintFlags = holder.textTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.textTask.alpha = 0.6f
            } else {
                holder.textTask.paintFlags = holder.textTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.textTask.alpha = 1.0f
            }
        }

        // Клик по карточке
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }

        // Долгое нажатие
        holder.itemView.setOnLongClickListener {
            onItemLongClick(position)
            true
        }
    }

    override fun getItemCount(): Int = tasks.size

    // Обновление данных адаптера
    fun updateData(newTasks: List<String>, newCompletedTasks: Set<String>) {
        tasks = newTasks
        completedTasks = newCompletedTasks.toMutableSet()
        notifyDataSetChanged()
    }
}
```

---

## Листинг `MainActivity.kt` с изменениями

```kotlin
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
```

---

## Скриншоты работающего приложения

### Список задач до поворота

![Список задач до поворота](<Снимок экрана 2026-05-07 210838.png>)


### Список задач после поворота

![Список задач после поворота](<Снимок экрана 2026-05-07 211015.png>)

---

## Ответы на контрольные вопросы

**1. Для чего нужен `ViewModel`? Как он помогает при повороте экрана?**

 `ViewModel` - это компонент архитектуры Android, который хранит данные, связанные с UI. Он способен переживать изменения конфигурации, например повороты экрана, что обеспечивает сохранность данных.

 При повороте экрана происходит следующее:

  * `Activity` пересоздается, но `ViewModel` остается в памяти. 
  * Данные (счетчик, список задач, состояние чекбоксов) не теряются.
  * После пересоздания `Activity` подписывается на тот же `ViewModel` и получает актуальные данные.

*Пример*:

```kotlin
private val viewModel: MainViewModel by viewModels()
// При повороте ViewModel остается, данные сохраняются
```

<br>

**2. Чем `StateFlow` отличается от `LiveData`? В каких случаях предпочтительнее использовать `StateFlow`?**

|Характеристика          |LiveData                             |StateFlow                                           |
|------------------------|-------------------------------------|----------------------------------------------------|
|**Библиотека**          |`androidx.lifecycle`                 |`kotlinx.coroutines`                                |
|**Язык**                |Java + Kotlin                        |Только Kotlin                                       |
|**Начальное значение**  |Не требуется                         |Обязательно при создании                            |
|**Жизненный цикл**      |Знает о Lifecycle                    |Нужен `repeatOnLifecycle`                           |
|**Операторы преобразования**|Ограниченные (`map`, `switchMap`)|Все операторы Flow (`filter`, `debounce`, `combine`)|
|**Корутины**            |Не поддерживает                      |Поддерживает                                        |

`StateFlow` предпочтительнее использовать в следующих случаях:

* Проект написан на Kotlin с использованием корутин.
* Нужны сложные преобразования потока данных.
* Требуется доступ к текущему значению через `.value`.

<br>

**3. Что такое `lifecycleScope` и `repeatOnLifecycle`? Зачем они нужны при подписке на `StateFlow`?**

`lifecycleScope` - это CoroutineScope, привязанный к жизненному циклу Activity/Fragment. Корутины, запущенные в этом scope, автоматически отменяются при уничтожении компонента.

*Пример:*

```kotlin
lifecycleScope.launch {
    // Эта корутина отменится, когда Activity будет уничтожена
    viewModel.tasks.collect { ... }
}
```

`repeatOnLifecycle` - это функция, которая запускает блок кода только когда жизненный цикл находится в определенном состоянии (например, `STARTED`), и приостанавливает его, когда компонент уходит в фон.

*Пример:*

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Коллектируем данные, только когда экран виден пользователю
        viewModel.tasks.collect { tasks ->
            adapter.updateData(tasks)
        }
    }
}
```
При подписке на `StateFlow` они необходимы для:

* **Экономии ресурсов** - когда приложение в фоне, поток данных не собирается.
* **Предотвращения утечек** - корутина автоматически приостанавливается/возобновляется.

Без `repeatOnLifecycle` сбор данных шел бы постоянно, даже когда `Activity` невидима.

<br>

**4. Как обновить данные в `StateFlow`?**

`StateFlow` сам по себе неизменяемый. Для обновления используется приватный `MutableStateFlow`, а наружу предоставляется `asStateFlow()`.

*Пример*:

```kotlin
class MyViewModel : ViewModel() {
    // Приватный изменяемый поток
    private val _tasks = MutableStateFlow<List<String>>(emptyList())
    
    // Публичный неизменяемый поток для UI
    val tasks: StateFlow<List<String>> = _tasks.asStateFlow()
    
    // Метод обновления данных
    fun addTask(newTask: String) {
        // Создание копии списка, модифицикация и присвоение нового значения
        _tasks.value = _tasks.value.toMutableList().apply { add(newTask) }
    }
}
```

**Ключевые особенности**:

* `MutableStateFlow` - `private`, доступен только внутри `ViewModel`.
* `StateFlow` - public, доступен для подписки из UI.
* Обновление через присваивание нового значения `_tasks.value = newList`.
* Для `List` обязательно создавать копию (`toMutableList()`), так как Flow сравнивает ссылки.

<br>

**5. Какие преимущества даёт вынос логики в `ViewModel` с точки зрения тестирования?**

С точки зрения тестирования, вынос логики в `ViewModel` дает следующие преимущества:

* **Изоляция от Android-зависимостей**:

    ```kotlin
    class MainViewModelTest {
        private lateinit var viewModel: MainViewModel

        @Test
        fun testAddTask() {
            viewModel.addTask("Сделать ЛР8")
            assertEquals(listOf("Сделать ЛР8"), viewModel.tasks.value)
        }

        @Test
        fun testDeleteTask() {
            viewModel.addTask("Задача 1")
            viewModel.deleteTask(0)
            assertEquals(emptyList(), viewModel.tasks.value)
        }
    }
    ```

* **Разделение ответственности** - UI занимается только отображением.
* **Переиспользование** - одна `ViewModel` для разных экранов (планшет/телефон).
* **Устойчивость** - данные не теряются при поворотах.
* **Поддержка `StateFlow`** - реактивное обновление данных.

`ViewModel` превращает логику в чистые функции, которые легко тестировать автоматически, без UI-тестов и эмулятора.

---

## Вывод

В ходе выполнения лабораторной работы №8 изучил архитектурный компонент `ViewModel` и реактивные потоки `StateFlow` для хранения состояния UI.

Выполняя задание, создал класс `MainViewModel.kt`, где вынес хранение счетчика, введенного текста, списка задач и состояния чекбоксов с помощью `MutableStateFlow`. В файле `TaskAdapter.kt` добавил колбэк `onTaskCheckedChange` для передачи изменений в `ViewModel` и метод `updateData()` для обновления данных адаптера.

В `MainActivity.kt` заменил прямое управление списком задач на использование `ViewModel` с помощью `by viewModels()`. Реализовал подписку на изменения `counter`, `enteredText`, `tasks` и `completedTasks` с помощью `lifecycleScope` и `repeatOnLifecycle`, благодаря чему данные сохраняются при повороте экрана.

Помимо этого, реализовал хранение состояния чекбоксов в `ViewModel` с помощью `StateFlow<Set<String>>`. Исправил проблему восстановления состояния чекбокса при отмене удаления задачи: метод `deleteTask()` возвращает задачу и её состояние, а `restoreTask()` восстанавливает их вместе.

Результат работы **успешно** протестирован на эмуляторе.