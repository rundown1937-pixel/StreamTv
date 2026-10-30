package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_channels")
data class OfflineChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val genre: String = "General",
    val country: String = "GLOBAL",
    val language: String = "English",
    val resolution: String = "1080p FHD",
    val fileSizeMb: Double = 0.0,
    val durationMinutes: Int = 0,
    val downloadProgress: Float = 1.0f,
    val isDownloaded: Boolean = true,
    val localUri: String? = null,
    val downloadedAt: Long = System.currentTimeMillis()
)
