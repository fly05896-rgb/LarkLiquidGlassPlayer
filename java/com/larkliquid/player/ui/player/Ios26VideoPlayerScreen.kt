package com.larkliquid.player.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.larkliquid.player.data.AppLanguage
import com.larkliquid.player.data.AppLocalization
import com.larkliquid.player.data.AppSettingsConfig
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.engine.EgNativeEngine
import com.larkliquid.player.player.AudioEqualizerEngine
import com.larkliquid.player.player.AudioPreset
import com.larkliquid.player.player.ClassQueueRepeatMode
import com.larkliquid.player.player.PlaybackQueueManager
import com.larkliquid.player.player.SubtitleCueItem
import com.larkliquid.player.player.SubtitleParserEngine
import com.larkliquid.player.ui.components.AnimatedFastForward2xIndicator
import com.larkliquid.player.ui.components.IosPlayPauseMorphIcon
import com.larkliquid.player.ui.components.IosPrevNextTrackIcon
import com.larkliquid.player.ui.components.IosSkip10sCanvasIcon
import com.larkliquid.player.ui.components.VectorCheckIcon
import com.larkliquid.player.ui.glass.LiquidCapsulePill
import com.larkliquid.player.ui.glass.LiquidCircleButton
import com.larkliquid.player.ui.glass.ios26LiquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VideoAspectMode(val label: String) {
    FIT("Fit (True Ratio)"),
    FILL_CROP("Fill Screen"),
    WIDESCREEN_16_9("16:9 Cinema"),
    CLASSIC_4_3("4:3 Classic")
}

