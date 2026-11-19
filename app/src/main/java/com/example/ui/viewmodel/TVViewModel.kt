package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.ChannelGenre
import com.example.data.model.OfflineMedia
import com.example.data.model.UserProfile
import com.example.data.model.isOfflineMetadata
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.HistoryAndOfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import java.net.InetAddress
import java.net.URI

enum class ScreenTab {
    HOME,
    FAVORITES,
    CHANNELS,
    PROFILES,
    PARENTAL,
    SETTINGS
}

@Immutable
data class RegionItem(val code: String, val name: String, val flag: String)

@Immutable
data class StorageMetrics(
    val offlineChannelsCount: Int = 0,
    val historyEntriesCount: Int = 0,
    val totalCacheSizeMb: Double = 0.0,
)

enum class AspectRatioMode(val label: String) {
    AUTO("Auto Device"),
    FIT("16:9 Fit"),
    FILL("Crop / Fill"),
    STRETCH("Stretch"),
    FOUR_THREE("4:3 Standard"),
}

enum class PlaybackQuality(val label: String, val bitRateLimitDesc: String) {
    AUTO("Auto Adaptive", "Dynamic resolution"),
    HIGH("1080p FHD", "High bitrate"),
    DATA_SAVER("Low Bandwidth Eco", "Saves 70% mobile data"),
    AUDIO_ONLY("Audio Only Mode", "Saves 95% data for commuting"),
}

enum class ChannelSortOrder(val label: String) {
    A_TO_Z("A → Z"),
    Z_TO_A("Z → A"),
}

class TVViewModel(private val app: Application) : AndroidViewModel(app) {

    val playlistRepo = PlaylistRepository(app)
    val profileRepo = ProfileRepository(app)
    val historyAndOfflineRepo = HistoryAndOfflineRepository(app)

    val scannedChannelCount = playlistRepo.scannedChannelCount
    val activeChannelCount = playlistRepo.activeChannelCount
    val inactiveChannelCount = playlistRepo.inactiveChannelCount
    val isScanComplete = playlistRepo.isScanComplete
    val scanEtaSeconds = playlistRepo.scanEtaSeconds

    fun startChannelScan() {
        viewModelScope.launch {
            playlistRepo.verifyAllChannels()
        }
    }

    private val _localNetworkPermissionRequested = MutableStateFlow(value = false)
    val localNetworkPermissionRequested: StateFlow<Boolean> = _localNetworkPermissionRequested.asStateFlow()
    private val pendingLocalNetworkActions = mutableListOf<() -> Unit>()

    val appUpdateRepo = com.example.data.repository.AppUpdateRepository()
    val appUpdateStatus: StateFlow<com.example.data.repository.AppUpdateStatus> = appUpdateRepo.updateStatus

    fun checkForAppUpdate(force: Boolean = true) {
        viewModelScope.launch {
            appUpdateRepo.checkForUpdates(force)
        }
    }

    // Cached SharedPreferences instance to eliminate repeated Context reflection & disk lookups
    private val settingsPrefs by lazy {
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
    }

    private val _appInterfaceLanguage = MutableStateFlow(
        try {
            val code = settingsPrefs.getString("app_interface_lang", "en") ?: "en"
            com.example.util.AppLanguage.entries.find { it.code == code } ?: com.example.util.AppLanguage.ENGLISH
        } catch (_: Exception) {
            com.example.util.AppLanguage.ENGLISH
        },
    )
    val appInterfaceLanguage: StateFlow<com.example.util.AppLanguage> = _appInterfaceLanguage.asStateFlow()

    fun setAppInterfaceLanguage(lang: com.example.util.AppLanguage) {
        _appInterfaceLanguage.value = lang
        settingsPrefs.edit { putString("app_interface_lang", lang.code) }
    }

