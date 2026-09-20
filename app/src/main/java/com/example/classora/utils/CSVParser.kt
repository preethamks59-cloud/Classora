package com.example.classora.utils

import android.content.Context
import android.net.Uri
import com.example.classora.data.TimetableEntry

object CSVParser {
    fun parseCsv(content: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        // Normalize line endings to \n
        val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalizedContent.split("\n")
        
        for (line in lines) {
            if (line.isBlank()) continue
            
            val fields = mutableListOf<String>()
            var currentField = StringBuilder()
            var inQuotes = false
            var i = 0
            
            while (i < line.length) {
                val char = line[i]
                when {
                    char == '\"' -> {
                        if (inQuotes && i + 1 < line.length && line[i+1] == '\"') {
                            currentField.append('\"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    }
                    char == ',' && !inQuotes -> {
                        fields.add(currentField.toString().trim())
                        currentField = StringBuilder()
                    }
                    else -> currentField.append(char)
                }
                i++
            }
            fields.add(currentField.toString().trim())
            result.add(fields)
        }
        return result
    }

    fun parseTimetableText(text: String): List<TimetableEntry> {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return emptyList()

        // Detect header: if first row contains keywords like "subject", "day", "time"
        val firstRow = rows[0]
        val isHeader = firstRow.any { it.contains("subject", true) || it.contains("day", true) || it.contains("time", true) }
        
        val dataRows = if (isHeader) rows.drop(1) else rows
        
        return dataRows.mapNotNull { parts ->
            // Support formats with 3 or more columns
            if (parts.size >= 3) {
                val subject = parts[0].trim()
                val day = parts[1].trim()
                val time = parts[2].trim()
                
                if (subject.isNotEmpty() && day.isNotEmpty()) {
                    TimetableEntry(
                        subject = subject,
                        day = day,
                        time = time
                    )
                } else null
            } else null
        }
    }

    fun parseTimetableCsv(context: Context, uri: Uri): List<TimetableEntry> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: ""
            
            if (content.isBlank()) return emptyList()
            
            parseTimetableText(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
