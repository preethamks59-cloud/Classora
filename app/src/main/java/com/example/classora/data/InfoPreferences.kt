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

    val deletedInfoFlow: Flow<List<InfoNotification>> = context.infoDataStore.data.map { preferences ->
        val json = preferences[infoKey] ?: "[]"
        val type = object : TypeToken<List<InfoNotification>>() {}.type
        val allInfo: List<InfoNotification> = gson.fromJson(json, type)
        val deletedIds = preferences[deletedKey] ?: emptySet()
        allInfo.filter { it.id in deletedIds }
    }

    suspend fun syncInfoNotifications(remoteInfo: List<InfoNotification>): List<InfoNotification> {
        val newlyAdded = mutableListOf<InfoNotification>()
        
        context.infoDataStore.edit { editPrefs ->
            val currentJson = editPrefs[infoKey] ?: "[]"
            val type = object : TypeToken<MutableList<InfoNotification>>() {}.type
            val localList: MutableList<InfoNotification> = gson.fromJson(currentJson, type)
            
            val notifiedIds = (editPrefs[notifiedKey] ?: emptySet()).toMutableSet()
            val deletedIds = (editPrefs[deletedKey] ?: emptySet()).toMutableSet()

            // 1. Remove items that are no longer in the remote source
            val remoteIds = remoteInfo.map { it.id }.toSet()
            val iterator = localList.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.id !in remoteIds) {
                    iterator.remove()
                    deletedIds.remove(item.id)
                    notifiedIds.remove(item.id)
                }
            }

            // 2. Add new items from remote
            remoteInfo.forEach { remoteItem ->
                if (localList.none { it.id == remoteItem.id }) {
                    localList.add(0, remoteItem)
                    if (remoteItem.id !in deletedIds) {
                        newlyAdded.add(remoteItem)
                    }
                }
            }

            // 3. Save back
            val limitedList = if (localList.size > 150) localList.take(150) else localList
            editPrefs[infoKey] = gson.toJson(limitedList)
            editPrefs[deletedKey] = deletedIds
            editPrefs[notifiedKey] = notifiedIds
        }
        
        return newlyAdded
    }

    // Keep legacy method for compatibility if needed, but redirects to sync
    suspend fun saveInfoNotifications(newInfo: List<InfoNotification>): List<InfoNotification> {
        return syncInfoNotifications(newInfo)
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

    suspend fun restoreMultipleInfo(ids: Set<String>) {
        context.infoDataStore.edit { preferences ->
            val deletedIds = (preferences[deletedKey] ?: emptySet()).toMutableSet()
            deletedIds.removeAll(ids)
            preferences[deletedKey] = deletedIds
        }
    }
}
