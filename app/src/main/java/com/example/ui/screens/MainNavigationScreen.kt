package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Home
import com.example.ui.components.EpgGridComponent
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CastDialog
import com.example.ui.components.EpgScheduleDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PinDialog
import com.example.ui.components.ProfileSwitchDialog
import com.example.ui.components.SubtitlesAudioDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.CyanNeon
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TVViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isPipMode by viewModel.isPipMode.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isMiniPlayerDismissed by viewModel.isMiniPlayerDismissed.collectAsState()
    val currentProgramsMap by viewModel.currentProgramsMap.collectAsState()

    val activeProfile by viewModel.profileRepo.activeProfile.collectAsState()
    val isCastingActive by viewModel.isCastingActive.collectAsState()

    // Dialog state
    val isPinDialogVisible by viewModel.isPinDialogVisible.collectAsState()
    val pinDialogReason by viewModel.pinDialogReason.collectAsState()
    val isCastDialogVisible by viewModel.isCastDialogVisible.collectAsState()

    // EPG Schedule Dialog state
    val selectedScheduleChannel by viewModel.selectedScheduleChannel.collectAsState()
    val currentChannelSchedule by viewModel.currentChannelSchedule.collectAsState()

    var isProfileSwitchDialogVisible by remember { mutableStateOf(false) }
    var isSubtitlesDialogVisible by remember { mutableStateOf(false) }

    val subtitlesEnabled by viewModel.subtitlesEnabled.collectAsState()
    val selectedSubtitle by viewModel.selectedSubtitleLanguage.collectAsState()
    val selectedAudio by viewModel.selectedAudioTrack.collectAsState()
    val profiles by viewModel.profileRepo.profiles.collectAsState()

    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val playbackQuality by viewModel.playbackQuality.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val isFullScreenSearchOpen by viewModel.isFullScreenSearchOpen.collectAsState()
    val appInterfaceLang by viewModel.appInterfaceLanguage.collectAsState()
    val appStrings = com.example.util.getStringsForLanguage(appInterfaceLang.code)

    // Handle back button when in sub screens, search, or fullscreen
    BackHandler(enabled = isFullscreen || isFullScreenSearchOpen || currentTab != ScreenTab.HOME) {
        if (isFullscreen) {
            viewModel.setFullscreen(false)
        } else if (isFullScreenSearchOpen) {
            viewModel.closeFullScreenSearch()
        } else if (currentTab != ScreenTab.HOME) {
            viewModel.selectTab(ScreenTab.HOME)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.captureToastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.util.LocalAppStrings provides appStrings
    ) {
        // SYSTEM PICTURE-IN-PICTURE (PiP) MODE:
        // When the app is in system PiP mode, show exclusively the pure video stream
        if (isPipMode) {
            val streamUrl = viewModel.playingOfflineMedia.value?.streamUrl ?: currentChannel?.streamUrl
            val title = viewModel.playingOfflineMedia.value?.title ?: currentChannel?.name ?: "StreamPulse TV"
            val subtitle = viewModel.playingOfflineMedia.value?.channelName ?: currentChannel?.genre ?: "Live"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayerView(
                    streamUrl = streamUrl,
                    title = title,
                    subtitle = subtitle,
                    isPlaying = isPlaying,
                    isFullscreen = false,
                    aspectRatioMode = aspectRatio,
                    playbackQuality = playbackQuality,
                    subtitlesEnabled = subtitlesEnabled,
                    isCasting = isCastingActive,
                    castDeviceName = null,
                    isFavorite = false,
                    isLocked = false,
                    isPipMode = true,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onToggleFullscreen = {},
                    onCycleAspectRatio = {},
                    onCycleQuality = {},
                    onToggleSubtitles = {},
                    onOpenCast = {},
                    onToggleFavorite = {},
                    onEnterPip = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@CompositionLocalProvider
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isFullscreen) {
                // Fullscreen video player only
                val title = viewModel.playingOfflineMedia.value?.title ?: currentChannel?.name ?: "StreamPulse TV Live"
                val nowPlaying = currentChannel?.let { currentProgramsMap[it.id] }
                val subtitle = viewModel.playingOfflineMedia.value?.channelName
                    ?: if (nowPlaying != null) "NOW: ${nowPlaying.title} • ${currentChannel?.resolution ?: "1080p FHD"}"
                    else "${currentChannel?.genre ?: "Entertainment"} • ${currentChannel?.resolution ?: "1080p FHD"}"
                val streamUrl = viewModel.playingOfflineMedia.value?.streamUrl ?: currentChannel?.streamUrl

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    VideoPlayerView(
                        streamUrl = streamUrl,
                        title = title,
                        subtitle = subtitle,
                        isPlaying = isPlaying,
                        isFullscreen = true,
                        aspectRatioMode = aspectRatio,
                        playbackQuality = playbackQuality,
                        subtitlesEnabled = subtitlesEnabled,
                        isCasting = isCastingActive,
                        castDeviceName = if (isCastingActive) "Wireless Screen Cast" else null,
                        isFavorite = currentChannel?.let { activeProfile?.favorites?.contains(it.id) } ?: false,
                        isLocked = currentChannel?.isLocked ?: false,
                        httpUserAgent = currentChannel?.httpUserAgent,
                        httpReferrer = currentChannel?.httpReferrer,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onToggleFullscreen = { viewModel.toggleFullscreen() },
                        onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                        onCycleQuality = { viewModel.cycleQuality() },
                        onSelectAspectRatio = { mode -> viewModel.setAspectRatio(mode) },
                        onSelectQuality = { q -> viewModel.setPlaybackQuality(q) },
                        onPreviousChannel = { viewModel.playPreviousChannel() },
                        onNextChannel = { viewModel.playNextChannel() },
                        currentProgram = nowPlaying,
                        isMuted = isMuted,
                        onToggleMute = { viewModel.toggleMute() },
                        sleepTimerMinutes = sleepTimerMinutes,
                        onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
                        onToggleSubtitles = { viewModel.toggleSubtitles() },
                        onOpenCast = { viewModel.openCastDialog() },
                        onToggleFavorite = {
                            currentChannel?.let { viewModel.toggleFavorite(it.id) }
                        },
                        onEnterPip = { viewModel.requestPip() },
                        onPlaybackStatusChanged = { isOnline ->
                            currentChannel?.let { ch ->
                                if (isOnline) viewModel.markChannelLive(ch.id)
                                else viewModel.markChannelBroken(ch.id)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.testTag("app_bar_title")
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyanNeon.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LiveTv,
                                            contentDescription = null,
                                            tint = CyanNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "StreamPulse TV",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Live IPTV & EPG Schedule",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = CyanNeon
                                    )
                                }
                            }
                        },
                        actions = {
                            // Full Screen Search Button
                            IconButton(
                                onClick = { viewModel.openFullScreenSearch() },
                                modifier = Modifier.testTag("top_bar_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Channels",
                                    tint = CyanNeon
                                )
                            }

                            // Cast Button (Opens real Android system cast settings)
                            IconButton(
                                onClick = { viewModel.openCastDialog() },
                                modifier = Modifier.testTag("top_bar_cast_button")
                            ) {
                                Icon(
                                    imageVector = if (isCastingActive) Icons.Default.CastConnected else Icons.Default.Cast,
                                    contentDescription = "Screen Cast",
                                    tint = if (isCastingActive) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Parental Controls Shortcut
                            IconButton(
                                onClick = {
                                    viewModel.requestParentalAccess(TVViewModel.PinActionType.OPEN_PARENTAL_SETTINGS)
                                },
                                modifier = Modifier.testTag("top_bar_parental_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Parental Controls",
                                    tint = Color(0xFFFBBF24)
                                )
                            }

                            // Active Profile Avatar
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeProfile?.avatarBgColorHex ?: 0xFF6366F1))
                                    .clickable { isProfileSwitchDialogVisible = true }
                                    .testTag("top_bar_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeProfile?.avatarEmoji ?: "🎬",
                                    fontSize = 18.sp
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // In-App Docked Mini Player: appears when user is browsing other tabs while video is playing
                        if (currentTab != ScreenTab.HOME && currentChannel != null && !isMiniPlayerDismissed) {
                            val activeCh = currentChannel!!
                            val nowProg = currentProgramsMap[activeCh.id]

                            MiniPlayerBar(
                                channel = activeCh,
                                currentProgram = nowProg,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onExpand = { viewModel.selectTab(ScreenTab.HOME) },
                                onEnterPip = { viewModel.requestPip() },
                                onDismiss = { viewModel.dismissMiniPlayer() }
                            )
                        }

                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = CyanNeon,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.HOME,
                                    onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text(appStrings.liveTab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00363D),
                                        indicatorColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("nav_home")
                                )
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.CHANNELS,
                                    onClick = { viewModel.selectTab(ScreenTab.CHANNELS) },
                                    icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
                                    label = { Text(appStrings.channelsTab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00363D),
                                        indicatorColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("nav_channels")
                                )
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.FAVORITES,
                                    onClick = { viewModel.selectTab(ScreenTab.FAVORITES) },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                                    label = { Text(appStrings.favoritesTab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00363D),
                                        indicatorColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("nav_favorites")
                                )
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.EPG_GUIDE,
                                    onClick = { viewModel.selectTab(ScreenTab.EPG_GUIDE) },
                                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "TV Guide") },
                                    label = { Text(appStrings.guideTab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00363D),
                                        indicatorColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("nav_epg_guide")
                                )
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.SETTINGS,
                                    onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text(appStrings.settingsTab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF00363D),
                                        indicatorColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Tablet / Landscape / TV Navigation Rail
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = CyanNeon,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.HOME,
                                onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text(appStrings.liveTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.CHANNELS,
                                onClick = { viewModel.selectTab(ScreenTab.CHANNELS) },
                                icon = { Icon(Icons.Default.Tv, contentDescription = "Channels") },
                                label = { Text(appStrings.channelsTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.FAVORITES,
                                onClick = { viewModel.selectTab(ScreenTab.FAVORITES) },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                                label = { Text(appStrings.favoritesTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.EPG_GUIDE,
                                onClick = { viewModel.selectTab(ScreenTab.EPG_GUIDE) },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "TV Guide") },
                                label = { Text(appStrings.guideTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.PROFILES,
                                onClick = { viewModel.selectTab(ScreenTab.PROFILES) },
                                icon = { Icon(Icons.Default.People, contentDescription = "Profiles") },
                                label = { Text(appStrings.profilesTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.PARENTAL,
                                onClick = { viewModel.selectTab(ScreenTab.PARENTAL) },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Parental") },
                                label = { Text("Safety") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                            NavigationRailItem(
                                selected = currentTab == ScreenTab.SETTINGS,
                                onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text(appStrings.settingsTab) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00363D),
                                    indicatorColor = CyanNeon
                                )
                            )
                        }
                    }

                    // Content Pane
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            ScreenTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenProfileSwitcher = { isProfileSwitchDialogVisible = true }
                            )
                            ScreenTab.CHANNELS -> ChannelsScreen(viewModel = viewModel)
                            ScreenTab.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                            ScreenTab.EPG_GUIDE -> EpgGridComponent(viewModel = viewModel)
                            ScreenTab.PROFILES -> ProfilesScreen(
                                viewModel = viewModel,
                                onOpenAddProfileDialog = { isProfileSwitchDialogVisible = true }
                            )
                            ScreenTab.PARENTAL -> ParentalControlsScreen(viewModel = viewModel)
                            ScreenTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onOpenSubtitlesDialog = { isSubtitlesDialogVisible = true },
                                onOpenParentalSettings = {
                                    viewModel.requestParentalAccess(TVViewModel.PinActionType.OPEN_PARENTAL_SETTINGS)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal EPG Schedule Dialog from Room Database
    if (selectedScheduleChannel != null) {
        val channel = selectedScheduleChannel!!
        EpgScheduleDialog(
            channel = channel,
            schedule = currentChannelSchedule,
            isReminderSet = { viewModel.isReminderSet(it) },
            onToggleReminder = { prog -> viewModel.toggleProgramReminder(prog, channel.name) },
            onDismiss = { viewModel.closeChannelSchedule() }
        )
    }

    // Modal PIN Dialog
    if (isPinDialogVisible) {
        PinDialog(
            subtitle = pinDialogReason,
            onDismiss = { viewModel.dismissPinDialog() },
            onVerify = { pin -> viewModel.verifyPinAndExecute(pin) }
        )
    }

    // Modal Cast Dialog (Real Screen Cast)
    if (isCastDialogVisible) {
        CastDialog(
            isCastingActive = isCastingActive,
            onToggleCastMode = { viewModel.toggleCastActive() },
            onDismiss = { viewModel.closeCastDialog() }
        )
    }

    // Modal Subtitles & Audio Dialog
    if (isSubtitlesDialogVisible) {
        SubtitlesAudioDialog(
            subtitlesEnabled = subtitlesEnabled,
            selectedSubtitle = selectedSubtitle,
            selectedAudio = selectedAudio,
            onToggleSubtitles = { viewModel.toggleSubtitles() },
            onSelectSubtitle = { viewModel.setSubtitleLanguage(it) },
            onSelectAudio = { viewModel.setAudioTrack(it) },
            onDismiss = { isSubtitlesDialogVisible = false }
        )
    }

    // Modal Profile Switcher / Creator Dialog
    if (isProfileSwitchDialogVisible) {
        ProfileSwitchDialog(
            profiles = profiles,
            activeProfile = activeProfile,
            onSelectProfile = { viewModel.profileRepo.setActiveProfile(it) },
            onCreateProfile = { name, emoji, isKids, colorHex ->
                viewModel.profileRepo.createProfile(name, emoji, isKids, colorHex)
            },
            onDismiss = { isProfileSwitchDialogVisible = false }
        )
    }

    // Dedicated Full-Screen Search Modal
    if (isFullScreenSearchOpen) {
        FullScreenSearchScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeFullScreenSearch() }
        )
    }
    }
}
