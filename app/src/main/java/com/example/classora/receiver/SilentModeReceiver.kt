package com.example.classora.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.classora.data.SettingsPreferences
import com.example.classora.utils.DndScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SilentModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_SET_SILENT -> {
                Log.d("SilentModeReceiver", "Attempting to silence phone")
                val settingsPreferences = SettingsPreferences(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = settingsPreferences.settingsFlow.first()
                    
                    // Check if we should silence today based on regular DND or Exam DND
                    val currentDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    
                    val isRegularDndDay = when(settings.dndRepeatType) {
                        "Weekdays" -> currentDow != Calendar.SATURDAY && currentDow != Calendar.SUNDAY
                        "Weekends" -> currentDow == Calendar.SATURDAY || currentDow == Calendar.SUNDAY
                        "Everyday" -> true
                        "Today only" -> true
                        else -> true
                    }
                    
                    val isExamDay = settings.dndDuringExams && 
                            settings.examDates.split(",").contains(todayStr)

                    if (isRegularDndDay || isExamDay) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                    Log.d("SilentModeReceiver", "Ringer set to SILENT")
                                } else {
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                                    Log.d("SilentModeReceiver", "Ringer set to VIBRATE (No DND permission)")
                                }
                            } else {
                                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            }
                        } catch (e: Exception) {
                            Log.e("SilentModeReceiver", "Failed to set silent mode", e)
                        }
                    } else {
                        Log.d("SilentModeReceiver", "Not a DND day, skipping silence")
                    }
                }
            }
            ACTION_SET_NORMAL -> {
                Log.d("SilentModeReceiver", "Restoring normal mode")
                try {
                    // 1. Reset ringer mode to Normal
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    
                    // 2. Unmute streams explicitly
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                        
                        // 3. Clear DND filter just in case it's lingering
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    }
                    Log.d("SilentModeReceiver", "Normal mode restored successfully")
                } catch (e: Exception) {
                    Log.e("SilentModeReceiver", "Failed to restore normal mode", e)
                }
            }
        }

        // Always reschedule future DND alarms whenever one triggers to ensure everyday repetition
        val settingsPreferences = SettingsPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
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

    companion object {
        const val ACTION_SET_SILENT = "com.example.classora.ACTION_SET_SILENT"
        const val ACTION_SET_NORMAL = "com.example.classora.ACTION_SET_NORMAL"
    }
}
