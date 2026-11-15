package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.data.model.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

data class PlaylistPreset(
    val name: String,
    val url: String,
    val description: String,
)

class PlaylistRepository(private val context: Context) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val playlistMutex = Mutex()
    private var initialLoadJob: Job? = null

    private val cacheFile: File
        get() = File(context.filesDir, "synced_channels_cache.jsonl")

    private val prefs: SharedPreferences =
        context.getSharedPreferences("stream_pulse_playlist", Context.MODE_PRIVATE)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(FOLLOW_REDIRECTS)
        .followSslRedirects(FOLLOW_SSL_REDIRECTS)
        .retryOnConnectionFailure(RETRY_ON_CONNECTION_FAILURE)
        .build()

    companion object {
        private const val FOLLOW_REDIRECTS = true
        private const val FOLLOW_SSL_REDIRECTS = true
        private const val RETRY_ON_CONNECTION_FAILURE = true
        const val DEFAULT_PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.country.m3u"
        private const val PREF_URL = "playlist_url"
        private const val PREF_LAST_SYNC = "last_sync_time"
        private const val PREF_AUTO_UPDATE = "auto_update_enabled"
        private const val PREF_UPDATE_INTERVAL_HOURS = "update_interval_hours"
        private const val PREF_AUTO_UPDATE_DEFAULT_APPLIED = "auto_update_24h_default_applied"
        private const val PREF_CLEARED_LEGACY = "cleared_legacy_dw_fix_v6"

        val PRESET_PLAYLISTS = listOf(
            PlaylistPreset(
                name = "Worldwide 12,000+ Channels (Official Streams)",
                url = "https://iptv-org.github.io/iptv/index.country.m3u",
                description = "12,800+ Live Channels by Country with Original Broadcaster Names & Links",
            ),
            PlaylistPreset(
                name = "Global Curated 11,300+ Channels",
                url = "https://iptv-org.github.io/iptv/index.m3u",
                description = "11,320+ International Live Streams & News",
            ),
            PlaylistPreset(
                name = "Curated High-Priority HD Broadcasters",
                url = "local://verified_global_streams",
                description = "Curated high-priority HD stream links",
            ),
        )
    }

    private val _playlistUrl = MutableStateFlow(
        prefs.getString(PREF_URL, DEFAULT_PLAYLIST_URL) ?: DEFAULT_PLAYLIST_URL,
    )
    val playlistUrl: StateFlow<String> = _playlistUrl.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        prefs.getLong(PREF_LAST_SYNC, 0L),
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(readAutoUpdatePreference())
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    private val _updateIntervalHours = MutableStateFlow(
        prefs.getInt(PREF_UPDATE_INTERVAL_HOURS, 24),
    )
    val updateIntervalHours: StateFlow<Int> = _updateIntervalHours.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _isSyncing = MutableStateFlow(value = false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Indicates whether the full bundled 12k+ playlist has finished loading
    private val _isFullPlaylistLoaded = MutableStateFlow(false)
    val isFullPlaylistLoaded: StateFlow<Boolean> = _isFullPlaylistLoaded.asStateFlow()

    private fun readAutoUpdatePreference(): Boolean {
        if (!prefs.getBoolean(PREF_AUTO_UPDATE_DEFAULT_APPLIED, false)) {
            prefs.edit {
                putBoolean(PREF_AUTO_UPDATE, true)
                putInt(PREF_UPDATE_INTERVAL_HOURS, 24)
                putBoolean(PREF_AUTO_UPDATE_DEFAULT_APPLIED, true)
            }
            return true
        }
        return prefs.getBoolean(PREF_AUTO_UPDATE, true)
    }

    init {
        purgeOldScraperCacheIfPresent()
        val curatedChannels = getCuratedChannels()
        _channels.value = curatedChannels
        _syncMessage.value = "${curatedChannels.size} channel links loaded"

        initialLoadJob = repositoryScope.launch {
            val cached = loadCachedChannels()
            if (cached.size >= 10000) {
                playlistMutex.withLock {
                    _channels.value = cached
                    _syncMessage.value = "${cached.size} channel links loaded"
                    _isFullPlaylistLoaded.value = true
                }
                return@launch
            }

            val allAccumulated = ArrayList<Channel>(14000)
            allAccumulated.addAll(curatedChannels)

            val assetManager = context.assets
            val playlistAssets = assetManager?.list("")
                ?.asSequence()
                ?.filter { assetName ->
                    (assetName == "channels_12000.m3u") ||
                        assetName.matches(Regex("channels_12000_part_\\d{3}\\.m3u"))
                }
                ?.sortedBy { if (it == "channels_12000.m3u") "" else it }
                ?.toList()
                ?: emptyList()

            for (assetName in playlistAssets) {
                try {
                    val partChannels = ArrayList<Channel>(5000)
                    assetManager.open(assetName).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                            parseM3UStreamRobust(reader, partChannels)
                        }
                    }
                    if (partChannels.isNotEmpty()) {
                        allAccumulated.addAll(partChannels)
                        playlistMutex.withLock {
                            _channels.value = allAccumulated.toList()
                            _syncMessage.value = "${allAccumulated.size} channel links loaded"
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PlaylistRepo", "Bundled playlist part $assetName error: ${e.message}")
                }
            }

            _isFullPlaylistLoaded.value = true
        }
    }

    /**
     * Loads the complete set of 12,000+ worldwide channels bundled locally in assets
     * with original live stream links and exact broadcaster names.
     */
    fun loadBundledChannels(): List<Channel> {
        val assetManager = context.assets ?: return emptyList()
        val channels = ArrayList<Channel>(13000)
        val playlistAssets = assetManager.list("")
            ?.asSequence()
            ?.filter { assetName ->
                (assetName == "channels_12000.m3u") ||
                    assetName.matches(Regex("channels_12000_part_\\d{3}\\.m3u"))
            }
            ?.sortedBy { if (it == "channels_12000.m3u") "" else it }
            ?.toList()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf("channels_12000.m3u")

        for (assetName in playlistAssets) {
            if (channels.size >= 50000) break
            try {
                assetManager.open(assetName).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        parseM3UStreamRobust(reader, channels)
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaylistRepo", "Bundled playlist part $assetName is unreadable: ${e.message}")
            }
        }
        return channels
    }

    /**
     * Purges legacy caches and resets any stuck channel ID overrides so every channel
     * plays its own distinct, original live stream.
     */
    fun purgeOldScraperCacheIfPresent() {
        val alreadyCleared = prefs.getBoolean(PREF_CLEARED_LEGACY, false)
        val settingsPrefs = context.getSharedPreferences("stream_pulse_settings", Context.MODE_PRIVATE)

        if (!alreadyCleared) {
            prefs.edit {
                remove("cached_channels_json")
                remove(PREF_LAST_SYNC)
                putString(PREF_URL, DEFAULT_PLAYLIST_URL)
                putBoolean(PREF_CLEARED_LEGACY, true)
            }

            // Reset stuck last played channel so it doesn't persistently load DW
            settingsPrefs.edit {
                remove("last_played_channel_id")
            }

            Log.d("PlaylistRepo", "Purged legacy scraper cache and reset default channel successfully")
        }
    }

    fun setPlaylistUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty()) {
            _playlistUrl.value = cleanUrl
            prefs.edit { putString(PREF_URL, cleanUrl) }
        }
    }

    fun setAutoUpdate(enabled: Boolean, intervalHours: Int = 24) {
        _isAutoUpdateEnabled.value = enabled
        _updateIntervalHours.value = intervalHours
        prefs.edit {
            putBoolean(PREF_AUTO_UPDATE, enabled)
            putInt(PREF_UPDATE_INTERVAL_HOURS, intervalHours)
        }
    }

    fun checkAndTriggerAutoUpdate(force: Boolean = false) {
        if (!force && !_isAutoUpdateEnabled.value) {
            return
        }

        val url = _playlistUrl.value
        if (url == "local://verified_global_streams") {
            return
        }

        val hasFullSyncedCache = cacheFile.exists() && (prefs.getLong(PREF_LAST_SYNC, 0L) > 0L)
        val now = System.currentTimeMillis()
        val lastSync = prefs.getLong(PREF_LAST_SYNC, 0L)
        val intervalMs = _updateIntervalHours.value * 60 * 60 * 1000L
        if ((!force) && hasFullSyncedCache && (lastSync > 0L) && ((now - lastSync) < intervalMs)) {
            return
        }

        repositoryScope.launch {
            try {
                fetchPlaylist(url)
            } catch (e: Exception) {
                Log.e("PlaylistRepo", "Auto-update failed", e)
            }
        }
    }

    suspend fun fetchPlaylist(urlOverride: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        val targetUrl = (urlOverride ?: _playlistUrl.value).trim()
        if (targetUrl == "local://verified_global_streams") {
            initialLoadJob?.join()
            val curated = getCuratedChannels()
            playlistMutex.withLock {
                _channels.value = curated
                _syncMessage.value = "Loaded ${curated.size} curated channel links"
            }
            return@withContext Result.success(curated.size)
        }

        if (targetUrl.startsWith("local://") || targetUrl.contains("channels_12000.m3u")) {
            initialLoadJob?.join()
            val curated = getCuratedChannels()
            val bundled = loadBundledChannels()
            val merged = if (bundled.isNotEmpty()) mergeChannels(curated, bundled) else curated
            playlistMutex.withLock {
                _channels.value = merged
                saveCachedChannels(merged)
                _syncMessage.value = "Loaded ${merged.size} channel links"
            }
            return@withContext Result.success(merged.size)
        }

        _isSyncing.value = true
        _syncMessage.value = "Connecting to playlist: ${targetUrl.take(45)}..."

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 StreamPulseTV/1.0")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _syncMessage.value = "Server returned HTTP ${response.code}"
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            _syncMessage.value = "Parsing live channel links..."
            val body = response.body

            val contentEncoding = response.header("Content-Encoding")
            val rawStream = body.byteStream()
            val inputStream = if (contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                GZIPInputStream(rawStream)
            } else {
                rawStream
            }

            val parsedChannels = mutableListOf<Channel>()
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                parseM3UStreamRobust(reader, parsedChannels)
            }

            initialLoadJob?.join()

            if ((parsedChannels.isNotEmpty()) && ((parsedChannels.size >= 100) || !targetUrl.contains("iptv-org.github.io"))) {
                val combined = mergeChannels(getCuratedChannels(), parsedChannels)
                playlistMutex.withLock {
                    _channels.value = combined
                    saveCachedChannels(combined)
                    _isFullPlaylistLoaded.value = true
                }
                val now = System.currentTimeMillis()
                _lastSyncTimestamp.value = now
                prefs.edit { putLong(PREF_LAST_SYNC, now) }
                _syncMessage.value = "Updated ${combined.size} channels successfully"
                Result.success(combined.size)
            } else {
                _syncMessage.value = "Loaded ${_channels.value.size} original channel links."
                Result.success(_channels.value.size)
            }
        } catch (e: Exception) {
            Log.e("PlaylistRepo", "Sync failed: ${e.message}", e)
            _syncMessage.value = "Using saved channel links (${_channels.value.size} loaded)"
            Result.success(_channels.value.size)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun parseM3UStreamRobust(
        reader: BufferedReader,
        destination: MutableList<Channel>,
    ) {
        val seenIds = HashSet<String>(15000)
        destination.mapTo(seenIds) { (id) -> id }

        var currentTvgId = ""
        var currentTvgName = ""
        var currentTvgLogo: String? = null
        var currentGroupTitle = "General"
        var currentCountry = ""
        var currentLanguage = ""
        var currentTitle = ""
        var currentUserAgent: String? = null
        var currentReferrer: String? = null
        var isAdult = false

        var line = reader.readLine()
        while (destination.size < 50000) {
            val trimmed = (line ?: break).trim()

            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                isAdult = false

                currentTvgId = extractAttribute(trimmed, "tvg-id")
                currentTvgName = extractAttribute(trimmed, "tvg-name")
                val logo = extractAttribute(trimmed, "tvg-logo")
                currentTvgLogo = logo.takeIf { it.isNotEmpty() }
                val group = extractAttribute(trimmed, "group-title")
                currentGroupTitle = group.ifEmpty { "General" }
                val country = extractAttribute(trimmed, "tvg-country")
                currentCountry = country.ifEmpty { "" }
                val lang = extractAttribute(trimmed, "tvg-language")
                currentLanguage = lang.ifEmpty { "" }
                val ua = extractAttribute(trimmed, "http-user-agent")
                currentUserAgent = ua.takeIf {
                    it.isNotBlank() && !it.equals("No response", ignoreCase = true)
                }
                val ref = extractAttribute(trimmed, "http-referrer")
                currentReferrer = ref.takeIf {
                    it.isNotBlank() && !it.equals("No response", ignoreCase = true)
                }

                currentTitle = extractM3UDisplayName(trimmed)

                if (trimmed.contains("adult", ignoreCase = true) ||
                    trimmed.contains("xxx", ignoreCase = true) ||
                    currentGroupTitle.contains("adult", ignoreCase = true)
                ) {
                    isAdult = true
                }
            } else if (trimmed.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val option = trimmed.substringAfter(':')
                val separatorIndex = option.indexOf('=')
                if (separatorIndex > 0) {
                    val optionName = option.substring(0, separatorIndex).trim().lowercase(Locale.ROOT)
                    val optionValue = option.substring(separatorIndex + 1).trim()
                    if (optionValue.isNotBlank() && !optionValue.equals("No response", ignoreCase = true)) {
                        when (optionName) {
                            "http-user-agent" -> currentUserAgent = optionValue
                            "http-referrer" -> currentReferrer = optionValue
                        }
                    }
                }
            } else if (isNetworkStreamUrl(trimmed)) {
                val finalTitle = currentTitle.ifEmpty { currentTvgName.ifEmpty { currentTvgId.ifEmpty { "Channel ${destination.size + 1}" } } }
                val detectedCountry = detectCountry(currentCountry, currentTvgId, finalTitle, currentGroupTitle, currentLanguage)
                val detectedLanguage = detectLanguage(currentLanguage, currentTvgId, finalTitle, currentGroupTitle, detectedCountry)
                val normalizedGenre = mapToStandardGenre(currentGroupTitle, finalTitle, currentTvgId)

                val baseId = currentTvgId.ifEmpty { "ch_${destination.size + 1}" }
                var finalId = baseId
                var suffix = 1
                while (seenIds.contains(finalId)) {
                    finalId = "${baseId}_$suffix"
                    suffix++
                }
                seenIds.add(finalId)

                destination.add(
                    Channel(
                        id = finalId,
                        name = finalTitle,
                        streamUrl = trimmed,
                        logoUrl = currentTvgLogo,
                        genre = normalizedGenre,
                        country = detectedCountry,
                        language = detectedLanguage,
                        isAdultOrRestricted = isAdult,
                        resolution = if (finalTitle.contains("1080") || trimmed.contains("1080") || trimmed.contains("fhd")) "1080p FHD" else "720p HD",
                        description = "$normalizedGenre broadcast • $detectedCountry • $detectedLanguage",
                        httpUserAgent = currentUserAgent,
                        httpReferrer = currentReferrer,
                    ),
                )

                currentTitle = ""
                currentTvgId = ""
                currentTvgName = ""
                currentTvgLogo = null
                currentGroupTitle = "General"
                currentCountry = ""
                currentLanguage = ""
                currentUserAgent = null
                currentReferrer = null
                isAdult = false
            }

            line = reader.readLine()
        }
    }

    private fun extractM3UDisplayName(line: String): String {
        var quote: Char? = null
        for (index in line.indices) {
            val character = line[index]
            if (quote != null) {
                if (character == quote) quote = null
            } else if ((character == '"') || (character.code == 39)) {
                quote = character
            } else if (character == ',') {
                return line.substring(index + 1).trim()
            }
        }
        return ""
    }

    private fun isNetworkStreamUrl(value: String): Boolean {
        if (value.isBlank() || value.any { it.isWhitespace() }) return false
        val schemeSeparator = value.indexOf("://")
        if ((schemeSeparator <= 0) || ((schemeSeparator + 3) >= value.length)) return false
        val scheme = value.substring(0, schemeSeparator)
        return scheme.matches(Regex("[A-Za-z][A-Za-z0-9+.-]*"))
    }

    private fun extractAttribute(line: String, attr: String): String {
        val pattern = "$attr=\""
        val start = line.indexOf(pattern, ignoreCase = true)
        if (start != -1) {
            val valStart = start + pattern.length
            val end = line.indexOf('"', valStart)
            if (end != -1) {
                return line.substring(valStart, end)
            }
        }
        val patternSingle = "$attr='"
        val startSingle = line.indexOf(patternSingle, ignoreCase = true)
        if (startSingle != -1) {
            val valStart = startSingle + patternSingle.length
            val end = line.indexOf('\'', valStart)
            if (end != -1) {
                return line.substring(valStart, end)
            }
        }
        return ""
    }

    private fun detectLanguage(
        rawLanguage: String,
        tvgId: String,
        title: String,
        groupTitle: String,
        country: String,
    ): String {
        val clean = rawLanguage.trim().lowercase(Locale.ROOT)
        val cleanCandidate = clean.split(";", ",", "/").firstOrNull()?.trim() ?: clean
        // First check standard language codes and names for all 83 worldwide languages
        val normalized = when {
            cleanCandidate.startsWith("en") -> "English"
            cleanCandidate.startsWith("es") || cleanCandidate.startsWith("spa") -> "Spanish"
            cleanCandidate.startsWith("hi") || cleanCandidate.startsWith("hin") -> "Hindi"
            cleanCandidate.startsWith("fr") || cleanCandidate.startsWith("fra") || cleanCandidate.startsWith("fre") -> "French"
            cleanCandidate.startsWith("de") || cleanCandidate.startsWith("deu") || cleanCandidate.startsWith("ger") -> "German"
            cleanCandidate.startsWith("ar") || cleanCandidate.startsWith("ara") -> "Arabic"
            cleanCandidate.startsWith("pt") || cleanCandidate.startsWith("por") -> "Portuguese"
            cleanCandidate.startsWith("ru") || cleanCandidate.startsWith("rus") -> "Russian"
            cleanCandidate.startsWith("ja") || cleanCandidate.startsWith("jpn") -> "Japanese"
            cleanCandidate.startsWith("ko") || cleanCandidate.startsWith("kor") -> "Korean"
            cleanCandidate.startsWith("zh") || cleanCandidate.startsWith("zho") || cleanCandidate.startsWith("chi") -> "Chinese"
            cleanCandidate.startsWith("it") || cleanCandidate.startsWith("ita") -> "Italian"
            cleanCandidate.startsWith("tr") || cleanCandidate.startsWith("tur") -> "Turkish"
            cleanCandidate.startsWith("ur") || cleanCandidate.startsWith("urd") -> "Urdu"
            cleanCandidate.startsWith("bn") || cleanCandidate.startsWith("ben") -> "Bengali"
            cleanCandidate.startsWith("pa") || cleanCandidate.startsWith("pan") -> "Punjabi"
            cleanCandidate.startsWith("ta") || cleanCandidate.startsWith("tam") -> "Tamil"
            cleanCandidate.startsWith("te") || cleanCandidate.startsWith("tel") -> "Telugu"
            cleanCandidate.startsWith("mr") || cleanCandidate.startsWith("mar") -> "Marathi"
            cleanCandidate.startsWith("gu") || cleanCandidate.startsWith("guj") -> "Gujarati"
            cleanCandidate.startsWith("ml") || cleanCandidate.startsWith("mal") -> "Malayalam"
            cleanCandidate.startsWith("kn") || cleanCandidate.startsWith("kan") -> "Kannada"
            cleanCandidate.startsWith("or") || cleanCandidate.startsWith("ori") -> "Odia"
            cleanCandidate.startsWith("as") || cleanCandidate.startsWith("asm") -> "Assamese"
            cleanCandidate.startsWith("uk") || cleanCandidate.startsWith("ukr") -> "Ukrainian"
            cleanCandidate.startsWith("id") || cleanCandidate.startsWith("ind") -> "Indonesian"
            cleanCandidate.startsWith("tl") || cleanCandidate.startsWith("fil") || cleanCandidate.startsWith("tgl") -> "Tagalog"
            cleanCandidate.startsWith("nl") || cleanCandidate.startsWith("nld") || cleanCandidate.startsWith("dut") -> "Dutch"
            cleanCandidate.startsWith("pl") || cleanCandidate.startsWith("pol") -> "Polish"
            cleanCandidate.startsWith("vi") || cleanCandidate.startsWith("vie") -> "Vietnamese"
            cleanCandidate.startsWith("th") || cleanCandidate.startsWith("tha") -> "Thai"
            cleanCandidate.startsWith("fa") || cleanCandidate.startsWith("fas") || cleanCandidate.startsWith("per") -> "Persian"
            cleanCandidate.startsWith("he") || cleanCandidate.startsWith("heb") -> "Hebrew"
            cleanCandidate.startsWith("el") || cleanCandidate.startsWith("ell") || cleanCandidate.startsWith("gre") -> "Greek"
            cleanCandidate.startsWith("sv") || cleanCandidate.startsWith("swe") -> "Swedish"
            cleanCandidate.startsWith("da") || cleanCandidate.startsWith("dan") -> "Danish"
            cleanCandidate.startsWith("no") || cleanCandidate.startsWith("nor") -> "Norwegian"
            cleanCandidate.startsWith("fi") || cleanCandidate.startsWith("fin") -> "Finnish"
            cleanCandidate.startsWith("cs") || cleanCandidate.startsWith("ces") || cleanCandidate.startsWith("cze") -> "Czech"
            cleanCandidate.startsWith("hu") || cleanCandidate.startsWith("hun") -> "Hungarian"
            cleanCandidate.startsWith("ro") || cleanCandidate.startsWith("ron") || cleanCandidate.startsWith("rum") -> "Romanian"
            cleanCandidate.startsWith("ms") || cleanCandidate.startsWith("msa") || cleanCandidate.startsWith("may") -> "Malay"
            cleanCandidate.startsWith("sw") || cleanCandidate.startsWith("swa") -> "Swahili"
            cleanCandidate.startsWith("am") || cleanCandidate.startsWith("amh") -> "Amharic"
            cleanCandidate.startsWith("ha") || cleanCandidate.startsWith("hau") -> "Hausa"
            cleanCandidate.startsWith("yo") || cleanCandidate.startsWith("yor") -> "Yoruba"
            cleanCandidate.startsWith("ig") || cleanCandidate.startsWith("ibo") -> "Igbo"
            cleanCandidate.startsWith("zu") || cleanCandidate.startsWith("zul") -> "Zulu"
            cleanCandidate.startsWith("af") || cleanCandidate.startsWith("afr") -> "Afrikaans"
            cleanCandidate.startsWith("sr") || cleanCandidate.startsWith("srp") -> "Serbian"
            cleanCandidate.startsWith("hr") || cleanCandidate.startsWith("hrv") -> "Croatian"
            cleanCandidate.startsWith("bs") || cleanCandidate.startsWith("bos") -> "Bosnian"
            cleanCandidate.startsWith("bg") || cleanCandidate.startsWith("bul") -> "Bulgarian"
            cleanCandidate.startsWith("sk") || cleanCandidate.startsWith("slk") || cleanCandidate.startsWith("slo") -> "Slovak"
            cleanCandidate.startsWith("sl") || cleanCandidate.startsWith("slv") -> "Slovenian"
            cleanCandidate.startsWith("lt") || cleanCandidate.startsWith("lit") -> "Lithuanian"
            cleanCandidate.startsWith("lv") || cleanCandidate.startsWith("lav") -> "Latvian"
            cleanCandidate.startsWith("et") || cleanCandidate.startsWith("est") -> "Estonian"
            cleanCandidate.startsWith("is") || cleanCandidate.startsWith("isl") || cleanCandidate.startsWith("ice") -> "Icelandic"
            cleanCandidate.startsWith("ga") || cleanCandidate.startsWith("gle") -> "Irish"
            cleanCandidate.startsWith("cy") || cleanCandidate.startsWith("cym") || cleanCandidate.startsWith("wel") -> "Welsh"
            cleanCandidate.startsWith("eu") || cleanCandidate.startsWith("eus") || cleanCandidate.startsWith("baq") -> "Basque"
            cleanCandidate.startsWith("ca") || cleanCandidate.startsWith("cat") -> "Catalan"
            cleanCandidate.startsWith("gl") || cleanCandidate.startsWith("glg") -> "Galician"
            cleanCandidate.startsWith("sq") || cleanCandidate.startsWith("sqi") || cleanCandidate.startsWith("alb") -> "Albanian"
            cleanCandidate.startsWith("mk") || cleanCandidate.startsWith("mkd") || cleanCandidate.startsWith("mac") -> "Macedonian"
            cleanCandidate.startsWith("hy") || cleanCandidate.startsWith("hye") || cleanCandidate.startsWith("arm") -> "Armenian"
            cleanCandidate.startsWith("ka") || cleanCandidate.startsWith("kat") || cleanCandidate.startsWith("geo") -> "Georgian"
            cleanCandidate.startsWith("az") || cleanCandidate.startsWith("aze") -> "Azerbaijani"
            cleanCandidate.startsWith("kk") || cleanCandidate.startsWith("kaz") -> "Kazakh"
            cleanCandidate.startsWith("uz") || cleanCandidate.startsWith("uzb") -> "Uzbek"
            cleanCandidate.startsWith("tk") || cleanCandidate.startsWith("tuk") -> "Turkmen"
            cleanCandidate.startsWith("ky") || cleanCandidate.startsWith("kir") -> "Kyrgyz"
            cleanCandidate.startsWith("tg") || cleanCandidate.startsWith("tgk") -> "Tajik"
            cleanCandidate.startsWith("ps") || cleanCandidate.startsWith("pus") -> "Pashto"
            cleanCandidate.startsWith("ku") || cleanCandidate.startsWith("kur") -> "Kurdish"
            cleanCandidate.startsWith("sd") || cleanCandidate.startsWith("snd") -> "Sindhi"
            cleanCandidate.startsWith("ne") || cleanCandidate.startsWith("nep") -> "Nepali"
            cleanCandidate.startsWith("si") || cleanCandidate.startsWith("sin") -> "Sinhala"
            cleanCandidate.startsWith("my") || cleanCandidate.startsWith("mya") || cleanCandidate.startsWith("bur") -> "Burmese"
            cleanCandidate.startsWith("km") || cleanCandidate.startsWith("khm") -> "Khmer"
            cleanCandidate.startsWith("lo") || cleanCandidate.startsWith("lao") -> "Lao"
            cleanCandidate.startsWith("mn") || cleanCandidate.startsWith("mon") -> "Mongolian"
            else -> ""
        }
        if (normalized.isNotEmpty()) return normalized

        // Fallback detection from tvgId, title, group
        val combined = "$tvgId $title $groupTitle".lowercase(Locale.ROOT)
        val textLang = when {
            combined.contains("hindi") || combined.contains("aaj tak") || combined.contains("zeenews") || combined.contains("dd ") -> "Hindi"
            combined.contains("tamil") || combined.contains("sun tv") || combined.contains("polimer") -> "Tamil"
            combined.contains("telugu") || combined.contains("etv") || combined.contains("tv9 telugu") -> "Telugu"
            combined.contains("punjabi") || combined.contains("ptc") -> "Punjabi"
            combined.contains("malayalam") || combined.contains("asianet") -> "Malayalam"
            combined.contains("bengali") || combined.contains("bangla") -> "Bengali"
            combined.contains("marathi") -> "Marathi"
            combined.contains("gujarati") -> "Gujarati"
            combined.contains("urdu") || combined.contains("geo news") || combined.contains("ary news") -> "Urdu"
            combined.contains("kannada") -> "Kannada"
            combined.contains("odia") || combined.contains("oriya") -> "Odia"
            combined.contains("assamese") -> "Assamese"
            combined.contains("swahili") || combined.contains("kiswahili") -> "Swahili"
            combined.contains("amharic") -> "Amharic"
            combined.contains("hausa") -> "Hausa"
            combined.contains("yoruba") -> "Yoruba"
            combined.contains("igbo") -> "Igbo"
            combined.contains("zulu") -> "Zulu"
            combined.contains("afrikaans") -> "Afrikaans"
            combined.contains("serbian") || combined.contains("срп") -> "Serbian"
            combined.contains("croatian") || combined.contains("hrvatski") -> "Croatian"
            combined.contains("bosnian") -> "Bosnian"
            combined.contains("bulgarian") || combined.contains("българ") -> "Bulgarian"
            combined.contains("slovak") -> "Slovak"
            combined.contains("slovenian") -> "Slovenian"
            combined.contains("lithuanian") || combined.contains("lietuvi") -> "Lithuanian"
            combined.contains("latvian") || combined.contains("latvie") -> "Latvian"
            combined.contains("estonian") || combined.contains("eesti") -> "Estonian"
            combined.contains("icelandic") -> "Icelandic"
            combined.contains("irish") || combined.contains("gaeilge") -> "Irish"
            combined.contains("welsh") || combined.contains("cymraeg") -> "Welsh"
            combined.contains("basque") || combined.contains("euskara") -> "Basque"
            combined.contains("catalan") || combined.contains("català") || combined.contains("catala") -> "Catalan"
            combined.contains("galician") || combined.contains("galego") -> "Galician"
            combined.contains("albanian") || combined.contains("shqip") -> "Albanian"
            combined.contains("macedonian") || combined.contains("македон") -> "Macedonian"
            combined.contains("armenian") || combined.contains("հայ") -> "Armenian"
            combined.contains("georgian") || combined.contains("ქართ") -> "Georgian"
            combined.contains("azerbaijani") || combined.contains("azeri") -> "Azerbaijani"
            combined.contains("kazakh") || combined.contains("қазақ") -> "Kazakh"
            combined.contains("uzbek") || combined.contains("oʻzbek") -> "Uzbek"
            combined.contains("turkmen") -> "Turkmen"
            combined.contains("kyrgyz") || combined.contains("кыргыз") -> "Kyrgyz"
            combined.contains("tajik") || combined.contains("тоҷик") -> "Tajik"
            combined.contains("pashto") || combined.contains("پښتو") -> "Pashto"
            combined.contains("kurdish") || combined.contains("kurdi") -> "Kurdish"
            combined.contains("sindhi") -> "Sindhi"
            combined.contains("nepali") -> "Nepali"
            combined.contains("sinhala") -> "Sinhala"
            combined.contains("burmese") || combined.contains("myanmar") -> "Burmese"
            combined.contains("khmer") || combined.contains("cambodia") -> "Khmer"
            combined.contains("lao") -> "Lao"
            combined.contains("mongolian") || combined.contains("монгол") -> "Mongolian"
            combined.contains("spanish") || combined.contains("español") || combined.contains("espanol") -> "Spanish"
            combined.contains("french") || combined.contains("français") || combined.contains("francais") -> "French"
            combined.contains("german") || combined.contains("deutsch") -> "German"
            combined.contains("italian") || combined.contains("italiano") -> "Italian"
            combined.contains("portuguese") || combined.contains("português") -> "Portuguese"
            combined.contains("arabic") || combined.contains("arabiya") || combined.contains("jazeera") -> "Arabic"
            combined.contains("turkish") || combined.contains("türk") || combined.contains("trt") -> "Turkish"
            combined.contains("russian") || combined.contains("русск") || combined.contains("russia") -> "Russian"
            combined.contains("ukrainian") || combined.contains("україн") -> "Ukrainian"
            combined.contains("japanese") || combined.contains("nhk") -> "Japanese"
            combined.contains("korean") || combined.contains("kbs") || combined.contains("sbs") || combined.contains("mbc") -> "Korean"
            combined.contains("chinese") || combined.contains("cctv") || combined.contains("cgtn") -> "Chinese"
            combined.contains("indonesian") || combined.contains("kompas") -> "Indonesian"
            combined.contains("tagalog") || combined.contains("filipino") || combined.contains("gma") -> "Tagalog"
            combined.contains("dutch") || combined.contains("nederland") -> "Dutch"
            combined.contains("polish") || combined.contains("polsk") -> "Polish"
            combined.contains("vietnamese") || combined.contains("viet") -> "Vietnamese"
            combined.contains("thai") -> "Thai"
            combined.contains("persian") || combined.contains("farsi") -> "Persian"
            combined.contains("hebrew") -> "Hebrew"
            else -> ""
        }
        if (textLang.isNotEmpty()) return textLang

        // Associate native language based on detected country code
        return when (country) {
            "AF" -> "Persian"
            "AL" -> "Albanian"
            "DZ", "EG", "SA", "AE", "QA", "IQ", "JO", "KW", "LB", "LY", "OM", "PS", "SD", "SY", "TN", "YE", "MA" -> "Arabic"
            "AM" -> "Armenian"
            "AZ" -> "Azerbaijani"
            "BD" -> "Bengali"
            "BY", "RU" -> "Russian"
            "BA" -> "Bosnian"
            "BG" -> "Bulgarian"
            "CN", "TW", "HK" -> "Chinese"
            "HR" -> "Croatian"
            "CZ" -> "Czech"
            "DK" -> "Danish"
            "NL" -> "Dutch"
            "EE" -> "Estonian"
            "ET" -> "Amharic"
            "FI" -> "Finnish"
            "FR", "MC" -> "French"
            "GE" -> "Georgian"
            "DE", "AT", "CH" -> "German"
            "GR", "CY" -> "Greek"
            "IL" -> "Hebrew"
            "HU" -> "Hungarian"
            "IS" -> "Icelandic"
            "ID" -> "Indonesian"
            "IR" -> "Persian"
            "IT", "SM", "VA" -> "Italian"
            "JP" -> "Japanese"
            "KZ" -> "Kazakh"
            "KR" -> "Korean"
            "KG" -> "Kyrgyz"
            "LV" -> "Latvian"
            "LT" -> "Lithuanian"
            "MK" -> "Macedonian"
            "MY" -> "Malay"
            "MN" -> "Mongolian"
            "NP" -> "Nepali"
            "NO" -> "Norwegian"
            "PK" -> "Urdu"
            "PL" -> "Polish"
            "PT", "BR" -> "Portuguese"
            "RO", "MD" -> "Romanian"
            "RS", "ME" -> "Serbian"
            "SK" -> "Slovak"
            "SI" -> "Slovenian"
            "ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR", "PA", "UY", "PR" -> "Spanish"
            "SE" -> "Swedish"
            "TH" -> "Thai"
            "TR" -> "Turkish"
            "UA" -> "Ukrainian"
            "UZ" -> "Uzbek"
            "VN" -> "Vietnamese"
            "PH" -> "Tagalog"
            "IN" -> "Hindi"
            "US", "UK", "CA", "AU", "NZ", "IE", "ZA", "NG", "SG", "GH", "KE", "JM" -> "English"
            else -> "English"
        }
    }

    private fun detectCountry(
        rawCountry: String,
        tvgId: String,
        title: String,
        groupTitle: String,
        language: String,
    ): String {
        val cleanRaw = rawCountry.trim().uppercase(Locale.ROOT)
        // 1. If rawCountry has a valid ISO code
        if (cleanRaw.isNotEmpty() && cleanRaw != "GLOBAL" && cleanRaw != "UNDEFINED" && cleanRaw != "GENERAL") {
            val candidate = cleanRaw.split(";", ",", "/", " ").firstOrNull()?.trim() ?: ""
            val mapped = mapIsoCodeToCountry(candidate)
            if (mapped.isNotEmpty()) return mapped
        }

        // 2. Check groupTitle - in M3U playlists like channels_12000.m3u, group-title is the country name!
        val mappedGroup = mapCountryNameToCode(groupTitle)
        if (mappedGroup.isNotEmpty()) return mappedGroup

        // 3. Detect from tvgId extension (e.g. .us, .in, .uk, AMC.af@SD, A2CNN.al@SD)
        if (tvgId.contains('.')) {
            val afterDot = tvgId.substringAfterLast('.')
            val candidate = if (afterDot.contains('@')) afterDot.substringBefore('@') else afterDot
            val cleanCode = candidate.trim().uppercase(Locale.ROOT)
            if (cleanCode.length in 2..3) {
                val mappedExt = mapIsoCodeToCountry(cleanCode)
                if (mappedExt.isNotEmpty()) return mappedExt
            }
        }

        // 4. Detect from bracketed title tags like [US], (UK), [IN], (DE), etc.
        val titleBracketMatch = Regex("""[\[(]([A-Za-z]{2,3})[)\]]""").find(title)
        if (titleBracketMatch != null) {
            val tag = titleBracketMatch.groupValues[1].uppercase(Locale.ROOT)
            val mappedTag = mapIsoCodeToCountry(tag)
            if (mappedTag.isNotEmpty()) return mappedTag
        }

        // 5. Check if title contains a country name directly
        val mappedTitleCountry = mapCountryNameToCode(title)
        if (mappedTitleCountry.isNotEmpty()) return mappedTitleCountry

        // 6. Broadcaster keywords matching
        val combined = "$tvgId $title $groupTitle $language".lowercase(Locale.ROOT)
        return when {
            combined.contains("afghanistan") || combined.contains("persiana") || combined.contains("kabul") -> "AF"
            combined.contains("albania") || combined.contains("tirana") || combined.contains("shqip") -> "AL"
            combined.contains("india") || combined.contains("[in]") || combined.contains("hindi") || combined.contains("tamil") || combined.contains("telugu") || combined.contains("punjabi") || combined.contains("bengali") || combined.contains("marathi") || combined.contains("gujarati") || combined.contains("kannada") || combined.contains("malayalam") || combined.contains("aaj tak") || combined.contains("dd ") -> "IN"
            combined.contains("united states") || combined.contains("usa") || combined.contains("[us]") || combined.contains("fox news") || combined.contains("abc news") || combined.contains("cbs") || combined.contains("nbc") || combined.contains("pbs") -> "US"
            combined.contains("united kingdom") || combined.contains("britain") || combined.contains("[uk]") || combined.contains("sky news") || combined.contains("bbc") || combined.contains("itv") -> "UK"
            combined.contains("canada") || combined.contains("[ca]") || combined.contains("cbc") || combined.contains("ctv") -> "CA"
            combined.contains("australia") || combined.contains("[au]") || combined.contains("abc australia") || combined.contains("sbs australia") -> "AU"
            combined.contains("germany") || combined.contains("deutsch") || combined.contains("[de]") || combined.contains("zdf") || combined.contains("ard") || combined.contains("dw") -> "DE"
            combined.contains("france") || combined.contains("french") || combined.contains("[fr]") || combined.contains("france 24") || combined.contains("tf1") -> "FR"
            combined.contains("spain") || combined.contains("spanish") || combined.contains("[es]") || combined.contains("rtve") -> "ES"
            combined.contains("italy") || combined.contains("italian") || combined.contains("[it]") || combined.contains("rai ") -> "IT"
            combined.contains("brazil") || combined.contains("brasil") || combined.contains("[br]") || combined.contains("globo") || combined.contains("sbt") || combined.contains("record") -> "BR"
            combined.contains("mexico") || combined.contains("[mx]") || combined.contains("televisa") || combined.contains("azteca") -> "MX"
            combined.contains("argentina") || combined.contains("[ar]") || combined.contains("telefe") -> "AR"
            combined.contains("colombia") || combined.contains("[co]") || combined.contains("caracol") || combined.contains("rcn") -> "CO"
            combined.contains("chile") || combined.contains("[cl]") -> "CL"
            combined.contains("peru") || combined.contains("[pe]") -> "PE"
            combined.contains("japan") || combined.contains("[jp]") || combined.contains("nhk") -> "JP"
            combined.contains("korea") || combined.contains("[kr]") || combined.contains("kbs") || combined.contains("arirang") || combined.contains("sbs") || combined.contains("mbc") -> "KR"
            combined.contains("china") || combined.contains("[cn]") || combined.contains("cctv") || combined.contains("cgtn") -> "CN"
            combined.contains("turkey") || combined.contains("turkiye") || combined.contains("[tr]") || combined.contains("trt") -> "TR"
            combined.contains("russia") || combined.contains("[ru]") || combined.contains("rt news") || combined.contains("rtr") -> "RU"
            combined.contains("ukraine") || combined.contains("[ua]") || combined.contains("1+1") -> "UA"
            combined.contains("saudi") || combined.contains("[sa]") || combined.contains("al ekhbariya") -> "SA"
            combined.contains("uae") || combined.contains("emirates") || combined.contains("dubai") || combined.contains("[ae]") -> "AE"
            combined.contains("qatar") || combined.contains("[qa]") || combined.contains("al jazeera") -> "QA"
            combined.contains("egypt") || combined.contains("[eg]") || combined.contains("nile") -> "EG"
            combined.contains("pakistan") || combined.contains("[pk]") || combined.contains("geo tv") || combined.contains("ary") || combined.contains("ptv") -> "PK"
            combined.contains("bangladesh") || combined.contains("[bd]") || combined.contains("somoy") || combined.contains("jamuna") -> "BD"
            combined.contains("indonesia") || combined.contains("[id]") || combined.contains("tvri") || combined.contains("kompas") -> "ID"
            combined.contains("philippines") || combined.contains("[ph]") || combined.contains("gma") || combined.contains("abs-cbn") -> "PH"
            combined.contains("nigeria") || combined.contains("[ng]") || combined.contains("channels tv") || combined.contains("nta") -> "NG"
            combined.contains("south africa") || combined.contains("[za]") || combined.contains("sabc") || combined.contains("enca") -> "ZA"
            combined.contains("netherlands") || combined.contains("[nl]") || combined.contains("nos") -> "NL"
            combined.contains("switzerland") || combined.contains("[ch]") || combined.contains("srf") || combined.contains("rts") -> "CH"
            combined.contains("austria") || combined.contains("[at]") || combined.contains("orf") || combined.contains("red bull") -> "AT"
            combined.contains("belgium") || combined.contains("[be]") || combined.contains("vrt") || combined.contains("rtbf") -> "BE"
            combined.contains("sweden") || combined.contains("[se]") || combined.contains("svt") -> "SE"
            combined.contains("norway") || combined.contains("[no]") || combined.contains("nrk") -> "NO"
            combined.contains("denmark") || combined.contains("[dk]") || combined.contains("dr tv") -> "DK"
            combined.contains("finland") || combined.contains("[fi]") || combined.contains("yle") -> "FI"
            combined.contains("poland") || combined.contains("[pl]") || combined.contains("tvp") -> "PL"
            combined.contains("greece") || combined.contains("[gr]") || combined.contains("ert") -> "GR"
            combined.contains("portugal") || combined.contains("[pt]") || combined.contains("rtp") -> "PT"
            combined.contains("ireland") || combined.contains("[ie]") || combined.contains("rte") -> "IE"
            combined.contains("new zealand") || combined.contains("[nz]") || combined.contains("tvnz") -> "NZ"
            combined.contains("singapore") || combined.contains("[sg]") || combined.contains("cna") -> "SG"
            combined.contains("malaysia") || combined.contains("[my]") || combined.contains("rtm") -> "MY"
            combined.contains("thailand") || combined.contains("[th]") || combined.contains("thai pbs") -> "TH"
            combined.contains("vietnam") || combined.contains("[vn]") || combined.contains("vtv") -> "VN"
            combined.contains("israel") || combined.contains("[il]") || combined.contains("kan 11") || combined.contains("i24") -> "IL"
            combined.contains("morocco") || combined.contains("[ma]") || combined.contains("medi1") -> "MA"
            combined.contains("algeria") || combined.contains("[dz]") -> "DZ"
            combined.contains("georgia") || combined.contains("[ge]") || combined.contains("1tv") -> "GE"
            else -> "GLOBAL"
        }
    }

    private fun mapCountryNameToCode(name: String): String {
        val clean = name.trim().uppercase(Locale.ROOT)
        return when {
            clean.contains("AFGHANISTAN") -> "AF"
            clean.contains("ALBANIA") -> "AL"
            clean.contains("ALGERIA") -> "DZ"
            clean.contains("ARGENTINA") -> "AR"
            clean.contains("ARMENIA") -> "AM"
            clean.contains("AUSTRALIA") -> "AU"
            clean.contains("AUSTRIA") -> "AT"
            clean.contains("AZERBAIJAN") -> "AZ"
            clean.contains("BANGLADESH") -> "BD"
            clean.contains("BELARUS") -> "BY"
            clean.contains("BELGIUM") -> "BE"
            clean.contains("BOLIVIA") -> "BO"
            clean.contains("BOSNIA") -> "BA"
            clean.contains("BRAZIL") || clean.contains("BRASIL") -> "BR"
            clean.contains("BULGARIA") -> "BG"
            clean.contains("CANADA") -> "CA"
            clean.contains("CHILE") -> "CL"
            clean.contains("CHINA") -> "CN"
            clean.contains("COLOMBIA") -> "CO"
            clean.contains("COSTA RICA") -> "CR"
            clean.contains("CROATIA") -> "HR"
            clean.contains("CUBA") -> "CU"
            clean.contains("CYPRUS") -> "CY"
            clean.contains("CZECH") -> "CZ"
            clean.contains("DENMARK") -> "DK"
            clean.contains("DOMINICAN") -> "DO"
            clean.contains("ECUADOR") -> "EC"
            clean.contains("EGYPT") -> "EG"
            clean.contains("EL SALVADOR") -> "SV"
            clean.contains("ESTONIA") -> "EE"
            clean.contains("ETHIOPIA") -> "ET"
            clean.contains("FINLAND") -> "FI"
            clean.contains("FRANCE") -> "FR"
            clean.contains("GEORGIA") -> "GE"
            clean.contains("GERMANY") || clean.contains("DEUTSCHLAND") -> "DE"
            clean.contains("GHANA") -> "GH"
            clean.contains("GREECE") -> "GR"
            clean.contains("GUATEMALA") -> "GT"
            clean.contains("HONDURAS") -> "HN"
            clean.contains("HONG KONG") -> "HK"
            clean.contains("HUNGARY") -> "HU"
            clean.contains("ICELAND") -> "IS"
            clean.contains("INDIA") -> "IN"
            clean.contains("INDONESIA") -> "ID"
            clean.contains("IRAN") -> "IR"
            clean.contains("IRAQ") -> "IQ"
            clean.contains("IRELAND") -> "IE"
            clean.contains("ISRAEL") -> "IL"
            clean.contains("ITALY") || clean.contains("ITALIA") -> "IT"
            clean.contains("JAMAICA") -> "JM"
            clean.contains("JAPAN") -> "JP"
            clean.contains("JORDAN") -> "JO"
            clean.contains("KAZAKHSTAN") -> "KZ"
            clean.contains("KENYA") -> "KE"
            clean.contains("KUWAIT") -> "KW"
            clean.contains("KYRGYZSTAN") -> "KG"
            clean.contains("LATVIA") -> "LV"
            clean.contains("LEBANON") -> "LB"
            clean.contains("LIBYA") -> "LY"
            clean.contains("LITHUANIA") -> "LT"
            clean.contains("LUXEMBOURG") -> "LU"
            clean.contains("MACEDONIA") -> "MK"
            clean.contains("MALAYSIA") -> "MY"
            clean.contains("MALTA") -> "MT"
            clean.contains("MEXICO") || clean.contains("MÉXICO") -> "MX"
            clean.contains("MOLDOVA") -> "MD"
            clean.contains("MONACO") -> "MC"
            clean.contains("MONGOLIA") -> "MN"
            clean.contains("MONTENEGRO") -> "ME"
            clean.contains("MOROCCO") -> "MA"
            clean.contains("NEPAL") -> "NP"
            clean.contains("NETHERLANDS") || clean.contains("HOLLAND") -> "NL"
            clean.contains("NEW ZEALAND") -> "NZ"
            clean.contains("NICARAGUA") -> "NI"
            clean.contains("NIGERIA") -> "NG"
            clean.contains("NORWAY") -> "NO"
            clean.contains("OMAN") -> "OM"
            clean.contains("PAKISTAN") -> "PK"
            clean.contains("PALESTINE") -> "PS"
            clean.contains("PANAMA") -> "PA"
            clean.contains("PARAGUAY") -> "PY"
            clean.contains("PERU") || clean.contains("PERÚ") -> "PE"
            clean.contains("PHILIPPINES") -> "PH"
            clean.contains("POLAND") -> "PL"
            clean.contains("PORTUGAL") -> "PT"
            clean.contains("PUERTO RICO") -> "PR"
            clean.contains("QATAR") -> "QA"
            clean.contains("ROMANIA") -> "RO"
            clean.contains("RUSSIA") -> "RU"
            clean.contains("SAUDI") -> "SA"
            clean.contains("SENEGAL") -> "SN"
            clean.contains("SERBIA") -> "RS"
            clean.contains("SINGAPORE") -> "SG"
            clean.contains("SLOVAKIA") -> "SK"
            clean.contains("SLOVENIA") -> "SI"
            clean.contains("SOMALIA") -> "SO"
            clean.contains("SOUTH AFRICA") -> "ZA"
            clean.contains("SOUTH KOREA") || clean.contains("KOREA") -> "KR"
            clean.contains("SPAIN") || clean.contains("ESPAÑA") -> "ES"
            clean.contains("SRI LANKA") -> "LK"
            clean.contains("SUDAN") -> "SD"
            clean.contains("SWEDEN") -> "SE"
            clean.contains("SWITZERLAND") -> "CH"
            clean.contains("SYRIA") -> "SY"
            clean.contains("TAIWAN") -> "TW"
            clean.contains("TAJIKISTAN") -> "TJ"
            clean.contains("TANZANIA") -> "TZ"
            clean.contains("THAILAND") -> "TH"
            clean.contains("TUNISIA") -> "TN"
            clean.contains("TURKEY") || clean.contains("TÜRKIYE") || clean.contains("TURKIYE") -> "TR"
            clean.contains("TURKMENISTAN") -> "TM"
            clean.contains("UGANDA") -> "UG"
            clean.contains("UKRAINE") -> "UA"
            clean.contains("UNITED ARAB EMIRATES") || clean.contains("UAE") || clean.contains("EMIRATES") -> "AE"
            clean.contains("UNITED KINGDOM") || clean.contains("BRITAIN") -> "UK"
            clean.contains("UNITED STATES") || clean.contains("USA") -> "US"
            clean.contains("URUGUAY") -> "UY"
            clean.contains("UZBEKISTAN") -> "UZ"
            clean.contains("VENEZUELA") -> "VE"
            clean.contains("VIETNAM") -> "VN"
            clean.contains("YEMEN") -> "YE"
            clean.contains("ZAMBIA") -> "ZM"
            clean.contains("ZIMBABWE") -> "ZW"
            else -> ""
        }
    }

    private fun mapIsoCodeToCountry(code: String): String {
        return when (code.uppercase(Locale.ROOT)) {
            "AF", "AFG" -> "AF"
            "AL", "ALB" -> "AL"
            "DZ", "DZA" -> "DZ"
            "US", "USA" -> "US"
            "UK", "GB", "GBR" -> "UK"
            "IN", "IND" -> "IN"
            "CA", "CAN" -> "CA"
            "AU", "AUS" -> "AU"
            "DE", "DEU" -> "DE"
            "FR", "FRA" -> "FR"
            "ES", "ESP" -> "ES"
            "IT", "ITA" -> "IT"
            "BR", "BRA" -> "BR"
            "MX", "MEX" -> "MX"
            "AR", "ARG" -> "AR"
            "CL", "CHL" -> "CL"
            "CO", "COL" -> "CO"
            "PE", "PER" -> "PE"
            "JP", "JPN" -> "JP"
            "KR", "KOR" -> "KR"
            "CN", "CHN" -> "CN"
            "TR", "TUR" -> "TR"
            "RU", "RUS" -> "RU"
            "UA", "UKR" -> "UA"
            "SA", "SAU" -> "SA"
            "AE", "ARE" -> "AE"
            "QA", "QAT" -> "QA"
            "EG", "EGY" -> "EG"
            "PK", "PAK" -> "PK"
            "BD", "BGD" -> "BD"
            "ID", "IDN" -> "ID"
            "PH", "PHL" -> "PH"
            "NG", "NGA" -> "NG"
            "ZA", "ZAF" -> "ZA"
            "NL", "NLD" -> "NL"
            "CH", "CHE" -> "CH"
            "AT", "AUT" -> "AT"
            "BE", "BEL" -> "BE"
            "SE", "SWE" -> "SE"
            "NO", "NOR" -> "NO"
            "DK", "DNK" -> "DK"
            "FI", "FIN" -> "FI"
            "PL", "POL" -> "PL"
            "GR", "GRC" -> "GR"
            "PT", "PRT" -> "PT"
            "IE", "IRL" -> "IE"
            "NZ", "NZL" -> "NZ"
            "SG", "SGP" -> "SG"
            "MY", "MYS" -> "MY"
            "TH", "THA" -> "TH"
            "VN", "VNM" -> "VN"
            "IL", "ISR" -> "IL"
            "MA", "MAR" -> "MA"
            "GE", "GEO" -> "GE"
            "IQ", "IRQ" -> "IQ"
            "JO", "JOR" -> "JO"
            "LB", "LBN" -> "LB"
            "KW", "KWT" -> "KW"
            "OM", "OMN" -> "OM"
            "SY", "SYR" -> "SY"
            "YE", "YEM" -> "YE"
            "LY", "LBY" -> "LY"
            "TN", "TUN" -> "TN"
            "SD", "SDN" -> "SD"
            "BA", "BIH" -> "BA"
            "HR", "HRV" -> "HR"
            "RS", "SRB" -> "RS"
            "SI", "SVN" -> "SI"
            "MK", "MKD" -> "MK"
            "BG", "BGR" -> "BG"
            "RO", "ROU" -> "RO"
            "CZ", "CZE" -> "CZ"
            "SK", "SVK" -> "SK"
            "HU", "HUN" -> "HU"
            "EE", "EST" -> "EE"
            "LV", "LVA" -> "LV"
            "LT", "LTU" -> "LT"
            "BY", "BLR" -> "BY"
            "MD", "MDA" -> "MD"
            "AM", "ARM" -> "AM"
            "AZ", "AZE" -> "AZ"
            "KZ", "KAZ" -> "KZ"
            "UZ", "UZB" -> "UZ"
            "TM", "TKM" -> "TM"
            "KG", "KGZ" -> "KG"
            "TJ", "TJK" -> "TJ"
            "MN", "MNG" -> "MN"
            "NP", "NPL" -> "NP"
            "LK", "LKA" -> "LK"
            "KH", "KHM" -> "KH"
            "LA", "LAO" -> "LA"
            "MM", "MMR" -> "MM"
            "IR", "IRN" -> "IR"
            "ET", "ETH" -> "ET"
            "KE", "KEN" -> "KE"
            "TZ", "TZA" -> "TZ"
            "UG", "UGA" -> "UG"
            "GH", "GHA" -> "GH"
            "SN", "SEN" -> "SN"
            "CI", "CIV" -> "CI"
            "CM", "CMR" -> "CM"
            "CU", "CUB" -> "CU"
            "DO", "DOM" -> "DO"
            "PR", "PRI" -> "PR"
            "VE", "VEN" -> "VE"
            "EC", "ECU" -> "EC"
            "BO", "BOL" -> "BO"
            "PY", "PRY" -> "PY"
            "UY", "URY" -> "UY"
            "GT", "GTM" -> "GT"
            "HN", "HND" -> "HN"
            "SV", "SLV" -> "SV"
            "NI", "NIC" -> "NI"
            "CR", "CRI" -> "CR"
            "PA", "PAN" -> "PA"
            "JM", "JAM" -> "JM"
            "IS", "ISL" -> "IS"
            "CY", "CYP" -> "CY"
            "LU", "LUX" -> "LU"
            "MT", "MLT" -> "MT"
            "GLOBAL", "INT" -> "GLOBAL"
            else -> if (code.length in 2..3) code.uppercase(Locale.ROOT) else ""
        }
    }

    private fun mapToStandardGenre(groupTitle: String, title: String = "", tvgId: String = ""): String {
        val combined = "$groupTitle $title $tvgId".lowercase(Locale.ROOT)
        return when {
            combined.contains("adult") || combined.contains("xxx") -> "Adult (18+)"
            combined.contains("news") || combined.contains("noticias") || combined.contains("nouvelles") || combined.contains("nachrichten") || combined.contains("notizie") || combined.contains("новости") || combined.contains("أخبار") || combined.contains("समाचार") || combined.contains("khabar") || combined.contains("weather") || combined.contains("météo") || combined.contains("wetter") || combined.contains("cnn") || combined.contains("bbc") || combined.contains("sky news") || combined.contains("al jazeera") || combined.contains("dw") || combined.contains("france 24") || combined.contains("bloomberg") || combined.contains("cnbc") || combined.contains("aaj tak") || combined.contains("ndtv") || combined.contains("republic") || combined.contains("wion") || combined.contains("press") -> "News"
            combined.contains("sport") || combined.contains("deporte") || combined.contains("futbol") || combined.contains("futebol") || combined.contains("soccer") || combined.contains("cricket") || combined.contains("racing") || combined.contains("nba") || combined.contains("nfl") || combined.contains("mma") || combined.contains("boxing") || combined.contains("espn") || combined.contains("eurosport") || combined.contains("red bull") || combined.contains("fight") || combined.contains("wwe") || combined.contains("tennis") || combined.contains("golf") -> "Sports"
            combined.contains("movie") || combined.contains("cinema") || combined.contains("cine") || combined.contains("film") || combined.contains("pelicula") || combined.contains("кино") || combined.contains("أفلام") || combined.contains("filmes") || combined.contains("hbo") || combined.contains("cinemax") || combined.contains("starz") || combined.contains("paramount") -> "Movies"
            combined.contains("series") || combined.contains("serie") || combined.contains("drama") || combined.contains("telenovela") || combined.contains("novelas") || combined.contains("сериал") || combined.contains("مسلسل") || combined.contains("soap") -> "Series & Drama"
            combined.contains("music") || combined.contains("musica") || combined.contains("musique") || combined.contains("musik") || combined.contains("radio") || combined.contains("song") || combined.contains("vevo") || combined.contains("музык") || combined.contains("أغاني") || combined.contains("संगीत") || combined.contains("mtv") || combined.contains("vh1") || combined.contains("hits") -> "Music & Radio"
            combined.contains("kid") || combined.contains("anim") || combined.contains("cartoon") || combined.contains("toon") || combined.contains("kinder") || combined.contains("infantil") || combined.contains("enfant") || combined.contains("anime") || combined.contains("детск") || combined.contains("أطفال") || combined.contains("बच्चे") || combined.contains("disney") || combined.contains("nickelodeon") || combined.contains("nick") -> "Kids & Animation"
            combined.contains("doc") || combined.contains("history") || combined.contains("nature") || combined.contains("wildlife") || combined.contains("discovery") || combined.contains("geograph") || combined.contains("doku") || combined.contains("документ") || combined.contains("وثائقي") || combined.contains("national geographic") || combined.contains("animal") -> "Documentary"
            combined.contains("food") || combined.contains("cook") || combined.contains("chef") || combined.contains("cuisine") || combined.contains("cocina") || combined.contains("kochen") || combined.contains("recipe") || combined.contains("culinary") || combined.contains("खाना") || combined.contains("kitchen") -> "Cooking & Food"
            combined.contains("travel") || combined.contains("voyage") || combined.contains("reise") || combined.contains("viaje") || combined.contains("outdoor") || combined.contains("adventure") || combined.contains("tour") -> "Travel & Outdoors"
            combined.contains("comedy") || combined.contains("comedia") || combined.contains("humor") || combined.contains("satire") || combined.contains("юмор") || combined.contains("كوميديا") || combined.contains("हास्य") || combined.contains("standup") -> "Comedy"
            combined.contains("sci") || combined.contains("tech") || combined.contains("space") || combined.contains("nasa") || combined.contains("astronom") || combined.contains("robot") || combined.contains("наука") || combined.contains("विज्ञान") -> "Science & Tech"
            combined.contains("educat") || combined.contains("learning") || combined.contains("school") || combined.contains("university") || combined.contains("academy") || combined.contains("wissen") || combined.contains("culture") || combined.contains("शिक्षा") || combined.contains("arte") -> "Education & Culture"
            combined.contains("business") || combined.contains("financ") || combined.contains("market") || combined.contains("bloomberg") || combined.contains("cnbc") || combined.contains("economy") || combined.contains("börse") || combined.contains("व्यापार") || combined.contains("stock") -> "Business & Finance"
            combined.contains("gaming") || combined.contains("esport") || combined.contains("game") || combined.contains("twitch") || combined.contains("playstation") || combined.contains("xbox") -> "Gaming & Esports"
            combined.contains("relig") || combined.contains("church") || combined.contains("gospel") || combined.contains("bible") || combined.contains("jesus") || combined.contains("islam") || combined.contains("quran") || combined.contains("makkah") || combined.contains("madina") || combined.contains("hindu") || combined.contains("mandir") || combined.contains("puja") || combined.contains("bhakti") || combined.contains("sikh") || combined.contains("gurbani") || combined.contains("spiritual") || combined.contains("faith") || combined.contains("धार्मिक") || combined.contains("prayer") -> "Religious & Spiritual"
            combined.contains("lifestyle") || combined.contains("fashion") || combined.contains("beauty") || combined.contains("estilo") || combined.contains("living") || combined.contains("home") || combined.contains("garden") -> "Lifestyle"
            combined.contains("regional") || combined.contains("local") || combined.contains("community") || combined.contains("state tv") -> "Regional & Local"
            combined.contains("classic") || combined.contains("retro") || combined.contains("vintage") || combined.contains("oldies") || combined.contains("nostalgia") -> "Classic & Retro"
            combined.contains("entertainment") || combined.contains("entretenimiento") || combined.contains("divertissement") || combined.contains("unterhaltung") || combined.contains("развлеч") || combined.contains("मनोरंजन") -> "Entertainment"
            else -> "General"
        }
    }

    private fun saveCachedChannels(channels: List<Channel>) {
        try {
            val target = cacheFile
            val tempFile = File(target.parentFile, "${target.name}.tmp")
            BufferedWriter(OutputStreamWriter(FileOutputStream(tempFile), Charsets.UTF_8)).use { writer ->
                for ((id, name, streamUrl, logoUrl, genre, country, language, isLocked, isFavorite, resolution, isAdultOrRestricted, description, httpUserAgent, httpReferrer) in channels) {
                    val obj = org.json.JSONObject()
                    obj.put("id", id)
                    obj.put("name", name)
                    obj.put("streamUrl", streamUrl)
                    obj.put("logoUrl", logoUrl ?: "")
                    obj.put("genre", genre)
                    obj.put("country", country)
                    obj.put("language", language)
                    obj.put("isLocked", isLocked)
                    obj.put("isFavorite", isFavorite)
                    obj.put("resolution", resolution)
                    obj.put("isAdultOrRestricted", isAdultOrRestricted)
                    obj.put("description", description)
                    obj.put("httpUserAgent", httpUserAgent ?: "")
                    obj.put("httpReferrer", httpReferrer ?: "")
                    writer.write(obj.toString())
                    writer.newLine()
                }
            }
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
                tempFile.delete()
            }
            prefs.edit { remove("cached_channels_json") }
        } catch (e: Exception) {
            Log.e("PlaylistRepo", "Failed to cache channels", e)
        }
    }

    private fun loadCachedChannels(): List<Channel> {
        val file = cacheFile
        if (!file.exists()) return emptyList()
        return try {
            val result = ArrayList<Channel>(14000)
            BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        val obj = org.json.JSONObject(line)
                        result.add(
                            Channel(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                streamUrl = obj.getString("streamUrl"),
                                logoUrl = obj.optString("logoUrl", "").takeIf { it.isNotEmpty() },
                                genre = obj.optString("genre", "General"),
                                country = obj.optString("country", "GLOBAL"),
                                language = obj.optString("language", "English"),
                                isLocked = obj.optBoolean("isLocked", false),
                                isFavorite = obj.optBoolean("isFavorite", false),
                                resolution = obj.optString("resolution", "720p HD"),
                                isAdultOrRestricted = obj.optBoolean("isAdultOrRestricted", false),
                                description = obj.optString("description", ""),
                                httpUserAgent = obj.optString("httpUserAgent", "").takeIf { it.isNotEmpty() },
                                httpReferrer = obj.optString("httpReferrer", "").takeIf { it.isNotEmpty() },
                            ),
                        )
                    }
                    line = reader.readLine()
                }
            }
            result
        } catch (e: Exception) {
            Log.w("PlaylistRepo", "Failed to read cached channels: ${e.message}")
            emptyList()
        }
    }

    private fun mergeChannels(curated: List<Channel>, parsed: List<Channel>): List<Channel> {
        val map = LinkedHashMap<String, Channel>()
        for (channel in curated) {
            map[channel.id] = channel
        }
        // Keep the bundled or refreshed playlist's source name and stream URL for matching IDs.
        for (channel in parsed) {
            map[channel.id] = channel
        }
        return map.values.toList()
    }

    /**
     * Local curated channels with configured stream links. Availability depends on each
     * broadcaster's server and can change independently of the bundled playlist.
     */
    fun getCuratedChannels(): List<Channel> {
        return listOf(
            // 1. FRANCE 24 ENGLISH - Verified 1080p HLS stream
            Channel(
                id = "france24_english",
                name = "France 24 English",
                streamUrl = "https://live.france24.com/hls/live/2037218-b/F24_EN_HI_HLS/master_5000.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/France_24_logo.svg/320px-France_24_logo.svg.png",
                genre = "News",
                country = "FR",
                language = "English",
                resolution = "1080p FHD",
                description = "International news 24/7 in English from Paris covering world politics, economy, and culture",
            ),

            // 2. SKY NEWS LIVE HD - Premier Australian / British Breaking News
            Channel(
                id = "sky_news_live",
                name = "Sky News Live HD",
                streamUrl = "https://skynewsau-live.akamaized.net/hls/live/2002689/skynewsau-extra1/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Sky_News_2018.svg/320px-Sky_News_2018.svg.png",
                genre = "News",
                country = "UK",
                language = "English",
                resolution = "1080p FHD",
                description = "Live breaking international news, political interviews, and round-the-clock investigative journalism",
            ),

            // 3. BLOOMBERG TV LIVE - Global Financial & Markets Live
            Channel(
                id = "bloomberg_tv",
                name = "Bloomberg TV Live",
                streamUrl = "https://bloomberg.com/media-manifest/streams/us.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/56/Bloomberg_Television_logo.svg/320px-Bloomberg_Television_logo.svg.png",
                genre = "News",
                country = "US",
                language = "English",
                resolution = "1080p FHD",
                description = "Global financial markets, Wall Street indices, business news, and executive interviews",
            ),

            // 4. TRT WORLD HD - English International Broadcast
            Channel(
                id = "trt_world_global",
                name = "TRT World HD",
                streamUrl = "https://tv-trtworld.medya.trt.com.tr/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/TRT_World_logo.svg/320px-TRT_World_logo.svg.png",
                genre = "News",
                country = "TR",
                language = "English",
                resolution = "1080p FHD",
                description = "International news channel broadcasting in English with human-centered global storytelling",
            ),

            // 5. CGTN ENGLISH LIVE - Worldwide News & Analysis
            Channel(
                id = "cgtn_english",
                name = "CGTN English Live",
                streamUrl = "https://news.cgtn.com/resource/live/english/cgtn-news.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/CGTN_logo.svg/320px-CGTN_logo.svg.png",
                genre = "News",
                country = "GLOBAL",
                language = "English",
                resolution = "1080p FHD",
                description = "Global news television network providing news, current affairs, and comprehensive world coverage",
            ),

            // 6. AL JAZEERA ARABIC - Middle East & Global Arab World
            Channel(
                id = "aljazeera_arabic",
                name = "Al Jazeera Arabic",
                streamUrl = "https://live-hls-web-aja.getaj.net/AJA/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Al_Jazeera_Arabic_logo.svg/320px-Al_Jazeera_Arabic_logo.svg.png",
                genre = "News",
                country = "QA",
                language = "Arabic",
                resolution = "1080p FHD",
                description = "Leading Arab world news broadcaster with live coverage, political debates, and regional updates",
            ),

            // 7. AL JAZEERA MUBASHER - Live Debates & Unfiltered Events
            Channel(
                id = "aljazeera_mubasher",
                name = "Al Jazeera Mubasher",
                streamUrl = "https://live-hls-web-ajm.getaj.net/AJM/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Al_Jazeera_Arabic_logo.svg/320px-Al_Jazeera_Arabic_logo.svg.png",
                genre = "News",
                country = "QA",
                language = "Arabic",
                resolution = "1080p FHD",
                description = "Live unedited public conferences, parliament debates, and breaking press briefings",
            ),

            // 8. DW NEWS ENGLISH - German International Broadcaster
            Channel(
                id = "dw_news_english",
                name = "DW News English",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_symbol_2012.svg/320px-Deutsche_Welle_symbol_2012.svg.png",
                genre = "News",
                country = "DE",
                language = "English",
                resolution = "1080p FHD",
                description = "German international public broadcaster delivering global breaking news, business, and analysis",
            ),

            // 9. FRANCE 24 FRANÇAIS - Le Journal Télévisé en Continu
            Channel(
                id = "france24_french",
                name = "France 24 Français",
                streamUrl = "https://live.france24.com/hls/live/2037179-b/F24_FR_HI_HLS/master_5000.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/France_24_logo.svg/320px-France_24_logo.svg.png",
                genre = "News",
                country = "FR",
                language = "French",
                resolution = "1080p FHD",
                description = "Le journal télévisé international en continu, débats et reportages en langue française",
            ),

            // 10. FRANCE 24 ESPAÑOL - Noticias Internacionales en Español
            Channel(
                id = "france24_spanish",
                name = "France 24 Español",
                streamUrl = "https://live.france24.com/hls/live/2037220-b/F24_ES_HI_HLS/master_5000.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/France_24_logo.svg/320px-France_24_logo.svg.png",
                genre = "News",
                country = "ES",
                language = "Spanish",
                resolution = "1080p FHD",
                description = "Noticias del mundo en vivo en español, actualidad internacional y análisis político",
            ),

            // 11. FRANCE 24 ARABIC - الأخبار الدولية باللغة العربية
            Channel(
                id = "france24_arabic",
                name = "France 24 Arabic",
                streamUrl = "https://live.france24.com/hls/live/2037222-b/F24_AR_HI_HLS/master_5000.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/France_24_logo.svg/320px-France_24_logo.svg.png",
                genre = "News",
                country = "AE",
                language = "Arabic",
                resolution = "1080p FHD",
                description = "الأخبار الدولية على مدار 24 ساعة باللغة العربية مع برامج حوارية وتحقيقات صحفية",
            ),

            // 12. TRT HABER HD - Canlı Haber Yayını
            Channel(
                id = "trt_haber_turkey",
                name = "TRT Haber HD",
                streamUrl = "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/TRT_Haber_logo.svg/320px-TRT_Haber_logo.svg.png",
                genre = "News",
                country = "TR",
                language = "Turkish",
                resolution = "1080p FHD",
                description = "Türkiye ve dünyadan sıcak gelişmeler, ekonomi, kültür ve gündem haberleri canlı yayında",
            ),

            // 13. TRT ARABİ HD - البث الإخباري التركي باللغة العربية
            Channel(
                id = "trt_arabi",
                name = "TRT Arabi HD",
                streamUrl = "https://tv-trtarabi.medya.trt.com.tr/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/TRT_World_logo.svg/320px-TRT_World_logo.svg.png",
                genre = "News",
                country = "TR",
                language = "Arabic",
                resolution = "1080p FHD",
                description = "قناة الأخبار العربية التابعة لهيئة الإذاعة والتلفزيون التركية تغطي قضايا الشرق الأوسط والعالم",
            ),

            // 14. TRT TÜRK HD - Kültür, Haber ve Eğlence
            Channel(
                id = "trt_turk",
                name = "TRT Türk HD",
                streamUrl = "https://tv-trtturk.medya.trt.com.tr/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/TRT_Haber_logo.svg/320px-TRT_Haber_logo.svg.png",
                genre = "General",
                country = "TR",
                language = "Turkish",
                resolution = "1080p FHD",
                description = "Dünyadaki Türk toplumuna yönelik haberler, kültür, müzik ve aile programları",
            ),

            // 15. 2GB NEWS NETWORK HD - Australian Live Journalism
            Channel(
                id = "news_2gb_live",
                name = "2GB News Network HD",
                streamUrl = "https://2gblive.akamaized.net/hls/live/2033805/2GB/index.m3u8",
                logoUrl = "https://i.ibb.co/jwM8DFG/2GB-1.png",
                genre = "News",
                country = "AU",
                language = "English",
                resolution = "1080p FHD",
                description = "Live Australian breaking journalism, political opinion, and current affairs coverage",
            ),

            // 16. 1TV WORLD BROADCAST - Georgian Public Broadcaster
            Channel(
                id = "one_tv_live",
                name = "1TV World Broadcast",
                streamUrl = "https://tv.cdn.xsg.ge/gpb-1tv/index.m3u8",
                logoUrl = "https://i.imgur.com/FSkYLPK.png",
                genre = "News",
                country = "GE",
                language = "English",
                resolution = "720p HD",
                description = "International news, regional culture, and live event public coverage",
            ),

            // 17. 2 TV PUBLIC BROADCAST - Parliamentary & Education
            Channel(
                id = "two_tv_live",
                name = "2 TV Public Broadcast",
                streamUrl = "https://tv.cdn.xsg.ge/gpb-2tv/index.m3u8",
                logoUrl = "https://i.imgur.com/FSkYLPK.png",
                genre = "News",
                country = "GE",
                language = "English",
                resolution = "720p HD",
                description = "Educational programming, parliamentary debates, and public interest documentaries",
            ),

            // 18. RED BULL TV LIVE - Action Sports & F1
            Channel(
                id = "redbull_tv",
                name = "Red Bull TV Live",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f5/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png",
                genre = "Sports",
                country = "AT",
                language = "English",
                resolution = "1080p 60fps",
                description = "High octane action sports, Formula 1, extreme motorsport, and global championship events",
            ),

            // 19. SPORTSGRID TV - Live Scores & Betting Analysis
            Channel(
                id = "sports_grid",
                name = "SportsGrid TV",
                streamUrl = "https://sportsgrid-klowdtv.amagi.tv/playlist.m3u8",
                logoUrl = "https://i.imgur.com/kS5eF5d.png",
                genre = "Sports",
                country = "US",
                language = "English",
                resolution = "720p HD",
                description = "Real-time sports scores, game day statistics, live betting odds, and expert analysis",
            ),

            // 20. 00s REPLAY PLUTO CINEMA - Blockbuster Movies
            Channel(
                id = "movies_00s_replay",
                name = "00s Replay Pluto Cinema",
                streamUrl = "https://jmp2.uk/plu-62ba60f059624e000781c436.m3u8",
                logoUrl = "https://images.pluto.tv/channels/62ba60f059624e000781c436/colorLogoPNG.png",
                genre = "Movies",
                country = "US",
                language = "English",
                resolution = "1080p FHD",
                description = "Classic 2000s feature films, blockbuster comedy hits, drama, and action spectacles",
            ),

            // 21. NASA TV HD - Live Rocket Launches & Space Exploration
            Channel(
                id = "nasa_tv_hd",
                name = "NASA TV HD",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/320px-NASA_logo.svg.png",
                genre = "Science & Tech",
                country = "US",
                language = "English",
                resolution = "1080p FHD",
                description = "Live rocket launches, spacewalks, International Space Station orbit views, and solar astronomy",
            ),

            // 22. CGTN DOCUMENTARY HD - Nature, History & Discovery
            Channel(
                id = "cgtn_documentary",
                name = "CGTN Documentary HD",
                streamUrl = "https://news.cgtn.com/resource/live/document/cgtn-doc.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/CGTN_logo.svg/320px-CGTN_logo.svg.png",
                genre = "Documentary",
                country = "GLOBAL",
                language = "English",
                resolution = "1080p FHD",
                description = "Award-winning international documentaries covering history, archaeological wonders, wildlife and human culture",
            ),

            // 23. RT DOCUMENTARY LIVE - In-Depth Global Stories
            Channel(
                id = "rt_doc_live",
                name = "RT Documentary Live",
                streamUrl = "https://rtd.rt.com/hls/rtd_en.m3u8",
                logoUrl = "https://i.imgur.com/kQoK0Z4.png",
                genre = "Documentary",
                country = "EU",
                language = "English",
                resolution = "1080p FHD",
                description = "Fascinating investigative documentaries, extreme survival, natural phenomena and human courage",
            ),

            // 24. 3ABN GLOBAL NETWORK - Health & Lifestyle
            Channel(
                id = "three_abn_global",
                name = "3ABN Global Network",
                streamUrl = "https://3abn.bozztv.com/3abn2/3abn_live/smil:3abn_live.smil/playlist.m3u8",
                logoUrl = "https://i.imgur.com/kQoK0Z4.png",
                genre = "Lifestyle",
                country = "US",
                language = "English",
                resolution = "1080p FHD",
                description = "Wholesome health lifestyle, nature exploration documentaries, cooking, and inspiration",
            ),

            // 25. 3ABN KIDS TV LIVE - Educational Cartoons & Stories
            Channel(
                id = "three_abn_kids",
                name = "3ABN Kids TV Live",
                streamUrl = "https://3abn.bozztv.com/3abn2/Kids_live/smil:Kids_live.smil/playlist.m3u8",
                logoUrl = "https://i.imgur.com/kQoK0Z4.png",
                genre = "Kids & Animation",
                country = "US",
                language = "English",
                resolution = "720p HD",
                description = "Wholesome children's animated stories, nature discovery series, and sing-along adventures",
            ),

            // 26. 1-2-3.TV LIVE HD - German Lifestyle & Shopping
            Channel(
                id = "one_two_three_tv",
                name = "1-2-3.tv Live HD",
                streamUrl = "https://123tv-mx1.flex-cdn.net/index.m3u8",
                logoUrl = "https://i.imgur.com/slSUDNX.png",
                genre = "Lifestyle",
                country = "DE",
                language = "German",
                resolution = "720p HD",
                description = "Live lifestyle, home products, innovative consumer gadgets, and entertainment channel",
            ),

            // 27. AFTERDARK LATE CINEMA (18+) - Parental PIN Protected
            Channel(
                id = "night_club_afterdark",
                name = "AfterDark Late Cinema (18+)",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://i.imgur.com/3VzM2g4.png",
                genre = "Adult (18+)",
                country = "US",
                language = "English",
                resolution = "1080p FHD",
                isAdultOrRestricted = true,
                description = "Mature late-night cinematic programming and extreme sports (Parental PIN protected)",
            ),

            // 28. DD NEWS HD (INDIA) - India's National Public Broadcaster
            Channel(
                id = "dd_news_india",
                name = "DD News HD",
                streamUrl = "https://d3qs3d2rkhfqrt.cloudfront.net/out/v1/0811cd8c37ca4c409d5385a6cd2fa18b/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/DD_News_logo.svg/320px-DD_News_logo.svg.png",
                genre = "News",
                country = "IN",
                language = "Hindi",
                resolution = "1080p FHD",
                description = "Doordarshan 24x7 terrestrial and satellite national news channel of India broadcasting in Hindi & English",
            ),

            // 29. DD INDIA HD (INDIA) - International Global Indian News
            Channel(
                id = "dd_india_hd",
                name = "DD India HD",
                streamUrl = "https://d3qs3d2rkhfqrt.cloudfront.net/out/v1/afd2e335b0ba40eb9bdf1096118c6ede/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/DD_India_logo.svg/320px-DD_India_logo.svg.png",
                genre = "News",
                country = "IN",
                language = "English",
                resolution = "1080p FHD",
                description = "India's premier international English news and current affairs channel representing India's perspective globally",
            ),

            // 30. AAJ TAK HD (INDIA) - सबसे तेज़ हिन्दी समाचार
            Channel(
                id = "aaj_tak_india",
                name = "Aaj Tak HD",
                streamUrl = "https://feeds.intoday.in/aajtak/api/aajtakhd/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/28/Aaj_tak_logo.png/320px-Aaj_tak_logo.png",
                genre = "News",
                country = "IN",
                language = "Hindi",
                resolution = "1080p FHD",
                description = "भारत का सबसे लोकप्रिय 24 घंटे का हिंदी समाचार चैनल - राजनीति, खेल, मनोरंजन और ताज़ा खबरें"
            ),

            // 31. WION NEWS (INDIA) - World Is One News
            Channel(
                id = "wion_news_india",
                name = "WION News HD",
                streamUrl = "https://d3648s4fuh7k92.cloudfront.net/out/v1/b855e966e3fa41fca82d96924844ce2f/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/WION_Logo.svg/320px-WION_Logo.svg.png",
                genre = "News",
                country = "IN",
                language = "English",
                resolution = "1080p FHD",
                description = "World Is One News - Indian multinational English news channel reporting global politics and diplomacy"
            ),

            // 32. NDTV INDIA - विश्वसनीय हिन्दी ख़बरें
            Channel(
                id = "ndtv_india",
                name = "NDTV India",
                streamUrl = "https://ndtvindiaelemarchana.akamaized.net/hls/live/2003679/ndtvindia/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/NDTV_India_logo.svg/320px-NDTV_India_logo.svg.png",
                genre = "News",
                country = "IN",
                language = "Hindi",
                resolution = "1080p FHD",
                description = "सटीक और निष्पक्ष विश्लेषण, देश और दुनिया की हर बड़ी ख़बर - एनडीटीवी इंडिया लाइव"
            ),

            // 33. SANSAD TV HD (INDIA) - Indian Parliament Broadcaster
            Channel(
                id = "sansad_tv_india",
                name = "Sansad TV HD",
                streamUrl = "https://d2lk5u59tns74c.cloudfront.net/out/v1/fff8f20221d5456e8922e689d71dedc3/index.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Sansad_TV_logo.svg/320px-Sansad_TV_logo.svg.png",
                genre = "News",
                country = "IN",
                language = "Hindi",
                resolution = "1080p FHD",
                description = "Live Lok Sabha and Rajya Sabha sessions, parliamentary debates, national governance and Indian public affairs"
            )
        )
    }
}
