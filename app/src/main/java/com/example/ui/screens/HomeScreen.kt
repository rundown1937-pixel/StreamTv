package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil3.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.ChannelGenre
import com.example.data.model.isOfflineMetadata
import com.example.ui.components.ChannelCard
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.OfflineRed
import com.example.ui.viewmodel.TVViewModel

@Composable
fun HomeScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier,
) {
    val currentChannel by viewModel.currentChannel.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()
    val recentlyWatchedChannels by viewModel.recentlyWatchedChannels.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedCountry by viewModel.selectedCountryFilter.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val brokenChannelIds by viewModel.brokenChannelIds.collectAsState()
    val channelSortOrder by viewModel.channelSortOrder.collectAsState()

    val advancedFilterCount = remember(selectedCountry, selectedLanguage, selectedSubcategory) {
        (if (selectedCountry != "ALL") 1 else 0) +
            (if (selectedLanguage != "ALL") 1 else 0) +
            (if (selectedSubcategory != "ALL") 1 else 0)
    }
    val hasActiveFilters = (selectedGenre != ChannelGenre.ALL.displayName) || (advancedFilterCount > 0) || searchQuery.isNotBlank()
    var showFiltersPanel by rememberSaveable { mutableStateOf(value = false) }

    val (cardBg, cardBorder, socketBg, socketBorder, _, _, statusLiveText, statusOfflineText) = LocalAppThemeColors.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 400.dp
        val columns = when {
            maxWidth >= 960.dp -> 3
            maxWidth >= 560.dp -> 2
            else -> 1
        }
        val rowChunks = remember(filteredChannels, columns) {
            if (columns > 1) filteredChannels.chunked(columns) else emptyList()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_column"),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Search Bar + Filters & Sort Controls in one clean row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Search Trigger Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(socketBg)
                            .border(1.dp, socketBorder, RoundedCornerShape(12.dp))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button) { viewModel.openFullScreenSearch() }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                            .testTag("home_search_bar"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = searchQuery.ifBlank { "Search channels..." },
                                color = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (searchQuery.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setSearchQuery("") },
                                )
                            }
                        }
                    }

                    // Collapsible Filters Button (Region, Language, Live/Offline, Topic)
                    if (searchQuery.isBlank()) {
                        val isFilterActive = showFiltersPanel || (advancedFilterCount > 0)
                        Surface(
                            onClick = { showFiltersPanel = !showFiltersPanel },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFilterActive) MaterialTheme.colorScheme.primary else socketBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isFilterActive) MaterialTheme.colorScheme.primary else socketBorder,
                            ),
                            modifier = Modifier.testTag("home_toggle_filters_btn"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactWidth) 10.dp else 12.dp,
                                    vertical = 10.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filters",
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp),
                                )
                                if (!isCompactWidth || (advancedFilterCount > 0)) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (advancedFilterCount > 0) "Filters ($advancedFilterCount)" else "Filters",
                                        color = if (isFilterActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }

                    // Sort Toggle Button (A-Z / Z-A)
                    Surface(
                        onClick = { viewModel.toggleChannelSortOrder() },
                        shape = RoundedCornerShape(12.dp),
                        color = socketBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, socketBorder),
                        modifier = Modifier.testTag("home_sort_chip"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort channels",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = channelSortOrder.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Primary Categories Row (Always 1 tap away)
            if (searchQuery.isBlank()) {
                item {
                    val genres = remember { ChannelGenre.entries.map { it.displayName } }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        items(genres, key = { it }) { genreName ->
                            val isSelected = selectedGenre == genreName
                            Surface(
                                onClick = {
                                    viewModel.selectGenre(genreName)
                                    viewModel.selectSubcategory("ALL")
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.testTag("genre_chip_$genreName"),
                            ) {
                                Text(
                                    text = genreName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Organized Filter Panel (Status/Topic, Country, Language)
            if (searchQuery.isBlank()) {
                item {
                    AnimatedVisibility(visible = showFiltersPanel) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Status & Topic
                            val subcategories = viewModel.getSubcategoriesForGenre(selectedGenre)
                            FilterSectionLabel(title = "Status & Topic")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(subcategories, key = { it }) { sub ->
                                    val isSelected = selectedSubcategory == sub
                                    val isLiveChip = sub == "Live Now"
                                    val isOfflineChip = sub == "Offline"
                                    val containerColor = when {
                                        isLiveChip -> if (isSelected) LiveGreen.copy(alpha = 0.28f) else LiveGreen.copy(alpha = 0.14f)
                                        isOfflineChip -> if (isSelected) OfflineRed.copy(alpha = 0.28f) else OfflineRed.copy(alpha = 0.14f)
                                        isSelected -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val borderColor = when {
                                        isLiveChip -> if (isSelected) LiveGreen else LiveGreen.copy(alpha = 0.5f)
                                        isOfflineChip -> if (isSelected) OfflineRed else OfflineRed.copy(alpha = 0.5f)
                                        isSelected -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                    val labelColor = when {
                                        isLiveChip -> if (isSelected) LiveGreen else statusLiveText
                                        isOfflineChip -> if (isSelected) OfflineRed else statusOfflineText
                                        isSelected -> MaterialTheme.colorScheme.onTertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Surface(
                                        onClick = { viewModel.selectSubcategory(sub) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = containerColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            if (isLiveChip || isOfflineChip) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isLiveChip) LiveGreen else OfflineRed),
                                                )
                                            }
                                            Text(
                                                text = sub,
                                                fontSize = 11.sp,
                                                color = labelColor,
                                                fontWeight = if (isSelected || isLiveChip || isOfflineChip) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }
                                }
                            }

                            // Country / Region
                            val regions = viewModel.availableRegions
                            FilterSectionLabel(title = "Country")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(regions, key = com.example.ui.viewmodel.RegionItem::code) { (code, name, flag) ->
                                    val isSelected = selectedCountry == code
                                    Surface(
                                        onClick = { viewModel.selectCountry(code) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                        modifier = Modifier.testTag("home_region_chip_$code"),
                                    ) {
                                        Text(
                                            text = "$flag $name",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }

                            // Language
                            val languages = viewModel.availableLanguages
                            FilterSectionLabel(title = "Language")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(languages, key = { it }) { lang ->
                                    val isSelected = selectedLanguage == lang
                                    Surface(
                                        onClick = { viewModel.selectLanguage(lang) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                    ) {
                                        Text(
                                            text = if (lang == "ALL") "All" else lang,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Single Active Filters Summary Bar (only shown when any filter is active)
            if (hasActiveFilters) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("home_active_filters_banner"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val activeLabels = buildList {
                                if (searchQuery.isNotBlank()) add("\"$searchQuery\"")
                                if (selectedGenre != ChannelGenre.ALL.displayName) add(selectedGenre)
                                if (selectedSubcategory != "ALL") add(selectedSubcategory)
                                if (selectedCountry != "ALL") add(selectedCountry)
                                if (selectedLanguage != "ALL") add(selectedLanguage)
                            }.joinToString(" • ")

                            Text(
                                text = activeLabels,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            )

                            Surface(
                                onClick = {
                                    viewModel.resetAllFilters()
                                    showFiltersPanel = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("reset_all_filters_btn"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Native Ad in Live Tab
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    com.example.util.AdNativeView(adUnitId = com.example.util.AdManager.HOME_NATIVE_AD_ID)
                }
            }

            // Recently Watched Section (Quick Resume)
            if (recentlyWatchedChannels.isNotEmpty() && !hasActiveFilters) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recently Watched",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(recentlyWatchedChannels, key = Channel::id) { channel ->
                                RecentlyWatchedCard(
                                    channel = channel,
                                    onResume = { viewModel.playChannel(channel) },
                                )
                            }
                        }
                    }
                }
            }

            // Featured Section (Shown on default browse view)
            if (!hasActiveFilters && recommendedChannels.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Featured",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(recommendedChannels, key = Channel::id) { channel ->
                                RecommendationCard(channel = channel) {
                                    viewModel.playChannel(channel)
                                }
                            }
                        }
                    }
                }
            }

            // Channel Directory Header
            item {
                val headerTitle = when {
                    searchQuery.isNotBlank() -> "Search Results (${filteredChannels.size})"
                    selectedSubcategory == "Live Now" -> "Live Channels (${filteredChannels.size})"
                    selectedSubcategory == "Offline" -> "Offline Channels (${filteredChannels.size})"
                    selectedGenre != ChannelGenre.ALL.displayName -> "$selectedGenre (${filteredChannels.size})"
                    else -> "All Channels (${filteredChannels.size})"
                }
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (filteredChannels.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No channels found for \"$searchQuery\"" else "No channels match your filters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.resetAllFilters()
                                    showFiltersPanel = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Reset Filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                if (columns == 1) {
                    items(filteredChannels, key = Channel::id) { channel ->
                        val isOffline = brokenChannelIds.contains(channel.id) || channel.isOfflineMetadata()
                        ChannelCard(
                            channel = channel,
                            isSelected = currentChannel?.id == channel.id,
                            isLive = !isOffline,
                            isBroken = isOffline,
                            onSelect = { viewModel.playChannel(channel) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            viewModel.toggleFavorite(channel.id)
                        }
                    }
                } else {
                    items(rowChunks, key = { chunk -> chunk.first().id }) { chunkChannels ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (channel in chunkChannels) {
                                Box(modifier = Modifier.weight(1f)) {
                                    val isOffline = brokenChannelIds.contains(channel.id) || channel.isOfflineMetadata()
                                    ChannelCard(
                                        channel = channel,
                                        isSelected = currentChannel?.id == channel.id,
                                        isLive = !isOffline,
                                        isBroken = isOffline,
                                        onSelect = { viewModel.playChannel(channel) },
                                    ) {
                                        viewModel.toggleFavorite(channel.id)
                                    }
                                }
                            }
                            if (chunkChannels.size < columns) {
                                repeat(columns - chunkChannels.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
fun RecommendationCard(
    channel: Channel,
    onSelect: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onSelect)
            .testTag("rec_card_${channel.id}"),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.size(56.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp),
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
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = channel.genre,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun RecentlyWatchedCard(
    channel: Channel,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onResume,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .width(185.dp)
            .testTag("recently_watched_${channel.id}"),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Resume",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
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
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = channel.resolution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
