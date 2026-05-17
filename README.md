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
  Лабораторная работа №11<br>
  «Рефакторинг: добавление слоя Repository между ViewModel и Room».<br>
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

Изучить архитектурный паттерн Repository, научиться выделять слой доступа к данным, отделяя его от бизнес-логики, выполнить рефакторинг существующего приложения для использования репозитория.

---

## Листинг интерфейса `TaskRepository.kt`

```kotlin
package com.example.todoapp.data.repository

import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    suspend fun addTask(title: String)
    suspend fun deleteTask(task: TaskEntity)
    suspend fun updateTask(task: TaskEntity)
    suspend fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean)
    suspend fun deleteAllTasks()
    suspend fun restoreTask(task: TaskEntity)
    suspend fun searchTasks(query: String): Flow<List<TaskEntity>>
}
```

---

## Листинг класса `TaskRepositoryImpl.kt`

```kotlin
package com.example.todoapp.data.repository

import com.example.todoapp.database.TaskDao
import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override suspend fun addTask(title: String) {
        val task = TaskEntity(title = title)
        taskDao.insertTask(task)
    }

    override suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    override suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    override suspend fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        val updatedTask = task.copy(isCompleted = isCompleted)
        taskDao.updateTask(updatedTask)
    }

    override suspend fun deleteAllTasks() {
        taskDao.deleteAll()
    }

    override suspend fun restoreTask(task: TaskEntity) {
        taskDao.insertTask(task)  // вставляем задачу как есть (с сохранением состояния)
    }

    override suspend fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchTasks(query)
    }
}
```

---

## Листинг интерфейса `InMemoryTaskRepository.kt` (индивидуальное задание 1)

```kotlin
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
```

---

## Листинг обновленного файла `MainViewModel.kt`

```kotlin
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

    // Получаем задачи через репозиторий
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
```

---

## Листинг обновленного файла `MainViewModelFactory.kt`

