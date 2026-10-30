package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.ui.components.ChannelCard
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.SkeuoScrew
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TVViewModel

@Composable
fun FavoritesScreen(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier
) {
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val currentProgramsMap by viewModel.currentProgramsMap.collectAsState()
    val brokenChannelIds by viewModel.brokenChannelIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("ALL") }

    val genres = remember(favoriteChannels) {
        listOf("ALL") + favoriteChannels.map { it.genre }.distinct().sorted()
    }

    val displayList = remember(favoriteChannels, searchQuery, selectedGenre) {
        favoriteChannels.filter { ch ->
            val genreMatch = selectedGenre == "ALL" || ch.genre.equals(selectedGenre, ignoreCase = true)
            val queryMatch = searchQuery.isBlank() || ch.name.contains(searchQuery, ignoreCase = true) ||
                    ch.country.contains(searchQuery, ignoreCase = true) ||
                    ch.language.contains(searchQuery, ignoreCase = true)
            genreMatch && queryMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("favorites_screen_root")
    ) {
        // Top Header (Skeuomorphic Console Faceplate)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .skeuomorphicPanel(cornerRadius = 0.dp, elevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeuoScrew(size = 9.dp, rotationDegrees = 30f, modifier = Modifier.padding(end = 8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .skeuomorphicWell(cornerRadius = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorites",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bookmarked Favorites",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${favoriteChannels.size} channels saved for quick access",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (favoriteChannels.isNotEmpty()) {
                        SkeuoButton(
                            onClick = {
                                val first = displayList.firstOrNull() ?: favoriteChannels.first()
                                viewModel.playChannel(first, directLandscape = true)
                            },
                            isAccent = true,
                            cornerRadius = 8.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play Live",
                                    color = Color(0xFF00363D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (favoriteChannels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Search within Favorites
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter your favorites...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    if (genres.size > 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(genres) { g ->
                                val isSelected = selectedGenre.equals(g, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGenre = g },
                                    label = { Text(g, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon,
                                        selectedLabelColor = Color(0xFF00363D),
                                        containerColor = DarkSurfaceElevated,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites Content
        if (favoriteChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Bookmarked Channels Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the Star icon on any channel card or in player controls to bookmark it here for quick 1-tap playback in landscape mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.selectTab(ScreenTab.CHANNELS) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browse 12,800+ Channels",
                            color = Color(0xFF00363D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No favorites match \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedGenre = "ALL"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Text("Reset Filter", color = Color(0xFF00363D))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = displayList,
                    key = { it.id }
                ) { channel ->
                    ChannelCard(
                        channel = channel.copy(isFavorite = true),
                        isSelected = currentChannel?.id == channel.id,
                        currentProgram = currentProgramsMap[channel.id],
                        isLive = !brokenChannelIds.contains(channel.id),
                        isBroken = brokenChannelIds.contains(channel.id),
                        onSelect = { viewModel.playChannel(channel, directLandscape = true) },
                        onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                        onOpenSchedule = { viewModel.openChannelSchedule(channel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
