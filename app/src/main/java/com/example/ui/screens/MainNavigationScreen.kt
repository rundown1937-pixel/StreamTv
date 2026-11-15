package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.isOfflineMetadata
import com.example.ui.components.CastDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PinDialog
import com.example.ui.components.ProfileSwitchDialog
import com.example.ui.components.SubtitlesAudioDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.findActivity
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TVViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier,
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isPipMode by viewModel.isPipMode.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val playingOfflineMedia by viewModel.playingOfflineMedia.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isMiniPlayerDismissed by viewModel.isMiniPlayerDismissed.collectAsState()
    val brokenChannelIds by viewModel.brokenChannelIds.collectAsState()

    val activeProfile by viewModel.profileRepo.activeProfile.collectAsState()
    val isCastingActive by viewModel.isCastingActive.collectAsState()

    // Dialog state
    val isPinDialogVisible by viewModel.isPinDialogVisible.collectAsState()
    val pinDialogReason by viewModel.pinDialogReason.collectAsState()
    val isCastDialogVisible by viewModel.isCastDialogVisible.collectAsState()

    var isProfileSwitchDialogVisible by remember { mutableStateOf(value = false) }
    var isSubtitlesDialogVisible by remember { mutableStateOf(value = false) }

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

    val context = androidx.compose.ui.platform.LocalContext.current

    // Handle back button when in sub screens, search, or fullscreen
    BackHandler(enabled = (isFullScreenSearchOpen) || (isFullscreen) || (currentTab != ScreenTab.HOME)) {
        context.findActivity()?.let { activity ->
            com.example.util.AdManager.showInterstitialAd(
                activity,
                com.example.util.AdManager.BACK_BUTTON_AD_ID,
            )
        }
        if (isFullScreenSearchOpen) {
            viewModel.closeFullScreenSearch()
        } else if (isFullscreen) {
            viewModel.setFullscreen(enabled = false)
        } else if (currentTab != ScreenTab.HOME) {
            viewModel.selectTab(ScreenTab.HOME)
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.captureToastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.util.LocalAppStrings provides appStrings,
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
                    .background(Color.Black),
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
                    isPipMode = true,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onToggleFullscreen = {},
                    onCycleAspectRatio = {},
                    onCycleQuality = {},
                    onToggleSubtitles = {},
                    onOpenCast = {},
                    onToggleFavorite = {},
                    onEnterPip = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
            return@CompositionLocalProvider
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isTablet = configuration.smallestScreenWidthDp >= 600
            val isWideScreen = isTablet || isLandscape || (maxWidth >= 600.dp)
            val hasActiveStream = (currentChannel != null) || (playingOfflineMedia != null)

            if (isFullscreen && hasActiveStream) {
                // Fullscreen video player only
                val title = playingOfflineMedia?.title ?: currentChannel?.name ?: "StreamPulse TV Live"
                val subtitle = playingOfflineMedia?.channelName
                    ?: "${currentChannel?.genre ?: "Entertainment"} • ${currentChannel?.resolution ?: "1080p FHD"}"
                val streamUrl = playingOfflineMedia?.streamUrl ?: currentChannel?.streamUrl

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
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
                        isFavorite = currentChannel?.id?.let(activeProfile?.favorites.orEmpty()::contains) ?: false,
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
                        isMuted = isMuted,
                        onToggleMute = { viewModel.toggleMute() },
                        sleepTimerMinutes = sleepTimerMinutes,
                        onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
                        onToggleSubtitles = { viewModel.toggleSubtitles() },
                        onOpenCast = { viewModel.openCastDialog() },
                        onToggleFavorite = {
                            currentChannel?.id?.let(viewModel::toggleFavorite)
                        },
                        onEnterPip = { viewModel.requestPip() },
                        onPlaybackStatusChanged = { isOnline ->
                            currentChannel?.id?.let { channelId ->
                                if (isOnline) viewModel.markChannelLive(channelId)
                                else viewModel.markChannelBroken(channelId)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
        } else {
            val topBarActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                // Full Screen Search Button (WCAG >= 48dp touch target)
                IconButton(
                    onClick = { viewModel.openFullScreenSearch() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_search_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Channels",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Cast Button (Opens real Android system cast settings, WCAG >= 48dp touch target)
                IconButton(
                    onClick = { viewModel.openCastDialog() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_cast_button"),
                ) {
                    Icon(
                        imageVector = if (isCastingActive) Icons.Default.CastConnected else Icons.Default.Cast,
                        contentDescription = "Screen Cast",
                        tint = if (isCastingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Parental Controls Shortcut (WCAG >= 48dp touch target)
                IconButton(
                    onClick = {
                        viewModel.requestParentalAccess(TVViewModel.PinActionType.OPEN_PARENTAL_SETTINGS)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_parental_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Parental Controls",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Rewards Ad Button (Watch ad to support / earn reward)
                IconButton(
                    onClick = {
                        context.findActivity()?.let { act ->
                            com.example.util.AdManager.showRewardedOrInterstitialAd(
                                act,
                                com.example.util.AdManager.VIDEO_PLAYER_SUPPORT_AD_ID,
                            ) {
                                android.widget.Toast.makeText(
                                    context,
                                    "🎉 Reward Unlocked! Thank you for supporting StreamPulse TV!",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_rewards_ad_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Watch Reward Ad",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            val contentPane: @Composable () -> Unit = {
                when (currentTab) {
                    ScreenTab.HOME -> HomeScreen(viewModel = viewModel)
                    ScreenTab.CHANNELS -> ChannelsScreen(viewModel = viewModel)
                    ScreenTab.PROFILES -> ProfilesScreen(
                        viewModel = viewModel,
                        onOpenAddProfileDialog = { isProfileSwitchDialogVisible = true },
                    )
                    ScreenTab.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                    ScreenTab.PARENTAL -> ParentalControlsScreen(viewModel = viewModel)
                    ScreenTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenSubtitlesDialog = { isSubtitlesDialogVisible = true },
                        onOpenParentalSettings = {
                            viewModel.requestParentalAccess(TVViewModel.PinActionType.OPEN_PARENTAL_SETTINGS)
                        },
                    )
                }
            }

            if (isWideScreen) {
                // Wide Screen / Landscape: Full-height Navigation Rail on left, content Scaffold on right
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        windowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Start + WindowInsetsSides.Vertical,
                        ),
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("tablet_nav_rail"),
                        header = {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.gold_tv_icon_1793366696144),
                                contentDescription = "StreamPulse TV",
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 12.dp)
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        },
                    ) {
                        NavigationRailItem(
                            selected = currentTab == ScreenTab.HOME,
                            onClick = { viewModel.selectTab(ScreenTab.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text(appStrings.liveTab) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("nav_home"),
                        )
                        NavigationRailItem(
                            selected = currentTab == ScreenTab.FAVORITES,
                            onClick = { viewModel.selectTab(ScreenTab.FAVORITES) },
                            icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                            label = { Text(appStrings.favoritesTab) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("nav_favorites"),
                        )
                        NavigationRailItem(
                            selected = (currentTab == ScreenTab.SETTINGS) || (currentTab == ScreenTab.PARENTAL),
                            onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text(appStrings.settingsTab) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("nav_settings"),
                        )
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        contentWindowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.End + WindowInsetsSides.Vertical,
                        ),
                        topBar = {
                            TopAppBar(
                                windowInsets = WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.End + WindowInsetsSides.Top,
                                ),
                                title = {
                                    Text(
                                        text = "StreamPulse TV",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("app_bar_title"),
                                    )
                                },
                                actions = topBarActions,
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                ),
                            )
                        },
                        bottomBar = {
                            if ((currentChannel != null) && !isMiniPlayerDismissed) {
                                val activeCh = currentChannel!!
                                MiniPlayerBar(
                                    channel = activeCh,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onExpand = {
                                        viewModel.setFullscreen(enabled = true)
                                    },
                                    onEnterPip = { viewModel.requestPip() },
                                    onDismiss = { viewModel.dismissMiniPlayer() },
                                    isBroken = brokenChannelIds.contains(activeCh.id) || activeCh.isOfflineMetadata(),
                                )
                            }
                        },
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            contentPane()
                        }
                    }
                }
            } else {
                // Portrait mode: Single Scaffold with TopAppBar, Content, and Bottom NavigationBar
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.testTag("app_bar_title"),
                                ) {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.gold_tv_icon_1793366696144),
                                        contentDescription = "StreamPulse TV",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "StreamPulse TV",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            actions = topBarActions,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if ((currentChannel != null) && !isMiniPlayerDismissed) {
                                val activeCh = currentChannel!!
                                MiniPlayerBar(
                                    channel = activeCh,
                                    isPlaying = isPlaying,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onExpand = {
                                        viewModel.setFullscreen(enabled = true)
                                    },
                                    onEnterPip = { viewModel.requestPip() },
                                    onDismiss = { viewModel.dismissMiniPlayer() },
                                    isBroken = brokenChannelIds.contains(activeCh.id) || activeCh.isOfflineMetadata(),
                                )
                            }

                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("bottom_nav_bar"),
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.HOME,
                                    onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text(appStrings.liveTab, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.testTag("nav_home"),
                                )
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.FAVORITES,
                                    onClick = { viewModel.selectTab(ScreenTab.FAVORITES) },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                                    label = { Text(appStrings.favoritesTab, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.testTag("nav_favorites"),
                                )
                                NavigationBarItem(
                                    selected = (currentTab == ScreenTab.SETTINGS) || (currentTab == ScreenTab.PARENTAL),
                                    onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text(appStrings.settingsTab, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.testTag("nav_settings"),
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        contentPane()
                    }
                }
            }
        }
    }

    // Dedicated Full-Screen Search Modal
    if (isFullScreenSearchOpen) {
        FullScreenSearchScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeFullScreenSearch() },
        )
    }

    // Modal PIN Dialog
    if (isPinDialogVisible) {
        PinDialog(
            subtitle = pinDialogReason,
            onDismiss = { viewModel.dismissPinDialog() },
        ) { pin ->
            viewModel.verifyPinAndExecute(pin)
        }
    }

    // Modal Cast Dialog (Real Screen Cast)
    if (isCastDialogVisible) {
        CastDialog(isCastingActive = isCastingActive) {
            viewModel.closeCastDialog()
        }
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
        ) {
            isSubtitlesDialogVisible = false
        }
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
        ) {
            isProfileSwitchDialogVisible = false
        }
    }
    }
}
