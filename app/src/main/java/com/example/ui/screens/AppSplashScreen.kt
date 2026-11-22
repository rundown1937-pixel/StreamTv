package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkeuoButton
import com.example.ui.viewmodel.TVViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Clean modern countdown splash screen with static graphics (no CPU-draining looping animations).
 */
@Composable
fun AppSplashScreen(
    viewModel: TVViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val channels by viewModel.allChannels.collectAsState()
    val channelCount = channels.size
    val isFullPlaylistLoaded by viewModel.playlistRepo.isFullPlaylistLoaded.collectAsState()

    // Smooth progress representation
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(channelCount, isFullPlaylistLoaded) {
        if (isFullPlaylistLoaded || (channelCount >= 10000)) {
            progressAnim.animateTo(1f, animationSpec = tween(durationMillis = 150, easing = LinearEasing))
            delay(100.milliseconds)
            onFinished()
        } else {
            val approxProgress = (channelCount.toFloat() / 12000f).coerceIn(0.1f, 0.95f)
            progressAnim.animateTo(approxProgress, animationSpec = tween(durationMillis = 150, easing = LinearEasing))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_countdown_splash_screen"),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            val isLandscape = maxWidth > maxHeight
            val splashInsets = WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout).let { all ->
                if (isLandscape) all else all.only(WindowInsetsSides.Vertical)
            }
            val isCompactLandscape = isLandscape && (maxHeight < 520.dp)
            val dialSize = if (isCompactLandscape) 104.dp else 132.dp
            val logoOuterSize = if (isCompactLandscape) 76.dp else 92.dp
            val logoIconSize = if (isCompactLandscape) 40.dp else 48.dp

            val brandSection: @Composable (Modifier) -> Unit = { sectionModifier ->
                Column(
                    modifier = sectionModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(logoOuterSize)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = "StreamPulse TV Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(logoIconSize),
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 10.dp else 18.dp))

                    Text(
                        text = "STREAMPULSE TV",
                        style = (if (isCompactLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium).copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.SansSerif,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Live TV Worldwide",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            val progressSection: @Composable (Modifier) -> Unit = { sectionModifier ->
                Column(
                    modifier = sectionModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Center Dial Indicator
                    Box(
                        modifier = Modifier.size(dialSize + 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnim.value },
                            modifier = Modifier.size(dialSize),
                            color = if (isFullPlaylistLoaded) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 5.dp,
                            strokeCap = StrokeCap.Round,
                        )

                        AnimatedContent(
                            targetState = isFullPlaylistLoaded,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.8f))
                                    .togetherWith(fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 1.2f))
                            },
                            label = "load_status",
                        ) { ready ->
                            if (!ready) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    val percentage = (progressAnim.value * 100).toInt()
                                    Text(
                                        text = "$percentage%",
                                        style = (if (isCompactLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall).copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.testTag("scan_progress_text"),
                                    )
                                    Text(
                                        text = "LOADING",
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
                                        modifier = Modifier.size(if (isCompactLandscape) 34.dp else 44.dp),
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

                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 14.dp else 24.dp))

                    // Loading Progress Bar
                    LinearProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("channel_loading_progress_bar"),
                        color = if (isFullPlaylistLoaded) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 10.dp else 16.dp))

                    // Live Channels Counter Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(horizontal = 8.dp),
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
                                    "$channelCount channels ready"
                                } else {
                                    "Loading channels..."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 14.dp else 20.dp))

                    // "Enter TV" button - Always active so user can enter immediately
                    SkeuoButton(
                        onClick = onFinished,
                        isAccent = true,
                        cornerRadius = 12.dp,
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("enter_tv_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enter TV",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(splashInsets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isCompactLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        brandSection(Modifier.weight(1f).padding(end = 16.dp))
                        progressSection(Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        brandSection(Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(28.dp))
                        progressSection(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
