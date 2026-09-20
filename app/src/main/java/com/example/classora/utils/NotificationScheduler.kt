package com.example.classora.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.classora.receiver.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {

    fun scheduleNotification(
        context: Context,
        taskId: String,
        title: String,
        message: String,
        date: String, // YYYY-MM-DD
        time: String, // HH:mm AM/PM
        repeatType: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        
        try {
            val dateObj = dateFormat.parse("$date $time")
            if (dateObj != null) {
                calendar.time = dateObj
                
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    if (repeatType == "Everyday") {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else if (repeatType == "Weekdays") {
                        do {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    } else if (repeatType == "Weekends") {
                         do {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        } while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
                    } else {
                        return // Past date, no repeat
                    }
                }

                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("taskId", taskId)
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("repeatType", repeatType)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun cancelNotification(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleNextOccurrence(context: Context, taskId: String, title: String, message: String, repeatType: String) {
        val calendar = Calendar.getInstance()
        
        when (repeatType) {
            "Everyday" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            "Weekdays" -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            "Weekends" -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
            }
            else -> return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("repeatType", repeatType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun showLowAttendanceNotification(context: Context, percentage: Float) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskId", "low_attendance_alert")
            putExtra("title", "Low Attendance Alert!")
            putExtra("message", "Please attend classes, your attendance is only ${String.format("%.1f", percentage)}%!")
        }
        context.sendBroadcast(intent)
    }

    fun showInfoNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskId", "info_update_${System.currentTimeMillis()}")
            putExtra("title", title)
            putExtra("message", message)
            putExtra("type", "info")
        }
        context.sendBroadcast(intent)
    }
}
