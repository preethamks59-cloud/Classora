package com.example.classora.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsPreferences(private val context: Context) {
    companion object {
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_DND_ENABLED = booleanPreferencesKey("auto_dnd_enabled")
        val COLLEGE_START_TIME = androidx.datastore.preferences.core.stringPreferencesKey("college_start_time")
        val COLLEGE_END_TIME = androidx.datastore.preferences.core.stringPreferencesKey("college_end_time")
        val COLLEGE_TIME_24_HOUR = booleanPreferencesKey("college_time_24_hour")
        val HAS_SKIPPED_SPECIAL_PERMISSIONS = booleanPreferencesKey("has_skipped_special_permissions")
        val DND_REPEAT_TYPE = androidx.datastore.preferences.core.stringPreferencesKey("dnd_repeat_type")
        val TODAY_ONLY_DATE = androidx.datastore.preferences.core.stringPreferencesKey("today_only_date")
        val DND_DURING_EXAMS = booleanPreferencesKey("dnd_during_exams")
        val EXAM_DATES = androidx.datastore.preferences.core.stringPreferencesKey("exam_dates")
        val ALLOW_IMPORTANT_NOTIFS = booleanPreferencesKey("allow_important_notifs")
        val USE_DND_WITH_SILENT_MODE = booleanPreferencesKey("use_dnd_with_silent_mode")
        
        // Notification Details
        val ATTENDANCE_ALERTS = booleanPreferencesKey("attendance_alerts")
        val ACADEMIC_ALERTS = booleanPreferencesKey("academic_alerts")
        val EXAM_NOTIFICATIONS = booleanPreferencesKey("exam_notifications")
        val ASSIGNMENT_NOTIFICATIONS = booleanPreferencesKey("assignment_notifications")
        val CALENDAR_NOTIFICATIONS = booleanPreferencesKey("calendar_notifications")
        
        // Reminder Details
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val RECURRING_REMINDERS = booleanPreferencesKey("recurring_reminders")
        val CLASS_REMINDERS = booleanPreferencesKey("class_reminders")
        val STUDY_REMINDERS = booleanPreferencesKey("study_reminders")
        val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        
        // Calendar Sync
        val CALENDAR_SYNC_CONNECTED = booleanPreferencesKey("calendar_sync_connected")
        val SYNC_TIMETABLE = booleanPreferencesKey("sync_timetable")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
            soundEnabled = preferences[SOUND_ENABLED] ?: true,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            autoDndEnabled = preferences[AUTO_DND_ENABLED] ?: false,
            collegeStartTime = preferences[COLLEGE_START_TIME] ?: "09:00",
            collegeEndTime = preferences[COLLEGE_END_TIME] ?: "16:00",
            collegeTime24Hour = preferences[COLLEGE_TIME_24_HOUR] ?: false,
            hasSkippedSpecialPermissions = preferences[HAS_SKIPPED_SPECIAL_PERMISSIONS] ?: false,
            
            attendanceAlerts = preferences[ATTENDANCE_ALERTS] ?: true,
            academicAlerts = preferences[ACADEMIC_ALERTS] ?: true,
            examNotifications = preferences[EXAM_NOTIFICATIONS] ?: true,
            assignmentNotifications = preferences[ASSIGNMENT_NOTIFICATIONS] ?: true,
            calendarNotifications = preferences[CALENDAR_NOTIFICATIONS] ?: true,
            
            remindersEnabled = preferences[REMINDERS_ENABLED] ?: true,
            recurringReminders = preferences[RECURRING_REMINDERS] ?: true,
            classReminders = preferences[CLASS_REMINDERS] ?: false,
            studyReminders = preferences[STUDY_REMINDERS] ?: false,
            snoozeEnabled = preferences[SNOOZE_ENABLED] ?: true,
            
            calendarSyncConnected = preferences[CALENDAR_SYNC_CONNECTED] ?: false,
            syncTimetable = preferences[SYNC_TIMETABLE] ?: true,
            autoSync = preferences[AUTO_SYNC] ?: false,
            
            dndRepeatType = preferences[DND_REPEAT_TYPE] ?: "Weekdays",
            todayOnlyDate = preferences[TODAY_ONLY_DATE] ?: "",
            dndDuringExams = preferences[DND_DURING_EXAMS] ?: false,
            examDates = preferences[EXAM_DATES] ?: "",
            allowImportantNotifs = preferences[ALLOW_IMPORTANT_NOTIFS] ?: true,
            useDndWithSilentMode = preferences[USE_DND_WITH_SILENT_MODE] ?: true
        )
    }

    suspend fun updateVibration(enabled: Boolean) {
        context.settingsDataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateSound(enabled: Boolean) {
        context.settingsDataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateAutoDnd(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[AUTO_DND_ENABLED] = enabled
            if (enabled && (preferences[DND_REPEAT_TYPE] ?: "Weekdays") == "Today only") {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                preferences[TODAY_ONLY_DATE] = todayStr
            }
        }
    }

    suspend fun updateDndRepeatType(type: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[DND_REPEAT_TYPE] = type
            if (type == "Today only") {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                preferences[TODAY_ONLY_DATE] = todayStr
            }
        }
    }

    suspend fun updateDndDuringExams(enabled: Boolean) {
        context.settingsDataStore.edit { it[DND_DURING_EXAMS] = enabled }
    }

    suspend fun updateExamDates(dates: String) {
        context.settingsDataStore.edit { it[EXAM_DATES] = dates }
    }

    suspend fun updateAllowImportantNotifs(enabled: Boolean) {
        context.settingsDataStore.edit { it[ALLOW_IMPORTANT_NOTIFS] = enabled }
    }

    suspend fun updateUseDndWithSilentMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[USE_DND_WITH_SILENT_MODE] = enabled }
    }

    suspend fun updateCollegeStartTime(time: String) {
        context.settingsDataStore.edit { it[COLLEGE_START_TIME] = time }
    }

    suspend fun updateCollegeEndTime(time: String) {
        context.settingsDataStore.edit { it[COLLEGE_END_TIME] = time }
    }

    suspend fun updateCollegeTime24Hour(enabled: Boolean) {
        context.settingsDataStore.edit { it[COLLEGE_TIME_24_HOUR] = enabled }
    }

    suspend fun setHasSkippedSpecialPermissions(skipped: Boolean) {
        context.settingsDataStore.edit { it[HAS_SKIPPED_SPECIAL_PERMISSIONS] = skipped }
    }

    suspend fun updateNotificationSetting(key: String, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            when(key) {
                "attendance" -> prefs[ATTENDANCE_ALERTS] = enabled
                "academic" -> prefs[ACADEMIC_ALERTS] = enabled
                "exam" -> prefs[EXAM_NOTIFICATIONS] = enabled
                "assignment" -> prefs[ASSIGNMENT_NOTIFICATIONS] = enabled
                "calendar" -> prefs[CALENDAR_NOTIFICATIONS] = enabled
            }
        }
    }

    suspend fun updateReminderSetting(key: String, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            when(key) {
                "enabled" -> prefs[REMINDERS_ENABLED] = enabled
                "recurring" -> prefs[RECURRING_REMINDERS] = enabled
                "class" -> prefs[CLASS_REMINDERS] = enabled
                "study" -> prefs[STUDY_REMINDERS] = enabled
                "snooze" -> prefs[SNOOZE_ENABLED] = enabled
            }
        }
    }

    suspend fun updateCalendarSync(connected: Boolean) {
        context.settingsDataStore.edit { it[CALENDAR_SYNC_CONNECTED] = connected }
    }

    suspend fun updateSyncOption(key: String, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            when(key) {
                "timetable" -> prefs[SYNC_TIMETABLE] = enabled
                "auto" -> prefs[AUTO_SYNC] = enabled
            }
        }
    }
}

data class AppSettings(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoDndEnabled: Boolean = false,
    val collegeStartTime: String = "09:00",
    val collegeEndTime: String = "16:30",
    val collegeTime24Hour: Boolean = false,
    val hasSkippedSpecialPermissions: Boolean = false,
    
    val attendanceAlerts: Boolean = true,
    val academicAlerts: Boolean = true,
    val examNotifications: Boolean = true,
    val assignmentNotifications: Boolean = true,
    val calendarNotifications: Boolean = true,
    
    val remindersEnabled: Boolean = true,
    val recurringReminders: Boolean = true,
    val classReminders: Boolean = false,
    val studyReminders: Boolean = false,
    val snoozeEnabled: Boolean = true,
    
    val calendarSyncConnected: Boolean = false,
    val syncTimetable: Boolean = true,
    val autoSync: Boolean = false,
    
    val dndRepeatType: String = "Weekdays",
    val todayOnlyDate: String = "",
    val dndDuringExams: Boolean = false,
    val examDates: String = "",
    val allowImportantNotifs: Boolean = true,
    val useDndWithSilentMode: Boolean = true
)
