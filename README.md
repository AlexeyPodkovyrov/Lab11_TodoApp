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
  Лабораторная работа №7<br>
  «Добавление второго экрана (детали задачи). Переход по клику на элемент списка».<br>
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

Научиться создавать многоэкранные приложения, осуществлять переход между экранами с передачей данных через Intent, обрабатывать клики на элементах RecyclerView.

---

## Листинг файла `activity_detail.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="#F5F5F5">

    <TextView
        android:id="@+id/textTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Редактирование задачи"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="24dp"/>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editTextTask"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Текст задачи"
            android:minHeight="100dp"
            android:gravity="top"
            android:inputType="textMultiLine"
            android:maxLines="5"/>

    </com.google.android.material.textfield.TextInputLayout>

    <Button
        android:id="@+id/buttonSave"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Сохранить"
        android:layout_marginBottom="8dp"
        android:backgroundTint="#6750A4"/>

    <Button
        android:id="@+id/buttonBack"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Назад"
        android:backgroundTint="#757575"/>

</LinearLayout>
```

---

## Листинг `DetailActivity.kt`

```kotlin
package com.example.todoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    private lateinit var editTextTask: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonBack: Button

    private var taskText: String = ""
    private var taskPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        editTextTask = findViewById(R.id.editTextTask)
        buttonSave = findViewById(R.id.buttonSave)
        buttonBack = findViewById(R.id.buttonBack)

        // Получение данных
        taskText = intent.getStringExtra("task_text") ?: "Нет данных"
        taskPosition = intent.getIntExtra("task_position", -1)

        editTextTask.setText(taskText)
        editTextTask.setSelection(taskText.length)

        // Сохранение изменений
        buttonSave.setOnClickListener {
            val newText = editTextTask.text.toString().trim()
            if (newText.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("edited_text", newText)
                resultIntent.putExtra("task_position", taskPosition)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Задача не может быть пустой (свайп для удаления)", Toast.LENGTH_SHORT).show()
            }
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }
}
```

---

## Листинг обновленного `TaskAdapter.kt` (включая индивидуальное задание №2)

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
    private val tasks: MutableList<String>,
    private val onTaskCheckedChange: (Int, Boolean) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val checkedStates = mutableMapOf<Int, Boolean>()

    init {
        tasks.indices.forEach { index ->
            checkedStates[index] = false
        }
    }

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
        val isChecked = checkedStates[position] ?: false

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

        // Перечеркивание
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
            checkedStates[position] = isCheckedNow
            onTaskCheckedChange(position, isCheckedNow)
            notifyItemChanged(position)
        }

        // Обработка клика на карточку
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTask(position: Int, newText: String) {
        tasks[position] = newText
        notifyItemChanged(position)
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

## Скриншоты главного экрана и экрана деталей

### Главный экран до редактирования задачи

![Главный экран до редактирования](<Снимок экрана 2026-04-23 193054.png>)

### Экран деталей задачи

![Экран деталей](<Снимок экрана 2026-04-23 193154.png>)

### Главный экран после редактирования задачи

![Главный экран после редактирования](<Снимок экрана 2026-04-23 193405.png>)

---

## Ответы на контрольные вопросы

**1. Что такое `Intent`? Какие виды `Intent` существуют?**

 `Intent` - это объект для запуска компонентов (например, Activity) и передачи данных.

 Существуют следующие виды `Intent`:

 * **Явный (Explicit)** - четко указывает, какой компонент нужно вызвать (по имени класса).

*Пример:* `Intent(this, DetailActivity::class.java)` — переход на экран деталей.

* **Неявный (Implicit)** - не указывает конкретный класс, а описывает действие, которое нужно выполнить. система же в свою очередь сама находит подходящий компонент.

*Пример:* `Intent(Intent.ACTION_VIEW, Uri.parse("https://sakhgu.ru"))` — открыть сайт в браузере.

<br>

**2. Как передать данные из одной `Activity` в другую?**

Данные передаются через объект `Intent` с помощью методов `putExtra()`. В принимающей активности данные извлекаются из полученного `Intent`.

Споcобы передачи данных из одной  `Activity` в другую:

1. Через `putExtra()` (для простых типов):

    ```kotlin
    // В MainActivity (отправка)
    val intent = Intent(this, DetailActivity::class.java)
    intent.putExtra("task_text", "Сдать работу")
    intent.putExtra("task_position", 1)
    startActivity(intent)

    // В DetailActivity (получение)
    val taskText = intent.getStringExtra("task_text")  // "Сдать работу"
    val position = intent.getIntExtra("task_position", -1)  // 1
    ```

2. Через `Bundle`:

    ```kotlin
    val bundle = Bundle().apply {
        putString("task_text", "Сдать работу")
        putInt("task_position", 1)
    }
    intent.putExtras(bundle)
    ```

3. Через `Parcelable` (для объектов):

    ```kotlin
    // Data class с @Parcelize
    @Parcelize
    data class Task(val text: String, val isCompleted: Boolean) : Parcelable

    // Передача
    intent.putExtra("task", Task("Сдать работу", false))

    // Получение
    val task = intent.getParcelableExtra<Task>("task")
    ```

4. Через `ActivityResultLauncher` (для возврата данных):

    ```kotlin
    // Отправка с ожиданием результата
    editTaskLauncher.launch(intent)

    // Возврат результата
    val resultIntent = Intent()
    resultIntent.putExtra("edited_text", newText)
    setResult(Activity.RESULT_OK, resultIntent)
    finish()
    ```

<br>

**3. Какие способы обработки кликов на элементах `RecyclerView` вы знаете?**

Существует **три** основных способа обработки кликов на элементах `RecyclerView`:

1. **Через лямбду в адаптере (рекомендуемый)**:

    ``` kotlin
    // В адаптере
    class TaskAdapter(
        private val tasks: List<String>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<...>() {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    // В Activity
    adapter = TaskAdapter(tasks) { position ->
        Toast.makeText(this, tasks[position], Toast.LENGTH_SHORT).show()
    }
    ```

2. **Через интерфейс слушателя**:

    ```kotlin
    interface OnItemClickListener {
    fun onItemClick(position: Int)
    }

    class TaskAdapter(private val clickListener: OnItemClickListener) {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemView.setOnClickListener {
                clickListener.onItemClick(position)
            }
        }
    }
    ```

3. **Внутри `ViewHolder`**:

    ```kotlin
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // обработка клика
                }
            }
        }
    }
    ```

<br>

**4. Как создать новую `Activity` в Android Studio?**

Для создания новой `Activity` в Android Studio:

1. Нажмите ПКМ на пакете `com.example.todoapp` → `New` → `Activity` → `Empty Views Activity`.

2. Введите имя активности (например `DetailAcitity`).

3. Оставьте галочку `Generate Layout File` отмеченой (создание файла разметки).

4. Не изменяйте предложенное имя разметки (`activity_detail`).

5. Убедитесь, что галочка `Launcher Activity` снята (чтобы новая активность не стала точкой входа).

6. Нажмите `Finish`.

Таким образом, Android Studio автоматически создаст файл `DetailActivity.kt`, файл разметки `activity_detail.xml` и добавит запись об активности в `AndroidManifest.xml`.

<br>

**5. Для чего используется метод `finish()`?**

Метод `finish()` завершает работу текущей `Activity` и удаляет её из стека задач.


*В результате:*

* Текущий экран закрывается.

* Пользователь возвращается на предыдущий экран (ту Activity, которая была открыта до этой).

* Освобождаются ресурсы, занятые этой активностью.

Чаще всего используется в кнопке «Назад» или после успешного выполнения действия (например, сохранения данных), чтобы вернуться к списку:

```kotlin
buttonBack.setOnClickListener {
    finish() // Закрыть DetailActivity и вернуться в MainActivity
}
```

---

## Вывод

В ходе выполнения лабораторной работы №7 изучил создание многоэкранных приложений, освоил передачу данных между `Activity` через `Intent` и обработку кликов на элементах `RecyclerView`.

Выполняя задание, создал новый класс `DetailActivity.kt` и соответствующий файл разметки `activity_detail.xml` с `LinearLayout`, `EditText` и кнопками «Сохранить» и «Назад». В классе реализовал получение переданного текста задачи из `Intent` с помощью `getStringExtra()` и его отображение в поле ввода, а также обработку нажатия кнопки «Назад» через метод `finish()`.

В файле `TaskAdapter.kt` обновил адаптер, добавив лямбду-обработчик `onItemClick`, и в методе `onBindViewHolder` установил `setOnClickListener` на корневой элемент карточки, что позволило открывать экран деталей при клике на любую задачу. Передачу позиции и текста задачи на второй экран реализовал через `putExtra()`.

В `MainActivity.kt` изменил создание адаптера, передав лямбду, которая запускает `DetailActivity`. Для получения результата редактирования использовал `ActivityResultLauncher`, а также реализовал обработку возвращённых данных.

Помимо этого, выполнил индивидуальное задание №2 - реализовал редактирование задачи на отдельном экране с возможностью изменения текста и сохранением обновлённых данных в главном списке. Для этого
добавил в `DetailActivity` поле `EditText` для редактирования текста задачи и кнопку «Сохранить», которая возвращает изменённый текст обратно на главный экран. Также реализовал проверку на пустое значение с выводом Toast-уведомления.

Результат работы **успешно** протестирован на эмуляторе.