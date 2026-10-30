package com.example.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.SkeuoScrew
import com.example.ui.theme.skeuomorphicPanel
import com.example.ui.theme.skeuomorphicWell

@Composable
fun MiniPlayerBar(
    channel: Channel,
    currentProgram: ProgramEntity?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit,
    onEnterPip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .skeuomorphicPanel(cornerRadius = 16.dp, elevation = 10.dp)
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Corner hex screw on rack faceplate
            SkeuoScrew(size = 8.dp, rotationDegrees = 25f, modifier = Modifier.padding(end = 8.dp))

            // Channel Logo / Thumbnail in debossed well
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .skeuomorphicWell(cornerRadius = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Live jewel indicator on top corner
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .align(Alignment.TopEnd)
                ) {
                    SkeuoJewelLed(isActive = isPlaying, color = SkeuoRubyLed, size = 6.dp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Channel & EPG info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentProgram?.title ?: "Live Broadcast • ${channel.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanNeon,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // PiP Button
            IconButton(
                onClick = onEnterPip,
                modifier = Modifier.size(36.dp).testTag("mini_player_pip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tactile Play / Pause SkeuoButton
            SkeuoButton(
                onClick = onTogglePlayPause,
                isAccent = true,
                cornerRadius = 18.dp,
                modifier = Modifier.size(36.dp).testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF00363D),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Expand
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(36.dp).testTag("mini_player_expand")
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Close
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp).testTag("mini_player_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
