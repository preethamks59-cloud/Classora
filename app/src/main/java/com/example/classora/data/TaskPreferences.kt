package com.example.classora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.taskDataStore by preferencesDataStore(name = "tasks")

class TaskPreferences(private val context: Context) {
    private val TASKS_KEY = stringPreferencesKey("tasks_list")
    private val gson = Gson()

    val tasksFlow: Flow<List<Task>> = context.taskDataStore.data.map { preferences ->
        val json = preferences[TASKS_KEY] ?: "[]"
        val type = object : TypeToken<List<Task>>() {}.type
        gson.fromJson(json, type)
    }

    suspend fun saveTask(task: Task) {
        context.taskDataStore.edit { preferences ->
            val currentJson = preferences[TASKS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Task>>() {}.type
            val currentList: MutableList<Task> = gson.fromJson(currentJson, type)
            
            // If task exists, update it, otherwise add new
            val index = currentList.indexOfFirst { it.id == task.id }
            if (index != -1) {
                currentList[index] = task
            } else {
                currentList.add(task)
            }
            
            preferences[TASKS_KEY] = gson.toJson(currentList)
        }
    }

    suspend fun deleteTask(taskId: String) {
        context.taskDataStore.edit { preferences ->
            val currentJson = preferences[TASKS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Task>>() {}.type
            val currentList: MutableList<Task> = gson.fromJson(currentJson, type)
            currentList.removeAll { it.id == taskId }
            preferences[TASKS_KEY] = gson.toJson(currentList)
        }
    }
}
