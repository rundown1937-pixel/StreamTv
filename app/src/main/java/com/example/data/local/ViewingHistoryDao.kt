package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewingHistoryDao {
    @Query("SELECT * FROM viewing_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getHistoryForProfile(profileId: String): Flow<List<ViewingHistoryEntity>>

    @Query("SELECT * FROM viewing_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentViewingHistory(limit: Int): Flow<List<ViewingHistoryEntity>>

    @Query("SELECT * FROM viewing_history WHERE channelId = :channelId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEntryForChannel(channelId: String): ViewingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: ViewingHistoryEntity)

    @Query("DELETE FROM viewing_history WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM viewing_history WHERE profileId = :profileId")
    suspend fun clearHistoryForProfile(profileId: String)

    @Query("DELETE FROM viewing_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM viewing_history")
    suspend fun getHistoryCount(): Int
}
