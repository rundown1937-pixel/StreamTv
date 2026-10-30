package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "viewing_history",
    indices = [
        Index(value = ["profileId", "timestamp"]),
        Index(value = ["channelId"])
    ]
)
data class ViewingHistoryEntity(
    @PrimaryKey val id: String,
    val profileId: String = "default",
    val channelId: String,
    val channelName: String,
    val channelLogo: String? = null,
    val genre: String = "General",
    val country: String = "GLOBAL",
    val streamUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L
)
