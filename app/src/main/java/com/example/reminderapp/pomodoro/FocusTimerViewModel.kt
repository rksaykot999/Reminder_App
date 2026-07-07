package com.example.reminderapp.pomodoro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimerState {
    FOCUS, SHORT_BREAK, LONG_BREAK, IDLE
}

data class TimerUiState(
    val timeLeftSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val timerState: TimerState = TimerState.IDLE,
    val currentRound: Int = 1,
    val totalSessionsCompleted: Int = 0,
    val weeklyFocusedMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0), // Mon-Sun
    val isRunning: Boolean = false
)

class FocusTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(TimerUiState(
        weeklyFocusedMinutes = loadWeeklyData()
    ))
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var secondsCounter = 0

    private fun loadWeeklyData(): List<Int> {
        val data = mutableListOf<Int>()
        for (i in 0..6) {
            data.add(sharedPrefs.getInt("day_$i", 0))
        }
        return data
    }

    private fun saveWeeklyData(data: List<Int>) {
        sharedPrefs.edit().apply {
            data.forEachIndexed { index, minutes ->
                putInt("day_$index", minutes)
            }
            apply()
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_uiState.value.timerState == TimerState.IDLE) {
            _uiState.update { it.copy(timerState = TimerState.FOCUS) }
        }
        _uiState.update { it.copy(isRunning = true) }
        runTimer()
    }

    private fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        secondsCounter = 0
        val defaultTime = 25 * 60L
        _uiState.update {
            it.copy(
                timeLeftSeconds = defaultTime,
                totalSeconds = defaultTime,
                timerState = TimerState.IDLE,
                isRunning = false
            )
        }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)
                
                if (_uiState.value.timerState == TimerState.FOCUS) {
                    secondsCounter++
                    if (secondsCounter >= 60) {
                        incrementMinute()
                        secondsCounter = 0
                    }
                }
                
                _uiState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }

            if (_uiState.value.timeLeftSeconds <= 0L) {
                handleTimerFinished()
            }
        }
    }

    private fun incrementMinute() {
        val calendar = Calendar.getInstance()
        val rawDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentDayIndex = if (rawDay == Calendar.SUNDAY) 6 else rawDay - 2
        
        val updatedWeekly = _uiState.value.weeklyFocusedMinutes.toMutableList()
        updatedWeekly[currentDayIndex] += 1
        
        saveWeeklyData(updatedWeekly)
        _uiState.update { it.copy(weeklyFocusedMinutes = updatedWeekly) }
    }

    private fun handleTimerFinished() {
        val currentState = _uiState.value
        val sessionsCompleted = if (currentState.timerState == TimerState.FOCUS) currentState.totalSessionsCompleted + 1 else currentState.totalSessionsCompleted
        
        // Note: incrementMinute handles the live saving during FOCUS. 
        // We reset the seconds counter here to ensure precision between sessions.
        secondsCounter = 0

        when (currentState.timerState) {
            TimerState.FOCUS -> {
                if (currentState.currentRound >= 4) {
                    _uiState.update {
                        it.copy(
                            timerState = TimerState.LONG_BREAK,
                            timeLeftSeconds = 20 * 60L,
                            totalSeconds = 20 * 60L,
                            isRunning = false,
                            totalSessionsCompleted = sessionsCompleted,
                            currentRound = 4
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            timerState = TimerState.SHORT_BREAK,
                            timeLeftSeconds = 5 * 60L,
                            totalSeconds = 5 * 60L,
                            isRunning = false,
                            totalSessionsCompleted = sessionsCompleted,
                            currentRound = currentState.currentRound
                        )
                    }
                }
            }
            TimerState.SHORT_BREAK -> {
                _uiState.update {
                    it.copy(
                        timerState = TimerState.FOCUS,
                        timeLeftSeconds = 25 * 60L,
                        totalSeconds = 25 * 60L,
                        isRunning = false,
                        currentRound = it.currentRound + 1
                    )
                }
            }
            TimerState.LONG_BREAK -> {
                _uiState.update {
                    it.copy(
                        timerState = TimerState.FOCUS,
                        timeLeftSeconds = 25 * 60L,
                        totalSeconds = 25 * 60L,
                        isRunning = false,
                        currentRound = 1
                    )
                }
            }
            TimerState.IDLE -> {}
        }
        _onTimerFinished()
    }

    private var _onTimerFinished: () -> Unit = {}
    fun setOnTimerFinishedListener(listener: () -> Unit) {
        _onTimerFinished = listener
    }
}