```kotlin
package com.example.todoapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todoapp.data.repository.TaskRepository

class MainViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## Листинг обновленного файла `MainActivity.kt`

```kotlin
package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.todoapp.data.repository.InMemoryTaskRepository
import com.google.android.material.snackbar.Snackbar
import com.example.todoapp.data.repository.TaskRepositoryImpl
import com.example.todoapp.database.AppDatabase
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    // База данных
    private val database by lazy { AppDatabase.getInstance(this) }

    // Репозиторий в БД
    private val repository by lazy { TaskRepositoryImpl(database.taskDao()) }

    // Репозиторий в памяти
    //private val repository by lazy { InMemoryTaskRepository() }

    // ViewModel с фабрикой
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

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
                if (it.hasExtra("edited_text") && it.hasExtra("task_id")) {
                    val taskId = it.getLongExtra("task_id", -1)
                    val newText = it.getStringExtra("edited_text")
                    if (taskId != -1L && newText != null) {
                        val task = viewModel.tasks.value.find { it.id == taskId }
                        task?.let {
                            viewModel.updateTask(it.copy(title = newText))
                        }
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
        val editTextInput = findViewById<EditText>(R.id.editTextInput)
        val buttonShow = findViewById<Button>(R.id.buttonShow)
        val textEntered = findViewById<TextView>(R.id.textEntered)

        // Блок 2: ToDo список
        val editTextTask = findViewById<EditText>(R.id.editTextTask)
        val buttonAddTask = findViewById<Button>(R.id.buttonAddTask)
        val buttonDeleteLast = findViewById<Button>(R.id.buttonDeleteLast)
        recyclerView = findViewById(R.id.recyclerViewTasks)
        textTaskCount = findViewById(R.id.textTaskCount)

        // Поле поиска
        val editTextSearch = findViewById<EditText>(R.id.editTextSearch)

        setupRecyclerView()

        // Подписка на счётчик
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.counter.collect { count ->
                    textCounter.text = getString(R.string.counter_text, count)
                }
            }
        }

        // Подписка на введённый текст
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

        // Подписка на список задач
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collect { tasks ->
                    adapter.updateData(tasks)
                    updateTaskCount(tasks.size)
                }
            }
        }

        // Поиск
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s.toString())
            }
        })

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
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.toast_empty_task, Toast.LENGTH_SHORT).show()
            }
        }

        // Удаление последней задачи
        buttonDeleteLast.setOnClickListener {
            val currentTasks = viewModel.tasks.value
            if (currentTasks.isNotEmpty()) {
                val lastTask = currentTasks.last()
                viewModel.deleteTask(lastTask)
                Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        viewModel.restoreTask(lastTask)
                    }
                    .show()
            } else {
                Toast.makeText(this, "Список задач пуст", Toast.LENGTH_SHORT).show()
            }
        }

        // Очистка поиска по долгому нажатию
        editTextSearch.setOnLongClickListener {
            editTextSearch.text.clear()
            viewModel.clearSearch()
            true
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TaskAdapter(
            tasks = emptyList(),
            onItemClick = { task ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("task_id", task.id)
                intent.putExtra("task_text", task.title)
                intent.putExtra("task_number", viewModel.tasks.value.indexOf(task) + 1)
                editTaskLauncher.launch(intent)
            },
            onItemLongClick = { task ->
                viewModel.deleteTask(task)
                Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        viewModel.restoreTask(task)
                    }
                    .show()
            },
            onCheckChange = { task, isChecked ->
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
                    val task = adapter.getTaskAtPosition(position)
                    viewModel.deleteTask(task)
                    Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                        .setAction("Отмена") {
                            viewModel.restoreTask(task)
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

## Скриншоты работающего приложения

### Изначальный список задач

![Список задач до перезапуска](<Снимок экрана 2026-05-17 230755.png>)

### БД tasks в Database Inspector

![База данных](<Снимок экрана 2026-05-17 230846.png>)

### Сохраненный список задач после перезапуска

![Список задач после перезапуска](<Снимок экрана 2026-05-17 230955.png>)

### Индивидуальное задание 1 - репозиторий в памяти

Необходимо в файле `MainActivity.kt` сменить репозиторий с БД на память:

```kotlin
// Репозиторий в БД
//private val repository by lazy { TaskRepositoryImpl(database.taskDao()) }

// Репозиторий в памяти
private val repository by lazy { InMemoryTaskRepository() }
```
![Список задач хранится в памяти](<Снимок экрана 2026-05-17 231150.png>)

---

## Ответы на контрольные вопросы

**1. Какую роль выполняет слой Repository в архитектуре приложения?**

Repository - это слой между источниками данных (БД, сеть, кэш) и бизнес-логикой (ViewModel). Его основная роль - абстрагировать способ получения и хранения данных от остального приложения.

В архитектуре приложения Repository выполняет следующую роль:

* ViewModel не знает, откуда берутся данные (Room, сеть, файлы)

* При смене источника данных меняется только репозиторий, ViewModel остаётся без изменений

* Репозиторий - единое место для кэширования, синхронизации и обработки ошибок

*Пример из файла `MainViewModel.kt`:*

```kotlin
// Получение задач через репозиторий
    val tasks: StateFlow<List<TaskEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllTasks()
        } else {
            repository.searchTasks(query)
        }
    }
```

ViewModel вызывает `repository.getAllTasks()` и не знает, что данные приходят из Room через DAO.

<br>

**2. Какие преимущества даёт использование Repository по сравнению с прямым обращением к DAO из ViewModel?**

Использование Repository по сравнению с прямым обращением к DAO из ViewModel дает следующие преимущества:

|Критерий            |Без Repository (ViewModel -> DAO)                       |С Repository (ViewModel -> Repository -> DAO)                |
|--------------------|--------------------------------------------------------|-------------------------------------------------------------|
|**Зависимости**     |ViewModel зависит от конкретной реализации Room         |ViewModel зависит только от интерфейса репозитория           |
|**Тестируемость**   |Сложно тестировать без запуска БД                       |Легко подменить репозиторий на mock-объект                   |
|**Гибкость**        |Замена источника данных требует правки ViewModel        |Замена источника — только новая реализация репозитория       |
|**Чистота кода**    |Бизнес-логика смешана с логикой доступа к данным        |Чёткое разделение: ViewModel - логика UI, Repository - данные|
|**Масштабируемость**|Добавление нового источника - правки в нескольких местах|Новый источник - новый метод в репозитории                   |

*Пример из проекта: репозиторий `InMemoryTaskRepository`. ViewModel работает с ним без изменений, хотя данные теперь хранятся в памяти, а не в БД.*

<br>

**3. Как изменится ViewModel, если мы захотим добавить ещё один источник данных (например, сетевое API)**

**ViewModel не изменится**. Это главное преимущество Repository.

Если требуется добавить ещё один источник данных, необходимо:

1. Создать новый источник данных (например, TaskApi)

2. Изменить реализацию репозитория, чтобы он объединял данные из БД и сети

3. ViewModel остаётся без единого изменения, так как она зависит только от интерфейса `TaskRepository`

*Пример реализации:*

```kotlin
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskApi: TaskApi
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskEntity>> {
        // Сначала берутся задачи из БД, потом обновляются из сети
        // Затем объединяются два источника
    }
}
```
ViewModel остаётся такой же:

```kotlin
val tasks: StateFlow<List<TaskEntity>> = repository.getAllTasks() // без изменений
```

<br>

**4. Почему методы репозитория объявлены как `suspend`?**

Методы репозитория объявлены как `suspend`, потому что они выполняют долгие операции ввода-вывода (работа с БД, сетью, диском).

Причины объявления методов таким образом:

* Операции с БД или сетью могут занимать время

* Нельзя блокировать основной (UI) поток - приложение зависнет

* `suspend` позволяет вызывать методы из корутин, которые работают в фоновых потоках

*Пример из репозитория `TaskRepositoryImpl` и класса `MainViewModel`:*

```kotlin
// В репозитории
override suspend fun addTask(title: String) {
        val task = TaskEntity(title = title) // не блокирует UI
        taskDao.insertTask(task) 
    }

// В ViewModel
fun addTask(title: String) {
    viewModelScope.launch {  // корутина в фоне
        repository.addTask(title)
    }
}
```

<br>

**5. Что такое инверсия зависимостей и как она применяется в данном рефакторинге?**

Инверсия зависимостей (Dependency Inversion Principle) — пятый принцип SOLID, который гласит:

> Модули верхних уровней не должны зависеть от модулей нижних уровней. Оба должны зависеть от абстракций. Абстракции не должны зависеть от деталей. Детали должны зависеть от абстракций.

То есть зависимости должны идти от конкретного к абстрактному, а не наоборот.

В данном рефакторинге применяется в следующем виде:

|До рефакторинга                                                           |После рефакторинга                                                  |
|--------------------------------------------------------------------------|--------------------------------------------------------------------|
|ViewModel -> Room (DAO)                                                   |ViewModel -> TaskRepository (интерфейс) <- TaskRepositoryImpl (Room)|
|`MainViewModel` зависит от конкретной реализации `AppDatabase` / `TaskDao`|`MainViewModel` зависит от абстракции `TaskRepository`              |
|Чтобы сменить БД, нужно менять ViewModel                                  |Чтобы сменить БД, достаточно создать новую реализацию               |

*Пример из проекта:*

* Абстракция - интерфейс `TaskRepository`

* Деталь - класс `TaskRepositoryImpl` (работает с Room)

* Другая деталь - класс `InMemoryTaskRepository` (хранит в памяти)

Таким образом, ViewModel зависит от интерфейса `TaskRepository`, а не от конкретной реализации. Поэтому легко переключаться с `TaskRepositoryImpl` на `InMemoryTaskRepository`.Это и есть инверсия зависимостей.

---

## Вывод

В ходе выполнения лабораторной работы №11 изучил архитектурный паттерн Repository, освоил выделение слоя доступа к данным и выполнил рефакторинг существующего приложения для использования репозитория.

Выполняя задание, создал интерфейс `TaskRepository`, определяющий контракт для работы с задачами, и его реализацию `TaskRepositoryImpl`, которая делегирует вызовы к DAO. В файле `MainViewModel.kt` заменил прямое обращение к DAO на использование репозитория, благодаря чему ViewModel перестала зависеть от конкретной реализации базы данных. В `MainActivity.kt` обновил создание репозитория и передачу его во ViewModel через фабрику.

Помимо этого, выполнил индивидуальное задание №1 - создал альтернативную реализацию `InMemoryTaskRepository`, которая хранит данные в памяти. Переключение между `TaskRepositoryImpl` (Room) и `InMemoryTaskRepository` показало, что ViewModel работает без изменений, что демонстрирует гибкость архитектуры.

Таким образом, благодаря добавлению Repository, ViewModel теперь не знает, откуда берутся данные - из БД, сети или памяти. Это позволяет легко подменять источник данных без изменения кода ViewModel, упрощает тестирование и соблюдает принцип инверсии зависимостей.

Это позволяет при необходимости добавить сетевой источник данных и объединять данные из БД и API в одном репозитории. Архитектура становится готовой к масштабированию без изменения существующих классов.

Результат работы **успешно** протестирован на эмуляторе - вся функциональность, реализованная в предыдущих работах, сохранена. Рефакторинг не повлиял на поведение приложения, но значительно улучшил его архитектуру.