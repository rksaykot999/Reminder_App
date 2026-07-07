package com.example.reminderapp.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.app.NotificationManager
import android.media.Ringtone

class PomodoroAlarmReceiver : BroadcastReceiver() {
    companion object {
        var ringtone: Ringtone? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == "STOP_ALARM") {
            ringtone?.stop()
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1)
        } else {
            // Start alarm sound
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        }
    }
}
