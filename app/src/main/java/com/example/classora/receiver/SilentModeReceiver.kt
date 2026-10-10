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
        val settingsPreferences = SettingsPreferences(context)

        when (action) {
            ACTION_SET_SILENT -> {
                Log.d("SilentModeReceiver", "Attempting to silence phone")
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = settingsPreferences.settingsFlow.first()
                    
                    val now = Calendar.getInstance()
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                    val examDatesSet = settings.examDates.split(",").filter { it.isNotEmpty() }.toSet()
                    
                    val isExamDay = settings.dndDuringExams && examDatesSet.contains(todayStr)
                    val isRegularDndDay = DndScheduler.isDndDay(now, settings.dndRepeatType, settings.dndDuringExams, examDatesSet, settings.todayOnlyDate)

                    if (settings.autoDndEnabled && (isRegularDndDay || isExamDay)) {
                        try {
                            // 1. DND Interruption Filter (if option is enabled & permission granted)
                            if (settings.useDndWithSilentMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                            }

                            // 2. ALWAYS set ringer mode to SILENT / VIBRATE
                            if (settings.useDndWithSilentMode) {
                                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            } else {
                                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            }

                            // 3. Mute ring and notification streams explicitly
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                            }
                            Log.d("SilentModeReceiver", "Silent mode & DND set successfully")
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
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = settingsPreferences.settingsFlow.first()
                    val now = Calendar.getInstance()
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                    val isExamDay = settings.dndDuringExams && settings.examDates.split(",").contains(todayStr)
                    
                    val isEndOfExamDay = now.get(Calendar.HOUR_OF_DAY) == 23 && now.get(Calendar.MINUTE) >= 50
                    
                    if (settings.autoDndEnabled && isExamDay && !isEndOfExamDay) {
                        Log.d("SilentModeReceiver", "Still exam day, skipping normal mode restoration")
                        return@launch
                    }

                    try {
                        // 1. Clear Do Not Disturb interruption filter FIRST on Android M+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }

                        // 2. Reset ringer mode to Normal
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        
                        // 3. Unmute volume streams explicitly
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                        }

                        // 4. Ensure non-zero ring and notification volume
                        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                        if (currentRing == 0) {
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, (maxRing * 0.7).toInt().coerceAtLeast(1), 0)
                        }

                        val maxNotif = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                        val currentNotif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                        if (currentNotif == 0) {
                            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, (maxNotif * 0.7).toInt().coerceAtLeast(1), 0)
                        }

                        Log.d("SilentModeReceiver", "Normal mode restored successfully")
                    } catch (e: Exception) {
                        Log.e("SilentModeReceiver", "Failed to restore normal mode", e)
                    }
                }
            }
        }

        // Always reschedule future DND alarms whenever one triggers
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
