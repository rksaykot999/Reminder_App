package com.example.reminderapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val time: String = "",
    val location: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val isDone: Boolean = false
)
