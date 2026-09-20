package com.example.classora.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.classora.data.TaskPreferences
import com.example.classora.data.SettingsPreferences
import com.example.classora.utils.NotificationScheduler
import com.example.classora.utils.DndScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val taskPreferences = TaskPreferences(context)
            val settingsPreferences = SettingsPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                // Reschedule Tasks
                val tasks = taskPreferences.tasksFlow.first()
                tasks.filter { !it.isCompleted }.forEach { task ->
                    NotificationScheduler.scheduleNotification(
                        context = context,
                        taskId = task.id,
                        title = task.title,
                        message = task.description.ifBlank { "You have a task scheduled now." },
                        date = task.date,
                        time = task.time,
                        repeatType = task.repeatType
                    )
                }

                // Reschedule DND
                val settings = settingsPreferences.settingsFlow.first()
                if (settings.autoDndEnabled) {
                    DndScheduler.scheduleDnd(
                        context = context, 
                        startTime = settings.collegeStartTime, 
                        endTime = settings.collegeEndTime,
                        repeatType = settings.dndRepeatType
                    )
                }
            }
        }
    }
}
