package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.isOfflineMetadata
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.OfflineRed


@Composable
fun ChannelCard(
    channel: Channel,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isLive: Boolean = true,
    isBroken: Boolean = false,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val appColors = LocalAppThemeColors.current
    val (cardBg, cardBorder, socketBg, socketBorder) = appColors
    val isOfflineByMeta = channel.isOfflineMetadata()
    val isEffectivelyOffline = isBroken || !isLive || isOfflineByMeta
    val statusBgColor = if (isEffectivelyOffline) OfflineRed.copy(alpha = 0.15f) else LiveGreen.copy(alpha = 0.15f)
    val statusDotColor = if (isEffectivelyOffline) OfflineRed else LiveGreen
    val statusTextColor = if (isEffectivelyOffline) appColors.statusOfflineText else appColors.statusLiveText
    val statusText = if (isEffectivelyOffline) "OFFLINE" else if (isSelected) "STREAMING" else "LIVE"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else cardBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = "Play ${channel.name}",
                onClick = onSelect,
            )
            .semantics {
                stateDescription = if (isSelected) "Currently selected" else "Not selected"
            }
            .testTag("channel_card_${channel.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Channel Logo / Icon Container (flattened, matched 12.dp corner radius)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(socketBg)
                        .border(1.dp, socketBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = "${channel.name} logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(42.dp)
                                .padding(4.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    if (channel.isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Channel",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Live / Offline Status dot on thumbnail
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Channel Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        if (channel.isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "PIN Protected",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Live / Offline Status Badge (Strictly necessary status colors)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBgColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEffectivelyOffline) OfflineRed.copy(alpha = 0.35f) else LiveGreen.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier.testTag("status_badge_${channel.id}"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor),
                                )
                                Text(
                                    text = statusText,
                                    color = statusTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }

                        // Genre badge (bounded with weight to prevent vertical character stacking)
                        if (channel.genre.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Text(
                                    text = channel.genre,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        // Resolution badge (matched 8.dp radius)
                        if (channel.resolution.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = channel.resolution,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }

                        // Country tag
                        if (channel.country.isNotEmpty() && (channel.country != "GLOBAL")) {
                            Text(
                                text = channel.country,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                // Direct Play Button (matched 10.dp radius, WCAG >= 48dp touch target)
                Surface(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .testTag("play_button_${channel.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isSelected) "Playing" else "Play",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // Favorite button (WCAG >= 48dp touch target)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fav_button_${channel.id}"),
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (channel.isFavorite) "Remove ${channel.name} from favorites" else "Add ${channel.name} to favorites",
                        tint = if (channel.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
