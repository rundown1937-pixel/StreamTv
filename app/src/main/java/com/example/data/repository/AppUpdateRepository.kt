package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface AppUpdateStatus {
    object Idle : AppUpdateStatus
    object Checking : AppUpdateStatus
    data class UpToDate(
        val currentVersion: String,
        val checkedTimestamp: Long = System.currentTimeMillis(),
    ) : AppUpdateStatus

    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val githubReleaseUrl: String,
    ) : AppUpdateStatus

    data class Error(val message: String) : AppUpdateStatus
}

class AppUpdateRepository {

    private val _updateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val updateStatus: StateFlow<AppUpdateStatus> = _updateStatus.asStateFlow()

    // Primary GitHub Repo URL for StreamPulse TV releases
    private val primaryGithubRepo = "stratatechlabs/StreamTv"
    private val fallbackGithubRepo = "burhan/StreamTv"

    suspend fun checkForUpdates(force: Boolean = true) = withContext(Dispatchers.IO) {
        if (_updateStatus.value is AppUpdateStatus.Checking) return@withContext
        if (!force && (_updateStatus.value !is AppUpdateStatus.Idle)) return@withContext

        _updateStatus.value = AppUpdateStatus.Checking
        val currentVersion = BuildConfig.VERSION_NAME.ifEmpty { "1.0" }

        try {
            val releaseData = fetchLatestGitHubRelease(primaryGithubRepo)
                ?: fetchLatestGitHubRelease(fallbackGithubRepo)

            if (releaseData == null) {
                _updateStatus.value = AppUpdateStatus.UpToDate(currentVersion)
                return@withContext
            }

            val latestTag = releaseData.optString("tag_name", "").trim()
            val releaseName = releaseData.optString("name", latestTag).ifEmpty { "StreamPulse TV Update" }
            val releaseNotes = releaseData.optString("body", "Bug fixes and performance improvements.").trim()
            val htmlUrl = releaseData.optString("html_url", "https://github.com/rundown1937-pixel/StreamTv/releases/latest")

            // Extract APK asset download URL if available, fallback to htmlUrl
            var downloadUrl = htmlUrl
            val assetsArray = releaseData.optJSONArray("assets")
            val assetCount = assetsArray?.length() ?: 0
            if (assetCount > 0) {
                for (i in 0 until assetCount) {
                    val asset = assetsArray?.optJSONObject(i)
                    val assetName = asset?.optString("name", "") ?: ""
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset?.optString("browser_download_url", htmlUrl) ?: htmlUrl
                        break
                    }
                }
            }

            val cleanLatest = latestTag.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

            if (isNewerVersion(cleanCurrent, cleanLatest)) {
                _updateStatus.value = AppUpdateStatus.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = cleanLatest,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    githubReleaseUrl = htmlUrl,
                )
                Log.d("AppUpdateRepo", "New update found on GitHub: $cleanLatest (Current: $cleanCurrent)")
            } else {
                _updateStatus.value = AppUpdateStatus.UpToDate(currentVersion)
                Log.d("AppUpdateRepo", "App is up to date: $cleanCurrent (GitHub latest: $cleanLatest)")
            }
        } catch (e: Exception) {
            Log.e("AppUpdateRepo", "Error checking for GitHub updates", e)
            _updateStatus.value = AppUpdateStatus.Error("Unable to reach GitHub: ${e.localizedMessage ?: "Network error"}")
        }
    }

    private fun fetchLatestGitHubRelease(repoPath: String): JSONObject? {
        val apiUrl = "https://api.github.com/repos/$repoPath/releases/latest"
        var conn: HttpURLConnection? = null
        return try {
            val url = URI(apiUrl).toURL()
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "StreamPulseTV-App/1.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonText = reader.use { it.readText() }
                JSONObject(jsonText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isEmpty()) return false
        try {
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {
            return latest != current
        }
        return false
    }
}
