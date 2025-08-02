package com.example.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String?,
    val priority: Int,
    val dueDate: Long?,
    var isCompleted: Boolean = false
) : Serializable
