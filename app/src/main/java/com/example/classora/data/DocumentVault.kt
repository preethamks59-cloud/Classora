package com.example.classora.data

import java.util.UUID

data class CollegeDocument(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // e.g., "Student ID Card", "Fee Receipt", etc.
    val title: String,
    val description: String = "",
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

object DocumentCategories {
    val categories = listOf(
        "Student ID Card",
        "Fee Receipt",
        "Hall Ticket",
        "Marks Card",
        "Certificates",
        "Internship Documents",
        "Bonafide Certificate",
        "10th Marks Card",
        "12th Marks Card",
        "Study Certificate",
        "Scholarship Documents",
        "Hostel Document",
        "Exam Timetable",
        "Resume",
        "Other"
    )
}
