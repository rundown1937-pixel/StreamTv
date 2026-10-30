package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.SkeuoJewelLed
import com.example.ui.theme.SkeuoRubyLed
import com.example.ui.theme.SkeuoEmeraldLed
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoScrew
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell

@Composable
fun ChannelCard(
    channel: Channel,
    isSelected: Boolean,
    currentProgram: ProgramEntity? = null,
    isLive: Boolean = true,
    isBroken: Boolean = false,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSchedule: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .skeuomorphicPanel(
                cornerRadius = 14.dp,
                elevation = if (isSelected) 8.dp else 4.dp,
                highlightAlpha = if (isSelected) 0.5f else 0.25f,
                shadowAlpha = 0.9f
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(CyanNeon, CyanNeon.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        RoundedCornerShape(14.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onSelect)
            .testTag("channel_card_${channel.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Logo / Icon in debossed well
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .skeuomorphicWell(cornerRadius = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(42.dp)
                                .padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (channel.isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Channel",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Channel Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (channel.isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFB45309).copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "PIN",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live / Broken Status with SkeuoJewelLed
                        if (isBroken) {
                            SkeuoJewelLed(
                                isActive = true,
                                color = SkeuoRubyLed,
                                label = "OFFLINE",
                                size = 8.dp
                            )
                        } else {
                            SkeuoJewelLed(
                                isActive = isLive,
                                color = SkeuoEmeraldLed,
                                label = "LIVE",
                                size = 8.dp
                            )
                        }

                        // Genre badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = channel.genre,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Resolution badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = channel.resolution,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // Country tag
                        if (channel.country.isNotEmpty() && channel.country != "GLOBAL") {
                            Text(
                                text = channel.country,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Direct Play Button (Tactile SkeuoButton)
                SkeuoButton(
                    onClick = onSelect,
                    isAccent = true,
                    cornerRadius = 8.dp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .testTag("play_button_${channel.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Channel",
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Play",
                            color = Color(0xFF00363D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // EPG Schedule Button
                IconButton(
                    onClick = onOpenSchedule,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("schedule_button_${channel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Program Guide Schedule",
                        tint = CyanNeon.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Favorite button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("fav_button_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (channel.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (channel.isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Electronic Program Guide (EPG) Live Schedule Row
            if (currentProgram != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSchedule)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "NOW: ${currentProgram.title}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            val rem = currentProgram.getRemainingMinutes(now)
                            Text(
                                text = "${rem}m left",
                                fontSize = 10.sp,
                                color = CyanNeon,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val progFraction = currentProgram.getProgressFraction(now)
                        LinearProgressIndicator(
                            progress = { progFraction },
                            color = CyanNeon,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }
        }
    }
}
