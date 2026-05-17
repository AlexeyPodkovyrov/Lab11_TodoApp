package com.example.todoapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todoapp.database.AppDatabase
import kotlinx.coroutines.runBlocking

class DetailActivity : AppCompatActivity() {

    private lateinit var editTextTask: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonBack: Button

    private var taskId: Long = -1
    private var taskText: String = ""
    private var taskNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val textTitle = findViewById<TextView>(R.id.textTitle)
        editTextTask = findViewById(R.id.editTextTask)
        buttonSave = findViewById(R.id.buttonSave)
        buttonBack = findViewById(R.id.buttonBack)

        // Получение данных
        taskId = intent.getLongExtra("task_id", -1)
        taskText = intent.getStringExtra("task_text") ?: "Нет данных"
        taskNumber = intent.getIntExtra("task_number", 0)

        textTitle.text = "Задача №$taskNumber"
        editTextTask.setText(taskText)
        editTextTask.setSelection(taskText.length)

        buttonSave.setOnClickListener {
            val newText = editTextTask.text.toString().trim()
            if (newText.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("task_id", taskId)
                resultIntent.putExtra("edited_text", newText)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Задача не может быть пустой", Toast.LENGTH_SHORT).show()
            }
        }

        buttonBack.setOnClickListener {
            finish()
        }
    }
}