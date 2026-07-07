package com.example.reminderapp.stopwatch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.reminderapp.TimerViewModel
import com.example.reminderapp.ui.screens.TimerScreen

/**
 * Combines the count-up Stopwatch and the count-down Timer behind a tab switcher,
 * matching the "Stopwatch (stopwatch+timer)" bottom-nav destination.
 */
@Composable
fun StopwatchAndTimerScreen(timerViewModel: TimerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val titles = listOf("Stopwatch", "Timer")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
        when (selectedTab) {
            0 -> StopwatchScreen()
            1 -> TimerScreen(timerViewModel)
        }
    }
}
