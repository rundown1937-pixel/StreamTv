package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.OfflineChannelEntity
import com.example.data.local.ViewingHistoryEntity
import com.example.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HistoryAndOfflineRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val offlineDao = db.offlineChannelDao()
    private val historyDao = db.viewingHistoryDao()

    @Suppress("unused")
    val allOfflineChannels: Flow<List<OfflineChannelEntity>> = offlineDao.getAllOfflineChannels()

    @Suppress("unused")
    fun getRecentHistory(limit: Int = 10): Flow<List<ViewingHistoryEntity>> {
        return historyDao.getRecentViewingHistory(limit)
    }

    suspend fun recordViewing(
        channel: Channel,
        profileId: String = "default",
        durationSeconds: Long = 0L,
    ) = withContext(Dispatchers.IO) {
        val entry = ViewingHistoryEntity(
            id = "${channel.id}_${System.currentTimeMillis()}",
            profileId = profileId,
            channelId = channel.id,
            channelName = channel.name,
            channelLogo = channel.logoUrl,
            genre = channel.genre,
            country = channel.country,
            streamUrl = channel.streamUrl,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
        )
        historyDao.insertHistoryEntry(entry)
    }

    suspend fun saveOfflineChannel(
        channel: Channel,
        localUri: String? = null,
        fileSizeMb: Double = 120.0,
    ) = withContext(Dispatchers.IO) {
        val entity = OfflineChannelEntity(
            id = channel.id,
            name = channel.name,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            genre = channel.genre,
            country = channel.country,
            language = channel.language,
            resolution = channel.resolution,
            fileSizeMb = fileSizeMb,
            durationMinutes = 60,
            downloadProgress = 1.0f,
            isDownloaded = true,
            localUri = localUri,
            downloadedAt = System.currentTimeMillis(),
        )
        offlineDao.insertOfflineChannel(entity)
    }

    suspend fun deleteOfflineChannel(channelId: String) = withContext(Dispatchers.IO) {
        offlineDao.deleteOfflineChannelById(channelId)
    }

    suspend fun clearOfflineChannels() = withContext(Dispatchers.IO) {
        offlineDao.clearAll()
    }

    suspend fun clearAllLocalDatabaseCache() = withContext(Dispatchers.IO) {
        offlineDao.clearAll()
        historyDao.clearAll()
    }

    suspend fun saveOfflineChannels(channels: List<Channel>) = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext
        val entities = channels.mapIndexed { idx, (id, name, streamUrl, logoUrl, genre, country, language, _, _, resolution) ->
            OfflineChannelEntity(
                id = id,
                name = name,
                streamUrl = streamUrl,
                logoUrl = logoUrl,
                genre = genre,
                country = country,
                language = language,
                resolution = resolution,
                fileSizeMb = 45.0 + ((idx % 6) * 15.0),
                durationMinutes = 60,
                downloadProgress = 1.0f,
                isDownloaded = true,
                localUri = null,
                downloadedAt = System.currentTimeMillis(),
            )
        }
        offlineDao.insertAll(entities)
    }

    suspend fun getDatabaseStorageSummary(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val offlineCount = offlineDao.getOfflineCount()
        val historyCount = historyDao.getHistoryCount()
        Pair(offlineCount, historyCount)
    }
}
