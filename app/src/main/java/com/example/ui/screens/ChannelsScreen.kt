package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelGenre
import com.example.ui.components.ChannelCard
import com.example.ui.components.EpgGridComponent
import androidx.compose.foundation.clickable
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.SkeuoJewelLed
import com.example.ui.theme.SkeuoPhosphorCyan
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell
import com.example.ui.viewmodel.TVViewModel

@Composable
fun ChannelsScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier
) {
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentProgramsMap by viewModel.currentProgramsMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedCountry by viewModel.selectedCountryFilter.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    var isEpgViewActive by remember { mutableStateOf(false) }

    val regions = viewModel.availableRegions
    val genres = remember { ChannelGenre.entries.map { it.displayName } }
    val languages = viewModel.availableLanguages

    val brokenChannelIds by viewModel.brokenChannelIds.collectAsState()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsState()
    val subcategories = remember(selectedGenre) { viewModel.getSubcategoriesForGenre(selectedGenre) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("channels_screen_root")
    ) {
        // Redesigned User-Friendly Search Bar & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Debossed Skeuomorphic Search Socket
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .skeuomorphicWell(cornerRadius = 12.dp)
                        .clickable { viewModel.openFullScreenSearch() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("channels_search_input")
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) searchQuery else "Search 12,800+ live channels...",
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle between Channel List and EPG Guide Grid (Tactile 3D SkeuoButton)
                SkeuoButton(
                    onClick = { isEpgViewActive = !isEpgViewActive },
                    isAccent = isEpgViewActive,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = if (isEpgViewActive) Icons.AutoMirrored.Filled.ViewList else Icons.Default.CalendarMonth,
                        contentDescription = if (isEpgViewActive) "Channel List" else "EPG Guide",
                        tint = if (isEpgViewActive) Color(0xFF00363D) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // WHEN SEARCHING: Show ONLY the search results header without cluttering filter rows
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
                            text = "Found ${filteredChannels.size} channels for \"$searchQuery\"",
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
                // WHEN NOT SEARCHING: Display Worldwide Regions, Categories, Subcategories, and Languages
                Spacer(modifier = Modifier.height(10.dp))

                // Regions Filter Row (20+ Worldwide Countries)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(regions, key = { it.code }) { region ->
                            val isSelected = selectedCountry == region.code
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectCountry(region.code) },
                                label = {
                                    Text(
                                        text = "${region.flag} ${region.name}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color(0xFF00363D),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Worldwide Categories / Genres Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genres) { genreName ->
                        val isSelected = selectedGenre == genreName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectGenre(genreName)
                                viewModel.selectSubcategory("ALL")
                            },
                            label = { Text(genreName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subcategories Row (Requested by user)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subcategories) { sub ->
                        val isSelected = selectedSubcategory == sub
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSubcategory(sub) },
                            label = { Text(sub, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF06B6D4),
                                selectedLabelColor = Color(0xFF00363D),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Worldwide Languages Filter Row (18+ Languages)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(languages) { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectLanguage(lang) },
                            label = { Text(if (lang == "ALL") "All Languages" else lang, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }
        }

        // Channels Counter and Active Filters Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredChannels.size} Channels Found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedCountry != "ALL") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "Region: $selectedCountry",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (selectedGenre != ChannelGenre.ALL.displayName) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = selectedGenre,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (isEpgViewActive) {
            // Full Electronic Program Guide (EPG) Grid View
            EpgGridComponent(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            )
        } else {
            // Channels List View (Fast & direct rendering)
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No channels match your filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search keywords or switching category/region filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.selectGenre(ChannelGenre.ALL.displayName)
                                viewModel.selectCountry("ALL")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset All Filters", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isSelected = currentChannel?.id == channel.id,
                            currentProgram = currentProgramsMap[channel.id],
                            isLive = !brokenChannelIds.contains(channel.id),
                            isBroken = brokenChannelIds.contains(channel.id),
                            onSelect = { viewModel.playChannel(channel) },
                            onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                            onOpenSchedule = { viewModel.openChannelSchedule(channel) }
                        )
                    }
                }
            }
        }
    }
}
