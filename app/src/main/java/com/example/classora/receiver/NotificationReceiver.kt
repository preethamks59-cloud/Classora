package com.example.classora.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.classora.MainActivity
import com.example.classora.data.NotificationItem
import com.example.classora.data.NotificationPreferences
import com.example.classora.data.TaskPreferences
import com.example.classora.data.UserPreferences
import com.example.classora.utils.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getStringExtra("taskId") ?: ""
        
        if (action == "ACTION_MARK_COMPLETED" && taskId.isNotEmpty()) {
            val notificationId = intent.getIntExtra("notificationId", -1)
            val taskPreferences = TaskPreferences(context)
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val tasks = taskPreferences.tasksFlow.first()
                    val task = tasks.find { it.id == taskId }
                    if (task != null) {
                        taskPreferences.saveTask(task.copy(isCompleted = true))
                        NotificationScheduler.cancelNotification(context, taskId)
                    }
                    if (notificationId != -1) {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                    }
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error marking completed", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val title = intent.getStringExtra("title") ?: "Task Reminder"
        val message = intent.getStringExtra("message") ?: "You have a task scheduled now."
        val repeatType = intent.getStringExtra("repeatType") ?: "Does not repeat"
        val type = intent.getStringExtra("type") ?: "task"

        val notificationPrefs = NotificationPreferences(context)
        val userPrefs = UserPreferences(context)
        val settingsPrefs = com.example.classora.data.SettingsPreferences(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsPrefs.settingsFlow.first()
                if (settings.notificationsEnabled) {
                    showNotification(context, title, message, taskId, repeatType, type)
                }

                notificationPrefs.saveNotification(
                    NotificationItem(
                        taskId = taskId,
                        title = title,
                        message = message
                    )
                )
                userPrefs.setHasUnreadNotifications(true)
                
                if (repeatType != "Does not repeat" && taskId.isNotEmpty()) {
                    NotificationScheduler.scheduleNextOccurrence(context, taskId, title, message, repeatType)
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error processing notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, taskId: String, repeatType: String, type: String = "task") {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "classora_alerts_v11"
            val soundUri = try { RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) } catch (e: Exception) { null }
            
            val notificationId = if (taskId.isNotEmpty()) Math.abs(taskId.hashCode()) else Math.abs((System.currentTimeMillis() + (1000..9999).random()).toInt())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    channelId,
                    "Reminders & Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alerts for tasks and attendance"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    if (soundUri != null) {
                        try { setSound(soundUri, audioAttributes) } catch (e: Exception) { Log.e("NotificationReceiver", "Sound set failed", e) }
                    }
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", if (type == "info") "VIEW_NOTIFICATIONS" else "VIEW_TASK")
                putExtra("taskId", taskId)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.classora.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(if (type == "info") NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (soundUri != null) {
                try { builder.setSound(soundUri) } catch (e: Exception) { Log.e("NotificationReceiver", "Builder setSound failed", e) }
            }

            if (type != "info") {
                val reqCodeComplete = Math.abs((taskId + "_complete").hashCode())
                val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = "ACTION_MARK_COMPLETED"
                    putExtra("taskId", taskId)
                    putExtra("notificationId", notificationId)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqCodeComplete,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val reqCodeReschedule = Math.abs((taskId + "_reschedule").hashCode())
                val rescheduleIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("action", "RESCHEDULE_TASK")
                    putExtra("taskId", taskId)
                }
                val reschedulePendingIntent = PendingIntent.getActivity(
                    context,
                    reqCodeReschedule,
                    rescheduleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.addAction(android.R.drawable.ic_menu_edit, "Reschedule", reschedulePendingIntent)
                builder.addAction(android.R.drawable.ic_input_add, "Mark Completed", completePendingIntent)
            }

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error displaying notification", e)
        }
    }
}
