package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.SkeuoEmeraldLed
import com.example.ui.theme.SkeuoJewelLed
import com.example.ui.theme.SkeuoScrew
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TVViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfilesScreen(
    viewModel: TVViewModel,
    onOpenAddProfileDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profileRepo.profiles.collectAsState()
    val activeProfile by viewModel.profileRepo.activeProfile.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profiles_screen_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profiles Header & Switcher Row
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Household Profiles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SkeuoButton(
                        onClick = onOpenAddProfileDialog,
                        isAccent = true,
                        modifier = Modifier.testTag("profiles_add_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color(0xFF00363D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Profile", color = Color(0xFF00363D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profiles) { profile ->
                        val isSelected = activeProfile?.id == profile.id

                        Box(
                            modifier = Modifier
                                .width(124.dp)
                                .then(
                                    if (isSelected) Modifier
                                        .skeuomorphicWell(cornerRadius = 16.dp)
                                        .border(2.dp, CyanNeon, RoundedCornerShape(16.dp))
                                    else Modifier.skeuomorphicPanel(cornerRadius = 16.dp, elevation = 4.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.profileRepo.setActiveProfile(profile) }
                                .padding(12.dp)
                                .testTag("profile_card_${profile.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(profile.avatarBgColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = profile.avatarEmoji, fontSize = 28.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isSelected) {
                                    SkeuoJewelLed(isActive = true, color = SkeuoEmeraldLed, label = "ACTIVE", size = 7.dp)
                                } else if (profile.isKidsProfile) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "KIDS",
                                            fontSize = 9.sp,
                                            color = CyanNeon,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Profile Settings Card
        activeProfile?.let { current ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .skeuomorphicPanel(cornerRadius = 16.dp, elevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeuoScrew(size = 9.dp, rotationDegrees = 33f)
                            SkeuoJewelLed(isActive = true, color = SkeuoEmeraldLed, label = "PROFILE ACTIVE")
                            SkeuoScrew(size = 9.dp, rotationDegrees = 125f)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .skeuomorphicWell(cornerRadius = 24.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color(current.avatarBgColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = current.avatarEmoji, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = current.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${current.favorites.size} Favorites • ${current.watchHistory.size} Channels in History",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (profiles.size > 1) {
                                IconButton(
                                    onClick = { viewModel.profileRepo.deleteProfile(current.id) },
                                    modifier = Modifier.testTag("delete_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Kids Mode Toggle for Active Profile inside debossed well
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .skeuomorphicWell(cornerRadius = 10.dp)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Child Safe Mode",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Automatically hides mature channels & blocks 18+ content without PIN",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = current.isKidsProfile,
                                    onCheckedChange = { isKids ->
                                        viewModel.profileRepo.updateProfile(current.copy(isKidsProfile = isKids))
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon),
                                    modifier = Modifier.testTag("profile_kids_toggle")
                                )
                            }
                        }
                    }
                }
            }

            // Watch History for this profile
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Viewing History (${current.watchHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (current.watchHistory.isNotEmpty()) {
                        SkeuoButton(
                            onClick = {
                                viewModel.profileRepo.updateProfile(current.copy(watchHistory = emptyList()))
                            }
                        ) {
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            if (current.watchHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .skeuomorphicWell(cornerRadius = 12.dp)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No watch history for ${current.name} yet.\nStart streaming channels to see history and tailored recommendations!",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(current.watchHistory) { entry ->
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    val timeStr = sdf.format(Date(entry.timestamp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .skeuomorphicPanel(cornerRadius = 12.dp, elevation = 3.dp)
                            .clickable {
                                val channel = viewModel.playlistRepo.channels.value.firstOrNull { it.id == entry.channelId }
                                if (channel != null) {
                                    viewModel.playChannel(channel)
                                    viewModel.selectTab(ScreenTab.HOME)
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyanNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.channelName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Watched $timeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = CyanNeon
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
