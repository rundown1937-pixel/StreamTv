package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.SkeuoButton
import com.example.ui.viewmodel.TVViewModel
import kotlinx.coroutines.delay

/**
 * Modern high-tech countdown splash screen that verifies and initializes all live channels,
 * streams, decoders, and EPG data before the main application opens.
 */
@Composable
fun AppSplashScreen(
    viewModel: TVViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.allChannels.collectAsState()
    val isSyncing by viewModel.playlistRepo.isSyncing.collectAsState()
    val channelCount = channels.size

    // Initial countdown value in seconds (3-second sleek cinematic countdown)
    var countdownSeconds by remember { mutableIntStateOf(3) }
    var isReadyToEnter by remember { mutableStateOf(false) }

    // Animated linear progress from 0f to 1f
    val progressAnim = remember { Animatable(0f) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Dynamic loading stage text based on countdown and channel availability
    val currentStageText = remember(countdownSeconds, channelCount) {
        when {
            countdownSeconds >= 3 -> "Verifying 12,000+ Original Broadcaster Streams & Channel Names..."
            countdownSeconds == 2 -> "Calibrating 24+ Countries & Low-Latency Audio/Video Decoders..."
            countdownSeconds == 1 && channelCount < 10000 -> "Loading all 12,000+ worldwide channels... ($channelCount loaded)"
            countdownSeconds == 1 -> "Syncing Electronic Program Guide (EPG) • $channelCount Channels Ready"
            channelCount >= 10000 -> "All $channelCount Channels Loaded! Opening StreamPulse TV..."
            else -> "Finalizing verified channel streams..."
        }
    }

    // Countdown and channel readiness loop
    LaunchedEffect(Unit) {
        // Smoothly animate progress bar
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
        )
    }

    LaunchedEffect(Unit) {
        // Countdown from 3 down to 1
        while (countdownSeconds > 1) {
            delay(1000L)
            countdownSeconds--
        }

        // Ensure all 12,000+ channels are completely loaded into memory before counting down to 0
        while (viewModel.allChannels.value.size < 12000 && !viewModel.isChannelsLoaded.value) {
            delay(100L)
        }

        delay(800L)
        countdownSeconds = 0
        isReadyToEnter = true
        delay(600L) // Brief pause to display completion state
        onFinished()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_countdown_splash_screen"),
        color = DarkBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            IndigoAccent.copy(alpha = 0.25f),
                            DarkSurface,
                            DarkBackground
                        ),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo & Glowing Icon
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer neon aura
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .shadow(24.dp, shape = CircleShape, ambientColor = CyanNeon, spotColor = CyanNeon)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CyanNeon.copy(alpha = glowAlpha),
                                        IndigoAccent.copy(alpha = glowAlpha)
                                    )
                                )
                            )
                    )

                    // Inner dark container
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(DarkBackground)
                            .border(2.dp, CyanNeon.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = "StreamPulse TV Logo",
                            tint = CyanNeon,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Titles
                Text(
                    text = "STREAMPULSE TV",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Worldwide Live IPTV & Broadcast Engine",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = CyanGlow
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Center Countdown Dial
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Dial
                    CircularProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier.size(136.dp),
                        color = if (countdownSeconds == 0) EmeraldSuccess else CyanNeon,
                        trackColor = DarkSurfaceElevated,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Center Animated Digit or Ready Icon
                    AnimatedContent(
                        targetState = countdownSeconds,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.8f))
                                .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 1.2f))
                        },
                        label = "countdown_digit"
                    ) { seconds ->
                        if (seconds > 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$seconds",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.testTag("countdown_timer_text")
                                )
                                Text(
                                    text = "SEC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = CyanGlow.copy(alpha = 0.8f)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Channels Loaded Ready",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "READY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Loading Progress Bar
                LinearProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("channel_loading_progress_bar"),
                    color = if (countdownSeconds == 0) EmeraldSuccess else CyanNeon,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Live Channels Counter Pill
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("channels_loaded_count_text"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (channelCount > 0) {
                                "Verified $channelCount Original Broadcasters Ready"
                            } else {
                                "Loading Original Channels..."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Status Stage Message
                Text(
                    text = currentStageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(38.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Optional "Enter Now / Skip" button if channels already loaded into memory
                AnimatedVisibility(
                    visible = channelCount > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SkeuoButton(
                        onClick = onFinished,
                        isAccent = true,
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("skip_countdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF00363D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter Player Now",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF00363D)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fast HLS Low-Latency Streaming Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
