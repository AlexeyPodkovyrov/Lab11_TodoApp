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

    fun getTaskAtPosition(position: Int): TaskEntity {
        return tasks[position]
    }
}