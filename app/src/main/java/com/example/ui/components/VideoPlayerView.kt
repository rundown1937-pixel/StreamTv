package com.example.ui.components

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.NalUnitUtil
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegLibrary
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SkeuoButton
import com.example.ui.theme.tvScreenBezel
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.PlaybackQuality
import kotlinx.coroutines.delay
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
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
    PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
)

private val networkConnectionErrorCodes = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
)

private val playerOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
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

/**
 * Detects interlaced (1080i PAFF/MBAFF) or 10-bit High 10 H.264 streams from the SPS NAL unit.
 * Many Android OEM hardware MediaCodec decoders (e.g., MediaTek) produce heavy pixelation,
 * macroblocking, or combing artifacts on interlaced/10-bit H.264 without reporting an error.
 */
@OptIn(UnstableApi::class)
private fun isInterlacedOr10BitH264(format: Format): Boolean {
    if (format.sampleMimeType != MimeTypes.VIDEO_H264) return false
    for (data in format.initializationData) {
        if (data.size < 6) continue
        // Handle raw SPS NAL (without Annex-B start code)
        if ((data[0].toInt() and 0x1F) == NalUnitUtil.H264_NAL_UNIT_TYPE_SPS) {
            val sps = runCatching { NalUnitUtil.parseSpsNalUnit(data, 0, data.size) }.getOrNull()
            if ((sps != null) && (!sps.frameMbsOnlyFlag || (sps.bitDepthLumaMinus8 > 0) || (sps.bitDepthChromaMinus8 > 0))) {
                Log.i(
                    "VideoPlayerView",
                    "Detected H.264 interlaced=${!sps.frameMbsOnlyFlag} bitDepth=${8 + sps.bitDepthLumaMinus8} (${sps.width}x${sps.height}); routing to FFmpeg video decoder",
                )
                return true
            }
        }
        // Scan for Annex-B start code (00 00 01 or 00 00 00 01) followed by SPS NAL (type 7)
        var offset = 0
        while (offset + 4 < data.size) {
            val isStartCode3 = (data[offset] == 0.toByte()) && (data[offset + 1] == 0.toByte()) && (data[offset + 2] == 1.toByte())
            val isStartCode4 = (offset + 5 < data.size) && (data[offset] == 0.toByte()) && (data[offset + 1] == 0.toByte()) && (data[offset + 2] == 0.toByte()) && (data[offset + 3] == 1.toByte())
            if (isStartCode3 || isStartCode4) {
                val nalStart = if (isStartCode4) offset + 4 else offset + 3
                val nalType = data[nalStart].toInt() and 0x1F
                if (nalType == NalUnitUtil.H264_NAL_UNIT_TYPE_SPS) {
                    val sps = runCatching { NalUnitUtil.parseSpsNalUnit(data, nalStart, data.size) }.getOrNull()
                    if ((sps != null) && (!sps.frameMbsOnlyFlag || (sps.bitDepthLumaMinus8 > 0) || (sps.bitDepthChromaMinus8 > 0))) {
                        Log.i(
                            "VideoPlayerView",
                            "Detected H.264 interlaced=${!sps.frameMbsOnlyFlag} bitDepth=${8 + sps.bitDepthLumaMinus8} (${sps.width}x${sps.height}); routing to FFmpeg video decoder",
                        )
                        return true
                    }
                    break
                }
                offset = nalStart
            } else {
                offset++
            }
        }
    }
    return false
}

/**
 * Wraps hardware MediaCodecVideoRenderer so interlaced/10-bit H.264 streams cleanly fall through
 * to bundled multithreaded FfmpegVideoRenderer instead of pixelating on OEM hardware decoders.
 */
@OptIn(UnstableApi::class)
private class InterlacedSafeVideoRenderer(
    private val delegate: Renderer,
) : ForwardingRenderer(delegate) {
    private val wrappedCapabilities = object : RendererCapabilities {
        override fun getName(): String = delegate.capabilities.name
        override fun getTrackType(): Int = delegate.capabilities.trackType
        override fun supportsFormat(format: Format): Int {
            val mime = format.sampleMimeType
            if ((mime != null) && isInterlacedOr10BitH264(format) && FfmpegLibrary.isAvailable() && FfmpegLibrary.supportsFormat(mime)) {
                return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE)
            }
            return delegate.capabilities.supportsFormat(format)
        }
        override fun supportsMixedMimeTypeAdaptation(): Int =
            delegate.capabilities.supportsMixedMimeTypeAdaptation()
    }

    override fun getCapabilities(): RendererCapabilities = wrappedCapabilities
}

/**
 * Universal codec renderers factory:
 * - Prefers FFmpeg software decoding for ALL internet/broadcast audio codecs (AAC, MP1/MP2/MP3, AC-3,
 *   E-AC-3, E-AC-3 JOC, DTS, DTS-HD, TrueHD, Opus, Vorbis, FLAC, ALAC, AMR, PCM). Prevents silent or
 *   broken OEM hardware audio decoders and passthrough dropouts. Retains platform MediaCodec audio decoders
 *   as automatic fallback.
 * - Uses GPU hardware decoding first for progressive video (H.264, H.265/HEVC, VP8, VP9, AV1,
 *   MPEG-2/4, H.263). Automatically routes interlaced/10-bit H.264 to multithreaded FFmpeg to
 *   prevent OEM hardware pixelation, and provides full FFmpeg + AV1 decoder fallback for all codecs.
 * - Outputs universal 16-bit PCM compatible with every speaker, Bluetooth device, and LoudnessEnhancer.
 */
@OptIn(UnstableApi::class)
private class UniversalRenderersFactory(context: Context) : NextRenderersFactory(context) {
    init {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        setEnableDecoderFallback(true)
        setAllowedVideoJoiningTimeMs(5000)
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        super.buildAudioRenderers(
            context,
            EXTENSION_RENDERER_MODE_PREFER,
            mediaCodecSelector,
            true,
            audioSink,
            eventHandler,
            eventListener,
            out,
        )
    }

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        val startIndex = out.size
        super.buildVideoRenderers(
            context,
            EXTENSION_RENDERER_MODE_ON,
            mediaCodecSelector,
            true,
            eventHandler,
            eventListener,
            allowedVideoJoiningTimeMs,
            out,
        )
        for (i in startIndex until out.size) {
            if (out[i] is MediaCodecVideoRenderer) {
                out[i] = InterlacedSafeVideoRenderer(out[i])
            }
        }
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
            .build()
    }
}

private data class PlayerAudioTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
)

private data class PlayerVideoTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val isSelected: Boolean,
)

private fun formatAudioCodecName(mimeType: String?): String = when (mimeType) {
    MimeTypes.AUDIO_AAC -> "AAC"
    MimeTypes.AUDIO_MPEG -> "MP3"
    MimeTypes.AUDIO_MPEG_L2 -> "MP2"
    MimeTypes.AUDIO_MPEG_L1 -> "MP1"
    MimeTypes.AUDIO_AC3 -> "AC-3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3"
    MimeTypes.AUDIO_AC4 -> "AC-4"
    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> "DTS"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    MimeTypes.AUDIO_OPUS -> "Opus"
    MimeTypes.AUDIO_VORBIS -> "Vorbis"
    MimeTypes.AUDIO_FLAC -> "FLAC"
    MimeTypes.AUDIO_ALAC -> "ALAC"
    else -> mimeType?.substringAfter("/")?.uppercase(Locale.US) ?: "Audio"
}

