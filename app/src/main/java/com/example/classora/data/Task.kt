package com.example.classora.data

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val time: String,
    val isCompleted: Boolean = false,
    val date: String, // Store as YYYY-MM-DD for simple filtering
    val repeatType: String = "Does not repeat", // "Does not repeat", "Everyday", "Weekdays", "Weekends"
    val marks: String = "",
    val priority: String = "Medium",
    val category: String = "",
    val reminder: String = "None",
    val note: String = "",
    val attachmentName: String = "",
    val attachmentSize: String = "",
    val attachmentUri: String = "",
    val externalId: String? = null // New field for calendar sync
)