    private val _appThemeMode = MutableStateFlow(
        try {
            val modeName = settingsPrefs.getString("app_theme_mode", com.example.ui.theme.AppThemeMode.SLATE.name)
                ?: com.example.ui.theme.AppThemeMode.SLATE.name
            com.example.ui.theme.AppThemeMode.valueOf(modeName)
        } catch (_: Exception) {
            com.example.ui.theme.AppThemeMode.SLATE
        },
    )
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _appThemeMode.value = mode
        settingsPrefs.edit { putString("app_theme_mode", mode.name) }
    }

    val availableRegions = listOf(
        RegionItem("ALL", "All Regions", "🌍"),
        RegionItem("US", "United States", "🇺🇸"),
        RegionItem("UK", "United Kingdom", "🇬🇧"),
        RegionItem("IN", "India", "🇮🇳"),
        RegionItem("CA", "Canada", "🇨🇦"),
        RegionItem("AU", "Australia", "🇦🇺"),
        RegionItem("DE", "Germany", "🇩🇪"),
        RegionItem("FR", "France", "🇫🇷"),
        RegionItem("ES", "Spain", "🇪🇸"),
        RegionItem("IT", "Italy", "🇮🇹"),
        RegionItem("BR", "Brazil", "🇧🇷"),
        RegionItem("MX", "Mexico", "🇲🇽"),
        RegionItem("AR", "Argentina", "🇦🇷"),
        RegionItem("CO", "Colombia", "🇨🇴"),
        RegionItem("CL", "Chile", "🇨🇱"),
        RegionItem("PE", "Peru", "🇵🇪"),
        RegionItem("JP", "Japan", "🇯🇵"),
        RegionItem("KR", "South Korea", "🇰🇷"),
        RegionItem("CN", "China", "🇨🇳"),
        RegionItem("TR", "Turkey", "🇹🇷"),
        RegionItem("RU", "Russia", "🇷🇺"),
        RegionItem("UA", "Ukraine", "🇺🇦"),
        RegionItem("SA", "Saudi Arabia", "🇸🇦"),
        RegionItem("AE", "UAE & Arab", "🇦🇪"),
        RegionItem("QA", "Qatar", "🇶🇦"),
        RegionItem("EG", "Egypt", "🇪🇬"),
        RegionItem("PK", "Pakistan", "🇵🇰"),
        RegionItem("BD", "Bangladesh", "🇧🇩"),
        RegionItem("ID", "Indonesia", "🇮🇩"),
        RegionItem("PH", "Philippines", "🇵🇭"),
        RegionItem("NG", "Nigeria", "🇳🇬"),
        RegionItem("ZA", "South Africa", "🇿🇦"),
        RegionItem("NL", "Netherlands", "🇳🇱"),
        RegionItem("CH", "Switzerland", "🇨🇭"),
        RegionItem("AT", "Austria", "🇦🇹"),
        RegionItem("BE", "Belgium", "🇧🇪"),
        RegionItem("SE", "Sweden", "🇸🇪"),
        RegionItem("NO", "Norway", "🇳🇴"),
        RegionItem("DK", "Denmark", "🇩🇰"),
        RegionItem("FI", "Finland", "🇫🇮"),
        RegionItem("PL", "Poland", "🇵🇱"),
        RegionItem("GR", "Greece", "🇬🇷"),
        RegionItem("PT", "Portugal", "🇵🇹"),
        RegionItem("IE", "Ireland", "🇮🇪"),
        RegionItem("NZ", "New Zealand", "🇳🇿"),
        RegionItem("SG", "Singapore", "🇸🇬"),
        RegionItem("MY", "Malaysia", "🇲🇾"),
        RegionItem("TH", "Thailand", "🇹🇭"),
        RegionItem("VN", "Vietnam", "🇻🇳"),
        RegionItem("IL", "Israel", "🇮🇱"),
        RegionItem("MA", "Morocco", "🇲🇦"),
        RegionItem("DZ", "Algeria", "🇩🇿"),
        RegionItem("GE", "Georgia", "🇬🇪"),
        RegionItem("GLOBAL", "Global", "🌐"),
    )

    val availableLanguages = listOf("ALL") + com.example.util.AppLanguage.entries.map { it.displayName }

    private val _selectedLanguage = MutableStateFlow("ALL")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    private val _selectedSubcategory = MutableStateFlow("ALL")
    val selectedSubcategory: StateFlow<String> = _selectedSubcategory.asStateFlow()

    fun selectSubcategory(sub: String) {
        _selectedSubcategory.value = sub
    }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _selectedGenre.value = ChannelGenre.ALL.displayName
        _selectedSubcategory.value = "ALL"
        _selectedCountryFilter.value = "ALL"
        _selectedLanguage.value = "ALL"
    }

    fun getSubcategoriesForGenre(genre: String): List<String> {
        return when (genre) {
            ChannelGenre.NEWS.displayName -> listOf("ALL", "Live Now", "Offline", "World 24/7", "Financial & Markets", "Public Broadcast")
            ChannelGenre.SPORTS.displayName -> listOf("ALL", "Live Now", "Offline", "Extreme Action", "Motorsport", "Scores & Odds")
            ChannelGenre.MOVIES.displayName -> listOf("ALL", "Live Now", "Offline", "2000s Hits", "Blockbusters", "World Cinema")
            ChannelGenre.SERIES.displayName -> listOf("ALL", "Live Now", "Offline", "Drama", "Crime", "Telenovelas", "Sci-Fi")
            ChannelGenre.ENTERTAINMENT.displayName -> listOf("ALL", "Live Now", "Offline", "Reality TV", "Talk Shows", "Game Shows", "Celebrity")
            ChannelGenre.MUSIC.displayName -> listOf("ALL", "Live Now", "Offline", "Pop & Rock", "Hip-Hop", "Electronic", "Classical", "Jazz")
            ChannelGenre.KIDS.displayName -> listOf("ALL", "Live Now", "Offline", "Animation", "Discovery", "Stories", "Preschool")
            ChannelGenre.DOCUMENTARY.displayName -> listOf("ALL", "Live Now", "Offline", "Space", "Nature & Living", "History", "Wildlife")
            ChannelGenre.LIFESTYLE.displayName -> listOf("ALL", "Live Now", "Offline", "Tech & Gadgets", "Home & Shopping", "Culture", "Fashion")
            ChannelGenre.FOOD.displayName -> listOf("ALL", "Live Now", "Offline", "Recipes", "Baking", "Street Food", "Restaurants")
            ChannelGenre.TRAVEL.displayName -> listOf("ALL", "Live Now", "Offline", "Outdoors", "Adventures", "Road Trips", "Destinations")
            ChannelGenre.COMEDY.displayName -> listOf("ALL", "Live Now", "Offline", "Sketches", "Animation Satire", "Parody", "Stand-Up")
            ChannelGenre.SCIENCE.displayName -> listOf("ALL", "Live Now", "Offline", "Astronomy & ISS", "Rocket Launches", "Robotics", "AI & Cyber")
            ChannelGenre.EDUCATION.displayName -> listOf("ALL", "Live Now", "Offline", "History", "Languages", "University", "Culture")
            ChannelGenre.BUSINESS.displayName -> listOf("ALL", "Live Now", "Offline", "Stock Markets", "Crypto", "Economy", "Interviews")
            ChannelGenre.GAMING.displayName -> listOf("ALL", "Live Now", "Offline", "Esports", "Gameplays", "Tournaments", "Retro Gaming")
            ChannelGenre.RELIGIOUS.displayName -> listOf("ALL", "Live Now", "Offline", "Christian", "Islamic", "Hindu", "Buddhist", "Spirituality")
            ChannelGenre.REGIONAL.displayName -> listOf("ALL", "Live Now", "Offline", "Local News", "Community", "State Broadcast")
            ChannelGenre.CLASSIC.displayName -> listOf("ALL", "Live Now", "Offline", "Vintage Cinema", "Retro TV", "Golden Age")
            else -> listOf("ALL", "Popular", "Live Now", "Offline", "HD Broadcast")
        }
    }

    // Storage metrics for Room database cache
    private val _storageMetrics = MutableStateFlow(StorageMetrics())
    val storageMetrics: StateFlow<StorageMetrics> = _storageMetrics.asStateFlow()

    // Navigation state
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Search and Genre Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isFullScreenSearchOpen = MutableStateFlow(value = false)
    val isFullScreenSearchOpen: StateFlow<Boolean> = _isFullScreenSearchOpen.asStateFlow()

    fun openFullScreenSearch() {
        _isFullScreenSearchOpen.value = true
    }

    fun closeFullScreenSearch() {
        _isFullScreenSearchOpen.value = false
    }

    private val _selectedGenre = MutableStateFlow(ChannelGenre.ALL.displayName)
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow("ALL")
    val selectedCountryFilter: StateFlow<String> = _selectedCountryFilter.asStateFlow()

    private val _channelSortOrder = MutableStateFlow(
        try {
            val orderName = settingsPrefs.getString("channel_sort_order", ChannelSortOrder.A_TO_Z.name)
            ChannelSortOrder.entries.find { it.name == orderName } ?: ChannelSortOrder.A_TO_Z
        } catch (_: Exception) {
            ChannelSortOrder.A_TO_Z
        },
    )
    val channelSortOrder: StateFlow<ChannelSortOrder> = _channelSortOrder.asStateFlow()

    fun setChannelSortOrder(order: ChannelSortOrder) {
        _channelSortOrder.value = order
        settingsPrefs.edit { putString("channel_sort_order", order.name) }
    }

    fun toggleChannelSortOrder() {
        val next = if (_channelSortOrder.value == ChannelSortOrder.A_TO_Z) {
            ChannelSortOrder.Z_TO_A
        } else {
            ChannelSortOrder.A_TO_Z
        }
        setChannelSortOrder(next)
    }

    // Currently playing channel or offline media
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _playingOfflineMedia = MutableStateFlow<OfflineMedia?>(null)
    val playingOfflineMedia: StateFlow<OfflineMedia?> = _playingOfflineMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isFullscreen = MutableStateFlow(value = false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // Picture in Picture (PiP) State
    private val _isPipMode = MutableStateFlow(value = false)
    val isPipMode: StateFlow<Boolean> = _isPipMode.asStateFlow()

    private val _enterPipRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val enterPipRequest: SharedFlow<Unit> = _enterPipRequest.asSharedFlow()

    private val _isMiniPlayerDismissed = MutableStateFlow(value = false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _playbackQuality = MutableStateFlow(PlaybackQuality.AUTO)
    val playbackQuality: StateFlow<PlaybackQuality> = _playbackQuality.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(value = false)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    private val _isMuted = MutableStateFlow(value = false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    private val _captureToastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val captureToastMessage: SharedFlow<String> = _captureToastMessage.asSharedFlow()

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        val activeMinutes = minutes?.takeIf { it > 0 }
        if (activeMinutes != null) {
            sleepTimerJob = viewModelScope.launch {
                delay(activeMinutes.minutes)
                _isPlaying.value = false
                _sleepTimerMinutes.value = null
            }
        }
    }

    private val _selectedSubtitleLanguage = MutableStateFlow("English [CC]")
    val selectedSubtitleLanguage: StateFlow<String> = _selectedSubtitleLanguage.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow("Original Audio (Stereo)")
    val selectedAudioTrack: StateFlow<String> = _selectedAudioTrack.asStateFlow()

    // 18+ Adult Channels Restriction toggle (ON / OFF)
    private val _isAdultRestrictionEnabled = MutableStateFlow(
        settingsPrefs.getBoolean("adult_restriction_enabled", true),
    )
    val isAdultRestrictionEnabled: StateFlow<Boolean> = _isAdultRestrictionEnabled.asStateFlow()

    fun toggleAdultRestriction(enabled: Boolean) {
        _isAdultRestrictionEnabled.value = enabled
        settingsPrefs.edit { putBoolean("adult_restriction_enabled", enabled) }
    }

    // Channel Live / Broken health tracking
    private val _brokenChannelIds = MutableStateFlow(
        settingsPrefs.getStringSet("broken_channel_ids", emptySet())?.toSet() ?: emptySet(),
    )
    val brokenChannelIds: StateFlow<Set<String>> = _brokenChannelIds.asStateFlow()

    private val _liveChannelIds = MutableStateFlow(
        setOf(
            "france24_english", "sky_news_live", "bloomberg_tv", "trt_world_global",
            "cgtn_english", "aljazeera_arabic", "aljazeera_mubasher", "dw_news_english",
            "france24_french", "france24_spanish", "france24_arabic", "trt_haber_turkey",
            "trt_arabi", "trt_turk", "news_2gb_live", "one_tv_live", "two_tv_live",
            "redbull_tv", "sports_grid", "movies_00s_replay", "nasa_tv_hd",
            "cgtn_documentary", "rt_doc_live", "three_abn_global", "three_abn_kids",
            "one_two_three_tv", "night_club_afterdark",
        ),
    )
    val liveChannelIds: StateFlow<Set<String>> = _liveChannelIds.asStateFlow()

    fun markChannelLive(channelId: String) {
        _liveChannelIds.value += channelId
        _brokenChannelIds.value -= channelId
        settingsPrefs.edit { putStringSet("broken_channel_ids", _brokenChannelIds.value) }
    }

    fun markChannelBroken(channelId: String) {
        _brokenChannelIds.value += channelId
        _liveChannelIds.value -= channelId
        settingsPrefs.edit { putStringSet("broken_channel_ids", _brokenChannelIds.value) }
    }

    // Parental PIN Prompt Modal State
    private val _isPinDialogVisible = MutableStateFlow(value = false)
    val isPinDialogVisible: StateFlow<Boolean> = _isPinDialogVisible.asStateFlow()

    private val _pendingPinChannel = MutableStateFlow<Channel?>(null)

    private val _pinDialogReason = MutableStateFlow("Parental PIN required to unlock this channel")
    val pinDialogReason: StateFlow<String> = _pinDialogReason.asStateFlow()

    private val _pinActionType = MutableStateFlow(PinActionType.PLAY_CHANNEL)

    enum class PinActionType {
        PLAY_CHANNEL,
        OPEN_PARENTAL_SETTINGS,
        TOGGLE_CHANNEL_LOCK
    }

    // Wireless Screen Cast Feature
    private val _isCastDialogVisible = MutableStateFlow(value = false)
    val isCastDialogVisible: StateFlow<Boolean> = _isCastDialogVisible.asStateFlow()

    private val _isCastingActive = MutableStateFlow(value = false)
    val isCastingActive: StateFlow<Boolean> = _isCastingActive.asStateFlow()

    private class FilterParams(
        val query: String,
        val genre: String,
        val subcategory: String,
        val country: String,
        val language: String,
        val profile: UserProfile?,
        val isAdultRestricted: Boolean,
        val sortOrder: ChannelSortOrder,
    )

    // Debounce search query to eliminate redundant 12,000-channel filter sequence runs during fast keystrokes
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else 150L }

    private val filterParams = combine(
        debouncedSearchQuery,
        _selectedGenre,
        _selectedSubcategory,
        _selectedCountryFilter,
        _selectedLanguage,
        profileRepo.activeProfile,
        _isAdultRestrictionEnabled,
        _channelSortOrder,
    ) { args: Array<Any?> ->
        FilterParams(
            query = args[0] as String,
            genre = args[1] as String,
            subcategory = args[2] as String,
            country = args[3] as String,
            language = args[4] as String,
            profile = args[5] as? UserProfile,
            isAdultRestricted = args[6] as Boolean,
            sortOrder = args[7] as ChannelSortOrder,
        )
    }

    // Combined filtered channel stream
    val filteredChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        filterParams,
        profileRepo.globallyLockedChannelIds,
        brokenChannelIds,
    ) { channels, params, lockedIds, brokenIds ->
        val isKids = params.profile?.isKidsProfile == true
        val favs = params.profile?.favorites ?: emptyList()

        val mappedChannels = channels.asSequence().filter { channel ->
            val id = channel.id
            val name = channel.name
            val genre = channel.genre
            val country = channel.country
            val language = channel.language
            val resolution = channel.resolution
            val isAdultOrRestricted = channel.isAdultOrRestricted
            val description = channel.description

            if ((isKids || params.isAdultRestricted) && isAdultOrRestricted) {
                return@filter false
            }

            val isOffline = brokenIds.contains(id) || channel.isOfflineMetadata()

            val genreMatches = when (params.genre) {
                ChannelGenre.ALL.displayName -> true
                ChannelGenre.FAVORITES.displayName -> favs.contains(id)
                else -> {
                    val pGenre = params.genre.lowercase(Locale.ROOT)
                    val cGenre = genre.lowercase(Locale.ROOT)
                    (cGenre == pGenre) || cGenre.contains(pGenre) || pGenre.contains(cGenre) ||
                    (when (params.genre) {
                        ChannelGenre.NEWS.displayName -> cGenre.contains("news") || cGenre.contains("weather") || name.contains("news", ignoreCase = true)
                        ChannelGenre.SPORTS.displayName -> cGenre.contains("sport") || name.contains("sport", ignoreCase = true)
                        ChannelGenre.MOVIES.displayName -> cGenre.contains("movie") || cGenre.contains("cinema") || cGenre.contains("film") || name.contains("movie", ignoreCase = true) || name.contains("cinema", ignoreCase = true)
                        ChannelGenre.SERIES.displayName -> cGenre.contains("series") || cGenre.contains("drama") || cGenre.contains("novela")
                        ChannelGenre.ENTERTAINMENT.displayName -> cGenre.contains("entertainment") || cGenre.contains("variety")
                        ChannelGenre.MUSIC.displayName -> cGenre.contains("music") || cGenre.contains("radio") || cGenre.contains("song")
                        ChannelGenre.KIDS.displayName -> cGenre.contains("kid") || cGenre.contains("anim") || cGenre.contains("cartoon") || cGenre.contains("toon")
                        ChannelGenre.DOCUMENTARY.displayName -> cGenre.contains("doc") || cGenre.contains("history") || cGenre.contains("nature")
                        ChannelGenre.LIFESTYLE.displayName -> cGenre.contains("lifestyle") || cGenre.contains("fashion")
                        ChannelGenre.FOOD.displayName -> cGenre.contains("food") || cGenre.contains("cook") || cGenre.contains("recipe") || cGenre.contains("culinary")
                        ChannelGenre.TRAVEL.displayName -> cGenre.contains("travel") || cGenre.contains("outdoor") || cGenre.contains("adventure")
                        ChannelGenre.COMEDY.displayName -> cGenre.contains("comedy") || cGenre.contains("humor")
                        ChannelGenre.SCIENCE.displayName -> cGenre.contains("sci") || cGenre.contains("tech") || cGenre.contains("space")
                        ChannelGenre.EDUCATION.displayName -> cGenre.contains("educat") || cGenre.contains("learn")
                        ChannelGenre.BUSINESS.displayName -> cGenre.contains("business") || cGenre.contains("financ") || cGenre.contains("market")
                        ChannelGenre.GAMING.displayName -> cGenre.contains("gaming") || cGenre.contains("game") || cGenre.contains("esport")
                        ChannelGenre.RELIGIOUS.displayName -> cGenre.contains("relig") || cGenre.contains("church") || cGenre.contains("islam") || cGenre.contains("hindu")
                        ChannelGenre.REGIONAL.displayName -> cGenre.contains("regional") || cGenre.contains("local")
                        ChannelGenre.CLASSIC.displayName -> cGenre.contains("classic") || cGenre.contains("retro") || cGenre.contains("vintage")
                        else -> false
                    })
                }
            }

            val countryMatches = (params.country == "ALL") || country.equals(params.country, ignoreCase = true) || when (params.country) {
                    "US" -> country.contains("United States", ignoreCase = true) || country.contains("USA", ignoreCase = true) || id.endsWith(".us", ignoreCase = true)
                    "UK" -> country.equals("GB", ignoreCase = true) || country.contains("United Kingdom", ignoreCase = true) || country.contains("Britain", ignoreCase = true) || id.endsWith(".uk", ignoreCase = true)
                    "IN" -> {
                        country.contains("India", ignoreCase = true) ||
                        name.contains("India", ignoreCase = true) ||
                        id.endsWith(".in", ignoreCase = true) ||
                        id.contains("dd_", ignoreCase = true) ||
                        name.startsWith("DD ", ignoreCase = true) ||
                        name.contains("Aaj Tak", ignoreCase = true) ||
                        name.contains("NDTV", ignoreCase = true) ||
                        name.contains("News18", ignoreCase = true) ||
                        name.contains("ABP", ignoreCase = true) ||
                        name.contains("Republic", ignoreCase = true) ||
                        name.contains("Times Now", ignoreCase = true) ||
                        name.contains("WION", ignoreCase = true) ||
                        name.contains("Sansad", ignoreCase = true) ||
                        name.contains("Doordarshan", ignoreCase = true) ||
                        language.contains("Hindi", ignoreCase = true) ||
                        language.contains("Tamil", ignoreCase = true) ||
                        language.contains("Telugu", ignoreCase = true) ||
                        language.contains("Punjabi", ignoreCase = true) ||
                        language.contains("Malayalam", ignoreCase = true) ||
                        language.contains("Bengali", ignoreCase = true) ||
                        language.contains("Marathi", ignoreCase = true) ||
                        language.contains("Gujarati", ignoreCase = true) ||
                        language.contains("Kannada", ignoreCase = true)
                    }
                    "CA" -> country.contains("Canada", ignoreCase = true) || id.endsWith(".ca", ignoreCase = true)
                    "AU" -> country.contains("Australia", ignoreCase = true) || id.endsWith(".au", ignoreCase = true)
                    "DE" -> country.contains("Germany", ignoreCase = true) || id.endsWith(".de", ignoreCase = true) || language.contains("German", ignoreCase = true)
                    "FR" -> country.contains("France", ignoreCase = true) || id.endsWith(".fr", ignoreCase = true) || language.contains("French", ignoreCase = true)
                    "ES" -> country.contains("Spain", ignoreCase = true) || id.endsWith(".es", ignoreCase = true) || language.contains("Spanish", ignoreCase = true)
                    "IT" -> country.contains("Italy", ignoreCase = true) || id.endsWith(".it", ignoreCase = true) || language.contains("Italian", ignoreCase = true)
                    "BR" -> country.contains("Brazil", ignoreCase = true) || id.endsWith(".br", ignoreCase = true) || language.contains("Portuguese", ignoreCase = true)
                    "MX" -> country.contains("Mexico", ignoreCase = true) || id.endsWith(".mx", ignoreCase = true)
                    "AR" -> country.contains("Argentina", ignoreCase = true) || id.endsWith(".ar", ignoreCase = true)
                    "CO" -> country.contains("Colombia", ignoreCase = true) || id.endsWith(".co", ignoreCase = true)
                    "CL" -> country.contains("Chile", ignoreCase = true) || id.endsWith(".cl", ignoreCase = true)
                    "PE" -> country.contains("Peru", ignoreCase = true) || id.endsWith(".pe", ignoreCase = true)
                    "JP" -> country.contains("Japan", ignoreCase = true) || id.endsWith(".jp", ignoreCase = true) || language.contains("Japanese", ignoreCase = true)
                    "KR" -> country.contains("Korea", ignoreCase = true) || id.endsWith(".kr", ignoreCase = true) || language.contains("Korean", ignoreCase = true)
                    "CN" -> country.contains("China", ignoreCase = true) || id.endsWith(".cn", ignoreCase = true) || name.contains("CCTV", ignoreCase = true) || name.contains("CGTN", ignoreCase = true) || language.contains("Chinese", ignoreCase = true)
                    "TR" -> country.contains("Turkey", ignoreCase = true) || country.contains("Turkiye", ignoreCase = true) || id.contains("trt", ignoreCase = true) || language.contains("Turkish", ignoreCase = true)
                    "RU" -> country.contains("Russia", ignoreCase = true) || id.endsWith(".ru", ignoreCase = true) || language.contains("Russian", ignoreCase = true)
                    "UA" -> country.contains("Ukraine", ignoreCase = true) || id.endsWith(".ua", ignoreCase = true) || language.contains("Ukrainian", ignoreCase = true)
                    "SA" -> country.contains("Saudi", ignoreCase = true) || id.endsWith(".sa", ignoreCase = true)
                    "AE" -> country.contains("UAE", ignoreCase = true) || country.contains("Arab", ignoreCase = true) || country.contains("Dubai", ignoreCase = true) || id.endsWith(".ae", ignoreCase = true) || language.contains("Arabic", ignoreCase = true)
                    "QA" -> country.contains("Qatar", ignoreCase = true) || id.contains("aljazeera", ignoreCase = true) || id.endsWith(".qa", ignoreCase = true)
                    "EG" -> country.contains("Egypt", ignoreCase = true) || id.endsWith(".eg", ignoreCase = true)
                    "PK" -> country.contains("Pakistan", ignoreCase = true) || name.contains("Pakistan", ignoreCase = true) || id.endsWith(".pk", ignoreCase = true) || language.contains("Urdu", ignoreCase = true)
                    "BD" -> country.contains("Bangladesh", ignoreCase = true) || name.contains("Bangladesh", ignoreCase = true) || id.endsWith(".bd", ignoreCase = true) || language.contains("Bengali", ignoreCase = true)
                    "ID" -> country.contains("Indonesia", ignoreCase = true) || id.endsWith(".id", ignoreCase = true) || language.contains("Indonesian", ignoreCase = true)
                    "PH" -> country.contains("Philippines", ignoreCase = true) || id.endsWith(".ph", ignoreCase = true) || language.contains("Tagalog", ignoreCase = true)
                    "NG" -> country.contains("Nigeria", ignoreCase = true) || id.endsWith(".ng", ignoreCase = true)
                    "ZA" -> country.contains("South Africa", ignoreCase = true) || id.endsWith(".za", ignoreCase = true)
                    "NL" -> country.contains("Netherlands", ignoreCase = true) || id.endsWith(".nl", ignoreCase = true) || language.contains("Dutch", ignoreCase = true)
                    "CH" -> country.contains("Switzerland", ignoreCase = true) || id.endsWith(".ch", ignoreCase = true)
                    "AT" -> country.contains("Austria", ignoreCase = true) || id.contains("redbull", ignoreCase = true) || id.endsWith(".at", ignoreCase = true)
                    "BE" -> country.contains("Belgium", ignoreCase = true) || id.endsWith(".be", ignoreCase = true)
                    "SE" -> country.contains("Sweden", ignoreCase = true) || id.endsWith(".se", ignoreCase = true) || language.contains("Swedish", ignoreCase = true)
                    "NO" -> country.contains("Norway", ignoreCase = true) || id.endsWith(".no", ignoreCase = true) || language.contains("Norwegian", ignoreCase = true)
                    "DK" -> country.contains("Denmark", ignoreCase = true) || id.endsWith(".dk", ignoreCase = true) || language.contains("Danish", ignoreCase = true)
                    "FI" -> country.contains("Finland", ignoreCase = true) || id.endsWith(".fi", ignoreCase = true) || language.contains("Finnish", ignoreCase = true)
                    "PL" -> country.contains("Poland", ignoreCase = true) || id.endsWith(".pl", ignoreCase = true) || language.contains("Polish", ignoreCase = true)
                    "GR" -> country.contains("Greece", ignoreCase = true) || id.endsWith(".gr", ignoreCase = true) || language.contains("Greek", ignoreCase = true)
                    "PT" -> country.contains("Portugal", ignoreCase = true) || id.endsWith(".pt", ignoreCase = true) || language.contains("Portuguese", ignoreCase = true)
                    "IE" -> country.contains("Ireland", ignoreCase = true) || id.endsWith(".ie", ignoreCase = true)
                    "NZ" -> country.contains("New Zealand", ignoreCase = true) || id.endsWith(".nz", ignoreCase = true)
                    "SG" -> country.contains("Singapore", ignoreCase = true) || id.endsWith(".sg", ignoreCase = true)
                    "MY" -> country.contains("Malaysia", ignoreCase = true) || id.endsWith(".my", ignoreCase = true) || language.contains("Malay", ignoreCase = true)
                    "TH" -> country.contains("Thailand", ignoreCase = true) || id.endsWith(".th", ignoreCase = true) || language.contains("Thai", ignoreCase = true)
                    "VN" -> country.contains("Vietnam", ignoreCase = true) || id.endsWith(".vn", ignoreCase = true) || language.contains("Vietnamese", ignoreCase = true)
                    "IL" -> country.contains("Israel", ignoreCase = true) || id.endsWith(".il", ignoreCase = true) || language.contains("Hebrew", ignoreCase = true)
                    "MA" -> country.contains("Morocco", ignoreCase = true) || id.endsWith(".ma", ignoreCase = true)
                    "DZ" -> country.contains("Algeria", ignoreCase = true) || id.endsWith(".dz", ignoreCase = true)
                    "GE" -> country.contains("Georgia", ignoreCase = true) || id.contains("1tv", ignoreCase = true) || id.contains("2tv", ignoreCase = true) || id.endsWith(".ge", ignoreCase = true)
                    "GLOBAL" -> country.contains("Global", ignoreCase = true) || country.contains("International", ignoreCase = true)
                    else -> country.contains(params.country, ignoreCase = true)
                }

            val languageMatches = when (params.language) {
                "ALL" -> true
                else -> language.equals(params.language, ignoreCase = true) ||
                        language.contains(params.language, ignoreCase = true) ||
                        name.contains(params.language, ignoreCase = true)
            }

            val subcategoryMatches = when (val sub = params.subcategory) {
                "ALL", "Popular" -> true
                "Live Now" -> !isOffline
                "Offline" -> isOffline
                "HD Broadcast" -> resolution.contains("HD", ignoreCase = true) || resolution.contains("1080", ignoreCase = true) || resolution.contains("720", ignoreCase = true) || name.contains("HD", ignoreCase = true)
                "World 24/7" -> genre.contains("News", ignoreCase = true)
                "Financial & Markets" -> name.contains("Bloomberg", ignoreCase = true) || name.contains("CNBC", ignoreCase = true) || name.contains("Business", ignoreCase = true) || name.contains("Finance", ignoreCase = true) || name.contains("Market", ignoreCase = true) || description.contains("financ", ignoreCase = true) || description.contains("business", ignoreCase = true)
                "Public Broadcast" -> name.contains("DD ", ignoreCase = true) || name.contains("DW", ignoreCase = true) || name.contains("France 24", ignoreCase = true) || name.contains("TRT", ignoreCase = true) || name.contains("CGTN", ignoreCase = true) || name.contains("PBS", ignoreCase = true) || name.contains("1TV", ignoreCase = true) || name.contains("2TV", ignoreCase = true) || description.contains("public", ignoreCase = true) || genre.contains("News", ignoreCase = true)
                "Extreme Action" -> name.contains("Red Bull", ignoreCase = true) || name.contains("Action", ignoreCase = true) || name.contains("Extreme", ignoreCase = true) || genre.contains("Sports", ignoreCase = true)
                "Motorsport" -> name.contains("Motor", ignoreCase = true) || name.contains("Racing", ignoreCase = true) || name.contains("Red Bull", ignoreCase = true) || name.contains("Auto", ignoreCase = true) || genre.contains("Sports", ignoreCase = true)
                "Scores & Odds" -> name.contains("Grid", ignoreCase = true) || name.contains("Score", ignoreCase = true) || name.contains("Sport", ignoreCase = true) || genre.contains("Sports", ignoreCase = true)
                "2000s Hits", "Blockbusters", "World Cinema" -> genre.contains("Movies", ignoreCase = true) || name.contains("Movie", ignoreCase = true) || name.contains("Cinema", ignoreCase = true) || name.contains("Film", ignoreCase = true)
                "Animation", "Discovery", "Stories", "Preschool" -> genre.contains("Kids", ignoreCase = true) || name.contains("Kids", ignoreCase = true) || name.contains("Cartoon", ignoreCase = true) || name.contains("Anim", ignoreCase = true)
                "Space", "Nature & Living", "Health", "History", "Wildlife" -> genre.contains("Documentary", ignoreCase = true) || genre.contains("Science", ignoreCase = true) || name.contains("Doc", ignoreCase = true) || name.contains("NASA", ignoreCase = true)
                "Tech & Gadgets", "Home & Shopping", "Culture", "Fashion" -> genre.contains("Lifestyle", ignoreCase = true) || name.contains("TV", ignoreCase = true)
                "Recipes", "Baking", "Street Food", "Restaurants" -> genre.contains("Food", ignoreCase = true) || name.contains("Food", ignoreCase = true) || name.contains("Cook", ignoreCase = true)
                "Outdoors", "Adventures", "Road Trips", "Destinations" -> genre.contains("Travel", ignoreCase = true) || name.contains("Travel", ignoreCase = true)
                "Sketches", "Animation Satire", "Parody", "Stand-Up" -> genre.contains("Comedy", ignoreCase = true) || name.contains("Comedy", ignoreCase = true)
                "Astronomy & ISS", "Rocket Launches", "Robotics", "AI & Cyber" -> genre.contains("Science", ignoreCase = true) || name.contains("NASA", ignoreCase = true) || name.contains("Space", ignoreCase = true)
                "Drama", "Crime", "Telenovelas", "Sci-Fi" -> genre.contains("Series", ignoreCase = true) || name.contains("Drama", ignoreCase = true)
                "Reality TV", "Talk Shows", "Game Shows", "Celebrity" -> genre.contains("Entertainment", ignoreCase = true)
                "Pop & Rock", "Hip-Hop", "Electronic", "Classical", "Jazz" -> genre.contains("Music", ignoreCase = true)
                "Esports", "Gameplays", "Tournaments", "Retro Gaming" -> genre.contains("Gaming", ignoreCase = true)
                "Stock Markets", "Crypto", "Economy", "Interviews" -> genre.contains("Business", ignoreCase = true)
                "Christian", "Islamic", "Hindu", "Buddhist", "Spirituality" -> genre.contains("Religious", ignoreCase = true)
                "Local News", "Community", "State Broadcast" -> genre.contains("Regional", ignoreCase = true)
                "Vintage Cinema", "Retro TV", "Golden Age" -> genre.contains("Classic", ignoreCase = true)
                else -> name.contains(sub, ignoreCase = true) || genre.contains(sub, ignoreCase = true) || description.contains(sub, ignoreCase = true)
            }

            val queryMatches = if (params.query.isBlank()) {
                true
            } else {
                val qTokens = params.query.trim().lowercase(Locale.ROOT).split("\\s+".toRegex()).filter { it.isNotBlank() }
                val targetText = "$name $genre $country $language $description $id".lowercase(Locale.ROOT)
                qTokens.all { targetText.contains(it) }
            }

            // When searching by query, prioritize showing matching channels while respecting subcategory filters
            if (params.query.isNotBlank()) {
                queryMatches && subcategoryMatches
            } else {
                genreMatches && subcategoryMatches && countryMatches && languageMatches
            }
        }.map { c ->
            c.copy(
                isFavorite = favs.contains(c.id),
                isLocked = (lockedIds.contains(c.id)) || c.isAdultOrRestricted,
            )
        }.toList()

        if (params.sortOrder == ChannelSortOrder.A_TO_Z) {
            mappedChannels.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { (_, name) -> name.trim() })
        } else {
            mappedChannels.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { (_, name) -> name.trim() })
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, playlistRepo.channels.value)

    // Dedicated stream for all user-bookmarked favorite channels
    val favoriteChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile,
    ) { channels, profile ->
        val favIds = profile?.favorites ?: emptyList()
        channels.asSequence()
            .filter { (id) -> favIds.contains(id) }
            .map { it.copy(isFavorite = true) }
            .toList()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Profile recommendations based on top watch genres
    val recommendedChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile,
        profileRepo.globallyLockedChannelIds,
    ) { channels, profile, lockedIds ->
        val history = profile?.watchHistory ?: emptyList()
        val favs = profile?.favorites ?: emptyList()
        val isKids = profile?.isKidsProfile == true

        val eligible = if (isKids) {
            channels.filterNot(Channel::isAdultOrRestricted)
        } else {
            channels
        }

        if (history.isEmpty()) {
            eligible.asSequence().take(6).map { c ->
                c.copy(isFavorite = favs.contains(c.id), isLocked = lockedIds.contains(c.id))
            }.toList()
        } else {
            val topChannelNames = history.asSequence().map(com.example.data.model.WatchHistoryEntry::channelName).toSet()
            val matching = eligible.filter { (_, name) -> name in topChannelNames }
            val preferredGenres = matching.asSequence().map(Channel::genre).groupingBy { it }.eachCount()
            val bestGenre = preferredGenres.maxByOrNull { (_, count) -> count }?.key ?: "News"

            eligible.asSequence().filter { (_, _, _, _, genre) -> genre.equals(bestGenre, ignoreCase = true) }
                .take(6)
                .map { c ->
                    c.copy(isFavorite = favs.contains(c.id), isLocked = lockedIds.contains(c.id))
                }
                .toList()
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tracks the last 5 channels the user opened, enabling quick resume functionality
    val recentlyWatchedChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile,
        profileRepo.globallyLockedChannelIds,
    ) { channels, profile, lockedIds ->
        val history = profile?.watchHistory ?: emptyList()
        val favs = profile?.favorites ?: emptyList()
        val channelMap = channels.associateBy(Channel::id)

        // Find channels matching recent watch history
        val watched = history.asSequence().mapNotNull { (_, channelId) ->
            channelMap[channelId]
        }.distinctBy(Channel::id).take(5).toList()

        // If fewer than 5 watched yet, supplement with top verified channels so quick resume is always populated
        val finalChannels = if (watched.size < 5) {
            val remainingNeeded = 5 - watched.size
            val alreadyIds = watched.asSequence().map(Channel::id).toSet()
            val supplements = channels.asSequence()
                .filter { (id) -> id !in alreadyIds }
                .take(remainingNeeded)
                .toList()
            watched + supplements
        } else {
            watched
        }

        finalChannels.map { c ->
            c.copy(
                isFavorite = favs.contains(c.id),
                isLocked = lockedIds.contains(c.id) || c.isAdultOrRestricted,
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allChannels: StateFlow<List<Channel>> = playlistRepo.channels

    val isChannelsLoaded: StateFlow<Boolean> = playlistRepo.channels
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, playlistRepo.channels.value.isNotEmpty())

    // Indicates the full 12k+ playlist has been loaded and all channels are ready in memory
    @Suppress("unused")
    val isAllChannelsReady: StateFlow<Boolean> = combine(
        playlistRepo.isFullPlaylistLoaded,
        playlistRepo.channels,
    ) { fullLoaded, ch -> fullLoaded && ch.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    private val _isAppLaunchCountdownFinished = MutableStateFlow(value = false)
    val isAppLaunchCountdownFinished: StateFlow<Boolean> = _isAppLaunchCountdownFinished.asStateFlow()

    fun completeAppLaunch() {
        _isAppLaunchCountdownFinished.value = true
    }

    private fun saveLastPlayedChannel(channelId: String) {
        settingsPrefs.edit { putString("last_played_channel_id", channelId) }
    }

    init {
        // Keep startup work off the UI thread. Playback starts only after the user selects a channel.
        viewModelScope.launch {
            runWithLocalNetworkAccess(playlistRepo.playlistUrl.value) {
                playlistRepo.checkAndTriggerAutoUpdate(force = false)
            }
            checkForAppUpdate(force = false)
            refreshStorageMetrics()
        }

        viewModelScope.launch(Dispatchers.Default) {
            var lastProcessedChannelCount = 0
            playlistRepo.channels.collect { channels ->
                if ((channels.isNotEmpty()) && (channels.size != lastProcessedChannelCount)) {
                    lastProcessedChannelCount = channels.size
                    val offlineMetaIds = channels.asSequence()
                        .filter { it.isOfflineMetadata() }
                        .map(Channel::id)
                        .toSet()
                    val liveMetaIds = channels.asSequence()
                        .filterNot { it.isOfflineMetadata() }
                        .map(Channel::id)
                        .toSet()

                    if (offlineMetaIds.isNotEmpty()) {
                        val current = _brokenChannelIds.value
                        val combined = current + offlineMetaIds
                        if (combined != current) {
                            _brokenChannelIds.value = combined
                        }
                    }
                    _liveChannelIds.value = liveMetaIds - _brokenChannelIds.value
                    historyAndOfflineRepo.saveOfflineChannels(channels.take(35))
                    refreshStorageMetrics()
                }
            }
        }
    }

    fun playNextChannel() {
        val list = filteredChannels.value.ifEmpty { playlistRepo.channels.value }
        if (list.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = list.indexOfFirst { (id) -> id == current?.id }
        val nextIndex = (currentIndex + 1).takeIf { it in list.indices } ?: 0
        playChannel(list[nextIndex])
    }

    fun playPreviousChannel() {
        val list = filteredChannels.value.ifEmpty { playlistRepo.channels.value }
        if (list.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = list.indexOfFirst { (id) -> id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playChannel(list[prevIndex])
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
        com.example.util.AnalyticsManager.logScreenView(tab.name)
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        if (q.length >= 3) {
            com.example.util.AnalyticsManager.logSearch(q)
        }
    }

    fun selectGenre(genreName: String) {
        _selectedGenre.value = genreName
        _selectedSubcategory.value = "ALL"
    }

    fun selectCountry(country: String) {
        _selectedCountryFilter.value = country
    }

    fun playChannel(channel: Channel, directLandscape: Boolean = true) {
        _isFullScreenSearchOpen.value = false
        if (channel.isLocked) {
            _pendingPinChannel.value = channel
            _pinActionType.value = PinActionType.PLAY_CHANNEL
            _pinDialogReason.value = "Enter 4-digit PIN to watch ${channel.name}"
            _isPinDialogVisible.value = true
            return
        }

        playChannelNow(channel, directLandscape)
    }

    private fun playChannelNow(channel: Channel, directLandscape: Boolean) {
        _isFullScreenSearchOpen.value = false
        _playingOfflineMedia.value = null
        _currentChannel.value = channel
        _isPlaying.value = true
        _isMiniPlayerDismissed.value = false
        if (directLandscape) {
            _isFullscreen.value = true
        }

        saveLastPlayedChannel(channel.id)
        com.example.util.AnalyticsManager.logChannelPlay(channel.id, channel.name, channel.genre, channel.country)
        profileRepo.recordWatchHistory(channel.id, channel.name)
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = profileRepo.activeProfile.value?.id ?: "default"
            historyAndOfflineRepo.recordViewing(channel, activeId)
        }
    }

    fun onLocalNetworkPermissionResult(granted: Boolean) {
        _localNetworkPermissionRequested.value = false
        val actions = pendingLocalNetworkActions.toList()
        pendingLocalNetworkActions.clear()
        if (granted) actions.forEach { action -> action() }
    }

    private fun runWithLocalNetworkAccess(url: String, action: () -> Unit) {
        val requiresPermission = when {
            Build.VERSION.SDK_INT < 37 -> false
            !requiresLocalNetworkAccess(url) -> false
            else -> ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (!requiresPermission) {
            action()
            return
        }

        pendingLocalNetworkActions += action
        _localNetworkPermissionRequested.value = true
    }

    private fun requiresLocalNetworkAccess(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme.equals("local", ignoreCase = true) || uri.scheme.equals("file", ignoreCase = true)) {
            return false
        }

        val host = uri.host?.removeSurrounding("[", "]")?.lowercase() ?: return false
        val isLocalHost = when {
            host == "localhost" -> true
            host.endsWith(".local") -> true
            host.endsWith(".lan") -> true
            host.endsWith(".home") -> true
            host.endsWith(".home.arpa") -> true
            host.endsWith(".localdomain") -> true
            else -> !host.contains('.')
        }
        if (isLocalHost) return true

        val isIpLiteral = host.contains(':') || host.all { character -> (character.isDigit()) || (character == '.') }
        if (!isIpLiteral) return false

        val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) {
            return true
        }

        val bytes = address.address
        if (bytes.size == 4) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            return (first == 100) && (second >= 64) && (second <= 127)
        }
        if (bytes.size == 16) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            return ((first and 0xFE) == 0xFC) || ((first == 0xFE) && ((second and 0xC0) == 0x80))
        }
        return false
    }

    fun playChannelById(channelId: String, directLandscape: Boolean = true) {
        _isFullScreenSearchOpen.value = false
        val channel = playlistRepo.channels.value.firstOrNull { (id) -> id == channelId }
            ?: filteredChannels.value.firstOrNull { (id) -> id == channelId }
        if (channel != null) {
            playChannel(channel, directLandscape)
            return
        }

        // Deep links and reminders can arrive before the bundled playlist finishes loading.
        viewModelScope.launch {
            val loadedChannel = withTimeoutOrNull(30.seconds) {
                playlistRepo.channels
                    .map { channels -> channels.firstOrNull { (id) -> id == channelId } }
                    .flowOn(Dispatchers.Default)
                    .first { it != null }
            }
            loadedChannel?.let { playChannel(it, directLandscape) }
        }
    }

    fun refreshStorageMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            val (offlineCount, historyCount) = historyAndOfflineRepo.getDatabaseStorageSummary()
            val totalSize = (offlineCount * 1.5) + (historyCount * 0.05)
            _storageMetrics.value = StorageMetrics(
                offlineChannelsCount = offlineCount,
                historyEntriesCount = historyCount,
                totalCacheSizeMb = String.format(Locale.US, "%.1f", totalSize).toDoubleOrNull() ?: 14.2,
            )
        }
    }

    fun clearRoomChannelCache() {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.clearOfflineChannels()
            refreshStorageMetrics()
            autoClearRam()
        }
    }

    @Suppress("unused")
    fun clearAllStorageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.clearAllLocalDatabaseCache()
            refreshStorageMetrics()
            autoClearRam()
        }
    }

    fun autoClearRam() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                coil3.SingletonImageLoader.get(app).memoryCache?.clear()
                android.util.Log.d("TVViewModel", "Image memory cache cleared")
            } catch (e: Exception) {
                android.util.Log.e("TVViewModel", "RAM clean failed", e)
            }
        }
    }

    @Suppress("unused")
    fun saveChannelForOffline(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.saveOfflineChannel(channel)
        }
    }

    @Suppress("unused")
    fun deleteOfflineChannel(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.deleteOfflineChannel(channelId)
        }
    }

    @Suppress("unused")
    fun playOfflineMedia(media: OfflineMedia) {
        runWithLocalNetworkAccess(media.streamUrl) {
            _currentChannel.value = null
            _playingOfflineMedia.value = media
            _isPlaying.value = true
            _isFullscreen.value = true
            _isMiniPlayerDismissed.value = false
            profileRepo.logActivity("Offline Viewing: ${media.title}")
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun toggleFullscreen() {
        val enterFullscreen = !_isFullscreen.value
        _isFullscreen.value = enterFullscreen
    }

    fun setFullscreen(enabled: Boolean) {
        _isFullscreen.value = enabled
    }

    @Suppress("unused")
    val presetPlaylists = PlaylistRepository.PRESET_PLAYLISTS

    @Suppress("unused")
    fun selectPresetPlaylist(preset: com.example.data.repository.PlaylistPreset) {
        updatePlaylistUrl(preset.url)
    }

    // Picture-in-Picture
    fun requestPip() {
        _enterPipRequest.tryEmit(Unit)
    }

    fun setPipMode(enabled: Boolean) {
        _isPipMode.value = enabled
    }

    fun dismissMiniPlayer() {
        _isMiniPlayerDismissed.value = true
        _isPlaying.value = false
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatio.value = mode
    }

    fun cycleAspectRatio() {
        _aspectRatio.value = when (_aspectRatio.value) {
            AspectRatioMode.AUTO -> AspectRatioMode.FIT
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FOUR_THREE
            AspectRatioMode.FOUR_THREE -> AspectRatioMode.AUTO
        }
    }

    fun setPlaybackQuality(quality: PlaybackQuality) {
        _playbackQuality.value = quality
        viewModelScope.launch(Dispatchers.IO) {
            profileRepo.logActivity("Switched quality mode to ${quality.label}")
        }
    }

    fun cycleQuality() {
        _playbackQuality.value = when (_playbackQuality.value) {
            PlaybackQuality.AUTO -> PlaybackQuality.HIGH
            PlaybackQuality.HIGH -> PlaybackQuality.DATA_SAVER
            PlaybackQuality.DATA_SAVER -> PlaybackQuality.AUDIO_ONLY
            PlaybackQuality.AUDIO_ONLY -> PlaybackQuality.AUTO
        }
    }

    fun toggleSubtitles() {
        _subtitlesEnabled.value = !_subtitlesEnabled.value
    }

    fun setSubtitleLanguage(lang: String) {
        _selectedSubtitleLanguage.value = lang
    }

    fun setAudioTrack(track: String) {
        _selectedAudioTrack.value = track
    }

    fun toggleFavorite(channelId: String) {
        profileRepo.toggleFavorite(channelId)
        com.example.util.AnalyticsManager.logFavoriteToggle(channelId)
    }

    // Parental PIN handling
    fun requestParentalAccess(action: PinActionType, channel: Channel? = null) {
        _pinActionType.value = action
        _pendingPinChannel.value = channel
        _pinDialogReason.value = when (action) {
            PinActionType.OPEN_PARENTAL_SETTINGS -> "Enter Master PIN to access Parental Controls"
            PinActionType.PLAY_CHANNEL -> "Enter PIN to unlock restricted channel"
            PinActionType.TOGGLE_CHANNEL_LOCK -> "Enter PIN to modify channel restrictions"
        }
        _isPinDialogVisible.value = true
    }

    fun dismissPinDialog() {
        _isPinDialogVisible.value = false
        _pendingPinChannel.value = null
    }

    fun verifyPinAndExecute(pin: String): Boolean {
        val success = profileRepo.verifyPin(pin)
        if (success) {
            _isPinDialogVisible.value = false
            when (_pinActionType.value) {
                PinActionType.PLAY_CHANNEL -> {
                    val target = _pendingPinChannel.value
                    if (target != null) {
                        _playingOfflineMedia.value = null
                        _currentChannel.value = target
                        _isPlaying.value = true
                        _isMiniPlayerDismissed.value = false
                        _isFullscreen.value = true
                        profileRepo.recordWatchHistory(target.id, target.name)
                    }
                }
                PinActionType.OPEN_PARENTAL_SETTINGS -> {
                    _currentTab.value = ScreenTab.PARENTAL
                }
                PinActionType.TOGGLE_CHANNEL_LOCK -> {
                    _pendingPinChannel.value?.let { (id) ->
                        profileRepo.toggleChannelLock(id)
                    }
                }
            }
            _pendingPinChannel.value = null
        }
        return success
    }

    // Cast methods
    fun openCastDialog() {
        _isCastDialogVisible.value = true
    }

    fun closeCastDialog() {
        _isCastDialogVisible.value = false
    }

    @Suppress("unused")
    fun toggleCastActive() {
        _isCastingActive.value = !_isCastingActive.value
        profileRepo.logActivity(if (_isCastingActive.value) "Started Screen Cast" else "Ended Screen Cast")
    }

    // Playlist sync
    fun triggerManualSync() {
        runWithLocalNetworkAccess(playlistRepo.playlistUrl.value) {
            viewModelScope.launch {
                playlistRepo.fetchPlaylist()
            }
        }
    }

    fun updatePlaylistUrl(url: String) {
        runWithLocalNetworkAccess(url) {
            playlistRepo.setPlaylistUrl(url)
            viewModelScope.launch {
                playlistRepo.fetchPlaylist(url)
            }
        }
    }
}
