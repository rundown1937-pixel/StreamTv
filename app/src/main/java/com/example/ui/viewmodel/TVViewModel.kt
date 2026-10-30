package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProgramEntity
import com.example.data.model.ActivityLog
import com.example.data.model.Channel
import com.example.data.model.ChannelGenre
import com.example.data.model.OfflineMedia
import com.example.data.model.UserProfile
import com.example.data.repository.EpgRepository
import com.example.data.repository.OfflineDownloadRepository
import com.example.data.repository.PlaylistRepository
import com.example.data.repository.ProfileRepository
import com.example.data.local.OfflineChannelEntity
import com.example.data.local.ViewingHistoryEntity
import com.example.data.repository.HistoryAndOfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenTab {
    HOME,
    FAVORITES,
    CHANNELS,
    EPG_GUIDE,
    PROFILES,
    PARENTAL,
    SETTINGS
}

data class RegionItem(val code: String, val name: String, val flag: String)

data class StorageMetrics(
    val offlineChannelsCount: Int = 0,
    val historyEntriesCount: Int = 0,
    val epgProgramsCount: Int = 0,
    val totalCacheSizeMb: Double = 0.0
)

enum class AspectRatioMode(val label: String) {
    AUTO("Auto Device"),
    FIT("16:9 Fit"),
    FILL("Crop / Fill"),
    STRETCH("Stretch"),
    FOUR_THREE("4:3 Standard")
}

