package com.example.reminderapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String = "No description",
    val time: String,
    val timeInMillis: Long,
    val category: String = "General",
    val isChecked: Boolean = true
)
