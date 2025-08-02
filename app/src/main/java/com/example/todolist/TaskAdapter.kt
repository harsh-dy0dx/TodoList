package com.example.todolist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onCheckClicked: (Task) -> Unit,
    private val onTaskLongClicked: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onTaskClicked(getItem(adapterPosition))
                }
            }
            binding.checkIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCheckClicked(getItem(adapterPosition))
                }
            }
            itemView.setOnLongClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onTaskLongClicked(getItem(adapterPosition))
                }
                true
            }
        }

        fun bind(task: Task) {
            binding.textViewTitle.text = task.title
            if (task.isCompleted) {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                binding.checkIcon.setImageResource(R.drawable.ic_check_circle_filled)
            } else {
                binding.textViewTitle.paintFlags = binding.textViewTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textViewTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                binding.checkIcon.setImageResource(R.drawable.ic_check_circle_outline)
            }
            if (task.dueDate != null) {
                binding.textViewDueDate.visibility = View.VISIBLE
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                binding.textViewDueDate.text = sdf.format(Date(task.dueDate))
            } else {
                binding.textViewDueDate.visibility = View.INVISIBLE
            }

            // --- FIX: Set priority badge color based on task priority ---
            val priorityColor = when (task.priority) {
                1 -> R.color.priority_low
                3 -> R.color.priority_high
                else -> R.color.priority_medium
            }
            binding.priorityBadge.setBackgroundColor(ContextCompat.getColor(itemView.context, priorityColor))
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}
