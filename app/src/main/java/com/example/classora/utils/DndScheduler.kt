package com.example.classora.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.classora.data.SettingsPreferences
import com.example.classora.receiver.SilentModeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object DndScheduler {

    fun isDndDay(
        cal: Calendar, 
        repeatType: String, 
        dndDuringExams: Boolean, 
        examDates: Set<String>,
        todayOnlyDate: String = ""
    ): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(cal.time)
        val isExamDay = dndDuringExams && examDates.contains(dateStr)
        if (isExamDay) return true

        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val targetToday = todayOnlyDate.ifEmpty { sdf.format(Date()) }

        return when (repeatType) {
            "Everyday" -> true
            "Weekdays" -> dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
            "Weekends" -> dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            "Today only" -> dateStr == targetToday
            else -> true
        }
    }

    fun formatTime12Hour(timeStr: String): String {
        return try {
            val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parser.parse(timeStr)
            if (date != null) formatter.format(date) else timeStr
        } catch (e: Exception) {
            timeStr
        }
    }

    fun scheduleDnd(
        context: Context, 
        startTime: String, 
        endTime: String, 
        activateImmediately: Boolean = false,
        repeatType: String = "",
        showUserToast: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settingsPreferences = SettingsPreferences(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val settings = settingsPreferences.settingsFlow.first()
            if (!settings.autoDndEnabled) return@launch

            val effectiveRepeatType = repeatType.ifEmpty { settings.dndRepeatType }
            val now = Calendar.getInstance()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            val effectiveTodayOnlyDate = if (effectiveRepeatType == "Today only") {
                settings.todayOnlyDate.ifEmpty { todayStr }
            } else ""

            val examDatesSet = settings.examDates.split(",").filter { it.isNotEmpty() }.toSet()
            val endToday = getCalendarFromTime(now, endTime).apply { add(Calendar.MINUTE, 10) }

            // 0. Check if "Today only" filter is selected and target date/time has already passed
            if (effectiveRepeatType == "Today only") {
                if (effectiveTodayOnlyDate < todayStr || (effectiveTodayOnlyDate == todayStr && now.after(endToday))) {
                    if (showUserToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Schedule time for today has already passed (${formatTime12Hour(endTime)})",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    val normalIntent = Intent(context, SilentModeReceiver::class.java).apply {
                        action = SilentModeReceiver.ACTION_SET_NORMAL
                    }
                    context.sendBroadcast(normalIntent)
                    return@launch
                }
            }
            
            // 1. Handle Immediate Activation for Today
            var shouldBeSilentNow = false
            if (activateImmediately) {
                val isTodayDnd = isDndDay(now, effectiveRepeatType, settings.dndDuringExams, examDatesSet, effectiveTodayOnlyDate)

                if (isTodayDnd) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                    val isExamDayToday = settings.dndDuringExams && examDatesSet.contains(dateStr)

                    if (isExamDayToday) {
                        shouldBeSilentNow = true
                    } else {
                        val startToday = getCalendarFromTime(now, startTime).apply { add(Calendar.MINUTE, -10) }
                        if (now.after(startToday) && now.before(endToday)) {
                            shouldBeSilentNow = true
                        }
                    }
                }

                if (shouldBeSilentNow) {
                    Log.d("DndScheduler", "Immediate activation: Setting silent mode & DND")
                    val silentIntent = Intent(context, SilentModeReceiver::class.java).apply {
                        action = SilentModeReceiver.ACTION_SET_SILENT
                    }
                    context.sendBroadcast(silentIntent)
                    if (showUserToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Silent mode activated until ${formatTime12Hour(endTime)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Log.d("DndScheduler", "Immediate activation: Outside silent window, restoring normal")
                    val normalIntent = Intent(context, SilentModeReceiver::class.java).apply {
                        action = SilentModeReceiver.ACTION_SET_NORMAL
                    }
                    context.sendBroadcast(normalIntent)
                }
            }

            // 2. Schedule Next Start Alarm
            val nextStart = findNextTime(now, startTime, -10, effectiveRepeatType, settings.dndDuringExams, examDatesSet, isStart = true, effectiveTodayOnlyDate)
            if (nextStart != null) {
                val startIntent = Intent(context, SilentModeReceiver::class.java).apply {
                    action = SilentModeReceiver.ACTION_SET_SILENT
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context, 100, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, nextStart.timeInMillis, startPendingIntent)
                Log.d("DndScheduler", "Next Start Alarm: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(nextStart.time)}")

                if (showUserToast && activateImmediately && !shouldBeSilentNow) {
                    withContext(Dispatchers.Main) {
                        val startFormatted = formatTime12Hour(SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextStart.time))
                        val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(nextStart.time) == todayStr
                        val dayText = if (isToday) "today" else "for next schedule"
                        Toast.makeText(
                            context,
                            "Silent mode scheduled $dayText at $startFormatted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // 3. Schedule Next End Alarm
            val nextEnd = findNextTime(now, endTime, 10, effectiveRepeatType, settings.dndDuringExams, examDatesSet, isStart = false, effectiveTodayOnlyDate)
            if (nextEnd != null) {
                val endIntent = Intent(context, SilentModeReceiver::class.java).apply {
                    action = SilentModeReceiver.ACTION_SET_NORMAL
                }
                val endPendingIntent = PendingIntent.getBroadcast(
                    context, 101, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, nextEnd.timeInMillis, endPendingIntent)
                Log.d("DndScheduler", "Next End Alarm: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(nextEnd.time)}")
            }
        }
    }

    private fun findNextTime(
        baseTime: Calendar,
        timeStr: String,
        offsetMin: Int,
        repeatType: String,
        dndDuringExams: Boolean,
        examDates: Set<String>,
        isStart: Boolean,
        todayOnlyDate: String = ""
    ): Calendar? {
        val searchCal = baseTime.clone() as Calendar
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetTodayDate = todayOnlyDate.ifEmpty { sdf.format(baseTime.time) }
        
        // Search up to 365 days
        for (dayOffset in 0..365) {
            val dateStr = sdf.format(searchCal.time)
            val dow = searchCal.get(Calendar.DAY_OF_WEEK)
            
            val isExamDay = dndDuringExams && examDates.contains(dateStr)
            val isRegularDay = when (repeatType) {
                "Everyday" -> true
                "Weekdays" -> dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
                "Weekends" -> dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
                "Today only" -> dateStr == targetTodayDate
                else -> true
            }

            if (isExamDay || isRegularDay) {
                val targetTime = searchCal.clone() as Calendar
                if (isExamDay) {
                    if (isStart) {
                        targetTime.set(Calendar.HOUR_OF_DAY, 0)
                        targetTime.set(Calendar.MINUTE, 0)
                    } else {
                        targetTime.set(Calendar.HOUR_OF_DAY, 23)
                        targetTime.set(Calendar.MINUTE, 59)
                    }
                } else {
                    val parts = timeStr.split(":")
                    targetTime.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    targetTime.set(Calendar.MINUTE, parts[1].toInt())
                    targetTime.add(Calendar.MINUTE, offsetMin)
                }
                
                targetTime.set(Calendar.SECOND, 0)
                targetTime.set(Calendar.MILLISECOND, 0)

                if (targetTime.after(baseTime)) {
                    return targetTime
                }
            }
            
            if (repeatType == "Today only") {
                if (dateStr != targetTodayDate) break
            }
            
            searchCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
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
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    private fun getCalendarFromTime(baseDate: Calendar, time: String): Calendar {
        val parts = time.split(":")
        return (baseDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun cancelDnd(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val startIntent = Intent(context, SilentModeReceiver::class.java).apply { action = SilentModeReceiver.ACTION_SET_SILENT }
        val startPendingIntent = PendingIntent.getBroadcast(context, 100, startIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        val endIntent = Intent(context, SilentModeReceiver::class.java).apply { action = SilentModeReceiver.ACTION_SET_NORMAL }
        val endPendingIntent = PendingIntent.getBroadcast(context, 101, endIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }

        context.sendBroadcast(Intent(context, SilentModeReceiver::class.java).apply { action = SilentModeReceiver.ACTION_SET_NORMAL })
    }
}
