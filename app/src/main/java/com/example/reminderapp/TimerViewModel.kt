package com.example.reminderapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reminderapp.data.Event
import com.example.reminderapp.data.EventDao
import com.example.reminderapp.data.HistoryDao
import com.example.reminderapp.data.HistoryItem
import com.example.reminderapp.data.HistoryType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimerViewModel(
    private val eventDao: EventDao,
    private val historyDao: HistoryDao
) : ViewModel() {

    // --- Timer State ---
    private val _remainingTime = MutableStateFlow(930000L) // 15:30 default
    val remainingTime: StateFlow<Long> = _remainingTime.asStateFlow()

    private val _initialTime = MutableStateFlow(930000L)
    val initialTime: StateFlow<Long> = _initialTime.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    val quickPresets = listOf(
        "5 MIN" to 300000L,
        "15 MIN" to 900000L,
        "30 MIN" to 1800000L,
        "60 MIN" to 3600000L
    )

    private val _timerFinishedEvent = MutableSharedFlow<Unit>()
    val timerFinishedEvent = _timerFinishedEvent.asSharedFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        if (_isRunning.value || _remainingTime.value <= 0) return
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val initialRemaining = _remainingTime.value
            while (_remainingTime.value > 0) {
                val elapsed = System.currentTimeMillis() - startTime
                _remainingTime.value = (initialRemaining - elapsed).coerceAtLeast(0)
                if (_remainingTime.value <= 0) break
                delay(50)
            }
            _isRunning.value = false
            logTimerCompletion() // Log when timer completes
            _timerFinishedEvent.emit(Unit)
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
    }

    fun resetTimer() {
        pauseTimer()
        _remainingTime.value = _initialTime.value
    }

    fun setTimer(millis: Long) {
        pauseTimer()
        _initialTime.value = millis
        _remainingTime.value = millis
        startTimer()
    }

    fun updateTimer(millis: Long) {
        pauseTimer()
        _initialTime.value = millis
        _remainingTime.value = millis
    }

    // --- Calendar State ---
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _eventsMap = eventDao.getAllEvents().map { events ->
        events.groupBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val eventsMap: StateFlow<Map<LocalDate, List<Event>>> = _eventsMap

    val eventsForSelectedDate: StateFlow<List<Event>> = combine(_selectedDate, _eventsMap) { date, map ->
        map[date] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun addEvent(event: Event) {
        val date = _selectedDate.value
        viewModelScope.launch {
            eventDao.insertEvent(event.copy(date = date))
            // Log event creation to history
            logHistory("Created: ${event.title}", HistoryType.CALENDAR, date)
        }
    }

    fun toggleEventDone(event: Event) {
        viewModelScope.launch {
            val updatedEvent = event.copy(isDone = !event.isDone)
            eventDao.updateEvent(updatedEvent)
            val status = if (updatedEvent.isDone) "Completed" else "Reopened"
            logHistory("$status: ${updatedEvent.title}", HistoryType.CALENDAR, updatedEvent.date)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
            logHistory("Deleted: ${event.title}", HistoryType.CALENDAR, event.date)
        }
    }

    fun deleteHistoryItem(historyItem: HistoryItem) {
        viewModelScope.launch {
            historyDao.deleteHistory(historyItem)
        }
    }

    // --- History State ---
    val history: StateFlow<List<HistoryItem>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logHistory(message: String, type: HistoryType, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            historyDao.insertHistory(HistoryItem(message = message, timestamp = timestamp, type = type, date = date))
        }
    }

    fun logTimerCompletion() {
        val duration = formatTime(_initialTime.value)
        logHistory("Timer for $duration completed", HistoryType.TIMER)
    }

    fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    class Factory(
        private val eventDao: EventDao,
        private val historyDao: HistoryDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TimerViewModel(eventDao, historyDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
