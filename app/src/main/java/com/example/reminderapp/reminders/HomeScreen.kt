package com.example.reminderapp.reminders

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminderapp.data.AppDatabase
import com.example.reminderapp.data.Reminder
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModel.Factory(
            context.applicationContext as android.app.Application,
            database.reminderDao()
        )
    )

    val reminders by viewModel.reminders.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val nextTask = reminders
        .filter { it.isChecked && it.timeInMillis > System.currentTimeMillis() }
        .minByOrNull { it.timeInMillis }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingReminder = null
                    showSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Reminder") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NextTaskCard(nextTask)

            Text(
                "All Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No reminders yet.\nTap + to add your first one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onCheckedChange = { checked -> viewModel.setChecked(reminder, checked) },
                            onEdit = {
                                editingReminder = reminder
                                showSheet = true
                            },
                            onDelete = { viewModel.deleteReminder(reminder) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showSheet) {
        AddEditReminderSheet(
            existing = editingReminder,
            onDismiss = { showSheet = false },
            onSave = { name, desc, category, time, millis ->
                val editing = editingReminder
                if (editing == null) {
                    viewModel.addReminder(name, desc, category, time, millis)
                } else {
                    viewModel.updateReminder(editing, name, desc, category, time, millis)
                }
                showSheet = false
            }
        )
    }
}

@Composable
fun NextTaskCard(nextTask: Reminder?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "UP NEXT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = nextTask?.time ?: "-- : --",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = nextTask?.name ?: "No upcoming tasks",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

private fun categoryColors(category: String): Pair<Color, Color> = when (category.lowercase()) {
    "break" -> Color(0xFFFFE0C2) to Color(0xFF8A4B00)
    "meeting" -> Color(0xFFCFE3FF) to Color(0xFF00458A)
    "medicine" -> Color(0xFFC9F5D9) to Color(0xFF00702E)
    else -> Color(0xFFE3D9F7) to Color(0xFF4B2E83)
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (bg, fg) = categoryColors(reminder.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = reminder.isChecked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reminder.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                        Text(
                            reminder.category,
                            color = fg,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(reminder.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(reminder.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderSheet(
    existing: Reminder?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, category: String, time: String, millis: Long) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description?.takeIf { it != "No description" } ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var selectedTime by remember { mutableStateOf(existing?.time ?: "") }
    var selectedTimeMillis by remember { mutableStateOf(existing?.timeInMillis ?: 0L) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (existing == null) "Add Reminder" else "Edit Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (General, Meeting, Break, Medicine...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
                    val c = Calendar.getInstance()
                    TimePickerDialog(context, { _, hour, minute ->
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                        }
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        selectedTimeMillis = calendar.timeInMillis
                        val amPm = if (hour < 12) "AM" else "PM"
                        val formattedHour = if (hour % 12 == 0) 12 else hour % 12
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, minute, amPm)
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedTime.isEmpty()) "Pick Time" else selectedTime)
            }
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedTime.isNotEmpty()) {
                        onSave(name, description, category, selectedTime, selectedTimeMillis)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (existing == null) "Save Reminder" else "Update Reminder")
            }
        }
    }
}
