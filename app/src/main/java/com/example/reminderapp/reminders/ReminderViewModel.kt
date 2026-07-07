package com.example.reminderapp.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminderapp.data.Reminder
import com.example.reminderapp.data.ReminderDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(
    application: Application,
    private val reminderDao: ReminderDao
) : AndroidViewModel(application) {

    val reminders: StateFlow<List<Reminder>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(name: String, description: String, category: String, time: String, timeInMillis: Long) {
        val reminder = Reminder(
            id = System.currentTimeMillis().toInt(),
            name = name,
            description = description.ifEmpty { "No description" },
            time = time,
            timeInMillis = timeInMillis,
            category = category.ifEmpty { "General" },
            isChecked = true
        )
        viewModelScope.launch {
            reminderDao.insertReminder(reminder)
        }
        scheduleAlarm(reminder)
    }

    fun updateReminder(existing: Reminder, name: String, description: String, category: String, time: String, timeInMillis: Long) {
        cancelAlarm(existing)
        val updated = existing.copy(
            name = name,
            description = description.ifEmpty { "No description" },
            time = time,
            timeInMillis = timeInMillis,
            category = category.ifEmpty { "General" },
            isChecked = true
        )
        viewModelScope.launch {
            reminderDao.updateReminder(updated)
        }
        scheduleAlarm(updated)
    }

    fun setChecked(reminder: Reminder, checked: Boolean) {
        val updated = reminder.copy(isChecked = checked)
        viewModelScope.launch {
            reminderDao.updateReminder(updated)
        }
        if (checked) scheduleAlarm(updated) else cancelAlarm(updated)
    }

    fun deleteReminder(reminder: Reminder) {
        cancelAlarm(reminder)
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }

    private fun scheduleAlarm(reminder: Reminder) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("REMINDER_NAME", reminder.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Exact alarm permission not granted; silently ignore.
        }
    }

    private fun cancelAlarm(reminder: Reminder) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    class Factory(
        private val application: Application,
        private val reminderDao: ReminderDao
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
                return ReminderViewModel(application, reminderDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
