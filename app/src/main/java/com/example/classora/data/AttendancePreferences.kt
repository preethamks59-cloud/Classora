package com.example.classora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.attendanceDataStore by preferencesDataStore(name = "attendance")

class AttendancePreferences(private val context: Context) {
    private val TIMETABLE_VERSIONS_KEY = stringPreferencesKey("timetable_versions")
    private val ATTENDANCE_KEY = stringPreferencesKey("attendance_records")
    private val OVERRIDES_KEY = stringPreferencesKey("timetable_overrides")
    private val gson = Gson()

    val timetableVersionsFlow: Flow<List<TimetableVersion>> = context.attendanceDataStore.data.map { preferences ->
        val json = preferences[TIMETABLE_VERSIONS_KEY] ?: "[]"
        val type = object : TypeToken<List<TimetableVersion>>() {}.type
        gson.fromJson(json, type)
    }

    val attendanceFlow: Flow<List<AttendanceRecord>> = context.attendanceDataStore.data.map { preferences ->
        val json = preferences[ATTENDANCE_KEY] ?: "[]"
        val type = object : TypeToken<List<AttendanceRecord>>() {}.type
        gson.fromJson(json, type)
    }

    val overridesFlow: Flow<List<TimetableOverride>> = context.attendanceDataStore.data.map { preferences ->
        val json = preferences[OVERRIDES_KEY] ?: "[]"
        val type = object : TypeToken<List<TimetableOverride>>() {}.type
        gson.fromJson(json, type)
    }

    suspend fun saveTimetableOverride(override: TimetableOverride) {
        context.attendanceDataStore.edit { preferences ->
            val currentJson = preferences[OVERRIDES_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<TimetableOverride>>() {}.type
            val currentOverrides: MutableList<TimetableOverride> = gson.fromJson(currentJson, type)
            
            currentOverrides.removeAll { it.date == override.date }
            currentOverrides.add(override)
            preferences[OVERRIDES_KEY] = gson.toJson(currentOverrides)
        }
    }

    suspend fun saveTimetableVersion(version: TimetableVersion) {
        context.attendanceDataStore.edit { preferences ->
            val currentJson = preferences[TIMETABLE_VERSIONS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<TimetableVersion>>() {}.type
            val currentVersions: MutableList<TimetableVersion> = gson.fromJson(currentJson, type)
            
            currentVersions.add(version)
            preferences[TIMETABLE_VERSIONS_KEY] = gson.toJson(currentVersions)
        }
    }

    suspend fun updateTimetableVersions(versions: List<TimetableVersion>) {
        context.attendanceDataStore.edit { it[TIMETABLE_VERSIONS_KEY] = gson.toJson(versions) }
    }

    suspend fun markAttendance(record: AttendanceRecord) {
        context.attendanceDataStore.edit { preferences ->
            val currentJson = preferences[ATTENDANCE_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<AttendanceRecord>>() {}.type
            val currentList: MutableList<AttendanceRecord> = gson.fromJson(currentJson, type)
            
            currentList.removeAll { it.date == record.date && it.subject == record.subject }
            currentList.add(record)
            
            preferences[ATTENDANCE_KEY] = gson.toJson(currentList)
        }
    }

    suspend fun markAllAttendance(records: List<AttendanceRecord>) {
        context.attendanceDataStore.edit { preferences ->
            val currentJson = preferences[ATTENDANCE_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<AttendanceRecord>>() {}.type
            val currentList: MutableList<AttendanceRecord> = gson.fromJson(currentJson, type)
            
            records.forEach { record ->
                currentList.removeAll { it.date == record.date && it.subject == record.subject }
                currentList.add(record)
            }
            
            preferences[ATTENDANCE_KEY] = gson.toJson(currentList)
        }
    }

    suspend fun clearAttendance() {
        context.attendanceDataStore.edit { it[ATTENDANCE_KEY] = "[]" }
    }
}
