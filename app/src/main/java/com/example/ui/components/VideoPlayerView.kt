package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.net.Uri
import androidx.core.net.toUri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import kotlin.math.abs
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessMedium
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.SkeuoJewelLed
import com.example.ui.theme.SkeuoRubyLed
import com.example.ui.theme.tvScreenBezel
import androidx.compose.ui.platform.LocalConfiguration
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.PlaybackQuality
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val formatParsingErrorCodes = setOf(
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
)

private val decoderErrorCodes = setOf(
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
)

private val networkConnectionErrorCodes = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
)

private val playerOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(followRedirects = true)
        .followSslRedirects(followProtocolRedirects = true)
        .retryOnConnectionFailure(retryOnConnectionFailure = true)
        .build()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
    modifier: Modifier = Modifier,
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
    isMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    sleepTimerMinutes: Int? = null,
    onSetSleepTimer: ((Int?) -> Unit)? = null,
    onPlaybackStatusChanged: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(value = true) }
    var showControls by remember { mutableStateOf(value = true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var streamFallbackMode by remember(streamUrl) { mutableIntStateOf(0) }
    val savedVodPositionState = rememberSaveable(streamUrl) { mutableLongStateOf(0L) }
    var savedVodPositionMs by savedVodPositionState
    val isVodFile = remember(streamUrl) { isVodFileStream(streamUrl) }
    val currentStreamUrl by rememberUpdatedState(streamUrl)
    val currentFallbackMode by rememberUpdatedState(streamFallbackMode)
    val currentIsVodFile by rememberUpdatedState(isVodFile)
    val currentSavedVodPositionState by rememberUpdatedState(savedVodPositionState)
    var showAspectRatioDialog by remember { mutableStateOf(value = false) }
    var showQualityDialog by remember { mutableStateOf(value = false) }
    var showSleepTimerDialog by remember { mutableStateOf(value = false) }
    var playerFlashMessage by remember { mutableStateOf<String?>(null) }

    var isScreenLocked by remember { mutableStateOf(value = false) }
    var showUnlockButton by remember { mutableStateOf(value = false) }
    var volumeHudPercentage by remember { mutableStateOf<Int?>(null) }
    var brightnessHudPercentage by remember { mutableStateOf<Int?>(null) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var accumulatedVolumeDrag by remember { mutableFloatStateOf(0f) }
    var accumulatedBrightnessDrag by remember { mutableFloatStateOf(0f) }

    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Screen timeout prevention during landscape playback or fullscreen
    DisposableEffect(isLandscape, isFullscreen, isPlaying) {
        val shouldKeepScreenOn = (isLandscape || isFullscreen) && isPlaying
        if (shouldKeepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }

    // Auto-hide volume & brightness HUDs
    LaunchedEffect(volumeHudPercentage) {
        if (volumeHudPercentage != null) {
            delay(1_500.milliseconds)
            volumeHudPercentage = null
        }
    }
    LaunchedEffect(brightnessHudPercentage) {
        if (brightnessHudPercentage != null) {
            delay(1_500.milliseconds)
            brightnessHudPercentage = null
        }
    }
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3_000.milliseconds)
            showUnlockButton = false
        }
    }

    LaunchedEffect(playerFlashMessage) {
        if (playerFlashMessage != null) {
            delay(2_500.milliseconds)
            playerFlashMessage = null
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying, playbackError) {
        if (showControls && isPlaying && (playbackError == null) && !isPipMode) {
            delay(4_000.milliseconds)
            showControls = false
        }
    }

    val appContext = context.applicationContext
    val (_, inlineHeaders) = remember(streamUrl) { parsePipeUrl(streamUrl ?: "") }

    // Configure resilient ExoPlayer with universal codecs & OkHttp data source.
    // Keying by inlineHeaders (instead of every distinct cleanStreamUrl) allows the ExoPlayer engine,
    // codecs, and thread pools to be reused seamlessly across channel clicks, eliminating video/audio
    // engine recreation overhead and memory spikes.
    val exoPlayer = remember(appContext, httpUserAgent, httpReferrer, inlineHeaders) {
        val renderersFactory = DefaultRenderersFactory(appContext).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            setAllowedVideoJoiningTimeMs(5000)
        }

        val effectiveUserAgent = inlineHeaders["User-Agent"]
            ?: inlineHeaders["http-user-agent"]
            ?: httpUserAgent
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 StreamPulseTV/1.0"

        val okHttpDataSourceFactory = OkHttpDataSource.Factory(playerOkHttpClient)
            .setUserAgent(effectiveUserAgent)

        val defaultHeaders = mutableMapOf(
            "Accept" to "*/*",
        )
        val effectiveReferrer = inlineHeaders["Referer"]
            ?: inlineHeaders["http-referrer"]
            ?: httpReferrer
        if (!effectiveReferrer.isNullOrBlank()) {
            defaultHeaders["Referer"] = effectiveReferrer
        }
        for ((k, v) in inlineHeaders) {
            if (!k.equals("User-Agent", ignoreCase = true) && !k.equals("Referer", ignoreCase = true)) {
                defaultHeaders[k] = v
            }
        }
        okHttpDataSourceFactory.setDefaultRequestProperties(defaultHeaders)

        val defaultDataSourceFactory = DefaultDataSource.Factory(appContext, okHttpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2000,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 1500,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                playWhenReady = isPlaying
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
                Log.e("VideoPlayerView", "Playback exception for: $currentStreamUrl", error)

                // Behind live window recovery
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    runCatching {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                    return
                }

                val isFormatError = error.errorCode in formatParsingErrorCodes
                val isDecoderError = error.errorCode in decoderErrorCodes
                val fallbackUrl = currentStreamUrl

                if (!fallbackUrl.isNullOrBlank() && (isFormatError || isDecoderError)) {
                    if (currentFallbackMode < 2) {
                        streamFallbackMode = currentFallbackMode + 1
                        return
                    }
                }

                onPlaybackStatusChanged?.invoke(false)
                playbackError = when (error.errorCode) {
                    in networkConnectionErrorCodes -> "Network connection timeout"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Broadcaster returned HTTP error"
                    else -> error.localizedMessage ?: "Stream connection error"
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            runCatching {
                if ((currentIsVodFile) && (exoPlayer.playbackState != Player.STATE_IDLE) && (exoPlayer.currentPosition > 0L)) {
                    currentSavedVodPositionState.longValue = exoPlayer.currentPosition
                }
                exoPlayer.removeListener(listener)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            }
        }
    }

    LaunchedEffect(exoPlayer, streamUrl, isVodFile) {
        if (isVodFile) {
            while (true) {
                delay(1_000.milliseconds)
                runCatching {
                    if ((exoPlayer.playbackState != Player.STATE_IDLE) && (exoPlayer.playbackState != Player.STATE_ENDED)) {
                        if (exoPlayer.currentPosition > 0L) {
                            savedVodPositionMs = exoPlayer.currentPosition
                        }
                    }
                }
            }
        }
    }

    // Prepare and play stream whenever streamUrl, retryTrigger, or streamFallbackMode changes
    LaunchedEffect(streamUrl, retryTrigger, streamFallbackMode) {
        if (!streamUrl.isNullOrBlank()) {
            isBuffering = true
            playbackError = null
            try {
                val mediaItem = createUniversalMediaItem(streamUrl, fallbackMode = streamFallbackMode)
                if (isVodFile) {
                    exoPlayer.setMediaItem(mediaItem, savedVodPositionMs)
                } else {
                    exoPlayer.setMediaItem(mediaItem)
                }
                exoPlayer.prepare()
                if (isPlaying) {
                    exoPlayer.play()
                }
            } catch (e: Exception) {
                playbackError = e.localizedMessage ?: "Failed to initialize stream"
                isBuffering = false
                onPlaybackStatusChanged?.invoke(false)
            }
        } else {
            exoPlayer.stop()
            playbackError = if (streamUrl == null) null else "Channel stream URL is empty or unavailable"
            isBuffering = false
            if (streamUrl != null) {
                onPlaybackStatusChanged?.invoke(false)
            }
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

    MyApplicationTheme(themeMode = AppThemeMode.DARK) {
        Box(
            modifier = modifier
                .then(
                    if (!isFullscreen && !isPipMode) {
                        Modifier.tvScreenBezel(cornerRadius = 14.dp)
                    } else Modifier,
                )
                .background(Color.Black)
            // Vertical Drag Gestures: Left 35% = Brightness, Right 35% = Volume, Center 30% = Swipe Up/Down Fullscreen
            .pointerInput(isScreenLocked, isPipMode, isFullscreen) {
                if (!isScreenLocked && !isPipMode) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            accumulatedDragY = 0f
                            accumulatedVolumeDrag = 0f
                            accumulatedBrightnessDrag = 0f
                        },
                        onDragEnd = {
                            val viewWidth = size.width
                            if (viewWidth > 0) {
                                val isCenterRegion = (dragStartX >= (viewWidth * 0.30f)) && (dragStartX <= (viewWidth * 0.70f))
                                if (isCenterRegion) {
                                    if ((accumulatedDragY < -100f) && !isFullscreen) {
                                        onToggleFullscreen()
                                    } else if ((accumulatedDragY > 100f) && isFullscreen) {
                                        onToggleFullscreen()
                                    }
                                }
                            }
                            accumulatedDragY = 0f
                            accumulatedVolumeDrag = 0f
                            accumulatedBrightnessDrag = 0f
                        },
                        onDragCancel = {
                            accumulatedDragY = 0f
                            accumulatedVolumeDrag = 0f
                            accumulatedBrightnessDrag = 0f
                        },
                    ) { _, dragAmount ->
                        accumulatedDragY += dragAmount

                        val viewWidth = size.width
                        if (viewWidth > 0) {
                            if (dragStartX < (viewWidth * 0.35f)) {
                                // Slide left 35%: Brightness control
                                accumulatedBrightnessDrag += dragAmount
                                if (abs(accumulatedBrightnessDrag) > 10f) {
                                    activity?.let { act ->
                                        val current = act.window.attributes.screenBrightness.let {
                                            if (it < 0f) 0.5f else it
                                        }
                                        val brightnessDelta = -accumulatedBrightnessDrag / 300f
                                        val newBrightness = (current + brightnessDelta).coerceIn(0.01f, 1.0f)
                                        val lp = act.window.attributes
                                        lp.screenBrightness = newBrightness
                                        runCatching {
                                            act.window.attributes = lp
                                        }
                                        brightnessHudPercentage = (newBrightness * 100).toInt()
                                    }
                                    accumulatedBrightnessDrag = 0f
                                }
                            } else if (dragStartX > (viewWidth * 0.65f)) {
                                // Slide right 35%: Volume control
                                accumulatedVolumeDrag += dragAmount
                                if (abs(accumulatedVolumeDrag) > 12f) {
                                    val steps = (accumulatedVolumeDrag / 12f).toInt()
                                    if (steps != 0) {
                                        audioManager?.let { am ->
                                            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                            val delta = -steps
                                            val newVol = (curVol + delta).coerceIn(0, maxVolume)
                                            runCatching {
                                                am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            }
                                            volumeHudPercentage = ((newVol * 100) / maxVolume).coerceIn(0, 100)
                                        }
                                        accumulatedVolumeDrag %= 12f
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Tap & Double-Tap Gestures: Single tap = toggle controls; Double tap left/right = Rewind/Forward 10s
            .pointerInput(isScreenLocked, isPipMode) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isScreenLocked && !isPipMode) {
                            val viewWidth = size.width
                            if (viewWidth > 0) {
                                when {
                                    offset.x < (viewWidth * 0.35f) -> {
                                        if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                            runCatching { exoPlayer.seekTo(target) }
                                            playerFlashMessage = "Rewind 10s «"
                                        }
                                    }
                                    offset.x > (viewWidth * 0.65f) -> {
                                        if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                            val target = exoPlayer.currentPosition + 10_000L
                                            val duration = exoPlayer.duration
                                            if ((duration == androidx.media3.common.C.TIME_UNSET) || (target <= duration)) {
                                                runCatching { exoPlayer.seekTo(target) }
                                            }
                                            playerFlashMessage = "Forward 10s »"
                                        }
                                    }
                                    else -> {
                                        onTogglePlayPause()
                                    }
                                }
                            }
                        }
                    },
                ) {
                    if (isScreenLocked) {
                        showUnlockButton = !showUnlockButton
                    } else if (!isPipMode) {
                        showControls = !showControls
                    }
                }
            }
            .testTag("video_player_container"),
    ) {
        if (isCasting && (castDeviceName != null)) {
            // Casting display screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = "Casting",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Casting to Wireless Display",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = title,
                        color = Color(0xFF9E9E9E),
                        fontSize = 13.sp,
                    )
                }
            }
        } else if (playbackQuality == PlaybackQuality.AUDIO_ONLY) {
            // Audio-only visualizer state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Only",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Audio Only Mode Active",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Saving 95% bandwidth for background audio streaming",
                        color = Color(0xFF9E9E9E),
                        fontSize = 12.sp,
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
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { playerView ->
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
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
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Buffering indicator
        if (isBuffering && isPlaying && (playbackError == null)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        // Stream Error & Retry Overlay
        if ((playbackError != null) && !isPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
            ) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .testTag("error_close_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Player",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = LiveRed,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to connect to live stream",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Server may be temporarily unreachable or restricted.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                streamFallbackMode = 0
                                retryTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF121212),
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stream_retry_button"),
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF121212), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color(0xFF121212), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                streamFallbackMode = (streamFallbackMode + 1) % 3
                                retryTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stream_switch_format_button"),
                        ) {
                            val modeLabel = when (streamFallbackMode) {
                                1 -> "Format: MPEG-TS"
                                2 -> "Format: Auto"
                                else -> "Format: HLS"
                            }
                            Text(modeLabel, color = Color.White, fontSize = 12.sp)
                        }

                        onNextChannel?.let { nextChannel ->
                            Button(
                                onClick = nextChannel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("stream_skip_next_button"),
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
        if (showControls && !isPipMode && (playbackError == null)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    )
                    .pointerInput(isScreenLocked, isFullscreen) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                accumulatedDragY = 0f
                                accumulatedVolumeDrag = 0f
                                accumulatedBrightnessDrag = 0f
                            },
                            onDragEnd = {
                                val viewWidth = size.width
                                if (viewWidth > 0) {
                                    val isCenterRegion = (dragStartX >= (viewWidth * 0.30f)) && (dragStartX <= (viewWidth * 0.70f))
                                    if (isCenterRegion) {
                                        if ((accumulatedDragY < -100f) && !isFullscreen) {
                                            onToggleFullscreen()
                                        } else if ((accumulatedDragY > 100f) && isFullscreen) {
                                            onToggleFullscreen()
                                        }
                                    }
                                }
                                accumulatedDragY = 0f
                                accumulatedVolumeDrag = 0f
                                accumulatedBrightnessDrag = 0f
                            },
                            onDragCancel = {
                                accumulatedDragY = 0f
                                accumulatedVolumeDrag = 0f
                                accumulatedBrightnessDrag = 0f
                            },
                        ) { _, dragAmount ->
                            accumulatedDragY += dragAmount
                            val viewWidth = size.width
                            if (viewWidth > 0) {
                                if (dragStartX < (viewWidth * 0.35f)) {
                                    // Slide left 35%: Brightness control
                                    accumulatedBrightnessDrag += dragAmount
                                    if (abs(accumulatedBrightnessDrag) > 10f) {
                                        activity?.let { act ->
                                            val current = act.window.attributes.screenBrightness.let {
                                                if (it < 0f) 0.5f else it
                                            }
                                            val brightnessDelta = -accumulatedBrightnessDrag / 300f
                                            val newBrightness = (current + brightnessDelta).coerceIn(0.01f, 1.0f)
                                            val lp = act.window.attributes
                                            lp.screenBrightness = newBrightness
                                            runCatching { act.window.attributes = lp }
                                            brightnessHudPercentage = (newBrightness * 100).toInt()
                                        }
                                        accumulatedBrightnessDrag = 0f
                                    }
                                } else if (dragStartX > (viewWidth * 0.65f)) {
                                    // Slide right 35%: Volume control
                                    accumulatedVolumeDrag += dragAmount
                                    if (abs(accumulatedVolumeDrag) > 12f) {
                                        val steps = (accumulatedVolumeDrag / 12f).toInt()
                                        if (steps != 0) {
                                            audioManager?.let { am ->
                                                val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val delta = -steps
                                                val newVol = (curVol + delta).coerceIn(0, maxVolume)
                                                runCatching {
                                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                }
                                                volumeHudPercentage = ((newVol * 100) / maxVolume).coerceIn(0, 100)
                                            }
                                            accumulatedVolumeDrag %= 12f
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val viewWidth = size.width
                                if (viewWidth > 0) {
                                    when {
                                        offset.x < (viewWidth * 0.35f) -> {
                                            if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                                val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                                runCatching { exoPlayer.seekTo(target) }
                                                playerFlashMessage = "Rewind 10s «"
                                            }
                                        }
                                        offset.x > (viewWidth * 0.65f) -> {
                                            if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                                val target = exoPlayer.currentPosition + 10_000L
                                                val duration = exoPlayer.duration
                                                if ((duration == androidx.media3.common.C.TIME_UNSET) || (target <= duration)) {
                                                    runCatching { exoPlayer.seekTo(target) }
                                                }
                                                playerFlashMessage = "Forward 10s »"
                                            }
                                        }
                                        else -> {
                                            onTogglePlayPause()
                                        }
                                    }
                                }
                            },
                        ) {
                            showControls = false
                        }
                    },
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // ON AIR jewel indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 10.dp),
                    ) {
                        SkeuoJewelLed(
                            isActive = isPlaying,
                            color = SkeuoRubyLed,
                            label = "ON AIR",
                            size = 11.dp,
                        )
                    }

                    // Title & info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Sleep Timer Button
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("player_sleep_timer_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerMinutes != null) Color(0xFFF59E0B) else Color.White,
                        )
                    }

                    // Mute Button
                    onToggleMute?.let { toggleMute ->
                        IconButton(
                            onClick = toggleMute,
                            modifier = Modifier.testTag("player_mute_button"),
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                            )
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("player_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else Color.White,
                        )
                    }

                    // Support Us Heart Button (ca-app-pub-2382308752605431/2892383410)
                    IconButton(
                        onClick = {
                            context.findActivity()?.let { activity ->
                                com.example.util.AdManager.showRewardedOrInterstitialAd(
                                    activity,
                                    com.example.util.AdManager.VIDEO_PLAYER_SUPPORT_AD_ID,
                                )
                            }
                        },
                        modifier = Modifier.testTag("player_support_us_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Support Us",
                            tint = Color.White,
                        )
                    }

                    // Cast Button
                    IconButton(
                        onClick = onOpenCast,
                        modifier = Modifier.testTag("player_cast_button"),
                    ) {
                        Icon(
                            imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = if (isCasting) Color(0xFF38BDF8) else Color.White,
                        )
                    }

                    // Picture-in-Picture Button
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier.testTag("player_pip_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                        )
                    }

                    // Lock Screen Button
                    IconButton(
                        onClick = {
                            isScreenLocked = true
                            showControls = false
                            showUnlockButton = true
                        },
                        modifier = Modifier.testTag("player_lock_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Screen",
                            tint = Color.White,
                        )
                    }
                }

                // Center Controls (Prev Channel, 10s Rewind, Play/Pause, 10s Forward, Next Channel)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("player_center_controls_row"),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    onPreviousChannel?.let { previousChannel ->
                        SkeuoButton(
                            onClick = previousChannel,
                            cornerRadius = 24.dp,
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            borderColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("player_prev_channel_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Channel",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    SkeuoButton(
                        onClick = {
                            if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                runCatching { exoPlayer.seekTo(target) }
                            }
                        },
                        cornerRadius = 24.dp,
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        borderColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("player_rewind_10_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    SkeuoButton(
                        onClick = onTogglePlayPause,
                        isAccent = true,
                        cornerRadius = 30.dp,
                        containerColor = Color.White,
                        borderColor = Color.White,
                        modifier = Modifier
                            .size(62.dp)
                            .testTag("player_play_pause_button"),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    SkeuoButton(
                        onClick = {
                            if (isVodFile || exoPlayer.isCurrentMediaItemSeekable) {
                                val target = exoPlayer.currentPosition + 10_000L
                                val duration = exoPlayer.duration
                                if ((duration == androidx.media3.common.C.TIME_UNSET) || (target <= duration)) {
                                    runCatching { exoPlayer.seekTo(target) }
                                }
                            }
                        },
                        cornerRadius = 24.dp,
                        containerColor = Color.Black.copy(alpha = 0.65f),
                        borderColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("player_forward_10_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    onNextChannel?.let { nextChannel ->
                        SkeuoButton(
                            onClick = nextChannel,
                            cornerRadius = 24.dp,
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            borderColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("player_next_channel_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                // Bottom Section: Bottom Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.BottomCenter),
                ) {
                    // Bottom Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Left: Quality & Aspect Ratio Pickers
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quality picker pill
                            SkeuoButton(
                                onClick = { showQualityDialog = true },
                                cornerRadius = 8.dp,
                                containerColor = Color.Black.copy(alpha = 0.65f),
                                borderColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.testTag("player_quality_pill"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Quality",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = playbackQuality.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Aspect ratio picker pill
                            SkeuoButton(
                                onClick = { showAspectRatioDialog = true },
                                cornerRadius = 8.dp,
                                containerColor = Color.Black.copy(alpha = 0.65f),
                                borderColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.testTag("player_aspect_ratio_pill"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = aspectRatioMode.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // Right: Subtitles and Fullscreen
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleSubtitles,
                                modifier = Modifier.testTag("player_subtitles_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitlesEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                                )
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.testTag("player_fullscreen_button"),
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Brightness Control HUD (left side gesture)
        val currentBrightness = brightnessHudPercentage
        if (currentBrightness != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Brightness $currentBrightness%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (currentBrightness / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp),
                            color = Color(0xFF38BDF8),
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        // Volume Control HUD (right side gesture)
        val currentVolume = volumeHudPercentage
        if (currentVolume != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentVolume == 0) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Volume $currentVolume%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (currentVolume / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp),
                            color = Color(0xFF38BDF8),
                            trackColor = Color.White.copy(alpha = 0.3f),
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
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isFullscreen) 32.dp else 16.dp)
                    .testTag("player_flash_hud"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = playerFlashMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Lock Screen Floating Unlock Button
        if (isScreenLocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
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
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.testTag("player_unlock_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlock Screen",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Screen Locked • Tap to Unlock",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.Bold,
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
                                    .selectable(
                                        selected = isSelected,
                                        role = androidx.compose.ui.semantics.Role.RadioButton,
                                    ) {
                                        if (onSelectAspectRatio != null) {
                                            onSelectAspectRatio(mode)
                                        } else {
                                            onCycleAspectRatio()
                                        }
                                        showAspectRatioDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
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
                },
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
                        fontWeight = FontWeight.Bold,
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
                                    .selectable(
                                        selected = isSelected,
                                        role = androidx.compose.ui.semantics.Role.RadioButton,
                                    ) {
                                        if (onSelectQuality != null) {
                                            onSelectQuality(q)
                                        } else {
                                            onCycleQuality()
                                        }
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = q.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
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
                },
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
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (sleepTimerMinutes != null) "Current: Active (${sleepTimerMinutes}m)" else "Stop stream playback automatically.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        timerOptions.forEach { (mins, label) ->
                            val isSelected = if (mins == 0) {
                                sleepTimerMinutes == null
                            } else {
                                mins == sleepTimerMinutes
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .selectable(
                                        selected = isSelected,
                                        role = androidx.compose.ui.semantics.Role.RadioButton,
                                    ) {
                                        onSetSleepTimer?.invoke(if (mins == 0) null else mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
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
                },
            )
        }
    }
}
}

private fun parsePipeUrl(rawUrl: String): Pair<String, Map<String, String>> {
    if (!rawUrl.contains('|')) return Pair(rawUrl, emptyMap())
    val parts = rawUrl.split('|')
    val cleanUrl = parts[0].trim()
    val headers = mutableMapOf<String, String>()
    for (i in 1 until parts.size) {
        val param = parts[i].trim()
        val eqIndex = param.indexOf('=')
        if (eqIndex > 0) {
            val key = param.substring(0, eqIndex).trim()
            val value = param.substring(eqIndex + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                headers[key] = value
            }
        }
    }
    return Pair(cleanUrl, headers)
}

private fun isVodFileStream(url: String?): Boolean {
    val safeUrl = url ?: return false
    val (cleanUrl, _) = parsePipeUrl(safeUrl)
    val path = runCatching { cleanUrl.toUri().path?.lowercase() }.getOrNull() ?: return false
    return path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".webm") ||
        path.endsWith(".mov") || path.endsWith(".avi") || path.endsWith(".m4v")
}

@OptIn(UnstableApi::class)
private fun createUniversalMediaItem(url: String, fallbackMode: Int = 0): MediaItem {
    val rawCleanUrl = url.trim()
    val (streamUrlPart, _) = parsePipeUrl(rawCleanUrl)
    val safeStreamUrl = streamUrlPart.replace(" ", "%20")

    val uri = runCatching {
        if (safeStreamUrl.startsWith("/") || safeStreamUrl.startsWith("file://")) {
            if (safeStreamUrl.startsWith("file://")) safeStreamUrl.toUri() else Uri.fromFile(java.io.File(safeStreamUrl))
        } else {
            safeStreamUrl.toUri()
        }
    }.getOrElse {
        Uri.EMPTY
    }

    val builder = MediaItem.Builder().setUri(uri)
    val lower = safeStreamUrl.lowercase()

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
                lower.startsWith("rtmp://") -> {
                    builder.setMimeType(MimeTypes.VIDEO_FLV)
                }
                lower.startsWith("rtsp://") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_RTSP)
                }
                else -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
        }
        1 -> {
            if (lower.contains(".m3u8") || lower.contains("m3u8")) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            } else if (lower.contains(".ts")) {
                builder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }
        2 -> {
            // Fallback 2: Sniff without explicit MIME type
        }
    }
    return builder.build()
}
