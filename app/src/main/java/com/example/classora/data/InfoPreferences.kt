package com.example.classora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.infoDataStore by preferencesDataStore(name = "info_notifications")

class InfoPreferences(private val context: Context) {
    private val infoKey = stringPreferencesKey("info_list")
    private val deletedKey = stringSetPreferencesKey("deleted_info_ids")
    private val notifiedKey = stringSetPreferencesKey("notified_info_ids")
    private val gson = Gson()

    val infoFlow: Flow<List<InfoNotification>> = context.infoDataStore.data.map { preferences ->
        val json = preferences[infoKey] ?: "[]"
        val type = object : TypeToken<List<InfoNotification>>() {}.type
        val allInfo: List<InfoNotification> = gson.fromJson(json, type)
        val deletedIds = preferences[deletedKey] ?: emptySet()
        allInfo.filter { it.id !in deletedIds }
    }

    suspend fun saveInfoNotifications(newInfo: List<InfoNotification>): List<InfoNotification> {
        // Read once to check if there are actually new items
        val preferences = context.infoDataStore.data.first()
        val currentJson = preferences[infoKey] ?: "[]"
        val type = object : TypeToken<MutableList<InfoNotification>>() {}.type
        val currentList: MutableList<InfoNotification> = gson.fromJson(currentJson, type)
        
        val actualNewItems = newInfo.filter { item -> currentList.none { it.id == item.id } }
        if (actualNewItems.isEmpty()) return emptyList()

        val newlyAdded = mutableListOf<InfoNotification>()
        context.infoDataStore.edit { editPrefs ->
            val notifiedIds = editPrefs[notifiedKey] ?: emptySet()
            val deletedIds = editPrefs[deletedKey] ?: emptySet()
            
            actualNewItems.forEach { item ->
                currentList.add(0, item)
                if (item.id !in notifiedIds && item.id !in deletedIds) {
                    newlyAdded.add(item)
                }
            }
            
            // Keep only latest 100
            val limitedList = if (currentList.size > 100) currentList.take(100) else currentList
            editPrefs[infoKey] = gson.toJson(limitedList)
        }
        return newlyAdded
    }

    suspend fun markAsNotified(ids: List<String>) {
        if (ids.isEmpty()) return
        context.infoDataStore.edit { preferences ->
            val notified = (preferences[notifiedKey] ?: emptySet()).toMutableSet()
            notified.addAll(ids)
            preferences[notifiedKey] = notified
        }
    }

    suspend fun deleteInfo(id: String) {
        context.infoDataStore.edit { preferences ->
            val deletedIds = (preferences[deletedKey] ?: emptySet()).toMutableSet()
            deletedIds.add(id)
            preferences[deletedKey] = deletedIds
        }
    }
    
    suspend fun restoreInfo(id: String) {
        context.infoDataStore.edit { preferences ->
            val deletedIds = (preferences[deletedKey] ?: emptySet()).toMutableSet()
            deletedIds.remove(id)
            preferences[deletedKey] = deletedIds
        }
    }
}
