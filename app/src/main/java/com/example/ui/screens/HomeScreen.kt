package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.ChannelGenre
import com.example.ui.components.ChannelCard
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SkeuoScrew
import com.example.ui.theme.SkeuoJewelLed
import com.example.ui.theme.SkeuoRubyLed
import com.example.ui.theme.SkeuoEmeraldLed
import com.example.ui.theme.SkeuoPhosphorCyan
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell
import com.example.ui.theme.tvScreenBezel
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TVViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TVViewModel,
    onOpenProfileSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentChannel by viewModel.currentChannel.collectAsState()
    val playingOfflineMedia by viewModel.playingOfflineMedia.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val quality by viewModel.playbackQuality.collectAsState()
    val subtitlesEnabled by viewModel.subtitlesEnabled.collectAsState()
    val isCastingActive by viewModel.isCastingActive.collectAsState()
    val currentProgramsMap by viewModel.currentProgramsMap.collectAsState()
    val activeProfile by viewModel.profileRepo.activeProfile.collectAsState()

    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()
    val recentlyWatchedChannels by viewModel.recentlyWatchedChannels.collectAsState()
    val currentPlaylistUrl by viewModel.playlistRepo.playlistUrl.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncing by viewModel.playlistRepo.isSyncing.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val brokenChannelIds by viewModel.brokenChannelIds.collectAsState()

    val activeFavs = activeProfile?.favorites ?: emptyList()
    val favoriteChannels = filteredChannels.filter { it.isFavorite }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Live Video Player
        item {
            val title = playingOfflineMedia?.title ?: currentChannel?.name ?: "StreamPulse TV Live"
            val nowPlaying = currentChannel?.let { currentProgramsMap[it.id] }
            val subtitle = playingOfflineMedia?.channelName
                ?: if (nowPlaying != null) "NOW: ${nowPlaying.title} • ${currentChannel?.resolution ?: "1080p FHD"}"
                else "${currentChannel?.genre ?: "Entertainment"} • ${currentChannel?.resolution ?: "1080p FHD"}"
            val streamUrl = playingOfflineMedia?.streamUrl ?: currentChannel?.streamUrl

            VideoPlayerView(
                streamUrl = streamUrl,
                title = title,
                subtitle = subtitle,
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                aspectRatioMode = aspectRatio,
                playbackQuality = quality,
                subtitlesEnabled = subtitlesEnabled,
                isCasting = isCastingActive,
                castDeviceName = if (isCastingActive) "Wireless Screen Cast" else null,
                isFavorite = currentChannel?.let { activeFavs.contains(it.id) } ?: false,
                isLocked = currentChannel?.isLocked ?: false,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        // Search Bar & Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (searchQuery.isBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenProfileSwitcher() }
                                .padding(end = 8.dp)
                                .testTag("home_profile_badge")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeProfile?.avatarBgColorHex ?: 0xFF6366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeProfile?.avatarEmoji ?: "🎬",
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Viewing as ${activeProfile?.name ?: "Family"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (activeProfile?.isKidsProfile == true) "Child Safe Mode Active" else "Switch Profile ▾",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeProfile?.isKidsProfile == true) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSyncing) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanNeon.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Syncing",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-syncing...",
                                        color = CyanNeon,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Full Screen Search Trigger Bar (Recessed Skeuomorphic Well)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .skeuomorphicWell(cornerRadius = 12.dp)
                        .clickable { viewModel.openFullScreenSearch() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) searchQuery else "Search 12,800+ live channels, genres...",
                            color = if (searchQuery.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        SkeuoButton(
                            onClick = { viewModel.openFullScreenSearch() },
                            cornerRadius = 6.dp
                        ) {
                            Text(
                                text = "TUNER 🔍",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyanNeon.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Found ${filteredChannels.size} live channels for \"$searchQuery\"",
                                color = CyanNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.setSearchQuery("") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // Quick Search Tags
                    Spacer(modifier = Modifier.height(8.dp))
                    val quickTags = listOf("France 24", "Sky News", "Bloomberg", "TRT World", "Red Bull", "NASA", "Cinema", "Kids")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickTags) { tag ->
                            Surface(
                                onClick = {
                                    viewModel.setSearchQuery(tag)
                                    viewModel.openFullScreenSearch()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (searchQuery.equals(tag, ignoreCase = true)) CyanNeon.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (searchQuery.equals(tag, ignoreCase = true)) androidx.compose.foundation.BorderStroke(1.dp, CyanNeon) else null,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = if (searchQuery.equals(tag, ignoreCase = true)) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Genre Filter Chips (Hidden when searching to focus purely on channel results)
        if (searchQuery.isBlank()) {
            item {
                val genres = ChannelGenre.entries.map { it.displayName }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(genres) { genreName ->
                        val isSelected = selectedGenre == genreName
                        SkeuoButton(
                            onClick = { viewModel.selectGenre(genreName) },
                            isActive = isSelected,
                            isAccent = isSelected,
                            cornerRadius = 8.dp,
                            modifier = Modifier.testTag("genre_chip_$genreName")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                SkeuoJewelLed(
                                    isActive = isSelected,
                                    color = if (isSelected) SkeuoPhosphorCyan else Color(0xFF64748B),
                                    size = 6.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = genreName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }



        // Recently Watched Section (Last 5 channels opened, quick resume)
        if (recentlyWatchedChannels.isNotEmpty() && searchQuery.isBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recently Watched",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Quick Resume",
                            fontSize = 12.sp,
                            color = CyanNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyWatchedChannels) { channel ->
                            RecentlyWatchedCard(
                                channel = channel,
                                onResume = { viewModel.playChannel(channel) }
                            )
                        }
                    }
                }
            }
        }

        // Personalized Recommendations Section
        if (searchQuery.isBlank() && selectedGenre == ChannelGenre.ALL.displayName) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tailored For You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${activeProfile?.name ?: "Family"}'s Picks",
                            fontSize = 12.sp,
                            color = CyanNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendedChannels) { channel ->
                            RecommendationCard(
                                channel = channel,
                                onSelect = { viewModel.playChannel(channel) }
                            )
                        }
                    }
                }
            }
        }

        // Favorites Row (if any)
        if (favoriteChannels.isNotEmpty() && selectedGenre != ChannelGenre.FAVORITES.displayName) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Favorites (${favoriteChannels.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteChannels) { channel ->
                            RecommendationCard(
                                channel = channel,
                                onSelect = { viewModel.playChannel(channel) }
                            )
                        }
                    }
                }
            }
        }

        // Live Channel Directory Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results (${filteredChannels.size})" else "All Live Channels (${filteredChannels.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Verified Streams",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredChannels.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No channels found matching '$searchQuery'",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different genre or country tag, or tap below to reset filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.selectGenre(ChannelGenre.ALL.displayName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Search Filters", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Channel Cards List (Shows top 60 priority channels or all search results with unique IDs)
            val homeDisplayList = if (searchQuery.isNotBlank()) filteredChannels else filteredChannels.take(60)
            items(homeDisplayList, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    isSelected = currentChannel?.id == channel.id,
                    currentProgram = currentProgramsMap[channel.id],
                    isLive = !brokenChannelIds.contains(channel.id),
                    isBroken = brokenChannelIds.contains(channel.id),
                    onSelect = { viewModel.playChannel(channel) },
                    onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                    onOpenSchedule = { viewModel.openChannelSchedule(channel) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (searchQuery.isBlank() && filteredChannels.size > 60) {
                item {
                    Card(
                        onClick = { viewModel.selectTab(ScreenTab.CHANNELS) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("explore_all_channels_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Explore All ${filteredChannels.size} Live Channels",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Filter by 24+ countries, 18+ languages, genres & full EPG Guide in Channels Tab",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Browse All Channels",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    channel: Channel,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("rec_card_${channel.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = channel.genre,
                style = MaterialTheme.typography.bodySmall,
                color = CyanNeon,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun RecentlyWatchedCard(
    channel: Channel,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onResume,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f)),
        modifier = modifier
            .width(185.dp)
            .testTag("recently_watched_${channel.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanNeon.copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Resume",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = channel.resolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
