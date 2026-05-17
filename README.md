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
  Лабораторная работа №10<br>
  «Интеграция Room в проект. Сохранение списка задач в БД».<br>
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

Изучить основы работы с Room Database — официальной библиотекой для работы с SQLite в Android. Научиться создавать Entity, DAO, Database, интегрировать Room с ViewModel и корутинами, обеспечить сохранение списка задач между сессиями приложения.


---

## Листинг класса данных `TaskEntity.kt`

```kotlin
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
```

---

## Листинг интерфейса `TaskDao.kt`

```kotlin
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
```

---

## Листинг абстрактного класса `AppDatabase.kt`

```kotlin
package com.example.todoapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_database"
                )
                    //.fallbackToDestructiveMigration() // Для разработки: при изменении версии БД пересоздавать таблицы
                    .build()
                INSTANCE = instance
                instance
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
```

## Листинг обновленного файла `MainActivity.kt`

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
import com.example.todoapp.database.AppDatabase
import com.example.todoapp.database.TaskEntity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Получение БД
    private val database by lazy { AppDatabase.getInstance(this) }

    // Создание ViewModel с фабрикой
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    private lateinit var adapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textTaskCount: TextView

    // Получения результата редактирования задачи из DetailActivity
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
                        // Нахождение задачи по ID и обновление
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

        // Подписка на изменения списка задач из БД
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tasks.collect { tasks ->
                    adapter.updateData(tasks)
                    updateTaskCount(tasks.size)
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

        // Поиск задач
        val editTextSearch = findViewById<EditText>(R.id.editTextSearch)

        editTextSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateSearchQuery(s.toString())
            }
        })

        editTextSearch.setOnLongClickListener {
            editTextSearch.text.clear()
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
                val deletedTask = task
                viewModel.deleteTask(deletedTask)

                Snackbar.make(recyclerView, "Задача удалена", Snackbar.LENGTH_LONG)
                    .setAction("Отмена") {
                        viewModel.restoreTask(deletedTask)
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
                    val task = adapter.tasks[position]
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

## Листинг обновленного файла `TaskAdapter.kt`

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
import com.example.todoapp.database.TaskEntity

class TaskAdapter(
    var tasks: List<TaskEntity>,
    private val onItemClick: (TaskEntity) -> Unit,
    private val onItemLongClick: (TaskEntity) -> Unit,
    private val onCheckChange: (TaskEntity, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Состояние чекбоксов
    private var completedTasks = emptySet<Long>()  // ID выполненных задач

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
        val isChecked = task.isCompleted || task.id in completedTasks

        holder.textTask.text = task.title

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

            // Обновление состояния
            if (isCheckedNow) {
                completedTasks = completedTasks + task.id
            } else {
                completedTasks = completedTasks - task.id
            }

            // Уведомление ViewModel
            onCheckChange(task, isCheckedNow)

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
            onItemClick(task)
        }

        // Долгое нажатие
        holder.itemView.setOnLongClickListener {
            onItemLongClick(task)
            true
        }
    }

    override fun getItemCount(): Int = tasks.size

    // Обновление данных адаптера
    fun updateData(newTasks: List<TaskEntity>) {
        tasks = newTasks

        // Обновление состояния (удаление ID которых больше нет в списке)
        val existingIds = tasks.map { it.id }.toSet()
        completedTasks = completedTasks.filter { it in existingIds }.toSet()

        notifyDataSetChanged()
    }
}
```

---

## Скриншоты работающего приложения

### Изначальный список задач

![Список задач до перезапуска](<Снимок экрана 2026-05-17 015211.png>)

### БД tasks в Database Inspector

![База данных](<Снимок экрана 2026-05-17 020243.png>)

### Сохраненный список задач после перезапуска

![Список задач после перезапуска](<Снимок экрана 2026-05-17 015534.png>)

### Индивидуальное задание 2 - поиск задач

![Демонстрация поиска задачи](<Снимок экрана 2026-05-17 015632.png>)

---

## Ответы на контрольные вопросы

**1. Для чего нужна библиотека Room? Какие проблемы она решает по сравнению с прямым использованием SQLite?**

Room - это библиотека-обёртка над SQLite, предоставляющая абстракцию уровня объектов для удобной и безопасной работы с базами данных в Android.

Проблемы, которые решает Room:

|Проблема с прямым SQLite                                            |Решение Room                                                              |
|--------------------------------------------------------------------|--------------------------------------------------------------------------|
|Шаблонный код - ручное создание таблиц, курсоры, парсинг            |Аннотации - `@Entity`, `@Dao`, `@Query` генерируют код автоматически      |
|Ошибки в SQL-запросах - обнаруживаются только при выполнении        |Валидация на этапе компиляции - неверный SQL вызывает ошибку сборки       |
|Блокировка основного потока - нужно вручную управлять потоками      |Интеграция с корутинами - `suspend`-методы и `Flow` для асинхронности     |
|Отсутствие типобезопасности - работа с `Cursor` и индексами столбцов|Маппинг на data class - данные автоматически преобразуются в объекты      |
|Сложная миграция - ручное написание SQL-скриптов                    |Поддержка миграций - аннотации и версионирование упрощают обновление схемы|

Таким образом, Room уменьшает количество шаблонного кода, повышает надёжность и делает код более читаемым и тестируемым.

<br>

**2. Назовите три основных компонента Room и объясните их назначение**

Основными компонентами Room являются:

|Компонент   |Назначение                            |Пример из проекта                                 |
|------------|-------------------------------------|----------------------------------------------------|
|**Entity**  |Представляет таблицу в БД. Каждый экземпляр класса - строка, каждое поле - столбец|`TaskEntity` с полями `id`, `title`, `isCompleted`, `createdTime`|
|**DAO**     |Интерфейс с методами для доступа к данным. Room генерирует реализацию автоматически|`TaskDao` с методами `getAllTasks()`, `insertTask()`, `deleteTask()`|
|**Database**|Точка доступа к БД. Абстрактный класс, наследующий `RoomDatabase`. Связывает `Entity` и `DAO`, управляет версионированием и созданием экземпляра БД|`AppDatabase` с методом `taskDao()`|

Взаимодействие: Database -> DAO -> Entity.

<br>

**3. Почему методы DAO, изменяющие данные, объявляются как `suspend`?**

 Операции с БД (вставка, обновление, удаление) являются блокирующими и могут выполняться длительное время. Если выполнять их в основном потоке (UI-потоке), приложение «зависнет».

 *Пример без `suspend`*:

```kotlin
fun insertTask(task: TaskEntity) {
    taskDao.insertTask(task) // Room запрещает операции в UI-потоке
}
```

 Методы `suspend` выполняются асинхронно, не блокируя основной поток.

*Пример с `suspend`*:

```kotlin
suspend fun insertTask(task: TaskEntity) {
    taskDao.insertTask(task) // Выполняется в фоновом потоке
}
```

Принцип работы:

* `suspend`-функция может быть вызвана только из корутины или другой `suspend`-функции
* Room автоматически выполняет запрос в фоновом потоке
* После завершения результат возвращается в вызывающую корутину без блокировки UI

*Пример вызова из ViewModel*:

```kotlin
fun addTask(title: String) {
    viewModelScope.launch { // Корутина на фоне
        taskDao.insert(TaskEntity(title = title)) // Не блокирует UI
    }
}
```

<br>

**4. Что такое `Flow` и почему его удобно использовать с Room?**

`Flow` - это асинхронный поток данных из библиотеки Kotlin Coroutines, который выдаёт последовательность значений во времени и поддерживает реактивное программирование.

`Flow` удобно использовать с Room по следующим причинам:

* Реактивность - при изменении в БД Flow автоматически выдаёт новое значение
* Автообновление - не нужно вручную перезапрашивать данные
* Жизненный цикл - можно привязать к жизненному циклу через repeatOnLifecycle
* Операторы - поддерживает map, filter, combine и другие

*Пример из `TaskDao.kt`:*

```kotlin
// Достаточно подписаться один раз, UI будет обновляться автоматически
@Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdTime DESC")
fun getAllTasks(): Flow<List<TaskEntity>> 
```

При добавлении/удалении/обновлении задачи - Flow сам выдаст новый список, и адаптер обновится.

<br>

**5. Как Room обеспечивает проверку SQL-запросов на этапе компиляции?**

Room использует аннотационный процессор `kapt` (Kotlin Annotation Processing Tool), который анализирует аннотации `@Query`, `@Insert` и другие во время компиляции.

*Пример процесса проверки:*

1. Написан запрос с опечаткой в имени таблицы: @Query("SELECT * FROM taskss")
2. kapt анализирует аннотации и сравнивает с @Entity-классами
3. Обнаруживается, что таблицы "taskss" не существует
4. Генерируется ошибку компиляции: "Cannot find the table in the provided entities"
5. Сборка останавливается и ошибка не попадёт в релиз

Таким образом, ошибки в именах таблиц, столбцов, типах данных обнаруживаются до запуска приложения. Без Room (чистый SQLite) такая ошибка проявилась бы только в рантайме, когда пользователь запустил приложение.

<br>

**6. Зачем нужен паттерн Singleton для экземпляра базы данных?**

Singleton - это паттерн, гарантирующий, что во всём приложении существует только один экземпляр БД.

Причины использования паттерна Singleton:

* Создание экземпляра `RoomDatabase` - ресурсоёмкая операция (открытие файла БД, инициализация миграций, проверка схемы)
* Одновременный доступ к БД из нескольких потоков без синхронизации может привести к повреждению данных

Преимущества использования паттерна Singleton:

* Экономия ресурсов - БД создаётся один раз на всё время жизни приложения
* Потокобезопасность - 	предотвращает одновременный доступ из разных потоков
* Целостность данных - исключает конфликты при параллельной записи

*Пример реализации в `AppDatabase.kt`:*

```kotlin
companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(...).build()
            INSTANCE = instance
            instance
        }
    }
}
```

* `@Volatile` - гарантирует, что все потоки видят актуальное значение INSTANCE
* `INSTANCE ?:` - создается только если null
* `synchronized` - потокобезопасность: только один поток может создать экземпляр, остальные ждут

---

## Вывод

В ходе выполнения лабораторной работы №10 изучил библиотеку Room для работы с базами данных SQLite, освоил создание `Entity`, `DAO` и `Database`, а также интеграцию Room с ViewModel и корутинами.

Выполняя задание, создал класс `TaskEntity`, представляющий таблицу задач, интерфейс `TaskDao` с методами для работы с данными, и `AppDatabase` - синглтон для доступа к БД. В файле `MainViewModel.kt` заменил хранение списка задач в памяти на получение данных из Room через `Flow`, а методы работы с задачами теперь вызывают `DAO` внутри корутин. 

В адаптере `TaskAdapter.kt` обновил адаптер для работы с `TaskEntity`. В файле `MainActivity.kt` создал фабрику для передачи базы данных в ViewModel и обновил подписку на Flow.

Помимо этого, выполнил индивидуальное задание №2 - реализовал поиск задач по названию с автоматической фильтрацией списка при вводе текста. Также было реализовано индивидуальное задание №1 - сортировка задач: сначала невыполненные, затем выполненные, внутри каждой группы новые задачи сверху располагаются сверху.

Узнал, что Room позволяет проверять SQL-запросы на этапе компиляции, автоматически конвертировать данные в объекты и легко работать с БД в асинхронном стиле через корутины.

Улучшил архитектуру приложения с помощью Room - данные теперь сохраняются между сессиями, снижается нагрузка на память за счёт хранения только нужного в данный момент, упрощается тестирование благодаря изоляции БД, а реактивный подход с Flow обеспечивает автоматическое обновление UI при изменении данных.

Результат работы **успешно** протестирован на эмуляторе - данные сохраняются после перезапуска приложения.