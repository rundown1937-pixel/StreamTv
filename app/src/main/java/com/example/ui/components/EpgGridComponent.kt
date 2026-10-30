package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ProgramEntity
import com.example.data.model.Channel
import com.example.data.model.ChannelGenre
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LiveRed
import com.example.ui.viewmodel.TVViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EpgGridComponent(
    viewModel: TVViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.filteredChannels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentProgramsMap by viewModel.currentProgramsMap.collectAsState()

    var epgSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember {
        listOf(
            ChannelGenre.ALL.displayName,
            ChannelGenre.NEWS.displayName,
            ChannelGenre.SPORTS.displayName,
            ChannelGenre.MOVIES.displayName,
            ChannelGenre.MUSIC.displayName,
            ChannelGenre.ENTERTAINMENT.displayName,
            ChannelGenre.KIDS.displayName,
            ChannelGenre.DOCUMENTARY.displayName
        )
    }

    val filteredList = remember(channels, epgSearchQuery, selectedCategory) {
        channels.filter { ch ->
            val catMatches = if (selectedCategory == "All") true else ch.genre.equals(selectedCategory, ignoreCase = true)
            val queryMatches = if (epgSearchQuery.isBlank()) true else {
                ch.name.contains(epgSearchQuery, ignoreCase = true) ||
                        ch.genre.contains(epgSearchQuery, ignoreCase = true) ||
                        ch.country.contains(epgSearchQuery, ignoreCase = true)
            }
            catMatches && queryMatches
        }
    }

    val now = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Generate timeline slots (e.g., 6 half-hour intervals from current time)
    val timeSlots = remember(now) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            val minute = get(Calendar.MINUTE)
            set(Calendar.MINUTE, if (minute < 30) 0 else 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val slots = mutableListOf<Long>()
        for (i in 0 until 8) {
            slots.add(cal.timeInMillis)
            cal.add(Calendar.MINUTE, 30)
        }
        slots
    }

    val horizontalGridScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("epg_grid_root")
    ) {
        // EPG Header & Category Filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "EPG",
                        tint = CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Electronic Program Guide (EPG)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${filteredList.size} channels",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick EPG Search
            OutlinedTextField(
                value = epgSearchQuery,
                onValueChange = { epgSearchQuery = it },
                placeholder = { Text("Filter guide channels & shows...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanNeon, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (epgSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { epgSearchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("epg_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
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

        // Timeline Header Row (Sticky Header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Column Header
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "CHANNELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
            }

            // Horizontally Scrollable Time Slots Header
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalGridScrollState)
            ) {
                timeSlots.forEach { slotTime ->
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeFormat.format(Date(slotTime)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Channels & EPG Grid Rows
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No channels match the EPG filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("epg_grid_channel_list")
            ) {
                items(filteredList, key = { it.id }) { channel ->
                    val isCurrent = currentChannel?.id == channel.id
                    val currentProg = currentProgramsMap[channel.id]

                    EpgChannelRow(
                        channel = channel,
                        isCurrent = isCurrent,
                        currentProgram = currentProg,
                        timeSlots = timeSlots,
                        horizontalScrollState = horizontalGridScrollState,
                        onChannelClick = {
                            viewModel.playChannel(channel)
                        },
                        onOpenSchedule = {
                            viewModel.openChannelSchedule(channel)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: Channel,
    isCurrent: Boolean,
    currentProgram: ProgramEntity?,
    timeSlots: List<Long>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onChannelClick: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Channel Header
        Row(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight()
                .clickable(onClick = onChannelClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) CyanNeon else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${channel.country} • ${channel.genre}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right Area: Synchronized horizontal timeline program blocks
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timeSlots.forEachIndexed { index, slotTime ->
                val slotTitle = when {
                    index == 0 && currentProgram != null -> currentProgram.title
                    index == 0 -> "${channel.name} Live Broadcast"
                    index == 1 -> "Next: ${channel.genre} Special Edition"
                    index == 2 -> "${channel.name} Prime Report"
                    index == 3 -> "Global Highlights & Analysis"
                    index == 4 -> "Late Night ${channel.genre} Showcase"
                    index == 5 -> "Overnight Replay & Coverage"
                    else -> "Upcoming Feature Presentation"
                }

                val isLiveSlot = index == 0

                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLiveSlot) {
                            if (isCurrent) CyanNeon.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isLiveSlot && isCurrent) CyanNeon else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .width(180.dp)
                        .height(64.dp)
                        .padding(horizontal = 3.dp)
                        .clickable(onClick = {
                            if (isLiveSlot) {
                                onChannelClick()
                            } else {
                                onOpenSchedule()
                            }
                        })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeFormat.format(Date(slotTime)),
                                fontSize = 10.sp,
                                color = if (isLiveSlot) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )

                            if (isLiveSlot) {
                                Surface(
                                    color = LiveRed,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = slotTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isLiveSlot && currentProgram != null) {
                            LinearProgressIndicator(
                                progress = { currentProgram.getProgressFraction() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = CyanNeon,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        } else {
                            Text(
                                text = "${channel.genre} • PG",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
