package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineChannelDao {
    @Query("SELECT * FROM offline_channels ORDER BY downloadedAt DESC")
    fun getAllOfflineChannels(): Flow<List<OfflineChannelEntity>>

    @Query("SELECT * FROM offline_channels WHERE id = :id LIMIT 1")
    suspend fun getOfflineChannelById(id: String): OfflineChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineChannel(channel: OfflineChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<OfflineChannelEntity>)

    @Query("DELETE FROM offline_channels WHERE id = :id")
    suspend fun deleteOfflineChannelById(id: String)

    @Query("DELETE FROM offline_channels")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM offline_channels WHERE isDownloaded = 1")
    fun getDownloadedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM offline_channels")
    suspend fun getOfflineCount(): Int
}
