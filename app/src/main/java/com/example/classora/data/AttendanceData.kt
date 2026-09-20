package com.example.classora.data

import java.util.UUID

data class TimetableEntry(
    val subject: String,
    val day: String, // e.g., "Monday", "Tuesday", etc.
    val time: String = ""
)

data class TimetableVersion(
    val id: String = UUID.randomUUID().toString(),
    val entries: List<TimetableEntry>,
    val startDate: String, // yyyy-MM-dd (inclusive)
    val name: String = "Timetable"
)

data class AttendanceRecord(
    val date: String, // yyyy-MM-dd
    val subject: String,
    val isPresent: Boolean,
    val timetableVersionId: String? = null // Optional: link to specific version if needed
)

data class TimetableOverride(
    val date: String, // yyyy-MM-dd
    val sourceDay: String // e.g., "Monday" - which day's timetable to use
)

data class SubjectAttendance(
    val subject: String,
    val presentCount: Int,
    val totalCount: Int
) {
    val percentage: Float
        get() = if (totalCount > 0) (presentCount.toFloat() / totalCount) * 100 else 0f
}
