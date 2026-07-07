package com.example.reminderapp.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val eventDao: EventDao, private val historyDao: HistoryDao) {
    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun insertEvent(event: Event) = eventDao.insertEvent(event)
    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)

    suspend fun insertHistory(historyItem: HistoryItem) = historyDao.insertHistory(historyItem)
    suspend fun deleteHistory(historyItem: HistoryItem) = historyDao.deleteHistory(historyItem)
}
