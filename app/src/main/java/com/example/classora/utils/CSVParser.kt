package com.example.classora.utils

import android.content.Context
import android.net.Uri
import com.example.classora.data.TimetableEntry
import java.util.Calendar

object CSVParser {

    /**
     * Parse raw CSV/delimiter-separated string into a List of row fields.
     * Supports commas (,), semicolons (;), tabs (\t), or pipes (|).
     */
    fun parseCsv(content: String): List<List<String>> {
        val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
        val lines = normalizedContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Auto-detect delimiter by counting occurrences outside quotes across first few lines
        val delimiter = detectDelimiter(lines.take(5))

        val result = mutableListOf<List<String>>()
        for (line in lines) {
            val fields = parseCsvLine(line, delimiter)
            if (fields.any { it.isNotEmpty() }) {
                result.add(fields)
            }
        }
        return result
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        val candidates = listOf(',', ';', '\t', '|')
        val counts = candidates.associateWith { delim ->
            sampleLines.sumOf { line -> countDelimiterOutsideQuotes(line, delim) }
        }
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    private fun countDelimiterOutsideQuotes(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        for (ch in line) {
            if (ch == '\"') inQuotes = !inQuotes
            else if (ch == delimiter && !inQuotes) count++
        }
        return count
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        currentField.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == delimiter && !inQuotes -> {
                    fields.add(cleanField(currentField.toString()))
                    currentField = StringBuilder()
                }
                else -> currentField.append(char)
            }
            i++
        }
        fields.add(cleanField(currentField.toString()))
        return fields
    }

    private fun cleanField(str: String): String {
        return str.trim().removeSurrounding("\"").removeSurrounding("'").trim()
    }

    private val dayKeywords = listOf("day", "days", "date", "weekday", "schedule")
    private val subjectKeywords = listOf("subject", "course", "class", "lecture", "title", "topic", "name", "module", "sub", "paper")
    private val timeKeywords = listOf("time", "slot", "timing", "hour", "period", "timeslot")

    fun isDayValue(str: String): Boolean {
        val lower = str.lowercase().trim()
        if (lower.isEmpty()) return false

        val knownDays = listOf(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "mon", "tue", "wed", "thu", "fri", "sat", "sun",
            "everyday", "daily", "weekday", "weekend", "all days", "all day",
            "mon-fri", "m-f", "mon to fri", "sat-sun", "sat to sun",
            "mwf", "tth", "mon, wed, fri", "m/w/f", "t/th", "tu, th"
        )
        if (knownDays.contains(lower)) return true

        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val dayTokens = setOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "mon", "tue", "wed", "thu", "fri", "sat", "sun")

        return tokens.isNotEmpty() && tokens.all { it in dayTokens || it in setOf("everyday", "daily", "weekday", "weekend", "to", "and") }
    }

    fun parseTimetableText(text: String): List<TimetableEntry> {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return emptyList()

        val firstRow = rows[0]
        var subjectIdx = firstRow.indexOfFirst { col -> subjectKeywords.any { col.contains(it, ignoreCase = true) } }
        var dayIdx = firstRow.indexOfFirst { col -> dayKeywords.any { col.contains(it, ignoreCase = true) } }
        var timeIdx = firstRow.indexOfFirst { col -> timeKeywords.any { col.contains(it, ignoreCase = true) } }

        val isHeader = subjectIdx != -1 || dayIdx != -1 || timeIdx != -1
        val dataRows = if (isHeader) rows.drop(1) else rows

        return dataRows.mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null

            var subject = ""
            var day = ""
            var time = ""

            // Smart Day Detection across row fields to handle commas inside subject names
            val foundDayIdx = row.indexOfFirst { isDayValue(it) }

            if (foundDayIdx != -1) {
                day = row[foundDayIdx]
                if (foundDayIdx > 0) {
                    // Day is at index > 0 -> All fields before Day form the Subject
                    subject = row.subList(0, foundDayIdx).joinToString(", ") { cleanField(it) }
                    // All fields after Day form the Time
                    if (foundDayIdx + 1 < row.size) {
                        time = row.subList(foundDayIdx + 1, row.size).joinToString(", ") { cleanField(it) }
                    }
                } else {
                    // Day is at index 0 -> Day, Subject..., Time
                    if (row.size >= 3) {
                        time = cleanField(row.last())
                        subject = row.subList(1, row.size - 1).joinToString(", ") { cleanField(it) }
                    } else if (row.size == 2) {
                        subject = cleanField(row[1])
                    }
                }
            } else {
                // Fallback using header index mapping if no day keyword matched
                subject = if (subjectIdx in row.indices) cleanField(row[subjectIdx]) else ""
                day = if (dayIdx in row.indices) cleanField(row[dayIdx]) else ""
                time = if (timeIdx in row.indices) cleanField(row[timeIdx]) else ""

                if ((subject.isEmpty() || day.isEmpty()) && row.size >= 2) {
                    if (isDayValue(cleanField(row[0]))) {
                        day = cleanField(row[0])
                        subject = row.drop(1).joinToString(", ") { cleanField(it) }
                    } else {
                        subject = row.dropLast(1).joinToString(", ") { cleanField(it) }
                        day = cleanField(row.last())
                    }
                }
            }

            val cleanSubject = cleanField(subject)
            val cleanDay = cleanField(day)
            val cleanTime = cleanField(time)

            if (cleanSubject.isNotEmpty() && cleanDay.isNotEmpty()) {
                TimetableEntry(
                    subject = cleanSubject,
                    day = cleanDay,
                    time = cleanTime
                )
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

    /**
     * Helper to match day strings (e.g. "Mon, Wed", "MWF", "Everyday", "Monday") against target day/date.
     */
    fun matchesDay(dayInputRaw: String, targetDayName: String, date: Calendar? = null): Boolean {
        val dayInput = dayInputRaw.trim().lowercase()
        if (dayInput.isEmpty()) return false

        val targetFull = targetDayName.trim().lowercase()
        val targetShort = targetFull.take(3)

        val effectiveDow = when (targetFull) {
            "monday" -> Calendar.MONDAY
            "tue", "tuesday" -> Calendar.TUESDAY
            "wed", "wednesday" -> Calendar.WEDNESDAY
            "thu", "thursday" -> Calendar.THURSDAY
            "fri", "friday" -> Calendar.FRIDAY
            "sat", "saturday" -> Calendar.SATURDAY
            "sun", "sunday" -> Calendar.SUNDAY
            else -> date?.get(Calendar.DAY_OF_WEEK) ?: Calendar.MONDAY
        }

        val isWeekday = effectiveDow in Calendar.MONDAY..Calendar.FRIDAY
        val isWeekend = effectiveDow == Calendar.SATURDAY || effectiveDow == Calendar.SUNDAY

        if (dayInput.contains("everyday") || dayInput.contains("daily") || dayInput.contains("all days") || dayInput.contains("all day") || dayInput.contains("7 days") || dayInput == "always") {
            return true
        }

        if (dayInput.contains("weekday") || dayInput.contains("mon-fri") || dayInput.contains("mon to fri") || dayInput.contains("m-f") || dayInput.contains("monday-friday") || dayInput.contains("monday to friday")) {
            return isWeekday
        }

        if (dayInput.contains("weekend") || dayInput.contains("sat-sun") || dayInput.contains("sat to sun") || dayInput.contains("s-s") || dayInput.contains("saturday-sunday") || dayInput.contains("saturday to sunday")) {
            return isWeekend
        }

        if (dayInput.contains(targetFull) || (targetShort.length >= 3 && dayInput.contains(targetShort))) {
            return true
        }

        val tokens = dayInput.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val targetTokens = when (effectiveDow) {
            Calendar.MONDAY -> listOf("monday", "mon", "m")
            Calendar.TUESDAY -> listOf("tuesday", "tue", "tu", "t")
            Calendar.WEDNESDAY -> listOf("wednesday", "wed", "w")
            Calendar.THURSDAY -> listOf("thursday", "thu", "th")
            Calendar.FRIDAY -> listOf("friday", "fri", "f")
            Calendar.SATURDAY -> listOf("saturday", "sat", "sa")
            Calendar.SUNDAY -> listOf("sunday", "sun", "su")
            else -> listOf(targetFull, targetShort)
        }

        return tokens.any { token -> targetTokens.contains(token) }
    }
}
