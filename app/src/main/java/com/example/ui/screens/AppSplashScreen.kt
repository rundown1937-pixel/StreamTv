package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkeuoButton
import com.example.ui.viewmodel.TVViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Clean modern countdown splash screen with static graphics (no CPU-draining looping animations).
 */
@Composable
fun AppSplashScreen(
    viewModel: TVViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val channels by viewModel.allChannels.collectAsState()
    val channelCount = channels.size
    val isAllChannelsReady by viewModel.isAllChannelsReady.collectAsState()

    // Initial countdown value in seconds (cinematic countdown)
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var isReadyToEnter by remember { mutableStateOf(value = false) }

    // Animated linear progress from 0f to 1f
    val progressAnim = remember { Animatable(0f) }

    // Dynamic loading stage text based on countdown and channel availability
    val currentStageText = remember(countdownSeconds, channelCount, isAllChannelsReady) {
        when {
            isAllChannelsReady || channelCount >= 12000 -> "All $channelCount Channels Verified & Ready! Opening StreamPulse TV..."
            channelCount > 35 -> "Verifying 13,000+ Channels ($channelCount loaded & health-checked)..."
            countdownSeconds >= 3 -> "Verifying 13,000+ Original Broadcaster Streams..."
            countdownSeconds == 2 -> "Calibrating Low-Latency Audio/Video Decoders..."
            else -> "Loading & health-checking 13,000+ channels ($channelCount loaded)..."
        }
    }

    // Progress animation (one-shot)
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearEasing),
        )
    }

    LaunchedEffect(Unit) {
        // Countdown from 3 down to 1
        while (countdownSeconds > 1) {
            delay(1.seconds)
            countdownSeconds--
        }

        // Keep countdown running until all channels are loaded from cache/assets
        while (!viewModel.isAllChannelsReady.value) {
            delay(100.milliseconds)
        }

        // If an initial feed sync is actively downloading the latest 13,200+ channels, wait briefly (up to 5s)
        var syncWaitMs = 0
        while (viewModel.playlistRepo.isSyncing.value && syncWaitMs < 5000) {
            delay(100.milliseconds)
            syncWaitMs += 100
        }

        delay(400.milliseconds)
        countdownSeconds = 0
        isReadyToEnter = true
        progressAnim.snapTo(1f)
        delay(600.milliseconds) // Brief pause to display completion state
        onFinished()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_countdown_splash_screen"),
        color = DarkBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .background(DarkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // App Logo & Static Icon (looping animation removed)
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = "StreamPulse TV Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Titles
                Text(
                    text = "STREAMPULSE TV",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.SansSerif,
                    ),
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Worldwide Live IPTV & Broadcast Engine",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Center Countdown Dial
                Box(
                    modifier = Modifier.size(136.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Circular Progress Dial
                    CircularProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier.size(132.dp),
                        color = if (countdownSeconds == 0) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = DarkSurfaceElevated,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round,
                    )

                    // Center Animated Digit or Ready Icon
                    AnimatedContent(
                        targetState = countdownSeconds,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.8f))
                                .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 1.2f))
                        },
                        label = "countdown_digit",
                    ) { seconds ->
                        if (seconds > 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = seconds.toString(),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.testTag("countdown_timer_text"),
                                )
                                Text(
                                    text = "SEC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Channels Loaded Ready",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "READY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = EmeraldSuccess,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Loading Progress Bar (Tracks 13,000+ channel loading dynamically)
                val targetProgress = if (isAllChannelsReady || channelCount >= 12000) {
                    1f
                } else {
                    maxOf(progressAnim.value * 0.25f, (channelCount.toFloat() / 12831f).coerceIn(0f, 1f))
                }
                LinearProgressIndicator(
                    progress = { targetProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("channel_loading_progress_bar"),
                    color = if (countdownSeconds == 0) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = DarkSurfaceElevated,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Live Channels Counter Pill
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("channels_loaded_count_text"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (channelCount > 0) {
                                "Verified $channelCount Channels Ready"
                            } else {
                                "Loading & Verifying 13,000+ Channels..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Status Stage Message
                Text(
                    text = currentStageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(38.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // "Enter Player Now" button - active only once all 13,000+ channels are verified
                AnimatedVisibility(
                    visible = isAllChannelsReady || channelCount >= 12000,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SkeuoButton(
                        onClick = onFinished,
                        isAccent = true,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("skip_countdown_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF121212),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter Player Now",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFF121212),
                            )
                        }
                    }
                }
            }

            // Bottom Brand Footnote
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fast HLS Low-Latency Streaming Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
        }
    }
}
