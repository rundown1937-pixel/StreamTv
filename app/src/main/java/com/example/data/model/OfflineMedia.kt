package com.example.data.model

data class OfflineMedia(
    val id: String,
    val title: String,
    val channelName: String,
    val genre: String,
    val streamUrl: String,
    val localUri: String? = null,
    val fileSizeMb: Double,
    val durationMinutes: Int,
    val downloadProgress: Float = 0f,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val quality: String = "1080p FHD",
    val thumbnailPlaceholder: String = "📺"
)
