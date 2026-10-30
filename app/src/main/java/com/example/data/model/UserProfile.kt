package com.example.data.model

data class UserProfile(
    val id: String,
    val name: String,
    val avatarEmoji: String = "🎬",
    val avatarBgColorHex: Long = 0xFF6366F1,
    val isKidsProfile: Boolean = false,
    val favorites: List<String> = emptyList(),
    val watchHistory: List<WatchHistoryEntry> = emptyList(),
    val lockedChannelIds: List<String> = emptyList()
)

data class WatchHistoryEntry(
    val channelId: String,
    val channelName: String,
    val timestamp: Long,
    val durationSeconds: Long = 0
)

data class ActivityLog(
    val id: String,
    val profileName: String,
    val action: String,
    val channelName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
