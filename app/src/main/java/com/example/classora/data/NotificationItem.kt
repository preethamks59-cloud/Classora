package com.example.classora.data

import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String = "",
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "TASK"
)
