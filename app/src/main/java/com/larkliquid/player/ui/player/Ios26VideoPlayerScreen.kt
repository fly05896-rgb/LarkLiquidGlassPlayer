package com.larkliquid.player.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.TextureView
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.larkliquid.player.data.LiquidGlassStyleConfig
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.player.AudioEqualizerEngine
import com.larkliquid.player.player.AudioPreset
import com.larkliquid.player.ui.glass.LiquidCapsulePill
import com.larkliquid.player.ui.glass.LiquidCircleButton
import com.larkliquid.player.ui.glass.ios26LiquidGlass
import com.larkliquid.player.ui.glass.rememberLiquidInteractivePhysics
import kotlinx.coroutines.delay
import java.util.Locale

enum class VideoAspectMode(val label: String) {
    FIT("Fit"),
    FILL_CROP("Fill (Crop)"),
    STRETCH("16:9 Stretch")
}

/**
 * 1:1 Replica of the iOS 26 Liquid Glass Video Player shown in the user's reference photos,
 * supercharged with Lark Video Player's audio equalizer, 200% boost, A-B Repeat, Sleep Timer,
 * gestures, and speed controls.
 *
 * Uses `TextureView` inside `Modifier.layerBackdrop(videoBackdrop)` so that every live video frame
 * is optically refracted in real time by the circular center buttons, top pills, and bottom scrub bar.
 */
