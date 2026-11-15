package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
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
import com.example.ui.theme.LiveGreenText
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.OfflineRed
import com.example.ui.theme.OfflineRedText
import com.example.ui.viewmodel.TVViewModel
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

@Composable
fun HomeScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier,
) {
    val currentChannel by viewModel.currentChannel.collectAsState()
    val activeProfile by viewModel.profileRepo.activeProfile.collectAsState()

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
    val hasActiveFilters = (selectedGenre != ChannelGenre.ALL.displayName) || (selectedCountry != "ALL") || (selectedLanguage != "ALL") || (selectedSubcategory != "ALL") || searchQuery.isNotBlank()

    val (_, _, socketBg, socketBorder) = LocalAppThemeColors.current

    val favoriteChannels = remember(filteredChannels) { filteredChannels.filter(Channel::isFavorite) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = with(density) {
        androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp().value.toInt()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Native Ad in Live Tab (ca-app-pub-2382308752605431/3199058069)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                com.example.util.AdNativeView(adUnitId = com.example.util.AdManager.HOME_NATIVE_AD_ID)
            }
        }

        // Search Bar Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                // Full Screen Search Trigger Bar (Clean 12.dp rounded bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(socketBg)
                        .border(1.dp, socketBorder, RoundedCornerShape(12.dp))
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { viewModel.openFullScreenSearch() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = searchQuery.ifBlank { "Search live channels, genres..." },
                            color = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Found ${filteredChannels.size} live channels for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.setSearchQuery("") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                        items(quickTags, key = { it }) { tag ->
                            val isSelected = searchQuery.equals(tag, ignoreCase = true)
                            Surface(
                                onClick = {
                                    viewModel.setSearchQuery(tag)
                                    viewModel.openFullScreenSearch()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Worldwide Categories / Genres Filter Row (Hidden when searching)
        if (searchQuery.isBlank()) {
            item {
                val genres = ChannelGenre.entries.map { it.displayName }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                ) {
                    items(genres, key = { it }) { genreName ->
                        val isSelected = selectedGenre == genreName
                        Surface(
                            onClick = {
                                viewModel.selectGenre(genreName)
                                viewModel.selectSubcategory("ALL")
                            },
                            shape = RoundedCornerShape(8.dp),
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
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        // Subcategories Filter Row (Hidden when searching)
        if (searchQuery.isBlank()) {
            item {
                val subcategories = viewModel.getSubcategoriesForGenre(selectedGenre)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                ) {
                    items(subcategories, key = { it }) { sub ->
                        val isSelected = selectedSubcategory == sub
                        val isLiveChip = sub == "Live Now"
                        val isOfflineChip = sub == "Offline"
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSubcategory(sub) },
                            shape = RoundedCornerShape(8.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isLiveChip) LiveGreen.copy(alpha = 0.5f)
                                              else if (isOfflineChip) OfflineRed.copy(alpha = 0.5f)
                                              else MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = if (isLiveChip) LiveGreen
                                                      else if (isOfflineChip) OfflineRed
                                                      else MaterialTheme.colorScheme.tertiary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp,
                            ),
                            leadingIcon = if (isLiveChip || isOfflineChip) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isLiveChip) LiveGreen else OfflineRed),
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = sub,
                                    fontSize = 11.sp,
                                    color = if (isLiveChip) {
                                        if (isSelected) LiveGreen else LiveGreenText
                                    } else if (isOfflineChip) {
                                        if (isSelected) OfflineRed else OfflineRedText
                                    } else {
                                        if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (isSelected || isLiveChip || isOfflineChip) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isLiveChip) LiveGreen.copy(alpha = 0.14f)
                                                 else if (isOfflineChip) OfflineRed.copy(alpha = 0.14f)
                                                 else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isLiveChip) LiveGreenText
                                             else if (isOfflineChip) OfflineRedText
                                             else MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = if (isLiveChip) LiveGreen.copy(alpha = 0.28f)
                                                         else if (isOfflineChip) OfflineRed.copy(alpha = 0.28f)
                                                         else MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = if (isLiveChip) LiveGreen
                                                     else if (isOfflineChip) OfflineRed
                                                     else MaterialTheme.colorScheme.onTertiary,
                            ),
                        )
                    }
                }
            }
        }

        // Worldwide Regions Filter Chips (Quick country selector)
        if (searchQuery.isBlank()) {
            item {
                val regions = viewModel.availableRegions
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        // Worldwide Languages Filter Row (83 Languages)
        if (searchQuery.isBlank()) {
            item {
                val languages = viewModel.availableLanguages
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                ) {
                    items(languages, key = { it }) { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectLanguage(lang) },
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(if (lang == "ALL") "All Languages" else lang, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }

        if (hasActiveFilters && searchQuery.isBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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
                            if (selectedGenre != ChannelGenre.ALL.displayName) add("Genre: $selectedGenre")
                            if (selectedCountry != "ALL") add("Region: $selectedCountry")
                            if (selectedLanguage != "ALL") add("Lang: $selectedLanguage")
                            if (selectedSubcategory != "ALL") add(selectedSubcategory)
                        }.joinToString(" • ")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filtered: $activeLabels",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.resetAllFilters() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.testTag("reset_all_filters_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        .padding(vertical = 8.dp),
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
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recently Watched",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Quick Resume",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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

        // Personalized Recommendations Section
        if ((searchQuery.isBlank()) && (selectedGenre == ChannelGenre.ALL.displayName)) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tailored For You",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${activeProfile?.name ?: "Family"}'s Picks",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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

        // Favorites Row (if any)
        if ((favoriteChannels.isNotEmpty()) && (selectedGenre != ChannelGenre.FAVORITES.displayName)) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Favorites (${favoriteChannels.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favoriteChannels, key = Channel::id) { channel ->
                            RecommendationCard(channel = channel) {
                                viewModel.playChannel(channel)
                            }
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val headerTitle = when {
                    searchQuery.isNotEmpty() -> "Search Results (${filteredChannels.size})"
                    selectedSubcategory == "Live Now" -> "Live Channels (${filteredChannels.size})"
                    selectedSubcategory == "Offline" -> "Offline Channels (${filteredChannels.size})"
                    else -> "All Channels (${filteredChannels.size})"
                }
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (hasActiveFilters) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.resetAllFilters() },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Filters",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            label = {
                                Text(
                                    text = "Reset Filters",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            ),
                            modifier = Modifier.testTag("home_header_reset_filters_chip"),
                        )
                    }

                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleChannelSortOrder() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Direction",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(
                                text = channelSortOrder.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.testTag("home_sort_chip"),
                    )
                }
            }
        }

        if (filteredChannels.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
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
                            text = "No channels found matching '$searchQuery'",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different genre or country tag, or tap below to reset filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.resetAllFilters() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Reset Search Filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Channel Cards List (Adapts columns dynamically to device screen width)
            // On landscape phones (typically 750-950dp after 80dp rail), 2 columns gives ~380-450dp per card.
            // 3 columns requires >= 1150dp available content width (large landscape tablets).
            val availableWidthDp = if (screenWidthDp >= 600) screenWidthDp - 80 else screenWidthDp
            val columns = when {
                availableWidthDp >= 1150 -> 3
                availableWidthDp >= 580 -> 2
                else -> 1
            }

            val homeDisplayList = filteredChannels

            if (columns == 1) {
                items(homeDisplayList, key = Channel::id) { channel ->
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
                val rowChunks = homeDisplayList.chunked(columns)
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
