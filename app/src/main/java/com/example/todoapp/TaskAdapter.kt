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