@Composable
fun Ios26VideoPlayerScreen(
    video: VideoItem,
    glassConfig: LiquidGlassStyleConfig,
    onClose: () -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioEngine = remember { AudioEqualizerEngine() }

    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(15_000L)
            .setSeekForwardIncrementMs(15_000L)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(video.uri))
                prepare()
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(true) }

    // Skip seconds (15s like Image 1 or 10s like Image 2 — user can toggle anytime!)
    var skipSeconds by remember { mutableIntStateOf(15) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLongPress2xActive by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(VideoAspectMode.FIT) }
    var pinchZoomScale by remember { mutableFloatStateOf(1.0f) }

    // A-B Repeat Loop state & Sleep Timer
    var loopPointAMs by remember { mutableStateOf<Long?>(null) }
    var loopPointBMs by remember { mutableStateOf<Long?>(null) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }

    // Volume (0.0..1.0 system + up to 2.0f with Lark 200% LoudnessEnhancer) & Brightness
    var volumeLevel by remember {
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }
    var volumeBoostFactor by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.8f) }

    // Audio Equalizer & Subtitles Modal Sheets
    var showAudioEqualizerSheet by remember { mutableStateOf(false) }
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var activePreset by remember { mutableStateOf(AudioPreset.SPATIAL_CINEMA) }
    var bassStrength by remember { mutableFloatStateOf(0.6f) }
    var spatialStrength by remember { mutableFloatStateOf(0.75f) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var selectedSubtitleLang by remember { mutableStateOf("العربية (Arabic CC)") }

    // Gesture HUD indicator state
    var gestureHudText by remember { mutableStateOf<String?>(null) }

    // Bind ExoPlayer listener & AudioSession Equalizer
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
                if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioEngine.bindToAudioSession(exoPlayer.audioSessionId)
                    audioEngine.applyPreset(activePreset, bassStrength, spatialStrength)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            audioEngine.release()
            exoPlayer.release()
        }
    }

    // Real-time playback progress ticker & A-B Repeat Loop enforcement
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
            delay(100L)
        }
    }

    // Sleep Timer Countdown
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            exoPlayer.pause()
            sleepTimerMinutes = 0
            gestureHudText = "🌙 انتهى مؤقت النوم - تم إيقاف الفيديو"
        }
    }

    // Auto-hide controls after 4.5 seconds when playing and no sheet is open
    LaunchedEffect(controlsVisible, isPlaying, showAudioEqualizerSheet, showSubtitlesSheet) {
        if (controlsVisible && isPlaying && !showAudioEqualizerSheet && !showSubtitlesSheet) {
            delay(4500L)
            controlsVisible = false
        }
    }

    // Clear temporary gesture HUD after 950ms
    LaunchedEffect(gestureHudText) {
        if (gestureHudText != null) {
            delay(950L)
            gestureHudText = null
        }
    }

    // Capture the live video frame into `videoBackdrop` for real-time SDF optical lens refraction
    val videoBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    val refMult = glassConfig.refractionMultiplier
    val blurMult = glassConfig.blurMultiplier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ==================== 1. LIVE VIDEO SURFACE LAYER (Captured by videoBackdrop) ====================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(videoBackdrop)
                .pointerInput(isScreenLocked, skipSeconds) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            if (isScreenLocked) return@detectTapGestures
                            val isLeftHalf = offset.x < size.width / 2f
                            val deltaMs = skipSeconds * 1000L
                            if (isLeftHalf) {
                                val target = (exoPlayer.currentPosition - deltaMs).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                gestureHudText = "↺ -${skipSeconds}s"
                            } else {
                                val target = (exoPlayer.currentPosition + deltaMs).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                gestureHudText = "↻ +${skipSeconds}s"
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
                            gestureHudText = "☀ الإضاءة ${(brightnessLevel * 100).toInt()}%"
                        } else {
                            volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                (volumeLevel * maxVol).toInt(),
                                0
                            )
                            exoPlayer.volume = if (isMuted) 0f else volumeLevel
                            gestureHudText = "🔊 الصوت ${(volumeLevel * 100 * volumeBoostFactor).toInt()}%"
                        }
                    }
                }
                .pointerInput(isScreenLocked) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (!isScreenLocked && zoom != 1f) {
                            pinchZoomScale = (pinchZoomScale * zoom).coerceIn(0.85f, 2.5f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Hardware-accelerated TextureView so Compose GraphicsLayer captures every frame for Liquid Glass refraction!
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
                    .fillMaxSize()
                    .graphicsLayer {
                        val baseScale = when (aspectMode) {
                            VideoAspectMode.FIT -> 1.0f
                            VideoAspectMode.FILL_CROP -> 1.18f
                            VideoAspectMode.STRETCH -> 1.08f
                        }
                        scaleX = baseScale * pinchZoomScale
                        scaleY = baseScale * pinchZoomScale
                    }
            )

            // Subtle subtitle overlay above bottom scrub bar when enabled
            if (subtitlesEnabled && isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "♪ [iOS 26 Liquid Glass • ${video.title.take(32)}] ♪",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ==================== 2. LONG-PRESS 2.0X SPEED & GESTURE LIQUID GLASS HUD ====================
        AnimatedVisibility(
            visible = isLongPress2xActive || gestureHudText != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp)
        ) {
            LiquidCapsulePill(
                backdrop = videoBackdrop,
                isInteractive = false,
                refractionHeight = (18f * refMult).dp,
                refractionAmount = (36f * refMult).dp,
                surfaceTint = Color.White.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (isLongPress2xActive) "⏩ سرعة مضاعفة 2.0x" else (gestureHudText ?: ""),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ==================== 3. LOCKED SCREEN FLOATING UNLOCK PILL ====================
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
                    size = 52.dp,
                    refractionHeight = (20f * refMult).dp,
                    refractionAmount = (44f * refMult).dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Screen",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ==================== 4. MAIN iOS 26 LIQUID GLASS CONTROLS OVERLAY ====================
        AnimatedVisibility(
            visible = controlsVisible && !isScreenLocked,
            enter = fadeIn(spring(stiffness = 350f)),
            exit = fadeOut(spring(stiffness = 350f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                // ---------- A. TOP-LEFT CONTROLS: Close (X) Circle + PiP / Aspect / Rotate / Lock Capsule Pill ----------
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close Button (X) — Exact match to top-left circle in both photos
                    LiquidCircleButton(
                        onClick = onClose,
                        backdrop = videoBackdrop,
                        size = 44.dp,
                        refractionHeight = (18f * refMult).dp,
                        refractionAmount = (38f * refMult).dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Multi-Icon Capsule Pill (PiP + Aspect Ratio Resize + Rotate + Lock) — Exact match to Image 1
                    LiquidCapsulePill(
                        backdrop = videoBackdrop,
                        modifier = Modifier.height(44.dp),
                        refractionHeight = (18f * refMult).dp,
                        refractionAmount = (38f * refMult).dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onEnterPip() }
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Resize Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val next = (aspectMode.ordinal + 1) % VideoAspectMode.entries.size
                                    aspectMode = VideoAspectMode.entries[next]
                                    pinchZoomScale = 1.0f
                                    gestureHudText = "الأبعاد: ${aspectMode.label}"
                                }
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate Orientation",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    isScreenLocked = true
                                    gestureHudText = "🔒 تم قفل اللمس"
                                }
                        )
                    }
                }

                // ---------- B. TOP-RIGHT CONTROL: iOS 26 Volume Capsule Slider Pill ----------
                LiquidVolumePill(
                    backdrop = videoBackdrop,
                    volume = if (isMuted) 0f else volumeLevel,
                    refractionMultiplier = refMult,
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

                // ---------- C. CENTER CONTROLS: 3 Refractive Circular Liquid Glass Buttons ----------
                // (Skip Back 15/10s  •  Large Center Play/Pause  •  Skip Forward 15/10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Rewind 15s / 10s Circular Liquid Glass Lens Button
                    LiquidCircleButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - skipSeconds * 1000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        backdrop = videoBackdrop,
                        size = 66.dp,
                        liftHeightDp = 14.dp,
                        refractionHeight = (24f * refMult).dp,
                        refractionAmount = (52f * refMult).dp,
                        blurRadius = (1.8f * blurMult).dp,
                        surfaceTint = Color.White.copy(alpha = 0.07f)
                    ) {
                        IosSkipCircularIcon(
                            seconds = skipSeconds,
                            isForward = false
                        )
                    }

                    // 2. Center Large Play / Pause Circular Liquid Glass Lens Button
                    LiquidCircleButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        backdrop = videoBackdrop,
                        size = 88.dp,
                        liftHeightDp = 16.dp,
                        refractionHeight = (30f * refMult).dp,
                        refractionAmount = (64f * refMult).dp,
                        blurRadius = (1.5f * blurMult).dp,
                        surfaceTint = Color.White.copy(alpha = 0.08f)
                    ) {
                        IosPlayPauseIcon(isPlaying = isPlaying)
                    }

                    // 3. Fast-Forward 15s / 10s Circular Liquid Glass Lens Button
                    LiquidCircleButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + skipSeconds * 1000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        backdrop = videoBackdrop,
                        size = 66.dp,
                        liftHeightDp = 14.dp,
                        refractionHeight = (24f * refMult).dp,
                        refractionAmount = (52f * refMult).dp,
                        blurRadius = (1.8f * blurMult).dp,
                        surfaceTint = Color.White.copy(alpha = 0.07f)
                    ) {
                        IosSkipCircularIcon(
                            seconds = skipSeconds,
                            isForward = true
                        )
                    }
                }

                // ---------- D. BOTTOM CONTROLS: Audio/Subtitles/Loop Pill (Right) + Full-Width Scrubber Capsule ----------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Right-aligned Audio Waveform + Subtitles + A-B Loop + Sleep Timer Capsule Pill (Above timeline!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidCapsulePill(
                            backdrop = videoBackdrop,
                            modifier = Modifier.height(42.dp),
                            refractionHeight = (18f * refMult).dp,
                            refractionAmount = (38f * refMult).dp,
                            surfaceTint = Color.White.copy(alpha = 0.10f)
                        ) {
                            // A-B Repeat Loop button
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "A-B Repeat Loop",
                                tint = when {
                                    loopPointBMs != null -> Color(0xFF00E676)
                                    loopPointAMs != null -> Color(0xFFFF9100)
                                    else -> Color.White
                                },
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        when {
                                            loopPointAMs == null -> {
                                                loopPointAMs = currentPositionMs
                                                gestureHudText = "🔁 تم تحديد نقطة البداية A (${VideoItem.formatTimestamp(currentPositionMs)})"
                                            }
                                            loopPointBMs == null -> {
                                                if (currentPositionMs > (loopPointAMs ?: 0L) + 1000L) {
                                                    loopPointBMs = currentPositionMs
                                                    gestureHudText = "🔁 تكرار المقطع A ↔ B مفعل"
                                                } else {
                                                    loopPointAMs = null
                                                    gestureHudText = "تم إلغاء التكرار A-B"
                                                }
                                            }
                                            else -> {
                                                loopPointAMs = null
                                                loopPointBMs = null
                                                gestureHudText = "تم إيقاف التكرار A-B"
                                            }
                                        }
                                    }
                            )
                            Spacer(Modifier.width(16.dp))
                            // Sleep Timer button
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerMinutes > 0) Color(0xFF00E5FF) else Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable {
                                        sleepTimerMinutes = when (sleepTimerMinutes) {
                                            0 -> 15
                                            15 -> 30
                                            30 -> 60
                                            else -> 0
                                        }
                                        gestureHudText = if (sleepTimerMinutes > 0) {
                                            "🌙 مؤقت النوم: $sleepTimerMinutes دقيقة"
                                        } else {
                                            "🌙 تم إيقاف مؤقت النوم"
                                        }
                                    }
                            )
                            Spacer(Modifier.width(16.dp))
                            // Audio Track / Equalizer / 200% Boost button
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio & Equalizer",
                                tint = if (volumeBoostFactor > 1.0f) Color(0xFF00E5FF) else Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        showAudioEqualizerSheet = !showAudioEqualizerSheet
                                        showSubtitlesSheet = false
                                    }
                            )
                            Spacer(Modifier.width(16.dp))
                            // Subtitles / CC & Speed button
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Subtitles & Speed",
                                tint = if (subtitlesEnabled) Color.White else Color.White.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        showSubtitlesSheet = !showSubtitlesSheet
                                        showAudioEqualizerSheet = false
                                    }
                            )
                        }
                    }

                    // Bottom Full-Width Liquid Glass Timeline Scrubber Bar
                    Ios26TimelineScrubberPill(
                        backdrop = videoBackdrop,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        refractionMultiplier = refMult,
                        onSeekTo = { targetMs ->
                            exoPlayer.seekTo(targetMs)
                            currentPositionMs = targetMs
                        }
                    )
                }
            }
        }

        // ==================== 5. LIQUID GLASS AUDIO & 200% BOOST EQUALIZER SHEET ====================
        AnimatedVisibility(
            visible = showAudioEqualizerSheet,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .ios26LiquidGlass(
                        backdrop = videoBackdrop,
                        shape = RoundedCornerShape(30.dp),
                        blurRadius = 6.dp,
                        refractionHeight = (24f * refMult).dp,
                        refractionAmount = (48f * refMult).dp,
                        surfaceTint = Color(0xFF0A1020).copy(alpha = 0.45f)
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الصوت المحيطي والمضخم (Lark + iOS 26)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showAudioEqualizerSheet = false }
                    )
                }

                // 200% Volume Booster Toggle Row
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
                                    if (selected) Color(0xFF0091FF).copy(alpha = 0.65f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable {
                                    volumeBoostFactor = factor
                                    audioEngine.setVolumeBoostFactor(factor)
                                }
                                .padding(vertical = 6.dp),
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

                // Skip Duration Toggle (10s vs 15s vs 30s matching both uploaded screenshots!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدة القفز السريع (الأزرار الدائرية):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 15, 30).forEach { sec ->
                            val active = skipSeconds == sec
                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(if (active) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f))
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

                // Equalizer Presets
                AudioPreset.entries.take(5).forEach { preset ->
                    val isSelected = activePreset == preset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.20f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .clickable {
                                activePreset = preset
                                audioEngine.applyPreset(preset, bassStrength, spatialStrength)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.titleAr,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = preset.subtitleEn,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ==================== 6. LIQUID GLASS SUBTITLES & PLAYBACK SPEED SHEET ====================
        AnimatedVisibility(
            visible = showSubtitlesSheet,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .ios26LiquidGlass(
                        backdrop = videoBackdrop,
                        shape = RoundedCornerShape(30.dp),
                        blurRadius = 6.dp,
                        refractionHeight = (24f * refMult).dp,
                        refractionAmount = (48f * refMult).dp,
                        surfaceTint = Color(0xFF0A1020).copy(alpha = 0.45f)
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
                        text = "الترجمة وسرعة التشغيل",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showSubtitlesSheet = false }
                    )
                }

                // Playback Speed Selector
                Text(
                    text = "سرعة التشغيل (Playback Speed):",
                    color = Color.White.copy(alpha = 0.8f),
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
                                    if (active) Color(0xFF0091FF).copy(alpha = 0.65f)
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

                // Subtitle Tracks
                listOf("إيقاف الترجمة (Off)", "العربية (Arabic CC)", "English (SDH)").forEach { track ->
                    val isSelected = (track.startsWith("إيقاف") && !subtitlesEnabled) ||
                            (subtitlesEnabled && selectedSubtitleLang == track)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f))
                            .clickable {
                                if (track.startsWith("إيقاف")) {
                                    subtitlesEnabled = false
                                } else {
                                    subtitlesEnabled = true
                                    selectedSubtitleLang = track
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = track, color = Color.White, fontSize = 12.sp)
                        if (isSelected) {
                            Text(text = "✓", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top-Right iOS 26 Liquid Glass Volume Pill matching Screenshot 1 & 2 (`1000248966.jpg` & `1000248965.jpg`).
 * Features a sleek white progress bar inside the left side of the capsule and a speaker wave icon on the right.
 */
@Composable
private fun LiquidVolumePill(
    backdrop: Backdrop,
    volume: Float,
    refractionMultiplier: Float,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val physics = rememberLiquidInteractivePhysics(
        liftHeightDp = 10.dp,
        maxScaleBoost = 0.14f
    )

    Row(
        modifier = modifier
            .height(44.dp)
            .width(168.dp)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                physics = physics,
                blurRadius = 3.dp,
                refractionHeight = (18f * refractionMultiplier).dp,
                refractionAmount = (38f * refractionMultiplier).dp,
                surfaceTint = Color.White.copy(alpha = 0.09f)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Interactive Fluid Volume Bar
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
            // Track Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.26f))
            )
            // Filled White Volume Track
            Box(
                modifier = Modifier
                    .fillMaxWidth(volume.coerceIn(0.04f, 1f))
                    .height(6.dp)
                    .clip(Capsule())
                    .background(Color.White)
            )
        }

        Icon(
            imageVector = if (volume <= 0.01f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = "Volume",
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onToggleMute)
        )
    }
}

/**
 * Bottom Full-Width iOS 26 Liquid Glass Scrubber Bar (`16:50  [========-----]  -42:10`).
 * Rises upward (`liftHeightDp = 11.dp`) and expands smoothly when the user touches and scrubs the timeline!
 */
@Composable
private fun Ios26TimelineScrubberPill(
    backdrop: Backdrop,
    currentPositionMs: Long,
    durationMs: Long,
    refractionMultiplier: Float,
    onSeekTo: (Long) -> Unit
) {
    val physics = rememberLiquidInteractivePhysics(
        liftHeightDp = 11.dp,
        maxScaleBoost = 0.06f
    )
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isScrubbing) {
        scrubFraction
    } else {
        (currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    }

    val displayedElapsedMs = (displayFraction * durationMs).toLong()
    val displayedRemainingMs = (durationMs - displayedElapsedMs).coerceAtLeast(0L)

    val barHeight by animateDpAsState(
        targetValue = if (isScrubbing) 10.dp else 6.dp,
        animationSpec = spring(stiffness = 400f),
        label = "barHeight"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                physics = physics,
                blurRadius = 3.5.dp,
                refractionHeight = (20f * refractionMultiplier).dp,
                refractionAmount = (44f * refractionMultiplier).dp,
                surfaceTint = Color.White.copy(alpha = 0.09f)
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Elapsed Time (`16:50`)
        Text(
            text = VideoItem.formatTimestamp(displayedElapsedMs, prefixNegative = false),
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )

        // Center Interactive Glowing Progress Track
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            scrubFraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            scrubFraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            onSeekTo((scrubFraction * durationMs).toLong())
                        },
                        onDragEnd = {
                            onSeekTo((scrubFraction * durationMs).toLong())
                            isScrubbing = false
                        },
                        onDragCancel = {
                            isScrubbing = false
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
            // Remaining Track (Translucent White)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.25f))
            )
            // Elapsed Track (Solid Glowing White)
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayFraction.coerceIn(0.015f, 1f))
                    .height(barHeight)
                    .clip(Capsule())
                    .background(Color.White)
            )
        }

        // Right Remaining Time (`-42:10`)
        Text(
            text = VideoItem.formatTimestamp(displayedRemainingMs, prefixNegative = true),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Custom Vector Canvas for the iOS 26 Circular Skip Arrow + `15` / `10` Label inside.
 * Matches the exact Apple SF Symbols `gobackward.15` / `goforward.15` appearance in the screenshots.
 */
@Composable
private fun IosSkipCircularIcon(
    seconds: Int,
    isForward: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(38.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 2.6.dp.toPx()
            val arcPadding = 4.dp.toPx()
            val arcSize = Size(size.width - arcPadding * 2, size.height - arcPadding * 2)

            if (isForward) {
                drawArc(
                    color = Color.White,
                    startAngle = -75f,
                    sweepAngle = 295f,
                    useCenter = false,
                    topLeft = Offset(arcPadding, arcPadding),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                // Arrowhead at top-right
                val tipX = size.width * 0.62f
                val tipY = arcPadding + 1.dp.toPx()
                val path = Path().apply {
                    moveTo(tipX - 5.dp.toPx(), tipY - 4.dp.toPx())
                    lineTo(tipX + 2.dp.toPx(), tipY)
                    lineTo(tipX - 4.dp.toPx(), tipY + 5.dp.toPx())
                }
                drawPath(path, color = Color.White, style = Stroke(width = strokePx, cap = StrokeCap.Round))
            } else {
                drawArc(
                    color = Color.White,
                    startAngle = -105f,
                    sweepAngle = -295f,
                    useCenter = false,
                    topLeft = Offset(arcPadding, arcPadding),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                // Arrowhead at top-left
                val tipX = size.width * 0.38f
                val tipY = arcPadding + 1.dp.toPx()
                val path = Path().apply {
                    moveTo(tipX + 5.dp.toPx(), tipY - 4.dp.toPx())
                    lineTo(tipX - 2.dp.toPx(), tipY)
                    lineTo(tipX + 4.dp.toPx(), tipY + 5.dp.toPx())
                }
                drawPath(path, color = Color.White, style = Stroke(width = strokePx, cap = StrokeCap.Round))
            }
        }

        Text(
            text = String.format(Locale.US, "%d", seconds),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Custom Bold iOS 26 Play/Pause Vector Icon matching the thick rounded pause bars (`||`) in both screenshots.
 */
@Composable
private fun IosPlayPauseIcon(isPlaying: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 1.05f,
        animationSpec = spring(stiffness = 400f),
        label = "playPauseScale"
    )
    Canvas(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (isPlaying) {
            val barWidth = 8.dp.toPx()
            val barHeight = size.height * 0.86f
            val topY = (size.height - barHeight) / 2f
            val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.18f, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.82f - barWidth, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.12f)
                lineTo(size.width * 0.88f, size.height * 0.50f)
                lineTo(size.width * 0.24f, size.height * 0.88f)
                close()
            }
            drawPath(path = path, color = Color.White)
        }
    }
}
