package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.example.data.local.ProgramEntity
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LiveRed
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.PlaybackQuality
import kotlinx.coroutines.delay

private val playerOkHttpClient: OkHttpClient by lazy {
    try {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    } catch (e: Exception) {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    streamUrl: String?,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    aspectRatioMode: AspectRatioMode,
    playbackQuality: PlaybackQuality,
    subtitlesEnabled: Boolean,
    isCasting: Boolean,
    castDeviceName: String?,
    isFavorite: Boolean,
    isLocked: Boolean,
    isPipMode: Boolean = false,
    httpUserAgent: String? = null,
    httpReferrer: String? = null,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCycleQuality: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onOpenCast: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onPreviousChannel: (() -> Unit)? = null,
    onNextChannel: (() -> Unit)? = null,
    onSelectAspectRatio: ((AspectRatioMode) -> Unit)? = null,
    onSelectQuality: ((PlaybackQuality) -> Unit)? = null,
    currentProgram: ProgramEntity? = null,
    nextProgram: ProgramEntity? = null,
    isMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    sleepTimerMinutes: Int? = null,
    onSetSleepTimer: ((Int?) -> Unit)? = null,
    onPlaybackStatusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    var streamFallbackMode by remember(streamUrl) { mutableStateOf(0) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var playerFlashMessage by remember { mutableStateOf<String?>(null) }

    var isScreenLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
    var volumeHudPercentage by remember { mutableStateOf<Int?>(null) }
    var brightnessHudPercentage by remember { mutableStateOf<Int?>(null) }
    var dragStartX by remember { mutableStateOf(0f) }
    var accumulatedDragY by remember { mutableStateOf(0f) }

    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    // Auto-hide volume & brightness HUDs
    LaunchedEffect(volumeHudPercentage) {
        if (volumeHudPercentage != null) {
            delay(1500)
            volumeHudPercentage = null
        }
    }
    LaunchedEffect(brightnessHudPercentage) {
        if (brightnessHudPercentage != null) {
            delay(1500)
            brightnessHudPercentage = null
        }
    }
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3000)
            showUnlockButton = false
        }
    }

    LaunchedEffect(playerFlashMessage) {
        if (playerFlashMessage != null) {
            delay(2500)
            playerFlashMessage = null
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying, playbackError) {
        if (showControls && isPlaying && playbackError == null && !isPipMode) {
            delay(4000)
            showControls = false
        }
    }

    // Configure resilient ExoPlayer with universal codecs & OkHttp data source
    val exoPlayer = remember(context, httpUserAgent, httpReferrer) {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setAllowedVideoJoiningTimeMs(5000)
        }

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(playerOkHttpClient)
            .setUserAgent(httpUserAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 StreamPulseTV/1.0")

        if (!httpReferrer.isNullOrBlank()) {
            okHttpDataSourceFactory.setDefaultRequestProperties(mapOf("Referer" to httpReferrer))
        }

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2000,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 1500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    onPlaybackStatusChanged?.invoke(true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                Log.e("VideoPlayerView", "Playback exception for: $streamUrl", error)

                // Behind live window recovery
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    exoPlayer.play()
                    return
                }

                // If error is container/format parsing and we haven't tried fallback
                if (streamFallbackMode == 0 && !streamUrl.isNullOrBlank()) {
                    streamFallbackMode = 1
                    try {
                        val fallbackItem = createUniversalMediaItem(streamUrl, fallbackMode = 1)
                        exoPlayer.setMediaItem(fallbackItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                        return
                    } catch (_: Exception) {}
                }

                onPlaybackStatusChanged?.invoke(false)
                playbackError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network connection timeout"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Broadcaster returned HTTP error"
                    else -> error.localizedMessage ?: "Stream connection error"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Prepare and play stream whenever streamUrl, retryTrigger, or streamFallbackMode changes
    LaunchedEffect(streamUrl, retryTrigger, streamFallbackMode) {
        if (!streamUrl.isNullOrBlank()) {
            isBuffering = true
            playbackError = null
            try {
                val mediaItem = createUniversalMediaItem(streamUrl, fallbackMode = streamFallbackMode)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                playbackError = e.localizedMessage ?: "Failed to initialize stream"
                isBuffering = false
            }
        } else {
            exoPlayer.stop()
            isBuffering = false
        }
    }

    // React to play/pause state
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Instant Resolution & Track Selection Mode toggle
    LaunchedEffect(playbackQuality) {
        val paramsBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        when (playbackQuality) {
            PlaybackQuality.AUTO -> {
                paramsBuilder.clearVideoSizeConstraints()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false)
            }
            PlaybackQuality.HIGH -> {
                paramsBuilder.setMaxVideoSize(1920, 1080)
                    .setMaxVideoBitrate(10_000_000)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false)
            }
            PlaybackQuality.DATA_SAVER -> {
                paramsBuilder.setMaxVideoSize(854, 480)
                    .setMaxVideoBitrate(800_000)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, false)
            }
            PlaybackQuality.AUDIO_ONLY -> {
                paramsBuilder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, true)
            }
        }
        exoPlayer.trackSelectionParameters = paramsBuilder.build()
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(isScreenLocked, isPipMode) {
                if (isScreenLocked) {
                    detectTapGestures(
                        onTap = {
                            showUnlockButton = !showUnlockButton
                        }
                    )
                } else {
                    detectTapGestures(
                        onTap = {
                            if (!isPipMode) {
                                showControls = !showControls
                            }
                        }
                    )
                }
            }
            .pointerInput(isScreenLocked, isPipMode, isFullscreen) {
                if (!isScreenLocked && !isPipMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            accumulatedDragY = 0f
                        },
                        onDragEnd = {
                            // Swipe up on video player to enter fullscreen; swipe down to exit
                            if (accumulatedDragY < -100f && !isFullscreen) {
                                onToggleFullscreen()
                            } else if (accumulatedDragY > 100f && isFullscreen) {
                                onToggleFullscreen()
                            }
                            accumulatedDragY = 0f
                        },
                        onDragCancel = {
                            accumulatedDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragY += dragAmount.y

                            val viewWidth = size.width
                            if (viewWidth > 0) {
                                if (dragStartX < viewWidth / 2f) {
                                    // Slide left side: Brightness control
                                    activity?.let { act ->
                                        val current = act.window.attributes.screenBrightness.let {
                                            if (it < 0f) 0.5f else it
                                        }
                                        val delta = -dragAmount.y / 350f
                                        val newBrightness = (current + delta).coerceIn(0.01f, 1.0f)
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBrightness
                                        act.window.attributes = lp
                                        brightnessHudPercentage = (newBrightness * 100).toInt()
                                    }
                                } else {
                                    // Slide right side: Volume control
                                    audioManager?.let { am ->
                                        val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val delta = if (dragAmount.y < -6) 1 else if (dragAmount.y > 6) -1 else 0
                                        if (delta != 0) {
                                            val newVol = (curVol + delta).coerceIn(0, maxVolume)
                                            am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            volumeHudPercentage = (newVol * 100 / maxVolume)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            .testTag("video_player_container")
    ) {
        if (isCasting && castDeviceName != null) {
            // Casting display screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = "Casting",
                        tint = CyanNeon,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Casting to Wireless Display",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = title,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        } else if (playbackQuality == PlaybackQuality.AUDIO_ONLY) {
            // Audio-only visualizer state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF090D16))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Only",
                        tint = CyanNeon,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Audio Only Mode Active",
                        color = CyanNeon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saving 95% bandwidth for background audio streaming",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    val targetResizeMode = when (aspectRatioMode) {
                        AspectRatioMode.AUTO -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                    if (playerView.resizeMode != targetResizeMode) {
                        playerView.resizeMode = targetResizeMode
                        playerView.requestLayout()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering indicator
        if (isBuffering && isPlaying && playbackError == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CyanNeon,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Stream Error & Retry Overlay
        if (playbackError != null && !isPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = LiveRed,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to connect to live stream",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Server may be temporarily unreachable or restricted.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stream_retry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                streamFallbackMode = (streamFallbackMode + 1) % 3
                                retryTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stream_switch_format_button")
                        ) {
                            val modeLabel = when (streamFallbackMode) {
                                1 -> "Format: MPEG-TS"
                                2 -> "Format: Auto"
                                else -> "Format: HLS"
                            }
                            Text(modeLabel, color = Color.White, fontSize = 12.sp)
                        }

                        if (onNextChannel != null) {
                            Button(
                                onClick = onNextChannel,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("stream_skip_next_button")
                            ) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Next", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Overlay Controls (Direct rendering for max speed and zero animation delay)
        if (showControls && !isPipMode && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LIVE badge
                    Surface(
                        color = LiveRed,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Title & info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sleep Timer Button
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("player_sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerMinutes != null) CyanNeon else Color.White
                        )
                    }

                    // Mute Button
                    if (onToggleMute != null) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.testTag("player_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) Color(0xFFEF4444) else Color.White
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFBBF24) else Color.White
                        )
                    }

                    // Cast Button
                    IconButton(
                        onClick = onOpenCast,
                        modifier = Modifier.testTag("player_cast_button")
                    ) {
                        Icon(
                            imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = if (isCasting) CyanNeon else Color.White
                        )
                    }

                    // Picture-in-Picture Button
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("player_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }

                    // Lock Screen Button
                    IconButton(
                        onClick = {
                            isScreenLocked = true
                            showControls = false
                            showUnlockButton = true
                        },
                        modifier = Modifier.testTag("player_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Screen",
                            tint = Color.White
                        )
                    }
                }

                // Center Controls (Prev Channel, 10s Rewind, Play/Pause, 10s Forward, Next Channel)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("player_center_controls_row"),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onPreviousChannel != null) {
                        Surface(
                            onClick = onPreviousChannel,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("player_prev_channel_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_rewind_10_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = CyanNeon,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF00363D),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            val target = exoPlayer.currentPosition + 10_000L
                            exoPlayer.seekTo(target)
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_forward_10_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (onNextChannel != null) {
                        Surface(
                            onClick = onNextChannel,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("player_next_channel_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Section: EPG Schedule Progress + Bottom Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // EPG Now Playing Progress Card
                    if (currentProgram != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .testTag("player_epg_now_playing_card")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentProgram.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${currentProgram.getRemainingMinutes()}m left",
                                        color = CyanNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { currentProgram.getProgressFraction() },
                                    color = CyanNeon,
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                if (nextProgram != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "UP NEXT: ${nextProgram.title}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Quality & Aspect Ratio Pickers
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quality picker pill
                            Surface(
                                onClick = { showQualityDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.testTag("player_quality_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Quality",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = playbackQuality.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Aspect ratio picker pill
                            Surface(
                                onClick = { showAspectRatioDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.testTag("player_aspect_ratio_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = aspectRatioMode.label,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                        }

                        // Right: Subtitles and Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleSubtitles,
                                modifier = Modifier.testTag("player_subtitles_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitlesEnabled) CyanNeon else Color.White.copy(alpha = 0.7f)
                                )
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Brightness Control HUD (left side gesture)
        if (brightnessHudPercentage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Brightness $brightnessHudPercentage%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { brightnessHudPercentage!! / 100f },
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp),
                            color = CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Volume Control HUD (right side gesture)
        if (volumeHudPercentage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (volumeHudPercentage == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Volume $volumeHudPercentage%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { volumeHudPercentage!! / 100f },
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp),
                            color = CyanNeon,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }



        // In-Player Instant Action Flash Confirmation HUD (Snapshot / Recording feedback)
        if (playerFlashMessage != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isFullscreen) 32.dp else 16.dp)
                    .testTag("player_flash_hud")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = playerFlashMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Lock Screen Floating Unlock Button
        if (isScreenLocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (showUnlockButton) {
                    Surface(
                        onClick = {
                            isScreenLocked = false
                            showUnlockButton = false
                            showControls = true
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon),
                        modifier = Modifier.testTag("player_unlock_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlock Screen",
                                tint = CyanNeon,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Screen Locked • Tap to Unlock",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }



        // Aspect Ratio Selector Dialog (User-friendly picker)
        if (showAspectRatioDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAspectRatioDialog = false },
                title = {
                    Text(
                        text = "Aspect Ratio & Fit Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AspectRatioMode.entries.forEach { mode ->
                            val isSelected = aspectRatioMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (onSelectAspectRatio != null) {
                                            onSelectAspectRatio(mode)
                                        } else {
                                            onCycleAspectRatio()
                                        }
                                        showAspectRatioDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (onSelectAspectRatio != null) {
                                            onSelectAspectRatio(mode)
                                        } else {
                                            onCycleAspectRatio()
                                        }
                                        showAspectRatioDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    val desc = when (mode) {
                                        AspectRatioMode.FIT -> "16:9 Standard widescreen fit (black bars if needed)"
                                        AspectRatioMode.FILL -> "Zoom video to fill complete screen"
                                        AspectRatioMode.STRETCH -> "Stretch to fit exact display dimensions"
                                        AspectRatioMode.FOUR_THREE -> "4:3 Classic TV format"
                                        AspectRatioMode.AUTO -> "Auto-detect broadcaster video dimensions"
                                    }
                                    Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showAspectRatioDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Quality & Bitrate Selector Dialog (User-friendly picker)
        if (showQualityDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = {
                    Text(
                        text = "Video Resolution & Bitrate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PlaybackQuality.entries.forEach { q ->
                            val isSelected = playbackQuality == q
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (onSelectQuality != null) {
                                            onSelectQuality(q)
                                        } else {
                                            onCycleQuality()
                                        }
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (onSelectQuality != null) {
                                            onSelectQuality(q)
                                        } else {
                                            onCycleQuality()
                                        }
                                        showQualityDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = q.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Text(text = q.bitRateLimitDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showQualityDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Sleep Timer Dialog (User-friendly feature)
        if (showSleepTimerDialog) {
            val timerOptions = listOf(0 to "Turn Off", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "1 Hour", 90 to "1.5 Hours")
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = {
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (sleepTimerMinutes != null) "Current: Active (${sleepTimerMinutes}m)" else "Stop stream playback automatically.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        timerOptions.forEach { (mins, label) ->
                            val isSelected = (mins == 0 && sleepTimerMinutes == null) || (mins == sleepTimerMinutes)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSetSleepTimer?.invoke(if (mins == 0) null else mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSetSleepTimer?.invoke(if (mins == 0) null else mins)
                                        showSleepTimerDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

private fun createUniversalMediaItem(url: String, fallbackMode: Int = 0): MediaItem {
    val cleanUrl = url.trim()
    val uri = if (cleanUrl.startsWith("/") || cleanUrl.startsWith("file://")) {
        if (cleanUrl.startsWith("file://")) Uri.parse(cleanUrl) else Uri.fromFile(java.io.File(cleanUrl))
    } else {
        Uri.parse(cleanUrl)
    }
    val builder = MediaItem.Builder().setUri(uri)
    val lower = cleanUrl.lowercase()

    when (fallbackMode) {
        0 -> {
            when {
                lower.contains(".m3u8") || lower.contains("m3u8") || lower.contains("hls") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                lower.contains(".mpd") || lower.contains("mpd") || lower.contains("dash") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                lower.contains(".mp4") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                lower.contains(".ts") || lower.contains("mpegts") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MP2T)
                }
                lower.contains(".mkv") || lower.contains(".webm") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                }
                lower.startsWith("rtsp://") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_RTSP)
                }
                else -> {
                    // Default to HLS for IPTV streams without extension
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
        }
        1 -> {
            // Fallback 1: Try MPEG-TS format
            builder.setMimeType(MimeTypes.VIDEO_MP2T)
        }
        2 -> {
            // Fallback 2: Sniff without explicit MIME type
        }
    }
    return builder.build()
}
