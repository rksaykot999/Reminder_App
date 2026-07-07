package com.example.reminderapp.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel : ViewModel() {

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _laps = MutableStateFlow<List<Long>>(emptyList())
    val laps: StateFlow<List<Long>> = _laps.asStateFlow()

    private var job: Job? = null
    private var baseElapsed = 0L
    private var startedAt = 0L

    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        startedAt = System.currentTimeMillis()
        job = viewModelScope.launch {
            while (_isRunning.value) {
                _elapsedMillis.value = baseElapsed + (System.currentTimeMillis() - startedAt)
                delay(30)
            }
        }
    }

    fun pause() {
        job?.cancel()
        _isRunning.value = false
        baseElapsed = _elapsedMillis.value
    }

    fun reset() {
        job?.cancel()
        _isRunning.value = false
        baseElapsed = 0L
        _elapsedMillis.value = 0L
        _laps.value = emptyList()
    }

    fun lap() {
        if (_isRunning.value) {
            _laps.value = _laps.value + _elapsedMillis.value
        }
    }

    fun formatTime(millis: Long): String {
        val totalMillis = millis
        val minutes = (totalMillis / 60000) % 60
        val hours = totalMillis / 3600000
        val seconds = (totalMillis / 1000) % 60
        val centis = (totalMillis % 1000) / 10
        return if (hours > 0) {
            String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, centis)
        }
    }
}
