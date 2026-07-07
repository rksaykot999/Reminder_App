package com.example.reminderapp.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = viewModel()) {
    val elapsed by viewModel.elapsedMillis.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = viewModel.formatTime(elapsed),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { if (isRunning) viewModel.lap() else viewModel.reset() },
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isRunning) {
                    Icon(Icons.Default.Flag, contentDescription = "Lap")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            FilledIconButton(
                onClick = { if (isRunning) viewModel.pause() else viewModel.start() },
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(7.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.onPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .width(7.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(36.dp))
                }
            }
        }

        if (laps.isNotEmpty()) {
            HorizontalDivider()
            Text("Laps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(laps.reversed()) { index, lapTime ->
                    val lapNumber = laps.size - index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap $lapNumber", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(viewModel.formatTime(lapTime), fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
