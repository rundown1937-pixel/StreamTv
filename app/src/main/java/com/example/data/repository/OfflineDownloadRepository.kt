package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.OfflineMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class OfflineDownloadRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stream_pulse_offline", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<OfflineMedia>>(emptyList())
    val downloads: StateFlow<List<OfflineMedia>> = _downloads.asStateFlow()

    private val _storageUsedMb = MutableStateFlow(420.0)
    val storageUsedMb: StateFlow<Double> = _storageUsedMb.asStateFlow()

    private val _lowBandwidthDownloadMode = MutableStateFlow(false)
    val lowBandwidthDownloadMode: StateFlow<Boolean> = _lowBandwidthDownloadMode.asStateFlow()

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        val defaultCatalog = emptyList<OfflineMedia>()

        val raw = prefs.getString("downloads_json", null)
        if (raw != null) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<OfflineMedia>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OfflineMedia(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            channelName = obj.getString("channelName"),
                            genre = obj.getString("genre"),
                            streamUrl = obj.getString("streamUrl"),
                            fileSizeMb = obj.getDouble("fileSizeMb"),
                            durationMinutes = obj.getInt("durationMinutes"),
                            downloadProgress = obj.getDouble("downloadProgress").toFloat(),
                            isDownloaded = obj.getBoolean("isDownloaded"),
                            isDownloading = obj.optBoolean("isDownloading", false),
                            quality = obj.optString("quality", "1080p FHD"),
                            thumbnailPlaceholder = obj.optString("thumbnailPlaceholder", "📺")
                        )
                    )
                }
                _downloads.value = list
                updateStorageMetrics(list)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _downloads.value = defaultCatalog
        updateStorageMetrics(defaultCatalog)
        saveDownloads(defaultCatalog)
    }

    private fun updateStorageMetrics(list: List<OfflineMedia>) {
        val total = list.filter { it.isDownloaded }.sumOf { it.fileSizeMb }
        _storageUsedMb.value = total
    }

    private fun saveDownloads(list: List<OfflineMedia>) {
        try {
            val array = JSONArray()
            for (m in list) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("title", m.title)
                obj.put("channelName", m.channelName)
                obj.put("genre", m.genre)
                obj.put("streamUrl", m.streamUrl)
                obj.put("fileSizeMb", m.fileSizeMb)
                obj.put("durationMinutes", m.durationMinutes)
                obj.put("downloadProgress", m.downloadProgress)
                obj.put("isDownloaded", m.isDownloaded)
                obj.put("isDownloading", m.isDownloading)
                obj.put("quality", m.quality)
                obj.put("thumbnailPlaceholder", m.thumbnailPlaceholder)
                array.put(obj)
            }
            prefs.edit().putString("downloads_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleLowBandwidthMode(enabled: Boolean) {
        _lowBandwidthDownloadMode.value = enabled
    }

    fun startDownload(mediaId: String) {
        val current = _downloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == mediaId }
        if (index == -1) return

        val target = current[index]
        if (target.isDownloaded || target.isDownloading) return

        val adjustedSize = if (_lowBandwidthDownloadMode.value) target.fileSizeMb * 0.45 else target.fileSizeMb
        val adjustedQuality = if (_lowBandwidthDownloadMode.value) "480p Eco Saver" else target.quality

        current[index] = target.copy(
            isDownloading = true,
            downloadProgress = 0.05f,
            fileSizeMb = adjustedSize,
            quality = adjustedQuality
        )
        _downloads.value = current

        // Launch simulated download steps with progress updates
        scope.launch {
            for (p in 1..10) {
                delay(300)
                val progress = p / 10f
                val updatedList = _downloads.value.toMutableList()
                val idx = updatedList.indexOfFirst { it.id == mediaId }
                if (idx != -1) {
                    val isDone = progress >= 1.0f
                    updatedList[idx] = updatedList[idx].copy(
                        downloadProgress = progress,
                        isDownloading = !isDone,
                        isDownloaded = isDone
                    )
                    _downloads.value = updatedList
                    if (isDone) {
                        updateStorageMetrics(updatedList)
                        saveDownloads(updatedList)
                    }
                }
            }
        }
    }

    fun deleteDownload(mediaId: String) {
        val current = _downloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == mediaId }
        if (index != -1) {
            current[index] = current[index].copy(
                isDownloaded = false,
                isDownloading = false,
                downloadProgress = 0f
            )
            _downloads.value = current
            updateStorageMetrics(current)
            saveDownloads(current)
        }
    }
}
