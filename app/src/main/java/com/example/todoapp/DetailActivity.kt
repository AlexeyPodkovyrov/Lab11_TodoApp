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

    private var taskPosition: Int = -1
    private var taskNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val textTitle = findViewById<TextView>(R.id.textTitle)
        editTextTask = findViewById(R.id.editTextTask)
        buttonSave = findViewById(R.id.buttonSave)
        buttonBack = findViewById(R.id.buttonBack)

        // Получение данных
        val taskText = intent.getStringExtra("task_text") ?: "Нет данных"
        taskPosition = intent.getIntExtra("task_position", -1)
        taskNumber = intent.getIntExtra("task_number", 0)

        // Заголовок
        textTitle.text = "Задача №$taskNumber"

        editTextTask.setText(taskText)
        editTextTask.setSelection(taskText.length)

        buttonSave.setOnClickListener {
            val newText = editTextTask.text.toString().trim()
            if (newText.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("edited_text", newText)
                resultIntent.putExtra("task_position", taskPosition)
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