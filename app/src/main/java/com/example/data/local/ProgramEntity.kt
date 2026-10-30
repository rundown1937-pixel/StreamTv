package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["channelId", "startTime"]),
        Index(value = ["startTime", "endTime"])
    ]
)
data class ProgramEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val category: String,
    val startTime: Long, // Epoch ms
    val endTime: Long,   // Epoch ms
    val durationMinutes: Int,
    val rating: String = "TV-PG"
) {
    fun getProgressFraction(now: Long = System.currentTimeMillis()): Float {
        if (now <= startTime) return 0f
        if (now >= endTime) return 1f
        val elapsed = now - startTime
        val total = endTime - startTime
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun getRemainingMinutes(now: Long = System.currentTimeMillis()): Int {
        if (now >= endTime) return 0
        return (((endTime - now) / 60000).toInt()).coerceAtLeast(1)
    }
}
