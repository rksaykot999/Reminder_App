package com.example.reminderapp.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromHistoryType(value: HistoryType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toHistoryType(value: String?): HistoryType? {
        return value?.let { HistoryType.valueOf(it) }
    }
}