@Composable
fun Ios26VideoPlayerScreen(
    video: VideoItem,
    playlistQueue: List<VideoItem>,
    glassConfig: AppSettingsConfig,
    onSelectVideo: (VideoItem, Long) -> Unit,
    onClose: (Long) -> Unit,
    onMinimizeToFloating: (Long) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val currentLang = remember(configuration) { AppLanguage.fromConfiguration(configuration) }
    val strings = remember(currentLang) { AppLocalization.stringsFor(currentLang) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioEngine = remember { AudioEqualizerEngine() }

    var currentPositionMs by remember(video.id) { mutableLongStateOf(video.lastPositionMs.coerceAtLeast(0L)) }
    var durationMs by remember(video.id) { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }

    // CRITICAL FIX: Pressing the phone Back button inside the player returns to the Videos list instead of exiting the app!
    BackHandler(enabled = true) {
        onClose(currentPositionMs)
    }

    val exoPlayer = remember(context) {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(
                if (glassConfig.hardwareDecoderPriority) {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                } else {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                }
            )
        }
        ExoPlayer.Builder(context, renderersFactory)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
    }

    // Load or switch video seamlessly when `video` changes
    LaunchedEffect(video.id, video.uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
        exoPlayer.prepare()
        if (glassConfig.resumeLastPosition && video.lastPositionMs > 1000L && video.lastPositionMs < video.durationMs - 2000L) {
            exoPlayer.seekTo(video.lastPositionMs)
        } else {
            exoPlayer.seekTo(0L)
        }
        exoPlayer.playWhenReady = true
    }

    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var isMirroredHorizontally by remember { mutableStateOf(false) }

    var videoWidthPx by remember(video.id) { mutableFloatStateOf(video.width.toFloat().coerceAtLeast(1920f)) }
    var videoHeightPx by remember(video.id) { mutableFloatStateOf(video.height.toFloat().coerceAtLeast(1080f)) }
    var pixelRatio by remember(video.id) { mutableFloatStateOf(1.0f) }

    // Default skip duration is 10 seconds ("10") as requested
    var skipSeconds by remember { mutableIntStateOf(10) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLongPress2xActive by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }
    var pinchZoomScale by remember { mutableFloatStateOf(1.0f) }

    var loopPointAMs by remember { mutableStateOf<Long?>(null) }
    var loopPointBMs by remember { mutableStateOf<Long?>(null) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }

    var volumeLevel by remember {
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }
    var volumeBoostFactor by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.8f) }

    var showAudioEqualizerSheet by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var activePreset by remember { mutableStateOf(AudioPreset.FLAT) }
    var bassStrength by remember { mutableFloatStateOf(0.5f) }
    var spatialStrength by remember { mutableFloatStateOf(0.5f) }

    var subtitlesEnabled by remember { mutableStateOf(false) }
    var selectedSubtitleLang by remember { mutableStateOf("Off") }
    var parsedSubtitleCues by remember { mutableStateOf<List<SubtitleCueItem>>(emptyList()) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }
    var gestureHudIcon by remember { mutableStateOf<ImageVector?>(null) }

    val subtitleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val cues = SubtitleParserEngine.parseSubtitleUri(context, uri)
                parsedSubtitleCues = cues
                subtitlesEnabled = cues.isNotEmpty()
                selectedSubtitleLang = uri.lastPathSegment?.substringAfterLast('/') ?: "External SRT/VTT"
            }
        }
    }

    val activeSubtitleText by remember(currentPositionMs, subtitlesEnabled, parsedSubtitleCues, video.title) {
        derivedStateOf {
            if (!subtitlesEnabled) null
            else if (parsedSubtitleCues.isNotEmpty()) {
                SubtitleParserEngine.findActiveCueText(parsedSubtitleCues, currentPositionMs)
            } else {
                video.title.take(48)
            }
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
            audioEngine.release()
        }
    }

    // Listen to ExoPlayer events including STATE_ENDED for automatic next-video playback!
    DisposableEffect(exoPlayer, video.id, playlistQueue, glassConfig.autoPlayNext) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidthPx = videoSize.width.toFloat()
                    videoHeightPx = videoSize.height.toFloat()
                    pixelRatio = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1.0f
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
                if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioEngine.bindToAudioSession(exoPlayer.audioSessionId)
                    audioEngine.applyPreset(activePreset, bassStrength, spatialStrength)
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (glassConfig.autoPlayNext && playlistQueue.size > 1) {
                        val nextVid = PlaybackQueueManager.resolveNextVideo(
                            queue = playlistQueue,
                            currentVideoId = video.id,
                            repeatMode = ClassQueueRepeatMode.REPEAT_ALL
                        )
                        if (nextVid != null && nextVid.id != video.id) {
                            onSelectVideo(nextVid, 0L)
                        }
                    } else {
                        exoPlayer.seekTo(0L)
                        exoPlayer.pause()
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, loopPointAMs, loopPointBMs) {
        while (true) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            currentPositionMs = pos
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            val a = loopPointAMs
            val b = loopPointBMs
            if (a != null && b != null && b > a && pos >= b) {
                exoPlayer.seekTo(a)
                currentPositionMs = a
            }
            delay(200L)
        }
    }

    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            exoPlayer.pause()
            sleepTimerMinutes = 0
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, showAudioEqualizerSheet, showSubtitlesSheet, showQueueSheet) {
        if (controlsVisible && isPlaying && !showAudioEqualizerSheet && !showSubtitlesSheet && !showQueueSheet) {
            delay(4200L)
            controlsVisible = false
        }
    }

    LaunchedEffect(gestureHudText) {
        if (gestureHudText != null) {
            delay(950L)
            gestureHudText = null
            gestureHudIcon = null
        }
    }

    val videoBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ==================== 1. VIDEO SURFACE ====================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(videoBackdrop)
                    .pointerInput(isScreenLocked, skipSeconds) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (isScreenLocked) return@detectTapGestures
                                val isLeftHalf = offset.x < size.width / 2f
                                val deltaMs = skipSeconds * 1000L
                                if (isLeftHalf) {
                                    val target = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    gestureHudIcon = null
                                    gestureHudText = "-${skipSeconds}s"
                                } else {
                                    val target = (exoPlayer.currentPosition + deltaMs).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(target)
                                    currentPositionMs = target
                                    gestureHudIcon = null
                                    gestureHudText = "+${skipSeconds}s"
                                }
                            },
                            onLongPress = {
                                if (isScreenLocked) return@detectTapGestures
                                isLongPress2xActive = true
                                exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (isLongPress2xActive) {
                                    isLongPress2xActive = false
                                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                }
                            }
                        )
                    }
                    .pointerInput(isScreenLocked) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (isScreenLocked) return@detectVerticalDragGestures
                            val isLeftSide = change.position.x < size.width / 2f
                            val delta = -dragAmount / size.height.coerceAtLeast(1)
                            if (isLeftSide) {
                                brightnessLevel = (brightnessLevel + delta).coerceIn(0.05f, 1.0f)
                                activity?.window?.let { win ->
                                    val params: WindowManager.LayoutParams = win.attributes
                                    params.screenBrightness = brightnessLevel
                                    win.attributes = params
                                }
                                gestureHudIcon = Icons.Default.BrightnessHigh
                                gestureHudText = "${(brightnessLevel * 100).toInt()}%"
                            } else {
                                volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (volumeLevel * maxVol).toInt(),
                                    0
                                )
                                exoPlayer.volume = if (isMuted) 0f else volumeLevel
                                gestureHudIcon = Icons.Default.VolumeUp
                                gestureHudText = "${(volumeLevel * 100 * volumeBoostFactor).toInt()}%"
                            }
                        }
                    }
                    .pointerInput(isScreenLocked) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (!isScreenLocked && zoom != 1f) {
                                pinchZoomScale = (pinchZoomScale * zoom).coerceIn(0.75f, 2.2f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val aspectBox = remember(
                    maxWidth.value,
                    maxHeight.value,
                    videoWidthPx,
                    videoHeightPx,
                    pixelRatio,
                    aspectMode
                ) {
                    EgNativeEngine.computeOptimalAspectBox(
                        containerW = maxWidth.value,
                        containerH = maxHeight.value,
                        videoW = videoWidthPx,
                        videoH = videoHeightPx,
                        pixelRatio = pixelRatio,
                        aspectModeOrdinal = aspectMode.ordinal
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                    update = { textureView ->
                        exoPlayer.setVideoTextureView(textureView)
                    },
                    modifier = Modifier
                        .size(width = aspectBox.widthDp.dp, height = aspectBox.heightDp.dp)
                        .graphicsLayer {
                            scaleX = pinchZoomScale * (if (isMirroredHorizontally) -1f else 1f)
                            scaleY = pinchZoomScale
                        }
                )

                val subText = activeSubtitleText
                if (subText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 112.dp, start = 24.dp, end = 24.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ==================== 2. REAL ANIMATED VECTOR 2.0X & GESTURE HUD (ZERO EMOJIS) ====================
            AnimatedVisibility(
                visible = isLongPress2xActive || gestureHudText != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp)
            ) {
                LiquidCapsulePill(
                    backdrop = videoBackdrop,
                    isInteractive = false,
                    surfaceTint = Color.Black.copy(alpha = 0.36f)
                ) {
                    if (isLongPress2xActive) {
                        AnimatedFastForward2xIndicator(speedText = "2.0x")
                    } else {
                        val icon = gestureHudIcon
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = gestureHudText ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ==================== 3. LOCKED SCREEN UNLOCK PILL ====================
            if (isScreenLocked && controlsVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    LiquidCircleButton(
                        onClick = { isScreenLocked = false },
                        backdrop = videoBackdrop,
                        size = 50.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ==================== 4. MAIN LIQUID GLASS CONTROLS OVERLAY ====================
            AnimatedVisibility(
                visible = controlsVisible && !isScreenLocked,
                enter = fadeIn(spring(stiffness = 400f)),
                exit = fadeOut(spring(stiffness = 400f)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // ---------- A. TOP-LEFT: Close + iOS Utility Capsule ----------
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidCircleButton(
                            onClick = { onClose(currentPositionMs) },
                            backdrop = videoBackdrop,
                            size = 42.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        LiquidCapsulePill(
                            backdrop = videoBackdrop,
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Floating Window",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { onMinimizeToFloating(currentPositionMs) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "PiP",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { onEnterPip() }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable {
                                        val next = (aspectMode.ordinal + 1) % VideoAspectMode.entries.size
                                        aspectMode = VideoAspectMode.entries[next]
                                        pinchZoomScale = 1.0f
                                        gestureHudIcon = Icons.Default.AspectRatio
                                        gestureHudText = aspectMode.label
                                    }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Mirror Video",
                                tint = if (isMirroredHorizontally) Color(0xFF64D2FF) else Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable {
                                        isMirroredHorizontally = !isMirroredHorizontally
                                    }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable {
                                        isLandscape = !isLandscape
                                        activity?.requestedOrientation = if (isLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                        gestureHudIcon = Icons.Default.ScreenRotation
                                        gestureHudText = if (isLandscape) strings.landscapeModeHud else strings.portraitModeHud
                                    }
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable {
                                        isScreenLocked = true
                                        gestureHudIcon = Icons.Default.Lock
                                        gestureHudText = strings.touchLockedHud
                                    }
                            )
                        }
                    }

                    // ---------- B. TOP-RIGHT: Volume Slider Pill ----------
                    LiquidVolumePill(
                        backdrop = videoBackdrop,
                        volume = if (isMuted) 0f else volumeLevel,
                        onVolumeChange = { newVol ->
                            isMuted = false
                            volumeLevel = newVol.coerceIn(0f, 1f)
                            exoPlayer.volume = volumeLevel
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (volumeLevel * maxVol).toInt(),
                                0
                            )
                        },
                        onToggleMute = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else volumeLevel
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // ---------- C. CENTER: 5 iOS Liquid Glass Controls (Prev | -10s | Play/Pause | +10s | Next) ----------
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Video Track Button
                        LiquidCircleButton(
                            onClick = {
                                if (currentPositionMs > 4000L) {
                                    exoPlayer.seekTo(0L)
                                    currentPositionMs = 0L
                                } else {
                                    val prevVid = PlaybackQueueManager.resolvePreviousVideo(
                                        queue = playlistQueue,
                                        currentVideoId = video.id
                                    )
                                    if (prevVid != null) {
                                        onSelectVideo(prevVid, currentPositionMs)
                                    }
                                }
                            },
                            backdrop = videoBackdrop,
                            size = 48.dp
                        ) {
                            IosPrevNextTrackIcon(
                                isNext = false,
                                enabled = playlistQueue.isNotEmpty(),
                                iconSize = 20.dp
                            )
                        }

                        // Skip Back 10s Button
                        LiquidCircleButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - skipSeconds * 1000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            backdrop = videoBackdrop,
                            size = 62.dp
                        ) {
                            IosSkip10sCanvasIcon(
                                seconds = skipSeconds,
                                isForward = false
                            )
                        }

                        // Play / Pause Button
                        LiquidCircleButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            backdrop = videoBackdrop,
                            size = 80.dp
                        ) {
                            IosPlayPauseMorphIcon(isPlaying = isPlaying)
                        }

                        // Skip Forward 10s Button
                        LiquidCircleButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + skipSeconds * 1000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            backdrop = videoBackdrop,
                            size = 62.dp
                        ) {
                            IosSkip10sCanvasIcon(
                                seconds = skipSeconds,
                                isForward = true
                            )
                        }

                        // Next Video Track Button
                        LiquidCircleButton(
                            onClick = {
                                val nextVid = PlaybackQueueManager.resolveNextVideo(
                                    queue = playlistQueue,
                                    currentVideoId = video.id
                                )
                                if (nextVid != null) {
                                    onSelectVideo(nextVid, currentPositionMs)
                                }
                            },
                            backdrop = videoBackdrop,
                            size = 48.dp
                        ) {
                            IosPrevNextTrackIcon(
                                isNext = true,
                                enabled = playlistQueue.isNotEmpty(),
                                iconSize = 20.dp
                            )
                        }
                    }

                    // ---------- D. BOTTOM: Floating Pill + Queue/Audio/Subtitles + Timeline Scrubber ----------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiquidCapsulePill(
                                backdrop = videoBackdrop,
                                onClick = { onMinimizeToFloating(currentPositionMs) },
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Floating Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.floatingButtonLabel,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            LiquidCapsulePill(
                                backdrop = videoBackdrop,
                                modifier = Modifier.height(40.dp)
                            ) {
                                // Up Next Queue Button (Change Video directly from inside player)
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = strings.queueSheetTitle,
                                    tint = if (showQueueSheet) Color(0xFF64D2FF) else Color.White,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .clickable {
                                            showQueueSheet = !showQueueSheet
                                            showAudioEqualizerSheet = false
                                            showSubtitlesSheet = false
                                        }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "A-B Repeat",
                                    tint = when {
                                        loopPointBMs != null -> Color(0xFF34C759)
                                        loopPointAMs != null -> Color(0xFFFF9F0A)
                                        else -> Color.White
                                    },
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clickable {
                                            when {
                                                loopPointAMs == null -> {
                                                    loopPointAMs = currentPositionMs
                                                    gestureHudIcon = Icons.Default.Repeat
                                                    gestureHudText = "A: ${VideoItem.formatTimestamp(currentPositionMs)}"
                                                }
                                                loopPointBMs == null -> {
                                                    if (currentPositionMs > (loopPointAMs ?: 0L) + 1000L) {
                                                        loopPointBMs = currentPositionMs
                                                        gestureHudIcon = Icons.Default.Repeat
                                                        gestureHudText = "A - B Loop"
                                                    } else {
                                                        loopPointAMs = null
                                                    }
                                                }
                                                else -> {
                                                    loopPointAMs = null
                                                    loopPointBMs = null
                                                }
                                            }
                                        }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerMinutes > 0) Color(0xFF64D2FF) else Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            sleepTimerMinutes = when (sleepTimerMinutes) {
                                                0 -> 15
                                                15 -> 30
                                                30 -> 60
                                                else -> 0
                                            }
                                            gestureHudIcon = Icons.Default.Bedtime
                                            gestureHudText = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}m" else "Off"
                                        }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Audio & Equalizer",
                                    tint = if (volumeBoostFactor > 1.0f) Color(0xFF64D2FF) else Color.White,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clickable {
                                            showAudioEqualizerSheet = !showAudioEqualizerSheet
                                            showSubtitlesSheet = false
                                            showQueueSheet = false
                                        }
                                )
                                Spacer(Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Subtitles & Speed",
                                    tint = if (subtitlesEnabled) Color(0xFF64D2FF) else Color.White,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clickable {
                                            showSubtitlesSheet = !showSubtitlesSheet
                                            showAudioEqualizerSheet = false
                                            showQueueSheet = false
                                        }
                                )
                            }
                        }

                        Ios26TimelineScrubberPill(
                            backdrop = videoBackdrop,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            isRtl = currentLang.isRtl,
                            onScrubPrecisionChanged = { label ->
                                gestureHudIcon = null
                                gestureHudText = label
                            },
                            onSeekTo = { targetMs ->
                                exoPlayer.seekTo(targetMs)
                                currentPositionMs = targetMs
                            }
                        )
                    }
                }
            }

            // ==================== 5. IN-PLAYER UP NEXT VIDEO QUEUE DRAWER ====================
            AnimatedVisibility(
                visible = showQueueSheet,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides currentLang.layoutDirection) {
                    Column(
                        modifier = Modifier
                            .width(330.dp)
                            .ios26LiquidGlass(
                                backdrop = videoBackdrop,
                                shape = RoundedCornerShape(28.dp),
                                blurRadius = 18.dp,
                                surfaceTint = Color.Black.copy(alpha = 0.56f)
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.queueSheetTitle} (${playlistQueue.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showQueueSheet = false }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(playlistQueue, key = { it.id }) { item ->
                                val isCurrent = item.id == video.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isCurrent) Color(0xFF0088FF).copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            showQueueSheet = false
                                            if (!isCurrent) {
                                                onSelectVideo(item, currentPositionMs)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.formattedDuration} • ${item.resolutionBadge}",
                                            color = Color.White.copy(alpha = 0.70f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isCurrent) {
                                        VectorCheckIcon(color = Color.White, iconSize = 16.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 6. AUDIO & 200% BOOST SHEET ====================
            AnimatedVisibility(
                visible = showAudioEqualizerSheet,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides currentLang.layoutDirection) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .ios26LiquidGlass(
                                backdrop = videoBackdrop,
                                shape = RoundedCornerShape(28.dp),
                                blurRadius = 18.dp,
                                surfaceTint = Color.Black.copy(alpha = 0.56f)
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.audioEqTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showAudioEqualizerSheet = false }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1.0f to "100%", 1.5f to "150% Boost", 2.0f to "200% Max").forEach { (factor, label) ->
                                val selected = volumeBoostFactor == factor
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(Capsule())
                                        .background(
                                            if (selected) Color.White.copy(alpha = 0.30f)
                                            else Color.White.copy(alpha = 0.10f)
                                        )
                                        .clickable {
                                            volumeBoostFactor = factor
                                            audioEngine.setVolumeBoostFactor(factor)
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.skipDurationLabel,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(10, 15, 30).forEach { sec ->
                                    val active = skipSeconds == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(Capsule())
                                            .background(if (active) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.10f))
                                            .clickable { skipSeconds = sec }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        AudioPreset.entries.take(5).forEach { preset ->
                            val isSelected = activePreset == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.07f)
                                    )
                                    .clickable {
                                        activePreset = preset
                                        audioEngine.applyPreset(preset, bassStrength, spatialStrength)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLang.isRtl) preset.titleAr else preset.subtitleEn,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isSelected) {
                                    VectorCheckIcon(color = Color.White, iconSize = 14.dp)
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 7. SUBTITLES & SPEED SHEET ====================
            AnimatedVisibility(
                visible = showSubtitlesSheet,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides currentLang.layoutDirection) {
                    Column(
                        modifier = Modifier
                            .width(310.dp)
                            .ios26LiquidGlass(
                                backdrop = videoBackdrop,
                                shape = RoundedCornerShape(28.dp),
                                blurRadius = 18.dp,
                                surfaceTint = Color.Black.copy(alpha = 0.56f)
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.subtitlesSpeedTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showSubtitlesSheet = false }
                            )
                        }

                        Text(
                            text = strings.playbackSpeedLabel,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                val active = playbackSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(Capsule())
                                        .background(
                                            if (active) Color.White.copy(alpha = 0.30f)
                                            else Color.White.copy(alpha = 0.10f)
                                        )
                                        .clickable {
                                            playbackSpeed = spd
                                            exoPlayer.playbackParameters = PlaybackParameters(spd)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Load External Subtitle File (.srt / .vtt) Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0088FF).copy(alpha = 0.32f))
                                .clickable {
                                    runCatching {
                                        subtitleFileLauncher.launch(arrayOf("*/*"))
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.loadExternalSubtitlesBtn,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        listOf("Off", "العربية (Arabic CC)", "English (CC)").forEach { track ->
                            val isSelected = (track == "Off" && !subtitlesEnabled) ||
                                    (subtitlesEnabled && selectedSubtitleLang == track)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
                                    .clickable {
                                        if (track == "Off") {
                                            subtitlesEnabled = false
                                        } else {
                                            subtitlesEnabled = true
                                            selectedSubtitleLang = track
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = track, color = Color.White, fontSize = 12.sp)
                                if (isSelected) {
                                    VectorCheckIcon(color = Color.White, iconSize = 14.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidVolumePill(
    backdrop: Backdrop,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .width(136.dp)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                surfaceTint = Color.Black.copy(alpha = 0.28f)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onVolumeChange(fraction)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.26f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(volume.coerceIn(0.04f, 1f))
                    .height(5.dp)
                    .clip(Capsule())
                    .background(Color.White)
            )
        }

        Icon(
            imageVector = if (volume <= 0.01f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = "Volume",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onToggleMute)
        )
    }
}

@Composable
private fun Ios26TimelineScrubberPill(
    backdrop: Backdrop,
    currentPositionMs: Long,
    durationMs: Long,
    isRtl: Boolean,
    onScrubPrecisionChanged: (String) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val density = LocalDensity.current.density
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var verticalDragAccumPx by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isScrubbing) {
        scrubFraction
    } else {
        (currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    }

    val displayedElapsedMs = (displayFraction * durationMs).toLong()
    val displayedRemainingMs = (durationMs - displayedElapsedMs).coerceAtLeast(0L)

    val barHeight by animateDpAsState(
        targetValue = if (isScrubbing) 9.dp else 5.dp,
        animationSpec = spring(stiffness = 420f),
        label = "barHeight"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                surfaceTint = Color.Black.copy(alpha = 0.28f)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = VideoItem.formatTimestamp(displayedElapsedMs, prefixNegative = false),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(durationMs, isRtl) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            verticalDragAccumPx = 0f
                            scrubFraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            verticalDragAccumPx += dragAmount.y
                            val precision = PlaybackQueueManager.computeIosScrubPrecision(verticalDragAccumPx, density)
                            if (precision.multiplier < 1.0f) {
                                onScrubPrecisionChanged(if (isRtl) precision.labelAr else precision.labelEn)
                            }
                            val deltaFrac = (dragAmount.x / size.width.coerceAtLeast(1)) * precision.multiplier
                            scrubFraction = (scrubFraction + deltaFrac).coerceIn(0f, 1f)
                            onSeekTo((scrubFraction * durationMs).toLong())
                        },
                        onDragEnd = {
                            onSeekTo((scrubFraction * durationMs).toLong())
                            isScrubbing = false
                            verticalDragAccumPx = 0f
                        },
                        onDragCancel = {
                            isScrubbing = false
                            verticalDragAccumPx = 0f
                        }
                    )
                }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeekTo((frac * durationMs).toLong())
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.26f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayFraction.coerceIn(0.015f, 1f))
                    .height(barHeight)
                    .clip(Capsule())
                    .background(Color.White)
            )
        }

        Text(
            text = VideoItem.formatTimestamp(displayedRemainingMs, prefixNegative = true),
            color = Color.White.copy(alpha = 0.80f),
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
    }
}