enum class PlaybackQuality(val label: String, val bitRateLimitDesc: String) {
    AUTO("Auto Adaptive", "Dynamic resolution"),
    HIGH("1080p FHD", "High bitrate"),
    DATA_SAVER("Low Bandwidth Eco", "Saves 70% mobile data"),
    AUDIO_ONLY("Audio Only Mode", "Saves 95% data for commuting")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TVViewModel(private val app: Application) : AndroidViewModel(app) {

    val playlistRepo = PlaylistRepository(app)
    val profileRepo = ProfileRepository(app)
    val offlineRepo = OfflineDownloadRepository(app)
    val epgRepo = EpgRepository(app)
    val historyAndOfflineRepo = HistoryAndOfflineRepository(app)
    val epgReminderManager = com.example.data.repository.EpgReminderManager(app)
    val activeReminders: StateFlow<List<com.example.data.model.ProgramReminder>> = epgReminderManager.reminders

    private val _appInterfaceLanguage = MutableStateFlow(
        try {
            val code = app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
                .getString("app_interface_lang", "en") ?: "en"
            com.example.util.AppLanguage.entries.find { it.code == code } ?: com.example.util.AppLanguage.ENGLISH
        } catch (e: Exception) {
            com.example.util.AppLanguage.ENGLISH
        }
    )
    val appInterfaceLanguage: StateFlow<com.example.util.AppLanguage> = _appInterfaceLanguage.asStateFlow()

    fun setAppInterfaceLanguage(lang: com.example.util.AppLanguage) {
        _appInterfaceLanguage.value = lang
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .edit().putString("app_interface_lang", lang.code).apply()
    }

    val availableRegions = listOf(
        RegionItem("ALL", "All Regions", "🌍"),
        RegionItem("IN", "India", "🇮🇳"),
        RegionItem("US", "United States", "🇺🇸"),
        RegionItem("UK", "United Kingdom", "🇬🇧"),
        RegionItem("CA", "Canada", "🇨🇦"),
        RegionItem("AU", "Australia", "🇦🇺"),
        RegionItem("DE", "Germany", "🇩🇪"),
        RegionItem("FR", "France", "🇫🇷"),
        RegionItem("ES", "Spain", "🇪🇸"),
        RegionItem("IT", "Italy", "🇮🇹"),
        RegionItem("PK", "Pakistan", "🇵🇰"),
        RegionItem("BD", "Bangladesh", "🇧🇩"),
        RegionItem("QA", "Qatar", "🇶🇦"),
        RegionItem("AE", "UAE & Arab", "🇦🇪"),
        RegionItem("SA", "Saudi Arabia", "🇸🇦"),
        RegionItem("TR", "Turkey", "🇹🇷"),
        RegionItem("JP", "Japan", "🇯🇵"),
        RegionItem("KR", "South Korea", "🇰🇷"),
        RegionItem("CN", "China", "🇨🇳"),
        RegionItem("BR", "Brazil", "🇧🇷"),
        RegionItem("MX", "Mexico", "🇲🇽"),
        RegionItem("RU", "Russia", "🇷🇺"),
        RegionItem("UA", "Ukraine", "🇺🇦"),
        RegionItem("ID", "Indonesia", "🇮🇩"),
        RegionItem("PH", "Philippines", "🇵🇭"),
        RegionItem("NL", "Netherlands", "🇳🇱"),
        RegionItem("CH", "Switzerland", "🇨🇭"),
        RegionItem("AT", "Austria", "🇦🇹"),
        RegionItem("EG", "Egypt", "🇪🇬"),
        RegionItem("ZA", "South Africa", "🇿🇦"),
        RegionItem("NG", "Nigeria", "🇳🇬"),
        RegionItem("GE", "Georgia", "🇬🇪"),
        RegionItem("GLOBAL", "Global", "🌐")
    )

    val availableLanguages = listOf(
        "ALL", "Hindi", "English", "Tamil", "Telugu", "Punjabi", "Bengali",
        "Marathi", "Gujarati", "Malayalam", "Kannada", "Urdu", "French",
        "Spanish", "German", "Arabic", "Turkish", "Ukrainian", "Russian",
        "Japanese", "Korean", "Portuguese", "Italian", "Chinese", "Indonesian", "Dutch"
    )

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

    fun getSubcategoriesForGenre(genre: String): List<String> {
        return when (genre) {
            ChannelGenre.NEWS.displayName -> listOf("ALL", "World 24/7", "Financial & Markets", "Public Broadcast")
            ChannelGenre.SPORTS.displayName -> listOf("ALL", "Extreme Action", "Motorsport", "Scores & Odds")
            ChannelGenre.MOVIES.displayName -> listOf("ALL", "2000s Hits", "Blockbusters", "World Cinema")
            ChannelGenre.KIDS.displayName -> listOf("ALL", "Animation", "Discovery", "Stories")
            ChannelGenre.DOCUMENTARY.displayName -> listOf("ALL", "Space", "Nature & Living", "Health")
            ChannelGenre.LIFESTYLE.displayName -> listOf("ALL", "Tech & Gadgets", "Home & Shopping", "Culture")
            ChannelGenre.COMEDY.displayName -> listOf("ALL", "Sketches", "Animation Satire", "Parody")
            ChannelGenre.SCIENCE.displayName -> listOf("ALL", "Astronomy & ISS", "Rocket Launches", "Robotics")
            else -> listOf("ALL", "Popular", "HD Broadcast", "Live Now")
        }
    }

    // Storage metrics for Room database cache
    private val _storageMetrics = MutableStateFlow(StorageMetrics())
    val storageMetrics: StateFlow<StorageMetrics> = _storageMetrics.asStateFlow()

    // Room-backed offline channels and user viewing history
    val roomOfflineChannels: StateFlow<List<OfflineChannelEntity>> = historyAndOfflineRepo.allOfflineChannels
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val roomViewingHistory: StateFlow<List<ViewingHistoryEntity>> = historyAndOfflineRepo.getRecentHistory(15)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Navigation state
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Search and Genre Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isFullScreenSearchOpen = MutableStateFlow(false)
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

    // Currently playing channel or offline media
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _playingOfflineMedia = MutableStateFlow<OfflineMedia?>(null)
    val playingOfflineMedia: StateFlow<OfflineMedia?> = _playingOfflineMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // Picture in Picture (PiP) State
    private val _isPipMode = MutableStateFlow(false)
    val isPipMode: StateFlow<Boolean> = _isPipMode.asStateFlow()

    private val _enterPipRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val enterPipRequest: SharedFlow<Unit> = _enterPipRequest.asSharedFlow()

    private val _isMiniPlayerDismissed = MutableStateFlow(false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _playbackQuality = MutableStateFlow(PlaybackQuality.AUTO)
    val playbackQuality: StateFlow<PlaybackQuality> = _playbackQuality.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(false)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
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
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
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
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("adult_restriction_enabled", true)
    )
    val isAdultRestrictionEnabled: StateFlow<Boolean> = _isAdultRestrictionEnabled.asStateFlow()

    fun toggleAdultRestriction(enabled: Boolean) {
        _isAdultRestrictionEnabled.value = enabled
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("adult_restriction_enabled", enabled).apply()
    }

    // Two Karate Chop Motion Feature with ON/OFF Control
    private val _isKarateChopEnabled = MutableStateFlow(
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("karate_chop_enabled", true)
    )
    val isKarateChopEnabled: StateFlow<Boolean> = _isKarateChopEnabled.asStateFlow()

    fun setKarateChopEnabled(enabled: Boolean) {
        _isKarateChopEnabled.value = enabled
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("karate_chop_enabled", enabled).apply()

        if (enabled) {
            inAppKarateChopDetector?.startListening()
            com.example.service.KarateChopService.startService(app)
            _captureToastMessage.tryEmit("🥋 Karate Chop Gesture: ON")
        } else {
            inAppKarateChopDetector?.stopListening()
            com.example.service.KarateChopService.stopService(app)
            _karateChopDetectedCount.value = 0
            _karateChopLiveForce.value = 0f
            _captureToastMessage.tryEmit("🥋 Karate Chop Gesture: OFF")
        }
    }

    private val _karateChopLiveForce = MutableStateFlow(0f)
    val karateChopLiveForce: StateFlow<Float> = _karateChopLiveForce.asStateFlow()

    private val _karateChopDetectedCount = MutableStateFlow(0)
    val karateChopDetectedCount: StateFlow<Int> = _karateChopDetectedCount.asStateFlow()

    private val _karateChopBannerMessage = MutableStateFlow<String?>(null)
    val karateChopBannerMessage: StateFlow<String?> = _karateChopBannerMessage.asStateFlow()

    private var inAppKarateChopDetector: com.example.util.KarateChopDetector? = null

    fun setInAppKarateChopDetector(detector: com.example.util.KarateChopDetector?) {
        this.inAppKarateChopDetector = detector
    }

    fun updateKarateChopProgress(count: Int, force: Float) {
        if (!_isKarateChopEnabled.value) return
        _karateChopDetectedCount.value = count
        _karateChopLiveForce.value = force
    }

    fun simulateKarateChop() {
        if (!_isKarateChopEnabled.value) {
            _captureToastMessage.tryEmit("⚠️ Karate Chop Gesture is turned OFF. Enable it first!")
            return
        }
        if (inAppKarateChopDetector != null) {
            inAppKarateChopDetector?.simulateDoubleChop()
        } else {
            handleKarateChopGesture()
        }
    }

    fun handleKarateChopGesture() {
        if (!_isKarateChopEnabled.value) return
        completeAppLaunch()
        _isMiniPlayerDismissed.value = false
        _isPlaying.value = true
        _isFullscreen.value = true
        _isMuted.value = false

        val channelToPlay = _currentChannel.value ?: allChannels.value.firstOrNull() ?: playlistRepo.getCuratedChannels().first()
        playChannel(channelToPlay, directLandscape = true)

        _captureToastMessage.tryEmit("🥋 Two Karate Chops Detected! Resuming Live TV in Landscape")
        _karateChopBannerMessage.value = "🥋 Two Karate Chops Detected! Live TV Playing"

        viewModelScope.launch {
            delay(3500L)
            _karateChopBannerMessage.value = null
            _karateChopDetectedCount.value = 0
        }
    }

    // Channel Live / Broken health tracking
    private val _brokenChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val brokenChannelIds: StateFlow<Set<String>> = _brokenChannelIds.asStateFlow()

    private val _liveChannelIds = MutableStateFlow<Set<String>>(
        setOf(
            "france24_english", "sky_news_live", "bloomberg_tv", "trt_world_global",
            "cgtn_english", "aljazeera_arabic", "aljazeera_mubasher", "dw_news_english",
            "france24_french", "france24_spanish", "france24_arabic", "trt_haber_turkey",
            "trt_arabi", "trt_turk", "news_2gb_live", "one_tv_live", "two_tv_live",
            "redbull_tv", "sports_grid", "movies_00s_replay", "nasa_tv_hd",
            "cgtn_documentary", "rt_doc_live", "three_abn_global", "three_abn_kids",
            "one_two_three_tv", "night_club_afterdark"
        )
    )
    val liveChannelIds: StateFlow<Set<String>> = _liveChannelIds.asStateFlow()

    fun markChannelLive(channelId: String) {
        _liveChannelIds.value = _liveChannelIds.value + channelId
        _brokenChannelIds.value = _brokenChannelIds.value - channelId
    }

    fun markChannelBroken(channelId: String) {
        _brokenChannelIds.value = _brokenChannelIds.value + channelId
        _liveChannelIds.value = _liveChannelIds.value - channelId
    }

    // Parental PIN Prompt Modal State
    private val _isPinDialogVisible = MutableStateFlow(false)
    val isPinDialogVisible: StateFlow<Boolean> = _isPinDialogVisible.asStateFlow()

    private val _pendingPinChannel = MutableStateFlow<Channel?>(null)
    val pendingPinChannel: StateFlow<Channel?> = _pendingPinChannel.asStateFlow()

    private val _pinDialogReason = MutableStateFlow("Parental PIN required to unlock this channel")
    val pinDialogReason: StateFlow<String> = _pinDialogReason.asStateFlow()

    private val _pinActionType = MutableStateFlow<PinActionType>(PinActionType.PLAY_CHANNEL)
    val pinActionType: StateFlow<PinActionType> = _pinActionType.asStateFlow()

    enum class PinActionType {
        PLAY_CHANNEL,
        OPEN_PARENTAL_SETTINGS,
        TOGGLE_CHANNEL_LOCK
    }

    // Wireless Screen Cast Feature
    private val _isCastDialogVisible = MutableStateFlow(false)
    val isCastDialogVisible: StateFlow<Boolean> = _isCastDialogVisible.asStateFlow()

    private val _isCastingActive = MutableStateFlow(false)
    val isCastingActive: StateFlow<Boolean> = _isCastingActive.asStateFlow()

    // Electronic Program Guide (EPG) Schedule State from Room
    private val _selectedScheduleChannel = MutableStateFlow<Channel?>(null)
    val selectedScheduleChannel: StateFlow<Channel?> = _selectedScheduleChannel.asStateFlow()

    val currentChannelSchedule: StateFlow<List<ProgramEntity>> = _selectedScheduleChannel
        .flatMapLatest { channel ->
            if (channel != null) epgRepo.getScheduleForChannel(channel.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentProgramsMap: StateFlow<Map<String, ProgramEntity>> = epgRepo.getAllCurrentPrograms()
        .map { list -> list.associateBy { it.channelId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private data class FilterParams(
        val query: String,
        val genre: String,
        val country: String,
        val language: String,
        val profile: UserProfile?,
        val isAdultRestricted: Boolean
    )

    private val filterParams = combine(
        _searchQuery,
        _selectedGenre,
        _selectedCountryFilter,
        _selectedLanguage,
        profileRepo.activeProfile,
        _isAdultRestrictionEnabled
    ) { args: Array<Any?> ->
        FilterParams(
            query = args[0] as String,
            genre = args[1] as String,
            country = args[2] as String,
            language = args[3] as String,
            profile = args[4] as? UserProfile,
            isAdultRestricted = args[5] as Boolean
        )
    }

    // Combined filtered channel stream
    val filteredChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        filterParams,
        profileRepo.globallyLockedChannelIds
    ) { channels, params, lockedIds ->
        val isKids = params.profile?.isKidsProfile == true
        val favs = params.profile?.favorites ?: emptyList()

        channels.filter { channel ->
            if ((isKids || params.isAdultRestricted) && channel.isAdultOrRestricted) {
                return@filter false
            }

            val genreMatches = when (params.genre) {
                ChannelGenre.ALL.displayName -> true
                ChannelGenre.FAVORITES.displayName -> favs.contains(channel.id)
                else -> channel.genre.equals(params.genre, ignoreCase = true) ||
                        channel.genre.contains(params.genre, ignoreCase = true) ||
                        params.genre.contains(channel.genre, ignoreCase = true)
            }

            val countryMatches = when {
                params.country == "ALL" -> true
                params.country == "IN" -> {
                    channel.country.equals("IN", ignoreCase = true) ||
                    channel.country.contains("India", ignoreCase = true) ||
                    channel.name.contains("India", ignoreCase = true) ||
                    channel.id.endsWith(".in", ignoreCase = true) ||
                    channel.id.contains("dd_", ignoreCase = true) ||
                    channel.name.startsWith("DD ", ignoreCase = true) ||
                    channel.name.contains("Aaj Tak", ignoreCase = true) ||
                    channel.name.contains("NDTV", ignoreCase = true) ||
                    channel.name.contains("News18", ignoreCase = true) ||
                    channel.name.contains("ABP", ignoreCase = true) ||
                    channel.name.contains("Republic", ignoreCase = true) ||
                    channel.name.contains("Times Now", ignoreCase = true) ||
                    channel.name.contains("WION", ignoreCase = true) ||
                    channel.name.contains("Sansad", ignoreCase = true) ||
                    channel.name.contains("Doordarshan", ignoreCase = true) ||
                    channel.language.contains("Hindi", ignoreCase = true) ||
                    channel.language.contains("Tamil", ignoreCase = true) ||
                    channel.language.contains("Telugu", ignoreCase = true) ||
                    channel.language.contains("Punjabi", ignoreCase = true) ||
                    channel.language.contains("Malayalam", ignoreCase = true) ||
                    channel.language.contains("Bengali", ignoreCase = true) ||
                    channel.language.contains("Marathi", ignoreCase = true) ||
                    channel.language.contains("Gujarati", ignoreCase = true) ||
                    channel.language.contains("Kannada", ignoreCase = true)
                }
                params.country == "US" -> {
                    channel.country.equals("US", ignoreCase = true) ||
                    channel.country.contains("United States", ignoreCase = true) ||
                    channel.country.contains("USA", ignoreCase = true) ||
                    channel.id.endsWith(".us", ignoreCase = true)
                }
                params.country == "UK" -> {
                    channel.country.equals("UK", ignoreCase = true) ||
                    channel.country.equals("GB", ignoreCase = true) ||
                    channel.country.contains("United Kingdom", ignoreCase = true) ||
                    channel.country.contains("Britain", ignoreCase = true) ||
                    channel.id.endsWith(".uk", ignoreCase = true)
                }
                params.country == "CA" -> channel.country.equals("CA", ignoreCase = true) || channel.country.contains("Canada", ignoreCase = true) || channel.id.endsWith(".ca", ignoreCase = true)
                params.country == "AU" -> channel.country.equals("AU", ignoreCase = true) || channel.country.contains("Australia", ignoreCase = true) || channel.id.endsWith(".au", ignoreCase = true)
                params.country == "DE" -> channel.country.equals("DE", ignoreCase = true) || channel.country.contains("Germany", ignoreCase = true) || channel.id.endsWith(".de", ignoreCase = true) || channel.language.contains("German", ignoreCase = true)
                params.country == "FR" -> channel.country.equals("FR", ignoreCase = true) || channel.country.contains("France", ignoreCase = true) || channel.id.endsWith(".fr", ignoreCase = true) || channel.language.contains("French", ignoreCase = true)
                params.country == "ES" -> channel.country.equals("ES", ignoreCase = true) || channel.country.contains("Spain", ignoreCase = true) || channel.id.endsWith(".es", ignoreCase = true) || channel.language.contains("Spanish", ignoreCase = true)
                params.country == "IT" -> channel.country.equals("IT", ignoreCase = true) || channel.country.contains("Italy", ignoreCase = true) || channel.id.endsWith(".it", ignoreCase = true) || channel.language.contains("Italian", ignoreCase = true)
                params.country == "BR" -> channel.country.equals("BR", ignoreCase = true) || channel.country.contains("Brazil", ignoreCase = true) || channel.id.endsWith(".br", ignoreCase = true) || channel.language.contains("Portuguese", ignoreCase = true)
                params.country == "MX" -> channel.country.equals("MX", ignoreCase = true) || channel.country.contains("Mexico", ignoreCase = true) || channel.id.endsWith(".mx", ignoreCase = true)
                params.country == "JP" -> channel.country.equals("JP", ignoreCase = true) || channel.country.contains("Japan", ignoreCase = true) || channel.id.endsWith(".jp", ignoreCase = true) || channel.language.contains("Japanese", ignoreCase = true)
                params.country == "KR" -> channel.country.equals("KR", ignoreCase = true) || channel.country.contains("Korea", ignoreCase = true) || channel.id.endsWith(".kr", ignoreCase = true) || channel.language.contains("Korean", ignoreCase = true)
                params.country == "CN" -> channel.country.equals("CN", ignoreCase = true) || channel.country.contains("China", ignoreCase = true) || channel.id.endsWith(".cn", ignoreCase = true) || channel.name.contains("CCTV", ignoreCase = true) || channel.name.contains("CGTN", ignoreCase = true) || channel.language.contains("Chinese", ignoreCase = true)
                params.country == "AE" -> channel.country.equals("AE", ignoreCase = true) || channel.country.contains("UAE", ignoreCase = true) || channel.country.contains("Arab", ignoreCase = true) || channel.language.contains("Arabic", ignoreCase = true)
                params.country == "SA" -> channel.country.equals("SA", ignoreCase = true) || channel.country.contains("Saudi", ignoreCase = true) || channel.id.endsWith(".sa", ignoreCase = true)
                params.country == "PK" -> channel.country.equals("PK", ignoreCase = true) || channel.country.contains("Pakistan", ignoreCase = true) || channel.name.contains("Pakistan", ignoreCase = true) || channel.id.endsWith(".pk", ignoreCase = true) || channel.language.contains("Urdu", ignoreCase = true)
                params.country == "BD" -> channel.country.equals("BD", ignoreCase = true) || channel.country.contains("Bangladesh", ignoreCase = true) || channel.name.contains("Bangladesh", ignoreCase = true) || channel.id.endsWith(".bd", ignoreCase = true) || channel.language.contains("Bengali", ignoreCase = true)
                params.country == "QA" -> channel.country.equals("QA", ignoreCase = true) || channel.country.contains("Qatar", ignoreCase = true) || channel.id.contains("aljazeera", ignoreCase = true)
                params.country == "TR" -> channel.country.equals("TR", ignoreCase = true) || channel.country.contains("Turkey", ignoreCase = true) || channel.country.contains("Turkiye", ignoreCase = true) || channel.id.contains("trt", ignoreCase = true) || channel.language.contains("Turkish", ignoreCase = true)
                params.country == "RU" -> channel.country.equals("RU", ignoreCase = true) || channel.country.contains("Russia", ignoreCase = true) || channel.id.endsWith(".ru", ignoreCase = true) || channel.language.contains("Russian", ignoreCase = true)
                params.country == "ID" -> channel.country.equals("ID", ignoreCase = true) || channel.country.contains("Indonesia", ignoreCase = true) || channel.id.endsWith(".id", ignoreCase = true) || channel.language.contains("Indonesian", ignoreCase = true)
                params.country == "PH" -> channel.country.equals("PH", ignoreCase = true) || channel.country.contains("Philippines", ignoreCase = true) || channel.id.endsWith(".ph", ignoreCase = true) || channel.language.contains("Tagalog", ignoreCase = true)
                params.country == "AT" -> channel.country.equals("AT", ignoreCase = true) || channel.country.contains("Austria", ignoreCase = true) || channel.id.contains("redbull", ignoreCase = true)
                params.country == "UA" -> channel.country.equals("UA", ignoreCase = true) || channel.country.contains("Ukraine", ignoreCase = true) || channel.language.contains("Ukrainian", ignoreCase = true)
                params.country == "GE" -> channel.country.equals("GE", ignoreCase = true) || channel.country.contains("Georgia", ignoreCase = true) || channel.id.contains("1tv", ignoreCase = true) || channel.id.contains("2tv", ignoreCase = true)
                params.country == "NL" -> channel.country.equals("NL", ignoreCase = true) || channel.country.contains("Netherlands", ignoreCase = true) || channel.id.endsWith(".nl", ignoreCase = true)
                params.country == "CH" -> channel.country.equals("CH", ignoreCase = true) || channel.country.contains("Switzerland", ignoreCase = true) || channel.id.endsWith(".ch", ignoreCase = true)
                params.country == "EG" -> channel.country.equals("EG", ignoreCase = true) || channel.country.contains("Egypt", ignoreCase = true) || channel.id.endsWith(".eg", ignoreCase = true)
                params.country == "ZA" -> channel.country.equals("ZA", ignoreCase = true) || channel.country.contains("South Africa", ignoreCase = true) || channel.id.endsWith(".za", ignoreCase = true)
                params.country == "NG" -> channel.country.equals("NG", ignoreCase = true) || channel.country.contains("Nigeria", ignoreCase = true) || channel.id.endsWith(".ng", ignoreCase = true)
                params.country == "EU" -> channel.country.equals("EU", ignoreCase = true) || channel.id.contains("euronews", ignoreCase = true) || channel.country.contains("Europe", ignoreCase = true)
                else -> channel.country.equals(params.country, ignoreCase = true) ||
                        channel.country.contains(params.country, ignoreCase = true)
            }

            val languageMatches = when (params.language) {
                "ALL" -> true
                else -> channel.language.contains(params.language, ignoreCase = true) ||
                        channel.name.contains(params.language, ignoreCase = true)
            }

            val queryMatches = if (params.query.isBlank()) {
                true
            } else {
                val q = params.query.trim()
                channel.name.contains(q, ignoreCase = true) ||
                channel.genre.contains(q, ignoreCase = true) ||
                channel.country.contains(q, ignoreCase = true) ||
                channel.language.contains(q, ignoreCase = true) ||
                channel.description.contains(q, ignoreCase = true)
            }

            // When searching by query, prioritize showing matching channels directly
            if (params.query.isNotBlank()) {
                queryMatches
            } else {
                genreMatches && countryMatches && languageMatches
            }
        }.map { c ->
            c.copy(
                isFavorite = favs.contains(c.id),
                isLocked = lockedIds.contains(c.id) || c.isAdultOrRestricted
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, playlistRepo.channels.value)

    // Dedicated stream for all user-bookmarked favorite channels
    val favoriteChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile
    ) { channels, profile ->
        val favIds = profile?.favorites ?: emptyList()
        channels.filter { favIds.contains(it.id) }.map { it.copy(isFavorite = true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Profile recommendations based on top watch genres
    val recommendedChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile,
        profileRepo.globallyLockedChannelIds
    ) { channels, profile, lockedIds ->
        val history = profile?.watchHistory ?: emptyList()
        val favs = profile?.favorites ?: emptyList()
        val isKids = profile?.isKidsProfile == true

        val eligible = channels.filter { !isKids || !it.isAdultOrRestricted }

        if (history.isEmpty()) {
            eligible.take(6).map { c ->
                c.copy(isFavorite = favs.contains(c.id), isLocked = lockedIds.contains(c.id))
            }
        } else {
            val topChannelNames = history.map { it.channelName }
            val matching = eligible.filter { it.name in topChannelNames }
            val preferredGenres = matching.map { it.genre }.groupingBy { it }.eachCount()
            val bestGenre = preferredGenres.maxByOrNull { it.value }?.key ?: "News"

            eligible.filter { it.genre.equals(bestGenre, ignoreCase = true) }
                .take(6)
                .map { c ->
                    c.copy(isFavorite = favs.contains(c.id), isLocked = lockedIds.contains(c.id))
                }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tracks the last 5 channels the user opened, enabling quick resume functionality
    val recentlyWatchedChannels: StateFlow<List<Channel>> = combine(
        playlistRepo.channels,
        profileRepo.activeProfile,
        profileRepo.globallyLockedChannelIds
    ) { channels, profile, lockedIds ->
        val history = profile?.watchHistory ?: emptyList()
        val favs = profile?.favorites ?: emptyList()
        val channelMap = channels.associateBy { it.id }

        // Find channels matching recent watch history
        val watched = history.mapNotNull { entry ->
            channelMap[entry.channelId]
        }.distinctBy { it.id }.take(5)

        // If fewer than 5 watched yet, supplement with top verified channels so quick resume is always populated
        val finalChannels = if (watched.size < 5) {
            val remainingNeeded = 5 - watched.size
            val alreadyIds = watched.map { it.id }.toSet()
            val supplements = channels.filter { it.id !in alreadyIds }.take(remainingNeeded)
            watched + supplements
        } else {
            watched
        }

        finalChannels.map { c ->
            c.copy(
                isFavorite = favs.contains(c.id),
                isLocked = lockedIds.contains(c.id) || c.isAdultOrRestricted
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allChannels: StateFlow<List<Channel>> = playlistRepo.channels

    val isChannelsLoaded: StateFlow<Boolean> = playlistRepo.channels
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, playlistRepo.channels.value.isNotEmpty())

    private val _isAppLaunchCountdownFinished = MutableStateFlow(false)
    val isAppLaunchCountdownFinished: StateFlow<Boolean> = _isAppLaunchCountdownFinished.asStateFlow()

    fun completeAppLaunch() {
        _isAppLaunchCountdownFinished.value = true
    }

    private fun saveLastPlayedChannel(channelId: String) {
        app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
            .edit().putString("last_played_channel_id", channelId).apply()
    }

    init {
        val prefs = app.getSharedPreferences("stream_pulse_settings", android.content.Context.MODE_PRIVATE)
        val lastPlayedId = prefs.getString("last_played_channel_id", null)

        // Automatically sync playlist from iptv-org if first open or 24h passed, and clear RAM on app launch
        viewModelScope.launch {
            playlistRepo.checkAndTriggerAutoUpdate(force = false)
            refreshStorageMetrics()
            autoClearRam()
        }

        // Set last played channel or premier global channel as active preview/playback
        viewModelScope.launch {
            playlistRepo.channels.collect { list ->
                if (list.isNotEmpty()) {
                    if (_currentChannel.value == null) {
                        val target = if (!lastPlayedId.isNullOrEmpty() && list.any { it.id == lastPlayedId } && lastPlayedId != "dw_news_english") {
                            list.first { it.id == lastPlayedId }
                        } else {
                            list.first() // France 24 English
                        }
                        _currentChannel.value = target
                        _isPlaying.value = true // Automatically plays channel on app open
                    }
                    // Automatically sync EPG and Room offline channel cache
                    epgRepo.syncEpgForChannels(list)
                    historyAndOfflineRepo.saveOfflineChannels(list.take(35))
                    refreshStorageMetrics()
                }
            }
        }

        // Auto background clear device RAM periodically so app works fast continuously
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(120_000L) // Every 2 minutes
                autoClearRam()
            }
        }
    }

    fun playNextChannel() {
        val list = filteredChannels.value.ifEmpty { playlistRepo.channels.value }
        if (list.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex != -1 && currentIndex < list.size - 1) currentIndex + 1 else 0
        playChannel(list[nextIndex])
    }

    fun playPreviousChannel() {
        val list = filteredChannels.value.ifEmpty { playlistRepo.channels.value }
        if (list.isEmpty()) return
        val current = _currentChannel.value
        val currentIndex = list.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playChannel(list[prevIndex])
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun selectGenre(genreName: String) {
        _selectedGenre.value = genreName
    }

    fun selectCountry(country: String) {
        _selectedCountryFilter.value = country
    }

    fun playChannel(channel: Channel, directLandscape: Boolean = true) {
        if (channel.isLocked) {
            _pendingPinChannel.value = channel
            _pinActionType.value = PinActionType.PLAY_CHANNEL
            _pinDialogReason.value = "Enter 4-digit PIN to watch ${channel.name}"
            _isPinDialogVisible.value = true
            return
        }

        _playingOfflineMedia.value = null
        _currentChannel.value = channel
        _isPlaying.value = true
        _isMiniPlayerDismissed.value = false
        if (directLandscape) {
            _isFullscreen.value = true
        }
        markChannelLive(channel.id)

        saveLastPlayedChannel(channel.id)
        profileRepo.recordWatchHistory(channel.id, channel.name)
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = profileRepo.activeProfile.value?.id ?: "default"
            historyAndOfflineRepo.recordViewing(channel, activeId)
        }
    }

    fun playChannelById(channelId: String, directLandscape: Boolean = true) {
        val channel = playlistRepo.channels.value.firstOrNull { it.id == channelId }
        if (channel != null) {
            playChannel(channel, directLandscape)
        }
    }

    fun refreshStorageMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            val (offlineCount, historyCount, programCount) = historyAndOfflineRepo.getDatabaseStorageSummary()
            val totalSize = (offlineCount * 1.5) + (historyCount * 0.05) + (programCount * 0.02)
            _storageMetrics.value = StorageMetrics(
                offlineChannelsCount = offlineCount,
                historyEntriesCount = historyCount,
                epgProgramsCount = programCount,
                totalCacheSizeMb = String.format(java.util.Locale.US, "%.1f", totalSize).toDoubleOrNull() ?: 14.2
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
                coil.Coil.imageLoader(app).memoryCache?.clear()
                System.gc()
                android.util.Log.d("TVViewModel", "Auto RAM clean completed")
            } catch (e: Exception) {
                android.util.Log.e("TVViewModel", "RAM clean failed", e)
            }
        }
    }

    fun saveChannelForOffline(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.saveOfflineChannel(channel)
        }
    }

    fun deleteOfflineChannel(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyAndOfflineRepo.deleteOfflineChannel(channelId)
        }
    }

    fun playOfflineMedia(media: OfflineMedia) {
        _currentChannel.value = null
        _playingOfflineMedia.value = media
        _isPlaying.value = true
        _isFullscreen.value = true
        _isMiniPlayerDismissed.value = false
        profileRepo.logActivity("Offline Viewing: ${media.title}")
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(enabled: Boolean) {
        _isFullscreen.value = enabled
    }

    val presetPlaylists = PlaylistRepository.PRESET_PLAYLISTS

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
    }

    // EPG Schedule
    fun openChannelSchedule(channel: Channel) {
        _selectedScheduleChannel.value = channel
    }

    fun closeChannelSchedule() {
        _selectedScheduleChannel.value = null
    }

    // 5-Minute EPG Program Reminders
    fun toggleProgramReminder(program: ProgramEntity, channelName: String): Boolean {
        return if (epgReminderManager.isReminderSet(program.id)) {
            epgReminderManager.cancelReminder(program.id)
            _captureToastMessage.tryEmit("Reminder removed for ${program.title}")
            false
        } else {
            val scheduled = epgReminderManager.scheduleReminder(program, channelName)
            if (scheduled) {
                _captureToastMessage.tryEmit("Reminder set: Alert 5m before ${program.title} on $channelName")
            }
            scheduled
        }
    }

    fun isReminderSet(programId: String): Boolean = epgReminderManager.isReminderSet(programId)

    fun cancelReminder(programId: String) {
        epgReminderManager.cancelReminder(programId)
        _captureToastMessage.tryEmit("Reminder removed")
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
                    val target = _pendingPinChannel.value
                    if (target != null) {
                        profileRepo.toggleChannelLock(target.id)
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

    fun toggleCastActive() {
        _isCastingActive.value = !_isCastingActive.value
        profileRepo.logActivity(if (_isCastingActive.value) "Started Screen Cast" else "Ended Screen Cast")
    }

    // Playlist sync
    fun triggerManualSync() {
        viewModelScope.launch {
            playlistRepo.fetchPlaylist()
        }
    }

    fun updatePlaylistUrl(url: String) {
        playlistRepo.setPlaylistUrl(url)
        viewModelScope.launch {
            playlistRepo.fetchPlaylist(url)
        }
    }
}
