package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE channelId = :channelId ORDER BY startTime ASC")
    fun getScheduleForChannel(channelId: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE channelId = :channelId AND startTime <= :now AND endTime >= :now LIMIT 1")
    fun getCurrentProgram(channelId: String, now: Long): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs WHERE startTime <= :now AND endTime >= :now")
    fun getAllCurrentPrograms(now: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE endTime >= :fromTime ORDER BY startTime ASC")
    fun getUpcomingPrograms(fromTime: Long): Flow<List<ProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE endTime < :cutoff")
    suspend fun deletePastPrograms(cutoff: Long)

    @Query("DELETE FROM programs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun getProgramCount(): Int
}
