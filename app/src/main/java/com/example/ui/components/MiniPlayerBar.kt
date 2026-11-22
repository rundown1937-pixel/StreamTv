package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
fun MiniPlayerBar(
    channel: Channel,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit,
    onEnterPip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isBroken: Boolean = false,
) {
    val (cardBg, cardBorder, socketBg, socketBorder) = LocalAppThemeColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Expand ${channel.name} to fullscreen",
                onClick = onExpand,
            )
            .testTag("mini_player_bar"),
    ) {
        val isCompact = maxWidth < 400.dp
        val isUltraCompact = maxWidth < 340.dp
        val logoSize = if (isCompact) 36.dp else 44.dp
        val iconBtnSize = if (isCompact) 36.dp else 40.dp
        val playBtnSize = if (isCompact) 36.dp else 42.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = if (isCompact) 5.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Channel Logo / Thumbnail (matched 10.dp radius)
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(socketBg)
                    .border(1.dp, socketBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = "${channel.name} logo",
                        modifier = Modifier.size(if (isCompact) 30.dp else 36.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))

            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Live / Offline Status Badge (Strictly necessary status colors)
                    val appColors = LocalAppThemeColors.current
                    val isEffectivelyOffline = isBroken || channel.isOfflineMetadata()
                    val statusBgColor = if (isEffectivelyOffline) OfflineRed.copy(alpha = 0.15f) else LiveGreen.copy(alpha = 0.15f)
                    val statusDotColor = if (isEffectivelyOffline) OfflineRed else LiveGreen
                    val statusTextColor = if (isEffectivelyOffline) appColors.statusOfflineText else appColors.statusLiveText

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBgColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isEffectivelyOffline) OfflineRed.copy(alpha = 0.35f) else LiveGreen.copy(alpha = 0.35f),
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor),
                            )
                            Text(
                                text = if (isEffectivelyOffline) "OFFLINE" else "LIVE",
                                color = statusTextColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    Text(
                        text = channel.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // PiP Button (shown unless ultra-narrow portrait where PiP is still in fullscreen controls)
            if (!isUltraCompact) {
                IconButton(
                    onClick = onEnterPip,
                    modifier = Modifier
                        .size(iconBtnSize)
                        .testTag("mini_player_pip_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in Picture",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            // Play / Pause Button
            Surface(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(playBtnSize)
                    .testTag("mini_player_play_pause"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause ${channel.name}" else "Play ${channel.name}",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            // Expand
            IconButton(
                onClick = onExpand,
                modifier = Modifier
                    .size(iconBtnSize)
                    .testTag("mini_player_expand"),
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Expand player to fullscreen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Close
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(iconBtnSize)
                    .testTag("mini_player_close"),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss mini player",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
