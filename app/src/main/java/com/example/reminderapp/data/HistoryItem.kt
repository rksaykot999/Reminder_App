package com.example.reminderapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

enum class HistoryType {
    TIMER, CALENDAR
}

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: String,
    val type: HistoryType,
    val date: LocalDate = LocalDate.now()
)
