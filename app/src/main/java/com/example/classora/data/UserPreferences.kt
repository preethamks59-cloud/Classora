package com.example.classora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        val NAME = stringPreferencesKey("user_name")
        val STUDENT_ID = stringPreferencesKey("student_id")
        val DOB = stringPreferencesKey("dob")
        val COURSE = stringPreferencesKey("course")
        val SEMESTER = stringPreferencesKey("semester")
        val COLLEGE = stringPreferencesKey("college")
        val CLASS_NAME = stringPreferencesKey("class_name")
        val PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
        val HAS_UNREAD_NOTIFICATIONS = stringPreferencesKey("has_unread_notifications")
        val ACADEMIC_DATA = stringPreferencesKey("academic_data")
    }

    val userFlow: Flow<UserData> = context.dataStore.data.map { preferences ->
        UserData(
            name = preferences[NAME] ?: "",
            studentId = preferences[STUDENT_ID] ?: "",
            dob = preferences[DOB] ?: "",
            course = preferences[COURSE] ?: "",
            semester = preferences[SEMESTER] ?: "",
            college = preferences[COLLEGE] ?: "",
            className = preferences[CLASS_NAME] ?: "",
            profileImageUri = preferences[PROFILE_IMAGE_URI] ?: "",
            hasUnreadNotifications = preferences[HAS_UNREAD_NOTIFICATIONS]?.toBoolean() ?: false
        )
    }

    val academicFlow: Flow<AcademicData> = context.dataStore.data.map { preferences ->
        val json = preferences[ACADEMIC_DATA] ?: ""
        if (json.isEmpty()) AcademicData()
        else gson.fromJson(json, AcademicData::class.java)
    }

    suspend fun saveUserData(userData: UserData) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = userData.name
            preferences[STUDENT_ID] = userData.studentId
            preferences[DOB] = userData.dob
            preferences[COURSE] = userData.course
            preferences[SEMESTER] = userData.semester
            preferences[COLLEGE] = userData.college
            preferences[CLASS_NAME] = userData.className
            preferences[PROFILE_IMAGE_URI] = userData.profileImageUri
            preferences[HAS_UNREAD_NOTIFICATIONS] = userData.hasUnreadNotifications.toString()
        }
    }

    suspend fun setHasUnreadNotifications(hasUnread: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_UNREAD_NOTIFICATIONS] = hasUnread.toString()
        }
    }

    suspend fun saveAcademicData(academicData: AcademicData) {
        context.dataStore.edit { preferences ->
            preferences[ACADEMIC_DATA] = gson.toJson(academicData)
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class UserData(
    val name: String,
    val studentId: String,
    val dob: String,
    val course: String,
    val semester: String,
    val college: String,
    val className: String = "",
    val profileImageUri: String = "",
    val hasUnreadNotifications: Boolean = false
)

data class SemesterResult(
    val semester: String,
    val gpa: String,
    val status: String // "Completed", "Ongoing"
)

data class AcademicData(
    val currentCgpa: String = "0.0",
    val targetCgpa: String = "8.5",
    val semesterResults: List<SemesterResult> = emptyList()
)