@OptIn(UnstableApi::class)
@Suppress("DEPRECATION")
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var isBuffering by remember { mutableStateOf(value = true) }
    var showControls by remember { mutableStateOf(value = true) }
    var controlsInteractionTick by remember { mutableIntStateOf(0) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var streamFallbackMode by remember(streamUrl) { mutableIntStateOf(0) }
    val savedVodPositionState = rememberSaveable(streamUrl) { mutableLongStateOf(0L) }
    var savedVodPositionMs by savedVodPositionState
    val isVodFile = remember(streamUrl) { isVodFileStream(streamUrl) }

    var isSeekableStream by remember(streamUrl) { mutableStateOf(value = isVodFile) }
    var currentPositionMs by remember(streamUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(streamUrl) { mutableLongStateOf(0L) }
    var isUserScrubbing by remember { mutableStateOf(value = false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    val currentStreamUrl by rememberUpdatedState(streamUrl)
    val currentFallbackMode by rememberUpdatedState(streamFallbackMode)
    val currentIsVodFile by rememberUpdatedState(isVodFile)
    val currentSavedVodPositionState by rememberUpdatedState(savedVodPositionState)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentIsMuted by rememberUpdatedState(isMuted)
    val currentIsPipMode by rememberUpdatedState(isPipMode)
    val currentOnToggleMute by rememberUpdatedState(onToggleMute)
    val currentOnTogglePlayPause by rememberUpdatedState(onTogglePlayPause)
    val currentOnToggleSubtitles by rememberUpdatedState(onToggleSubtitles)

    var showAspectRatioDialog by remember { mutableStateOf(value = false) }
    var showQualityDialog by remember { mutableStateOf(value = false) }
    var showSleepTimerDialog by remember { mutableStateOf(value = false) }
    var showZoomDialog by remember { mutableStateOf(value = false) }
    var showAudioTrackDialog by remember { mutableStateOf(value = false) }
    var playerFlashMessage by remember { mutableStateOf<String?>(null) }

    var isScreenLocked by remember { mutableStateOf(value = false) }
    var showUnlockButton by remember { mutableStateOf(value = false) }
    var volumeHudPercentage by remember { mutableStateOf<Int?>(null) }
    var brightnessHudPercentage by remember { mutableStateOf<Int?>(null) }
    var zoomHudPercentage by remember { mutableStateOf<Int?>(null) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var accumulatedVolumeDrag by remember { mutableFloatStateOf(0f) }
    var accumulatedBrightnessDrag by remember { mutableFloatStateOf(0f) }

    var videoZoomScale by remember(streamUrl) { mutableFloatStateOf(1f) }
    var videoPanOffset by remember(streamUrl) { mutableStateOf(Offset.Zero) }
    var availableAudioTracks by remember(streamUrl) { mutableStateOf<List<PlayerAudioTrackOption>>(emptyList()) }
    var availableVideoTracks by remember(streamUrl) { mutableStateOf<List<PlayerVideoTrackOption>>(emptyList()) }
    var selectedVideoTrackOverride by remember(streamUrl) { mutableStateOf<PlayerVideoTrackOption?>(null) }
    var activeVideoHeight by remember(streamUrl) { mutableIntStateOf(0) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var systemVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVolume)
    }
    var isAudioBoostEnabled by remember { mutableStateOf(value = false) }
    var isDraggingVolumeSlider by remember { mutableStateOf(value = false) }

    // Keep screen awake while actively watching
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

    // Auto-hide HUDs
    LaunchedEffect(volumeHudPercentage) {
        if (volumeHudPercentage != null) {
            delay(1_400.milliseconds)
            volumeHudPercentage = null
        }
    }
    LaunchedEffect(brightnessHudPercentage) {
        if (brightnessHudPercentage != null) {
            delay(1_400.milliseconds)
            brightnessHudPercentage = null
        }
    }
    LaunchedEffect(zoomHudPercentage) {
        if (zoomHudPercentage != null) {
            delay(1_400.milliseconds)
            zoomHudPercentage = null
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
            delay(2_200.milliseconds)
            playerFlashMessage = null
        }
    }

    // Smart auto-hide controls: resets on every interaction and stays visible while dialogs are open, scrubbing, or paused
    val anyDialogOpen = showAspectRatioDialog || showQualityDialog || showSleepTimerDialog || showZoomDialog || showAudioTrackDialog
    LaunchedEffect(
        showControls,
        controlsInteractionTick,
        isPlaying,
        playbackError,
        anyDialogOpen,
        isPipMode,
        isDraggingVolumeSlider,
        isUserScrubbing,
    ) {
        if (showControls && isPlaying && (playbackError == null) && !anyDialogOpen && !isPipMode && !isDraggingVolumeSlider && !isUserScrubbing) {
            delay(4_500.milliseconds)
            showControls = false
        }
    }

    val appContext = context.applicationContext
    val (_, inlineHeaders) = remember(streamUrl) { parsePipeUrl(streamUrl ?: "") }

    // Universal media source factories: disables HLS chunkless preparation so multiplexed IPTV audio tracks are always discovered
    val mediaSourceFactories = remember(appContext, httpUserAgent, httpReferrer, inlineHeaders) {
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
        // Enable HDMV DTS audio in MPEG-TS without breaking H.264/HEVC GOP/access-unit framing
        // (FLAG_ALLOW_NON_IDR_KEYFRAMES / FLAG_DETECT_ACCESS_UNITS split broadcast slices & cause blocky/grainy video)
        val tsPayloadFlags = DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS
        val loadErrorPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 4)

        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(tsPayloadFlags)
            .setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 4)
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)

        val defaultMediaSourceFactory = DefaultMediaSourceFactory(defaultDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(loadErrorPolicy)
        val hlsMediaSourceFactory = HlsMediaSource.Factory(defaultDataSourceFactory)
            .setAllowChunklessPreparation(false)
            .setExtractorFactory(DefaultHlsExtractorFactory(tsPayloadFlags, true))
            .setLoadErrorHandlingPolicy(loadErrorPolicy)

        Pair(defaultMediaSourceFactory, hlsMediaSourceFactory)
    }

    // Configure resilient ExoPlayer with UniversalRenderersFactory (all internet audio/video codecs + FFmpeg audio priority)
    val exoPlayer = remember(appContext, mediaSourceFactories) {
        val renderersFactory = UniversalRenderersFactory(appContext)
        val (defaultMediaSourceFactory, _) = mediaSourceFactories

        // Generous multi-segment buffer for live IPTV HLS/DASH/TS streams to eliminate constant rebuffering
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000,
            )
            .setBackBuffer(/* backBufferDurationMs = */ 10_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Network-aware bandwidth meter with high initial estimate (25 Mbps) so Auto ABR opens HD/FHD streams immediately
        val bandwidthMeter = DefaultBandwidthMeter.Builder(appContext)
            .setInitialBitrateEstimate(25_000_000L)
            .setResetOnNetworkTypeChange(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Responsive Auto ABR: ramps up immediately to highest quality and steps down smoothly if network drops
        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory(
            /* minDurationForQualityIncreaseMs = */ 1_000,
            /* maxDurationForQualityDecreaseMs = */ 6_000,
            /* minDurationToRetainAfterDiscardMs = */ 4_000,
            /* bandwidthFraction = */ 0.85f,
        )

        val trackSelector = DefaultTrackSelector(appContext, adaptiveTrackSelectionFactory).apply {
            parameters = buildUponParameters()
                .setPreferredVideoMimeTypes(
                    MimeTypes.VIDEO_H265,
                    MimeTypes.VIDEO_H264,
                    MimeTypes.VIDEO_VP9,
                    MimeTypes.VIDEO_AV1,
                )
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedSampleRateAdaptiveness(true)
                .setAllowAudioMixedChannelCountAdaptiveness(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoMixedDecoderSupportAdaptiveness(true)
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .setMediaSourceFactory(defaultMediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                volume = if (currentIsMuted) 0f else 1f
                playWhenReady = currentIsPlaying
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    val currentExoPlayer by rememberUpdatedState(exoPlayer)
    var audioSessionId by remember(exoPlayer) { mutableIntStateOf(exoPlayer.audioSessionId) }

    // Safe +9 dB audio boost for quiet broadcast streams using Android's LoudnessEnhancer
    DisposableEffect(audioSessionId, isAudioBoostEnabled) {
        var enhancer: LoudnessEnhancer? = null
        if (isAudioBoostEnabled && (audioSessionId != C.AUDIO_SESSION_ID_UNSET) && (audioSessionId > 0)) {
            runCatching {
                enhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(900)
                    enabled = true
                }
            }
        }
        onDispose {
            runCatching {
                enhancer?.enabled = false
                enhancer?.release()
            }
        }
    }

    // Real-time sync with physical hardware Volume Up/Down keys & auto-unmute on Volume Up
    DisposableEffect(context, audioManager) {
        var lastKnownVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVolume
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if ((streamType == AudioManager.STREAM_MUSIC) || (streamType == -1)) {
                        val newVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: return
                        if (!isDraggingVolumeSlider) {
                            systemVolume = newVol
                        }
                        if ((newVol > lastKnownVol) && currentIsMuted) {
                            currentOnToggleMute?.invoke()
                            currentExoPlayer.volume = 1f
                        }
                        lastKnownVol = newVol
                    }
                }
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                volumeReceiver,
                IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        onDispose {
            runCatching { context.unregisterReceiver(volumeReceiver) }
        }
    }

    // Properly pause/resume audio & focus across app lifecycle transitions (e.g., ads or backgrounding)
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!currentIsPipMode) {
                        runCatching { exoPlayer.pause() }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    runCatching {
                        systemVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVolume
                        exoPlayer.volume = if (currentIsMuted) 0f else 1f
                        if (currentIsPlaying) {
                            exoPlayer.play()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    exoPlayer.volume = if (currentIsMuted) 0f else 1f
                    if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        audioSessionId = exoPlayer.audioSessionId
                    }
                    isSeekableStream = currentIsVodFile || (exoPlayer.isCurrentMediaItemSeekable && !exoPlayer.isCurrentMediaItemLive)
                    val dur = exoPlayer.duration
                    durationMs = if (dur > 0L) dur else 0L
                    onPlaybackStatusChanged?.invoke(true)
                }
            }

            override fun onAudioSessionIdChanged(newSessionId: Int) {
                if (newSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioSessionId = newSessionId
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    activeVideoHeight = videoSize.height
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val collectedVideo = mutableListOf<PlayerVideoTrackOption>()
                val seenVideoKeys = mutableSetOf<String>()
                tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.forEach { group ->
                    for (i in 0 until group.length) {
                        val fmt = group.getTrackFormat(i)
                        val selected = group.isTrackSelected(i)
                        val isSupported = group.getTrackSupport(i) != C.FORMAT_UNSUPPORTED_TYPE
                        Log.d(
                            "VideoPlayerView",
                            "Video track[$i]: mime=${fmt.sampleMimeType} ${fmt.width}x${fmt.height} bitrate=${fmt.bitrate} fps=${fmt.frameRate} selected=$selected trackSupport=${group.getTrackSupport(i)}",
                        )
                        if (selected && (fmt.height > 0)) {
                            activeVideoHeight = fmt.height
                        }
                        if (isSupported) {
                            val resName = when {
                                fmt.height >= 2160 -> "2160p 4K Ultra HD"
                                fmt.height >= 1440 -> "1440p 2K QHD"
                                fmt.height >= 1080 -> "1080p Full HD"
                                fmt.height >= 720 -> "720p HD"
                                fmt.height >= 576 -> "576p SD"
                                fmt.height >= 480 -> "480p SD"
                                fmt.height >= 360 -> "360p"
                                fmt.height >= 240 -> "240p"
                                fmt.height >= 144 -> "144p"
                                fmt.height > 0 -> "${fmt.height}p"
                                fmt.width > 0 -> "${fmt.width}p"
                                else -> "Video Track ${i + 1}"
                            }
                            val dimenStr = if (fmt.width > 0 && fmt.height > 0) "${fmt.width}x${fmt.height}" else ""
                            val bitrateLabel = when {
                                fmt.bitrate >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", fmt.bitrate / 1_000_000f)
                                fmt.bitrate > 0 -> "${fmt.bitrate / 1000} kbps"
                                else -> ""
                            }
                            val fpsLabel = if (fmt.frameRate > 0f) "${fmt.frameRate.roundToInt()}fps" else ""
                            val details = listOf(dimenStr, bitrateLabel, fpsLabel).filter(String::isNotBlank).joinToString(" • ")
                            val fullLabel = if (details.isNotBlank()) "$resName ($details)" else resName

                            val uniqueKey = "${fmt.width}x${fmt.height}_${fmt.bitrate}_$i"
                            if (seenVideoKeys.add(uniqueKey)) {
                                collectedVideo.add(
                                    PlayerVideoTrackOption(
                                        group = group,
                                        trackIndex = i,
                                        width = fmt.width,
                                        height = fmt.height,
                                        bitrate = fmt.bitrate,
                                        label = fullLabel,
                                        isSelected = selected,
                                    ),
                                )
                            }
                        }
                    }
                }
                availableVideoTracks = collectedVideo.sortedWith(
                    compareByDescending<PlayerVideoTrackOption> { it.height }.thenByDescending { it.bitrate },
                )

                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (audioGroups.isEmpty()) {
                    availableAudioTracks = emptyList()
                    Log.w("VideoPlayerView", "No audio track groups detected for stream: $currentStreamUrl")
                    return
                }

                val collected = mutableListOf<PlayerAudioTrackOption>()
                var anySelected = false
                var firstSupported: Pair<Tracks.Group, Int>? = null
                var trackCounter = 1

                audioGroups.forEach { group ->
                    for (i in 0 until group.length) {
                        val fmt = group.getTrackFormat(i)
                        val selected = group.isTrackSelected(i)
                        val supported = group.isTrackSupported(i)
                        if (selected) anySelected = true
                        Log.d(
                            "VideoPlayerView",
                            "Audio track[$i]: mime=${fmt.sampleMimeType} lang=${fmt.language} ch=${fmt.channelCount} sr=${fmt.sampleRate} selected=$selected supported=$supported",
                        )
                        if (supported) {
                            if (firstSupported == null) firstSupported = Pair(group, i)
                            val langName = fmt.language?.takeIf { it.isNotBlank() && it != "und" }
                                ?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) ?: it.uppercase(Locale.US) }
                            val codecName = formatAudioCodecName(fmt.sampleMimeType)
                            val chLabel = when (fmt.channelCount) {
                                1 -> "Mono"
                                2 -> "Stereo"
                                6 -> "5.1"
                                8 -> "7.1"
                                else -> if (fmt.channelCount > 0) "${fmt.channelCount}ch" else ""
                            }
                            val baseLabel = fmt.label?.takeIf(String::isNotBlank) ?: langName ?: "Audio $trackCounter"
                            val details = listOf(codecName, chLabel).filter(String::isNotBlank).joinToString(" ")
                            collected.add(
                                PlayerAudioTrackOption(
                                    group = group,
                                    trackIndex = i,
                                    label = if (details.isNotBlank()) "$baseLabel ($details)" else baseLabel,
                                    isSelected = selected,
                                ),
                            )
                            trackCounter++
                        }
                    }
                }
                availableAudioTracks = collected

                // Auto-recovery: if the stream has a supported audio track but none was selected, force-select it
                if (!anySelected) {
                    firstSupported?.let { (group, idx) ->
                        Log.i("VideoPlayerView", "Auto-selecting supported audio track[$idx] for stream: $currentStreamUrl")
                        runCatching {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, idx))
                                .build()
                        }
                    }
                }
            }

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
                    exoPlayer.volume = if (currentIsMuted) 0f else 1f
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
                if (currentIsVodFile && (exoPlayer.playbackState != Player.STATE_IDLE) && (exoPlayer.currentPosition > 0L)) {
                    currentSavedVodPositionState.longValue = exoPlayer.currentPosition
                }
                exoPlayer.removeListener(listener)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            }
        }
    }

    // Periodic progress & device volume sync
    LaunchedEffect(exoPlayer, streamUrl, isVodFile) {
        while (true) {
            delay(1_000.milliseconds)
            runCatching {
                if (!isDraggingVolumeSlider) {
                    systemVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVolume
                }
                if ((exoPlayer.playbackState != Player.STATE_IDLE) && (exoPlayer.playbackState != Player.STATE_ENDED)) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    if (!isUserScrubbing) {
                        currentPositionMs = pos
                    }
                    val dur = exoPlayer.duration
                    if (dur > 0L) {
                        durationMs = dur
                    }
                    if (isVodFile && pos > 0L) {
                        savedVodPositionMs = pos
                    }
                }
            }
        }
    }

    // Prepare and play stream whenever exoPlayer, streamUrl, retryTrigger, or streamFallbackMode changes
    LaunchedEffect(exoPlayer, mediaSourceFactories, streamUrl, retryTrigger, streamFallbackMode) {
        if (!streamUrl.isNullOrBlank()) {
            isBuffering = true
            playbackError = null
            try {
                val (defaultMediaSourceFactory, hlsMediaSourceFactory) = mediaSourceFactories
                val mediaItem = createUniversalMediaItem(streamUrl, fallbackMode = streamFallbackMode)
                val isHlsMime = mediaItem.localConfiguration?.mimeType == MimeTypes.APPLICATION_M3U8 ||
                    streamUrl.contains(".m3u8", ignoreCase = true) ||
                    streamUrl.contains("hls", ignoreCase = true)
                val mediaSource = if (isHlsMime) {
                    hlsMediaSourceFactory.createMediaSource(mediaItem)
                } else {
                    defaultMediaSourceFactory.createMediaSource(mediaItem)
                }
                if (isVodFile) {
                    exoPlayer.setMediaSource(mediaSource, savedVodPositionMs)
                } else {
                    exoPlayer.setMediaSource(mediaSource)
                }
                exoPlayer.volume = if (currentIsMuted) 0f else 1f
                exoPlayer.prepare()
                if (currentIsPlaying) {
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

    // React to play/pause state (keyed on exoPlayer too!)
    LaunchedEffect(exoPlayer, isPlaying) {
        if (isPlaying) {
            exoPlayer.volume = if (currentIsMuted) 0f else 1f
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // React to mute state (keyed on exoPlayer too!)
    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Apply Video Quality, Manual Stream Resolution Override, and Reliable Audio Preferences to ExoPlayer
    LaunchedEffect(exoPlayer, playbackQuality, selectedVideoTrackOverride, subtitlesEnabled) {
        val currentParams = (exoPlayer.trackSelectionParameters as? DefaultTrackSelector.Parameters)
            ?: DefaultTrackSelector.Parameters.DEFAULT

        val paramsBuilder = currentParams.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            .setPreferredVideoMimeTypes(
                MimeTypes.VIDEO_H265,
                MimeTypes.VIDEO_H264,
                MimeTypes.VIDEO_VP9,
                MimeTypes.VIDEO_AV1,
            )
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            .setAllowAudioMixedSampleRateAdaptiveness(true)
            .setAllowAudioMixedChannelCountAdaptiveness(true)
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowVideoMixedDecoderSupportAdaptiveness(true)
            .setExceedAudioConstraintsIfNecessary(true)
            .setExceedVideoConstraintsIfNecessary(true)
            .setExceedRendererCapabilitiesIfNecessary(true)

        val manualTrack = selectedVideoTrackOverride
        if (manualTrack != null) {
            paramsBuilder.clearVideoSizeConstraints()
                .setMinVideoSize(0, 0)
                .setMinVideoBitrate(0)
                .setMaxVideoBitrate(Int.MAX_VALUE)
                .setForceHighestSupportedBitrate(false)
                .setForceLowestBitrate(false)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .setOverrideForType(TrackSelectionOverride(manualTrack.group.mediaTrackGroup, manualTrack.trackIndex))
        } else {
            paramsBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            when (playbackQuality) {
                PlaybackQuality.AUTO -> {
                    paramsBuilder.clearVideoSizeConstraints()
                        .setMinVideoSize(0, 0)
                        .setMinVideoBitrate(0)
                        .setMaxVideoBitrate(Int.MAX_VALUE)
                        .setForceHighestSupportedBitrate(false)
                        .setForceLowestBitrate(false)
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                }
                PlaybackQuality.HIGH -> {
                    paramsBuilder.clearVideoSizeConstraints()
                        .setMinVideoSize(0, 0)
                        .setMinVideoBitrate(0)
                        .setMaxVideoBitrate(Int.MAX_VALUE)
                        .setForceHighestSupportedBitrate(true)
                        .setForceLowestBitrate(false)
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                }
                PlaybackQuality.DATA_SAVER -> {
                    paramsBuilder.setMinVideoSize(0, 0)
                        .setMaxVideoSize(854, 480)
                        .setMinVideoBitrate(0)
                        .setMaxVideoBitrate(1_200_000)
                        .setForceHighestSupportedBitrate(false)
                        .setForceLowestBitrate(false)
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                }
                PlaybackQuality.AUDIO_ONLY -> {
                    paramsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                }
            }
        }
        exoPlayer.trackSelectionParameters = paramsBuilder.build()
    }

    val isEffectivelyMuted = isMuted || (systemVolume == 0)
    val effectiveVolumePercent = if (isMuted) 0 else ((systemVolume * 100) / maxVolume).coerceIn(0, 100)

    fun applySystemVolume(targetVol: Int, showHud: Boolean = true) {
        val clamped = targetVol.coerceIn(0, maxVolume)
        audioManager?.let { am ->
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (clamped != curVol) {
                runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0) }
            }
            if ((clamped > 0) && am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                runCatching { am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0) }
            }
        }
        systemVolume = clamped
        if ((clamped > 0) && currentIsMuted) {
            currentOnToggleMute?.invoke()
            currentExoPlayer.volume = 1f
        } else if (clamped > 0) {
            currentExoPlayer.volume = 1f
        }
        if (showHud) {
            volumeHudPercentage = ((clamped * 100) / maxVolume).coerceIn(0, 100)
        }
    }

    fun handleMuteToggle() {
        controlsInteractionTick++
        val curSysVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVolume
        if (isMuted) {
            onToggleMute?.invoke()
            currentExoPlayer.volume = 1f
            if (curSysVol == 0) {
                val restored = (maxVolume * 0.5f).toInt().coerceIn(1, maxVolume)
                applySystemVolume(restored, showHud = true)
            } else {
                volumeHudPercentage = ((curSysVol * 100) / maxVolume).coerceIn(1, 100)
            }
        } else if (curSysVol == 0) {
            // Player isn't muted, but device media volume was at 0 — restore audible sound immediately!
            val restored = (maxVolume * 0.5f).toInt().coerceIn(1, maxVolume)
            applySystemVolume(restored, showHud = true)
        } else {
            onToggleMute?.invoke()
            currentExoPlayer.volume = 0f
            volumeHudPercentage = 0
        }
    }

    MyApplicationTheme(themeMode = AppThemeMode.DARK) {
        val playerSafeInsets = if (isFullscreen) {
            WindowInsets.displayCutout
        } else {
            WindowInsets.safeDrawing
        }

        Box(
            modifier = modifier
                .then(
                    if (!isFullscreen && !isPipMode) {
                        Modifier.tvScreenBezel(cornerRadius = 14.dp)
                    } else {
                        Modifier
                    },
                )
                .background(Color.Black)
                .testTag("video_player_container"),
        ) {
            // 1. Video / Casting / Audio-Only Surface
            if (isCasting && (castDeviceName != null)) {
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
                            text = "Audio Only",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Video paused to save data",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val container = FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                clipChildren = true
                                clipToPadding = true
                            }
                            val inflated = runCatching {
                                LayoutInflater.from(ctx).inflate(R.layout.player_texture_view, container, false) as? PlayerView
                            }.getOrNull()
                            val playerView = (inflated ?: PlayerView(ctx)).apply {
                                player = exoPlayer
                                useController = false
                                keepScreenOn = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            }
                            container.addView(playerView)
                            container
                        },
                        update = { container ->
                            val playerView = container.getChildAt(0) as? PlayerView ?: return@AndroidView
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
                            // Apply native View zoom/pan directly on the SurfaceView-backed PlayerView
                            // with exact hardware SurfaceFlinger clipBounds so zoomed video stays sharp and cropped
                            val w = container.width
                            val h = container.height
                            val maxPanX = if (w > 0) (w * (videoZoomScale - 1f)) / 2f else 0f
                            val maxPanY = if (h > 0) (h * (videoZoomScale - 1f)) / 2f else 0f
                            val clampedTx = if (videoZoomScale <= 1f) 0f else videoPanOffset.x.coerceIn(-maxPanX, maxPanX)
                            val clampedTy = if (videoZoomScale <= 1f) 0f else videoPanOffset.y.coerceIn(-maxPanY, maxPanY)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                playerView.resetPivot()
                            } else {
                                playerView.pivotX = playerView.width / 2f
                                playerView.pivotY = playerView.height / 2f
                            }
                            playerView.scaleX = videoZoomScale
                            playerView.scaleY = videoZoomScale
                            playerView.translationX = clampedTx
                            playerView.translationY = clampedTy

                            val surfaceView = playerView.videoSurfaceView
                            if (surfaceView != null) {
                                if ((videoZoomScale > 1.001f) && (w > 0) && (h > 0) && (surfaceView.width > 0) && (surfaceView.height > 0)) {
                                    var ox = 0f
                                    var oy = 0f
                                    var cur: View? = surfaceView
                                    while ((cur != null) && (cur !== playerView)) {
                                        ox += cur.left
                                        oy += cur.top
                                        cur = cur.parent as? View
                                    }
                                    val halfW = w / 2f
                                    val halfH = h / 2f
                                    val left = (((-halfW - clampedTx) / videoZoomScale) + halfW - ox).toInt().coerceIn(0, surfaceView.width)
                                    val top = (((-halfH - clampedTy) / videoZoomScale) + halfH - oy).toInt().coerceIn(0, surfaceView.height)
                                    val right = (((halfW - clampedTx) / videoZoomScale) + halfW - ox).toInt().coerceIn(left, surfaceView.width)
                                    val bottom = (((halfH - clampedTy) / videoZoomScale) + halfH - oy).toInt().coerceIn(top, surfaceView.height)
                                    surfaceView.clipBounds = Rect(left, top, right, bottom)
                                } else if (surfaceView.clipBounds != null) {
                                    surfaceView.clipBounds = null
                                }
                            }
                        },
                        onRelease = { container ->
                            (container.getChildAt(0) as? PlayerView)?.player = null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 2. Single Unified Touch / Gesture Surface (two-finger pinch-to-zoom + brightness/volume/tap/double-tap)
            if (!isPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isScreenLocked) {
                            if (!isScreenLocked) {
                                awaitEachGesture {
                                    var didPinch = false
                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedCount = event.changes.count { it.pressed }
                                        if (pressedCount >= 2) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            if (abs(zoomChange - 1f) > 0.005f || panChange != Offset.Zero) {
                                                didPinch = true
                                                val newScale = (videoZoomScale * zoomChange).coerceIn(1f, 4f)
                                                val snappedScale = if (newScale <= 1.02f) 1f else newScale
                                                videoZoomScale = snappedScale
                                                if (snappedScale <= 1f) {
                                                    videoPanOffset = Offset.Zero
                                                } else {
                                                    val maxPanX = (size.width * (snappedScale - 1f)) / 2f
                                                    val maxPanY = (size.height * (snappedScale - 1f)) / 2f
                                                    videoPanOffset = Offset(
                                                        x = (videoPanOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                                                        y = (videoPanOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                                                    )
                                                }
                                                zoomHudPercentage = (snappedScale * 100f).roundToInt()
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (didPinch && pressedCount == 1) {
                                            // Consume remaining lift motion after a two-finger pinch so it doesn't trigger a stray tap/swipe
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                        }
                        .pointerInput(isScreenLocked, maxVolume) {
                            if (!isScreenLocked) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        dragStartX = offset.x
                                        accumulatedVolumeDrag = 0f
                                        accumulatedBrightnessDrag = 0f
                                    },
                                    onDragEnd = {
                                        accumulatedVolumeDrag = 0f
                                        accumulatedBrightnessDrag = 0f
                                    },
                                    onDragCancel = {
                                        accumulatedVolumeDrag = 0f
                                        accumulatedBrightnessDrag = 0f
                                    },
                                ) { change, dragAmount ->
                                    if (change.isConsumed) return@detectVerticalDragGestures
                                    val viewWidth = size.width
                                    val viewHeight = size.height.coerceAtLeast(300)
                                    if (viewWidth > 0) {
                                        if (dragStartX < (viewWidth * 0.35f)) {
                                            // Left 35%: Smooth Brightness control
                                            accumulatedBrightnessDrag += dragAmount
                                            val threshold = (viewHeight * 0.025f).coerceAtLeast(16f)
                                            if (abs(accumulatedBrightnessDrag) >= threshold) {
                                                activity?.let { act ->
                                                    val current = act.window.attributes.screenBrightness.let {
                                                        if (it < 0f) 0.5f else it
                                                    }
                                                    val brightnessDelta = -accumulatedBrightnessDrag / (viewHeight * 0.65f)
                                                    val newBrightness = (current + brightnessDelta).coerceIn(0.05f, 1.0f)
                                                    val lp = act.window.attributes
                                                    lp.screenBrightness = newBrightness
                                                    runCatching { act.window.attributes = lp }
                                                    brightnessHudPercentage = (newBrightness * 100).toInt()
                                                }
                                                accumulatedBrightnessDrag = 0f
                                            }
                                        } else if (dragStartX > (viewWidth * 0.65f)) {
                                            // Right 35%: Smooth, height-normalized Volume control
                                            accumulatedVolumeDrag += dragAmount
                                            val stepPixels = ((viewHeight * 0.55f) / maxVolume.toFloat()).coerceAtLeast(28f)
                                            if (abs(accumulatedVolumeDrag) >= stepPixels) {
                                                val steps = (accumulatedVolumeDrag / stepPixels).toInt()
                                                if (steps != 0) {
                                                    val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: systemVolume
                                                    val delta = -steps
                                                    val newVol = (curVol + delta).coerceIn(0, maxVolume)
                                                    if ((delta > 0) && currentIsMuted) {
                                                        currentOnToggleMute?.invoke()
                                                        currentExoPlayer.volume = 1f
                                                    }
                                                    applySystemVolume(newVol, showHud = true)
                                                    accumulatedVolumeDrag %= stepPixels
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(isScreenLocked, isSeekableStream) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (!isScreenLocked) {
                                        val viewWidth = size.width
                                        if (viewWidth > 0) {
                                            when {
                                                (offset.x < (viewWidth * 0.35f)) && isSeekableStream -> {
                                                    val target = (currentExoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                                    runCatching { currentExoPlayer.seekTo(target) }
                                                    currentPositionMs = target
                                                    playerFlashMessage = "« Rewind 10s"
                                                }
                                                (offset.x > (viewWidth * 0.65f)) && isSeekableStream -> {
                                                    val target = currentExoPlayer.currentPosition + 10_000L
                                                    val dur = currentExoPlayer.duration
                                                    if ((dur == C.TIME_UNSET) || (target <= dur)) {
                                                        runCatching { currentExoPlayer.seekTo(target) }
                                                        currentPositionMs = target
                                                    }
                                                    playerFlashMessage = "Forward 10s »"
                                                }
                                                else -> {
                                                    currentOnTogglePlayPause()
                                                    controlsInteractionTick++
                                                }
                                            }
                                        }
                                    }
                                },
                                onTap = {
                                    if (isScreenLocked) {
                                        showUnlockButton = !showUnlockButton
                                    } else {
                                        showControls = !showControls
                                        if (showControls) controlsInteractionTick++
                                    }
                                },
                            )
                        },
                )
            }

            // 3. Standalone Buffering indicator (only shown when controls are hidden so it never overlaps Play/Pause)
            if (isBuffering && isPlaying && (playbackError == null) && (!showControls || isScreenLocked || isPipMode)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            // 4. Stream Error & Retry Overlay
            if ((playbackError != null) && !isPipMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .windowInsetsPadding(playerSafeInsets),
                ) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(48.dp)
                            .testTag("error_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Player",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = LiveRed,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stream unavailable",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Check your connection or try another format.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFF121212),
                                    modifier = Modifier.size(16.dp),
                                )
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
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Next", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Clean Overlay Controls
            if (showControls && !isPipMode && !isScreenLocked && (playbackError == null)) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.82f),
                                ),
                            ),
                        ),
                ) {
                    val isCompactWidth = maxWidth < 540.dp
                    val isNarrowPortrait = maxWidth < 400.dp

                    val topUtilityActions: @Composable () -> Unit = {
                        IconButton(
                            onClick = {
                                controlsInteractionTick++
                                onToggleFavorite()
                            },
                            modifier = Modifier.testTag("player_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFF59E0B) else Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                controlsInteractionTick++
                                showSleepTimerDialog = true
                            },
                            modifier = Modifier.testTag("player_sleep_timer_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerMinutes != null) Color(0xFFF59E0B) else Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                controlsInteractionTick++
                                onOpenCast()
                            },
                            modifier = Modifier.testTag("player_cast_button"),
                        ) {
                            Icon(
                                imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = if (isCasting) Color(0xFF38BDF8) else Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                controlsInteractionTick++
                                onEnterPip()
                            },
                            modifier = Modifier.testTag("player_pip_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                controlsInteractionTick++
                                context.findActivity()?.let { act ->
                                    com.example.util.AdManager.showRewardedOrInterstitialAd(
                                        act,
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
                    }

                    // Top Bar: Back, Channel Name, Utility Actions, Lock
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                playerSafeInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                            )
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = if (isFullscreen) 16.dp else 8.dp,
                                bottom = 8.dp,
                            )
                            .align(Alignment.TopCenter),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isFullscreen) {
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.testTag("player_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to channels",
                                        tint = Color.White,
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                            ) {
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

                            if (!isCompactWidth) {
                                topUtilityActions()
                            }

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

                        if (isCompactWidth) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                topUtilityActions()
                            }
                        }
                    }

                    // Center Transport Row (Clean & Contextual: Prev, [Seek -10s if seekable], Play/Pause, [Seek +10s if seekable], Next)
                    val centerSpacing = if (isNarrowPortrait) 14.dp else 22.dp
                    val sideBtnSize = if (isNarrowPortrait) 44.dp else 50.dp
                    val playBtnSize = if (isNarrowPortrait) 58.dp else 66.dp

                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp)
                            .testTag("player_center_controls_row"),
                        horizontalArrangement = Arrangement.spacedBy(centerSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        onPreviousChannel?.let { previousChannel ->
                            SkeuoButton(
                                onClick = {
                                    controlsInteractionTick++
                                    previousChannel()
                                },
                                cornerRadius = 26.dp,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                borderColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(sideBtnSize)
                                    .testTag("player_prev_channel_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isNarrowPortrait) 24.dp else 26.dp),
                                )
                            }
                        }

                        if (isSeekableStream) {
                            SkeuoButton(
                                onClick = {
                                    controlsInteractionTick++
                                    val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    runCatching { exoPlayer.seekTo(target) }
                                    currentPositionMs = target
                                },
                                cornerRadius = 26.dp,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                borderColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(sideBtnSize)
                                    .testTag("player_rewind_10_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        SkeuoButton(
                            onClick = {
                                controlsInteractionTick++
                                onTogglePlayPause()
                            },
                            isAccent = true,
                            cornerRadius = 34.dp,
                            containerColor = Color.White,
                            borderColor = Color.White,
                            modifier = Modifier
                                .size(playBtnSize)
                                .testTag("player_play_pause_button"),
                        ) {
                            if (isBuffering && isPlaying) {
                                CircularProgressIndicator(
                                    color = Color(0xFF121212),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(if (isNarrowPortrait) 26.dp else 30.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color(0xFF121212),
                                    modifier = Modifier.size(if (isNarrowPortrait) 32.dp else 38.dp),
                                )
                            }
                        }

                        if (isSeekableStream) {
                            SkeuoButton(
                                onClick = {
                                    controlsInteractionTick++
                                    val target = exoPlayer.currentPosition + 10_000L
                                    val dur = exoPlayer.duration
                                    if ((dur == C.TIME_UNSET) || (target <= dur)) {
                                        runCatching { exoPlayer.seekTo(target) }
                                        currentPositionMs = target
                                    }
                                },
                                cornerRadius = 26.dp,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                borderColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(sideBtnSize)
                                    .testTag("player_forward_10_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        onNextChannel?.let { nextChannel ->
                            SkeuoButton(
                                onClick = {
                                    controlsInteractionTick++
                                    nextChannel()
                                },
                                cornerRadius = 26.dp,
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                borderColor = Color.White.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .size(sideBtnSize)
                                    .testTag("player_next_channel_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isNarrowPortrait) 24.dp else 26.dp),
                                )
                            }
                        }
                    }

                    // Bottom Bar: Live/Seek Bar + Clear Audio & Video Tools
                    val bottomIconSize = if (isNarrowPortrait) 40.dp else 46.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                playerSafeInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .align(Alignment.BottomCenter),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Seek bar for VOD / seekable streams OR Live indicator + Muted quick-restore chip
                        if (isSeekableStream && (durationMs > 0L)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val shownPos = if (isUserScrubbing) scrubPositionMs.toLong() else currentPositionMs
                                Text(
                                    text = formatPlayerTime(shownPos),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Slider(
                                    value = shownPos.toFloat().coerceIn(0f, durationMs.toFloat()),
                                    onValueChange = { newVal ->
                                        isUserScrubbing = true
                                        scrubPositionMs = newVal
                                        controlsInteractionTick++
                                    },
                                    onValueChangeFinished = {
                                        val target = scrubPositionMs.toLong()
                                        runCatching { exoPlayer.seekTo(target) }
                                        currentPositionMs = target
                                        isUserScrubbing = false
                                    },
                                    valueRange = 0f..durationMs.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF38BDF8),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = formatPlayerTime(durationMs),
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    onClick = {
                                        controlsInteractionTick++
                                        runCatching { exoPlayer.seekToDefaultPosition() }
                                        playerFlashMessage = "Synced to Live"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = LiveGreen.copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, LiveGreen.copy(alpha = 0.55f)),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isPlaying) LiveGreen else Color(0xFFF59E0B)),
                                        )
                                        Text(
                                            text = if (isPlaying) "LIVE" else "PAUSED",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                if (videoZoomScale > 1.01f) {
                                    Surface(
                                        onClick = {
                                            controlsInteractionTick++
                                            videoZoomScale = 1f
                                            videoPanOffset = Offset.Zero
                                            playerFlashMessage = "Zoom Reset (1.0x)"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                        modifier = Modifier.testTag("player_zoom_reset_chip"),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1fx • Reset", videoZoomScale),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }

                                if (isEffectivelyMuted) {
                                    Surface(
                                        onClick = { handleMuteToggle() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = LiveRed.copy(alpha = 0.28f),
                                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.7f)),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = "Sound Off • Tap to Unmute",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Portrait Volume Slider Row (always accessible even on narrow screens)
                        if (isCompactWidth) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                IconButton(
                                    onClick = { handleMuteToggle() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("player_mute_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isEffectivelyMuted) {
                                            Icons.AutoMirrored.Filled.VolumeOff
                                        } else {
                                            Icons.AutoMirrored.Filled.VolumeUp
                                        },
                                        contentDescription = if (isEffectivelyMuted) "Unmute" else "Mute",
                                        tint = if (isEffectivelyMuted) LiveRed else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                Slider(
                                    value = if (isMuted) 0f else systemVolume.toFloat(),
                                    onValueChange = { newVal ->
                                        isDraggingVolumeSlider = true
                                        controlsInteractionTick++
                                        val intVol = newVal.roundToInt().coerceIn(0, maxVolume)
                                        applySystemVolume(intVol, showHud = false)
                                    },
                                    onValueChangeFinished = {
                                        isDraggingVolumeSlider = false
                                    },
                                    valueRange = 0f..maxVolume.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF38BDF8),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .testTag("player_volume_slider"),
                                )

                                Text(
                                    text = "$effectiveVolumePercent%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(34.dp),
                                    textAlign = TextAlign.End,
                                )

                                SkeuoButton(
                                    onClick = {
                                        controlsInteractionTick++
                                        isAudioBoostEnabled = !isAudioBoostEnabled
                                        playerFlashMessage = if (isAudioBoostEnabled) "Audio Boost On (+9 dB)" else "Audio Boost Off"
                                    },
                                    cornerRadius = 8.dp,
                                    containerColor = if (isAudioBoostEnabled) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.65f),
                                    borderColor = if (isAudioBoostEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.testTag("player_volume_boost_button"),
                                ) {
                                    Text(
                                        text = if (isAudioBoostEnabled) "Boost" else "100%",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        // Bottom Tools Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Left: Volume/Mute + Inline Slider (Landscape), Quality, Aspect Ratio
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isNarrowPortrait) 6.dp else 8.dp),
                            ) {
                                if (!isCompactWidth) {
                                    IconButton(
                                        onClick = { handleMuteToggle() },
                                        modifier = Modifier
                                            .size(bottomIconSize)
                                            .testTag("player_mute_button"),
                                    ) {
                                        Icon(
                                            imageVector = if (isEffectivelyMuted) {
                                                Icons.AutoMirrored.Filled.VolumeOff
                                            } else {
                                                Icons.AutoMirrored.Filled.VolumeUp
                                            },
                                            contentDescription = if (isEffectivelyMuted) "Unmute" else "Mute",
                                            tint = if (isEffectivelyMuted) LiveRed else Color.White,
                                        )
                                    }

                                    Slider(
                                        value = if (isMuted) 0f else systemVolume.toFloat(),
                                        onValueChange = { newVal ->
                                            isDraggingVolumeSlider = true
                                            controlsInteractionTick++
                                            val intVol = newVal.roundToInt().coerceIn(0, maxVolume)
                                            applySystemVolume(intVol, showHud = false)
                                        },
                                        onValueChangeFinished = {
                                            isDraggingVolumeSlider = false
                                        },
                                        valueRange = 0f..maxVolume.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF38BDF8),
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                        ),
                                        modifier = Modifier
                                            .width(104.dp)
                                            .height(28.dp)
                                            .testTag("player_volume_slider"),
                                    )

                                    Text(
                                        text = "$effectiveVolumePercent%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(34.dp),
                                    )

                                    SkeuoButton(
                                        onClick = {
                                            controlsInteractionTick++
                                            isAudioBoostEnabled = !isAudioBoostEnabled
                                            playerFlashMessage = if (isAudioBoostEnabled) "Audio Boost On (+9 dB)" else "Audio Boost Off"
                                        },
                                        cornerRadius = 8.dp,
                                        containerColor = if (isAudioBoostEnabled) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.65f),
                                        borderColor = if (isAudioBoostEnabled) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                        modifier = Modifier.testTag("player_volume_boost_button"),
                                    ) {
                                        Text(
                                            text = if (isAudioBoostEnabled) "Boost" else "+dB",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        )
                                    }
                                }

                                SkeuoButton(
                                    onClick = {
                                        controlsInteractionTick++
                                        showQualityDialog = true
                                    },
                                    cornerRadius = 8.dp,
                                    containerColor = Color.Black.copy(alpha = 0.65f),
                                    borderColor = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.testTag("player_quality_pill"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = if (isNarrowPortrait) 6.dp else 8.dp, vertical = 5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = "Quality",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val manualRes = selectedVideoTrackOverride
                                        val qualityText = when {
                                            manualRes != null -> "${manualRes.height}p"
                                            isNarrowPortrait -> when (playbackQuality) {
                                                PlaybackQuality.AUTO -> if (activeVideoHeight > 0) "Auto ${activeVideoHeight}p" else "Auto"
                                                PlaybackQuality.HIGH -> "HD"
                                                PlaybackQuality.DATA_SAVER -> "Saver"
                                                PlaybackQuality.AUDIO_ONLY -> "Audio"
                                            }
                                            playbackQuality == PlaybackQuality.AUTO && (activeVideoHeight > 0) -> "Auto • ${activeVideoHeight}p"
                                            else -> playbackQuality.label
                                        }
                                        Text(
                                            text = qualityText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }

                                SkeuoButton(
                                    onClick = {
                                        controlsInteractionTick++
                                        showAspectRatioDialog = true
                                    },
                                    cornerRadius = 8.dp,
                                    containerColor = Color.Black.copy(alpha = 0.65f),
                                    borderColor = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.testTag("player_aspect_ratio_pill"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = if (isNarrowPortrait) 6.dp else 8.dp, vertical = 5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val aspectText = if (isNarrowPortrait) {
                                            when (aspectRatioMode) {
                                                AspectRatioMode.AUTO -> "Auto"
                                                AspectRatioMode.FIT -> "Fit"
                                                AspectRatioMode.FILL -> "Fill"
                                                AspectRatioMode.STRETCH -> "Full"
                                                AspectRatioMode.FOUR_THREE -> "4:3"
                                            }
                                        } else {
                                            aspectRatioMode.label
                                        }
                                        Text(
                                            text = aspectText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }

                                SkeuoButton(
                                    onClick = {
                                        controlsInteractionTick++
                                        showZoomDialog = true
                                    },
                                    cornerRadius = 8.dp,
                                    containerColor = if (videoZoomScale > 1.01f) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.65f),
                                    borderColor = if (videoZoomScale > 1.01f) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.testTag("player_zoom_pill"),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = if (isNarrowPortrait) 6.dp else 8.dp, vertical = 5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = "Zoom",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (videoZoomScale > 1.01f) String.format(Locale.US, "%.1fx", videoZoomScale) else "Zoom",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }

                            // Right: Audio Track (when multi-audio), Rotate Screen, Exit Fullscreen
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (availableAudioTracks.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            controlsInteractionTick++
                                            showAudioTrackDialog = true
                                        },
                                        modifier = Modifier
                                            .size(bottomIconSize)
                                            .testTag("player_audio_track_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = "Audio Track",
                                            tint = Color(0xFF38BDF8),
                                        )
                                    }
                                }

                                if (isFullscreen) {
                                    IconButton(
                                        onClick = {
                                            controlsInteractionTick++
                                            (activity as? com.example.MainActivity)?.toggleScreenOrientation()
                                        },
                                        modifier = Modifier
                                            .size(bottomIconSize)
                                            .testTag("player_rotate_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenRotation,
                                            contentDescription = "Rotate Screen",
                                            tint = Color.White,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier
                                        .size(bottomIconSize)
                                        .testTag("player_fullscreen_button"),
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

            // 6. Brightness Control HUD (left side gesture)
            val currentBrightness = brightnessHudPercentage
            if (currentBrightness != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .windowInsetsPadding(playerSafeInsets.only(WindowInsetsSides.Start))
                        .padding(start = 24.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
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
                                    .width(84.dp)
                                    .height(4.dp),
                                color = Color(0xFF38BDF8),
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            // 7. Volume Control HUD (right side gesture)
            val currentVolume = volumeHudPercentage
            if (currentVolume != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(playerSafeInsets.only(WindowInsetsSides.End))
                        .padding(end = 24.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (currentVolume == 0) {
                                Icons.AutoMirrored.Filled.VolumeMute
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
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
                                    .width(84.dp)
                                    .height(4.dp),
                                color = Color(0xFF38BDF8),
                                trackColor = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            // 7b. Zoom HUD (two-finger pinch-to-zoom feedback)
            val currentZoomPercent = zoomHudPercentage
            if (currentZoomPercent != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.84f),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(playerSafeInsets.only(WindowInsetsSides.Top))
                        .padding(top = if (isFullscreen) 18.dp else 12.dp)
                        .testTag("player_zoom_hud"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (currentZoomPercent <= 102) {
                                "Zoom 1.0x (Normal)"
                            } else {
                                String.format(Locale.US, "Zoom %.1fx (%d%%)", currentZoomPercent / 100f, currentZoomPercent)
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // 8. Flash Feedback HUD
            if (playerFlashMessage != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.88f),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(playerSafeInsets.only(WindowInsetsSides.Top))
                        .padding(top = if (isFullscreen) 16.dp else 12.dp)
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

            // 9. Lock Screen Floating Unlock Button
            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(playerSafeInsets),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showUnlockButton) {
                        Surface(
                            onClick = {
                                isScreenLocked = false
                                showUnlockButton = false
                                showControls = true
                                controlsInteractionTick++
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.85f),
                            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
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

            // 10. Aspect Ratio Picker Dialog
            if (showAspectRatioDialog) {
                AlertDialog(
                    onDismissRequest = { showAspectRatioDialog = false },
                    title = {
                        Text(
                            text = "Aspect Ratio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AspectRatioMode.entries.forEach { mode ->
                                val isSelected = aspectRatioMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.RadioButton,
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
                                            AspectRatioMode.FIT -> "Standard 16:9 widescreen fit"
                                            AspectRatioMode.FILL -> "Zoom to fill the entire screen"
                                            AspectRatioMode.STRETCH -> "Stretch to exact screen size"
                                            AspectRatioMode.FOUR_THREE -> "Classic 4:3 TV format"
                                            AspectRatioMode.AUTO -> "Auto-detect stream ratio"
                                        }
                                        Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAspectRatioDialog = false }) {
                            Text("Close")
                        }
                    },
                )
            }

            // 11. Quality & Bitrate Picker Dialog (Adaptive Modes + Live Stream Resolutions)
            if (showQualityDialog) {
                val currentPlayingHeight = exoPlayer.videoFormat?.height?.takeIf { it > 0 } ?: activeVideoHeight
                AlertDialog(
                    onDismissRequest = { showQualityDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "Stream Quality & Resolution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Section 1: Default Auto Resolution Option
                            val isAutoActive = (selectedVideoTrackOverride == null) && (playbackQuality == PlaybackQuality.AUTO)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .selectable(
                                        selected = isAutoActive,
                                        role = Role.RadioButton,
                                    ) {
                                        selectedVideoTrackOverride = null
                                        onSelectQuality?.invoke(PlaybackQuality.AUTO)
                                        playerFlashMessage = "Quality: Auto (Adaptive)"
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isAutoActive,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Auto (Adaptive Resolution)",
                                            fontWeight = if (isAutoActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isAutoActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                text = "DEFAULT",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                    val autoSub = if (currentPlayingHeight > 0) {
                                        "Automatically adjusts to network speed • Currently playing ${currentPlayingHeight}p"
                                    } else {
                                        "Automatically adjusts quality to match network speed"
                                    }
                                    Text(text = autoSub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Section 2: All Available Stream Resolutions (2160p, 1080p, 720p, 480p, 360p, 240p, 144p)
                            if (availableVideoTracks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "AVAILABLE RESOLUTIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                                availableVideoTracks.forEach { track ->
                                    val isTrackSelected = (selectedVideoTrackOverride?.height == track.height) &&
                                        (selectedVideoTrackOverride?.bitrate == track.bitrate)
                                    val isCurrentPlayingRes = isAutoActive && (currentPlayingHeight > 0) && (track.height == currentPlayingHeight)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .selectable(
                                                selected = isTrackSelected,
                                                role = Role.RadioButton,
                                            ) {
                                                selectedVideoTrackOverride = track
                                                if (playbackQuality == PlaybackQuality.AUDIO_ONLY) {
                                                    onSelectQuality?.invoke(PlaybackQuality.AUTO)
                                                }
                                                playerFlashMessage = "Quality set to ${track.height}p"
                                                showQualityDialog = false
                                            }
                                            .padding(vertical = 8.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = isTrackSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = track.label,
                                                    fontWeight = if (isTrackSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isTrackSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp,
                                                )
                                                if (isCurrentPlayingRes) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 3: Preset Modes
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "PRESET MODES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                            PlaybackQuality.entries.filter { it != PlaybackQuality.AUTO }.forEach { q ->
                                val isSelected = (selectedVideoTrackOverride == null) && (playbackQuality == q)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.RadioButton,
                                        ) {
                                            selectedVideoTrackOverride = null
                                            if (onSelectQuality != null) {
                                                onSelectQuality(q)
                                            } else {
                                                onCycleQuality()
                                            }
                                            playerFlashMessage = "Mode: ${q.label}"
                                            showQualityDialog = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
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
                        TextButton(onClick = { showQualityDialog = false }) {
                            Text("Close")
                        }
                    },
                )
            }

            // 12. Sleep Timer Dialog
            if (showSleepTimerDialog) {
                val timerOptions = listOf(
                    0 to "Off",
                    15 to "15 Minutes",
                    30 to "30 Minutes",
                    45 to "45 Minutes",
                    60 to "1 Hour",
                    90 to "1.5 Hours",
                )
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = {
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (sleepTimerMinutes != null) "Active: ${sleepTimerMinutes}m remaining" else "Stop playback automatically.",
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
                                            role = Role.RadioButton,
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
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text("Close")
                        }
                    },
                )
            }

            // 13. Video Zoom Dialog (Presets + Smooth Slider + Two-Finger Pinch Tip)
            if (showZoomDialog) {
                val zoomPresets = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                AlertDialog(
                    onDismissRequest = { showZoomDialog = false },
                    title = {
                        Text(
                            text = "Video Zoom",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = String.format(Locale.US, "Current Zoom: %.2fx (%d%%)", videoZoomScale, (videoZoomScale * 100f).roundToInt()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Slider(
                                value = videoZoomScale,
                                onValueChange = { newScale ->
                                    val snapped = if (newScale <= 1.03f) 1f else newScale
                                    videoZoomScale = snapped
                                    if (snapped <= 1f) {
                                        videoPanOffset = Offset.Zero
                                    }
                                    controlsInteractionTick++
                                },
                                valueRange = 1.0f..4.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag("player_zoom_dialog_slider"),
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                zoomPresets.forEach { preset ->
                                    val isSelected = abs(videoZoomScale - preset) < 0.06f
                                    Surface(
                                        onClick = {
                                            videoZoomScale = preset
                                            if (preset <= 1f) {
                                                videoPanOffset = Offset.Zero
                                            }
                                            zoomHudPercentage = (preset * 100f).roundToInt()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(
                                            text = if (preset == 1.0f) "1.0x" else String.format(Locale.US, "%.2gx", preset),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Tip: Pinch with two fingers on the video anytime to zoom in or out.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                videoZoomScale = 1f
                                videoPanOffset = Offset.Zero
                                showZoomDialog = false
                            },
                        ) {
                            Text("Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showZoomDialog = false }) {
                            Text("Done")
                        }
                    },
                )
            }

            // 14. Audio & Subtitle Track Selector Dialog
            if (showAudioTrackDialog) {
                AlertDialog(
                    onDismissRequest = { showAudioTrackDialog = false },
                    title = {
                        Text(
                            text = "Audio & Subtitle Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Quick Subtitle Toggle Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        currentOnToggleSubtitles()
                                        playerFlashMessage = if (subtitlesEnabled) "Subtitles Off" else "Subtitles On"
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (subtitlesEnabled) "Subtitles: On" else "Subtitles: Off",
                                    fontWeight = FontWeight.Bold,
                                    color = if (subtitlesEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                )
                            }

                            if (availableAudioTracks.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Available Audio Tracks",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }

                            availableAudioTracks.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = option.isSelected,
                                            role = Role.RadioButton,
                                        ) {
                                            runCatching {
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                                    .setOverrideForType(
                                                        TrackSelectionOverride(
                                                            option.group.mediaTrackGroup,
                                                            option.trackIndex,
                                                        ),
                                                    )
                                                    .build()
                                            }
                                            playerFlashMessage = "Audio: ${option.label}"
                                            showAudioTrackDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = option.isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option.label,
                                        fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (option.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAudioTrackDialog = false }) {
                            Text("Close")
                        }
                    },
                )
            }
        }
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
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
                lower.contains(".m3u8") || lower.contains("m3u8") || lower.contains("/hls") ||
                lower.contains("output=hls") || lower.contains("type=hls") || lower.contains("format=hls") ||
                lower.contains("playlist") || lower.contains("manifest") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                lower.contains(".mpd") || lower.contains("/dash") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                lower.contains(".ism") || lower.contains(".isml") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_SS)
                }
                lower.contains(".mp4") || lower.contains(".m4v") || lower.contains(".mov") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                lower.contains(".ts") || lower.contains(".m2ts") || lower.contains(".mts") || lower.contains("mpegts") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MP2T)
                }
                lower.contains(".mkv") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                }
                lower.contains(".webm") -> {
                    builder.setMimeType(MimeTypes.VIDEO_WEBM)
                }
                lower.contains(".flv") || lower.startsWith("rtmp://") || lower.startsWith("rtmps://") -> {
                    builder.setMimeType(MimeTypes.VIDEO_FLV)
                }
                lower.contains(".avi") -> {
                    builder.setMimeType(MimeTypes.VIDEO_AVI)
                }
                lower.contains(".ogv") || lower.contains(".ogg") -> {
                    builder.setMimeType(MimeTypes.VIDEO_OGG)
                }
                lower.contains(".mpg") || lower.contains(".mpeg") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MPEG)
                }
                lower.startsWith("rtsp://") || lower.startsWith("rtsps://") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_RTSP)
                }
                else -> {
                    // Let Media3 auto-sniff extensionless streams (e.g. Xtream MPEG-TS / HLS / MP4)
                }
            }
        }
        1 -> {
            // Fallback 1: Explicitly try HLS
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        2 -> {
            // Fallback 2: Explicitly try raw MPEG-TS
            builder.setMimeType(MimeTypes.VIDEO_MP2T)
        }
    }
    return builder.build()
}
