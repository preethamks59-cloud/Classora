package com.example.classora.data

data class InfoNotification(
    val id: String,
    val title: String,
    val message: String,
    val link: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
