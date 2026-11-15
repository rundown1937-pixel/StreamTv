package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val id: String,
    val name: String,
    val avatarEmoji: String = "🎬",
    val avatarBgColorHex: Long = 0xFF333333,
    val isKidsProfile: Boolean = false,
    val favorites: List<String> = emptyList(),
    val watchHistory: List<WatchHistoryEntry> = emptyList(),
    val lockedChannelIds: List<String> = emptyList(),
)

@Immutable
data class WatchHistoryEntry(
    val channelId: String,
    val channelName: String,
    val timestamp: Long,
    val durationSeconds: Long = 0,
)

@Immutable
data class ActivityLog(
    val id: String,
    val profileName: String,
    val action: String,
    val channelName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
