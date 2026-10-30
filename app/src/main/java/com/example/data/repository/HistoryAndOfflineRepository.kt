package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.OfflineChannelEntity
import com.example.data.local.ViewingHistoryEntity
import com.example.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class HistoryAndOfflineRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val offlineDao = db.offlineChannelDao()
    private val historyDao = db.viewingHistoryDao()

    val allOfflineChannels: Flow<List<OfflineChannelEntity>> = offlineDao.getAllOfflineChannels()

    fun getHistoryForProfile(profileId: String): Flow<List<ViewingHistoryEntity>> {
        return historyDao.getHistoryForProfile(profileId)
    }

    fun getRecentHistory(limit: Int = 10): Flow<List<ViewingHistoryEntity>> {
        return historyDao.getRecentViewingHistory(limit)
    }

    suspend fun recordViewing(
        channel: Channel,
        profileId: String = "default",
        durationSeconds: Long = 0L
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
            durationSeconds = durationSeconds
        )
        historyDao.insertHistoryEntry(entry)
    }

    suspend fun saveOfflineChannel(
        channel: Channel,
        localUri: String? = null,
        fileSizeMb: Double = 120.0
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
            downloadedAt = System.currentTimeMillis()
        )
        offlineDao.insertOfflineChannel(entity)
    }

    suspend fun deleteOfflineChannel(channelId: String) = withContext(Dispatchers.IO) {
        offlineDao.deleteOfflineChannelById(channelId)
    }

    suspend fun clearHistory(profileId: String) = withContext(Dispatchers.IO) {
        historyDao.clearHistoryForProfile(profileId)
    }

    suspend fun clearOfflineChannels() = withContext(Dispatchers.IO) {
        offlineDao.clearAll()
    }

    suspend fun clearAllLocalDatabaseCache() = withContext(Dispatchers.IO) {
        offlineDao.clearAll()
        historyDao.clearAll()
        db.programDao().clearAll()
    }

    suspend fun saveOfflineChannels(channels: List<Channel>) = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext
        val entities = channels.mapIndexed { idx, ch ->
            OfflineChannelEntity(
                id = ch.id,
                name = ch.name,
                streamUrl = ch.streamUrl,
                logoUrl = ch.logoUrl,
                genre = ch.genre,
                country = ch.country,
                language = ch.language,
                resolution = ch.resolution,
                fileSizeMb = 45.0 + (idx % 6 * 15.0),
                durationMinutes = 60,
                downloadProgress = 1.0f,
                isDownloaded = true,
                localUri = null,
                downloadedAt = System.currentTimeMillis()
            )
        }
        offlineDao.insertAll(entities)
    }

    suspend fun getDatabaseStorageSummary(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val offlineCount = offlineDao.getOfflineCount()
        val historyCount = historyDao.getHistoryCount()
        val programCount = db.programDao().getProgramCount()
        Triple(offlineCount, historyCount, programCount)
    }

    suspend fun seedInitialOfflineAndHistory(channels: List<Channel>) = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext
        val initialOffline = channels.take(4).mapIndexed { idx, ch ->
            OfflineChannelEntity(
                id = ch.id,
                name = ch.name,
                streamUrl = ch.streamUrl,
                logoUrl = ch.logoUrl,
                genre = ch.genre,
                country = ch.country,
                language = ch.language,
                resolution = ch.resolution,
                fileSizeMb = 80.0 + (idx * 35.0),
                durationMinutes = 30 + (idx * 15),
                downloadProgress = 1.0f,
                isDownloaded = true,
                localUri = null,
                downloadedAt = System.currentTimeMillis() - (idx * 3600_000L)
            )
        }
        offlineDao.insertAll(initialOffline)

        channels.take(5).forEachIndexed { idx, ch ->
            historyDao.insertHistoryEntry(
                ViewingHistoryEntity(
                    id = "seed_hist_${ch.id}",
                    profileId = "default",
                    channelId = ch.id,
                    channelName = ch.name,
                    channelLogo = ch.logoUrl,
                    genre = ch.genre,
                    country = ch.country,
                    streamUrl = ch.streamUrl,
                    timestamp = System.currentTimeMillis() - (idx * 1800_000L),
                    durationSeconds = 120L + (idx * 60L)
                )
            )
        }
    }
}
