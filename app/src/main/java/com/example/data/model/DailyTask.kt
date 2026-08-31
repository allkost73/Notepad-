package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val timeString: String = "", // "14:30" or ""
    val priority: Priority = Priority.MEDIUM,
    val category: TaskCategory = TaskCategory.WORK,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
