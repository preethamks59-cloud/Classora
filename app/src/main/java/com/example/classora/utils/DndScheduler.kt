package com.example.classora.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.classora.data.SettingsPreferences
import com.example.classora.receiver.SilentModeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object DndScheduler {
    fun scheduleDnd(
        context: Context, 
        startTime: String, 
        endTime: String, 
        activateImmediately: Boolean = false,
        repeatType: String = "Everyday"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        
        // Original Times for today
        val startCalendar = getCalendarFromTime(startTime)
        startCalendar.add(Calendar.MINUTE, -10)
        
        val endCalendar = getCalendarFromTime(endTime)
        endCalendar.add(Calendar.MINUTE, 10)

        val settingsPreferences = SettingsPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsPreferences.settingsFlow.first()
            val examDatesSet = settings.examDates.split(",").filter { it.isNotEmpty() }.toSet()
            
            // Only activate immediately if explicitly requested (e.g., from UI Switch)
            if (activateImmediately && now >= startCalendar.timeInMillis && now <= endCalendar.timeInMillis) {
                val currentDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                val isExamDay = settings.dndDuringExams && examDatesSet.contains(todayStr)

                val shouldApplyToday = when(repeatType) {
                    "Weekdays" -> currentDow != Calendar.SATURDAY && currentDow != Calendar.SUNDAY
                    "Weekends" -> currentDow == Calendar.SATURDAY || currentDow == Calendar.SUNDAY
                    else -> true // Everyday or Today only
                }

                if (shouldApplyToday || isExamDay) {
                    Log.d("DndScheduler", "Within window, activating silent mode immediately")
                    val silentIntent = Intent(context, SilentModeReceiver::class.java).apply {
                        action = SilentModeReceiver.ACTION_SET_SILENT
                    }
                    context.sendBroadcast(silentIntent)
                }
            }

            // Schedule Start Alarm
            if (adjustToNextValidDay(startCalendar, repeatType, settings.dndDuringExams, examDatesSet)) {
                val startIntent = Intent(context, SilentModeReceiver::class.java).apply {
                    action = SilentModeReceiver.ACTION_SET_SILENT
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context, 100, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, startCalendar.timeInMillis, startPendingIntent)
            }

            // Schedule End Alarm
            if (adjustToNextValidDay(endCalendar, repeatType, settings.dndDuringExams, examDatesSet)) {
                val endIntent = Intent(context, SilentModeReceiver::class.java).apply {
                    action = SilentModeReceiver.ACTION_SET_NORMAL
                }
                val endPendingIntent = PendingIntent.getBroadcast(
                    context, 101, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, endCalendar.timeInMillis, endPendingIntent)
            }
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

    private fun adjustToNextValidDay(
        calendar: Calendar, 
        repeatType: String, 
        dndDuringExams: Boolean, 
        examDates: Set<String>
    ): Boolean {
        val now = System.currentTimeMillis()
        
        if (calendar.timeInMillis <= now) {
            if (repeatType == "Today only") return false
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        } else if (repeatType == "Today only") {
            return true
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        while (true) {
            val dow = calendar.get(Calendar.DAY_OF_WEEK)
            val dateStr = sdf.format(calendar.time)
            
            val isRegularDay = when (repeatType) {
                "Everyday" -> true
                "Weekdays" -> dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
                "Weekends" -> dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
                else -> true
            }
            
            val isExamDay = dndDuringExams && examDates.contains(dateStr)
            
            if (isRegularDay || isExamDay) return true
            
            // Limit search to 1 year to avoid infinite loop
            if (calendar.timeInMillis > now + (366L * 24 * 60 * 60 * 1000)) return false

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    fun cancelDnd(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val startIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = SilentModeReceiver.ACTION_SET_SILENT
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context, 100, startIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        val endIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = SilentModeReceiver.ACTION_SET_NORMAL
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context, 101, endIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }

        val restoreIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = SilentModeReceiver.ACTION_SET_NORMAL
        }
        context.sendBroadcast(restoreIntent)
    }

    private fun getCalendarFromTime(time: String): Calendar {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
