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

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

class NotificationPreferences(private val context: Context) {
    private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications_list")
    private val gson = Gson()

    val notificationsFlow: Flow<List<NotificationItem>> = context.notificationDataStore.data.map { preferences ->
        val json = preferences[NOTIFICATIONS_KEY] ?: "[]"
        val type = object : TypeToken<List<NotificationItem>>() {}.type
        gson.fromJson(json, type)
    }

    suspend fun saveNotification(notification: NotificationItem) {
        context.notificationDataStore.edit { preferences ->
            val currentJson = preferences[NOTIFICATIONS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val currentList: MutableList<NotificationItem> = gson.fromJson(currentJson, type)
            
            currentList.add(0, notification) // Add at the beginning
            
            // Keep only last 50 notifications
            val limitedList = if (currentList.size > 50) currentList.take(50) else currentList
            
            preferences[NOTIFICATIONS_KEY] = gson.toJson(limitedList)
        }
        
        // Also update unread status in UserPreferences
        UserPreferences(context).setHasUnreadNotifications(true)
    }

    suspend fun markAsRead(notificationId: String) {
        context.notificationDataStore.edit { preferences ->
            val currentJson = preferences[NOTIFICATIONS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val currentList: MutableList<NotificationItem> = gson.fromJson(currentJson, type)
            
            val index = currentList.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                currentList[index] = currentList[index].copy(isRead = true)
            }
            
            preferences[NOTIFICATIONS_KEY] = gson.toJson(currentList)
        }
    }

    suspend fun clearAll() {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = "[]"
        }
    }
}
