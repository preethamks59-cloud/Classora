package com.example.classora.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.documentDataStore by preferencesDataStore(name = "college_documents")

class DocumentPreferences(private val context: Context) {
    private val DOCS_KEY = stringPreferencesKey("docs_list")
    private val PINNED_KEY = stringSetPreferencesKey("pinned_categories")
    private val gson = Gson()

    val documentsFlow: Flow<List<CollegeDocument>> = context.documentDataStore.data.map { preferences ->
        val json = preferences[DOCS_KEY] ?: "[]"
        val type = object : TypeToken<List<CollegeDocument>>() {}.type
        gson.fromJson(json, type)
    }

    val pinnedCategoriesFlow: Flow<Set<String>> = context.documentDataStore.data.map { preferences ->
        preferences[PINNED_KEY] ?: emptySet()
    }

    suspend fun savePinnedCategories(categories: Set<String>) {
        context.documentDataStore.edit { preferences ->
            preferences[PINNED_KEY] = categories
        }
    }

    suspend fun saveDocument(doc: CollegeDocument) {
        context.documentDataStore.edit { preferences ->
            val currentJson = preferences[DOCS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CollegeDocument>>() {}.type
            val currentList: MutableList<CollegeDocument> = gson.fromJson(currentJson, type)
            
            val index = currentList.indexOfFirst { it.id == doc.id }
            if (index != -1) {
                currentList[index] = doc
            } else {
                currentList.add(doc)
            }
            
            preferences[DOCS_KEY] = gson.toJson(currentList)
        }
    }

    suspend fun deleteDocument(docId: String) {
        context.documentDataStore.edit { preferences ->
            val currentJson = preferences[DOCS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CollegeDocument>>() {}.type
            val currentList: MutableList<CollegeDocument> = gson.fromJson(currentJson, type)
            currentList.removeAll { it.id == docId }
            preferences[DOCS_KEY] = gson.toJson(currentList)
        }
    }
}
