package com.example.data.model

data class ProgramReminder(
    val programId: String,
    val channelId: String,
    val channelName: String,
    val programTitle: String,
    val programStartTime: Long,
    val reminderTime: Long, // 5 minutes before programStartTime
    val category: String = "General"
)
