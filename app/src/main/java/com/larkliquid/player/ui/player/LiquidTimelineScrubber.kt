package com.larkliquid.player.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.larkliquid.player.data.VideoItem
import com.larkliquid.player.player.PlaybackQueueManager
import com.larkliquid.player.player.VideoBookmarkItem
import com.larkliquid.player.ui.glass.ios26LiquidGlass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 2026 Interactive Animated Liquid Glass Timeline Scrubber (`LiquidTimelineScrubber.kt`).
 */
@Composable
fun LiquidTimelineScrubber(
    backdrop: Backdrop,
    currentPositionMs: Long,
    durationMs: Long,
    bookmarks: List<VideoBookmarkItem> = emptyList(),
    loopPointAMs: Long? = null,
    loopPointBMs: Long? = null,
    isRtlLocale: Boolean = false,
    onScrubPrecisionChanged: (String) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val safeDuration = durationMs.coerceAtLeast(1L)
    val livePlaybackFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    var isScrubbing by remember { mutableStateOf(false) }
    var isTapAnimating by remember { mutableStateOf(false) }
    var verticalDragAccumPx by remember { mutableFloatStateOf(0f) }

    val animatedProgress = remember { Animatable(livePlaybackFraction) }
    val tapPulseAnim = remember { Animatable(0f) }
    var lastTappedFraction by remember { mutableFloatStateOf(livePlaybackFraction) }

    LaunchedEffect(livePlaybackFraction, isScrubbing, isTapAnimating) {
        if (!isScrubbing && !isTapAnimating) {
            animatedProgress.snapTo(livePlaybackFraction)
        }
    }

    val displayFraction = animatedProgress.value.coerceIn(0f, 1f)
    val displayedElapsedMs = (displayFraction * safeDuration).toLong()
    val displayedRemainingMs = (safeDuration - displayedElapsedMs).coerceAtLeast(0L)

    val trackHeight by animateDpAsState(
        targetValue = when {
            isScrubbing -> 11.dp
            isTapAnimating -> 9.dp
            else -> 6.dp
        },
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "TimelineTrackHeight"
    )

    val thumbScale by animateFloatAsState(
        targetValue = when {
            isScrubbing -> 1.55f
            isTapAnimating -> 1.35f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 380f),
        label = "TimelineThumbScale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedVisibility(
            visible = isScrubbing || isTapAnimating,
            enter = fadeIn(spring(stiffness = 500f)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 500f)),
            exit = fadeOut(spring(stiffness = 500f)) + scaleOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier
                    .ios26LiquidGlass(
                        backdrop = backdrop,
                        shape = Capsule(),
                        isInteractive = false,
                        blurRadius = 4.dp,
                        refractionHeight = 14.dp,
                        refractionAmount = 26.dp,
                        surfaceTint = Color(0xFF0A84FF).copy(alpha = 0.28f)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = VideoItem.formatTimestamp(displayedElapsedMs, prefixNegative = false),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "/",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = VideoItem.formatTimestamp(safeDuration, prefixNegative = false),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .ios26LiquidGlass(
                    backdrop = backdrop,
                    shape = Capsule(),
                    blurRadius = 3.dp,
                    refractionHeight = 16.dp,
                    refractionAmount = 28.dp,
                    surfaceTint = Color.Black.copy(alpha = 0.20f)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = VideoItem.formatTimestamp(displayedElapsedMs, prefixNegative = false),
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(safeDuration) {
                        detectTapGestures { tapOffset ->
                            val targetFrac = (tapOffset.x / size.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            val targetMs = (targetFrac * safeDuration).toLong()
                            lastTappedFraction = targetFrac
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekTo(targetMs)

                            scope.launch {
                                isTapAnimating = true
                                launch {
                                    tapPulseAnim.snapTo(0.05f)
                                    tapPulseAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 260f)
                                    )
                                    tapPulseAnim.snapTo(0f)
                                }
                                animatedProgress.animateTo(
                                    targetValue = targetFrac,
                                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f)
                                )
                                isTapAnimating = false
                            }
                        }
                    }
                    .pointerInput(safeDuration, isRtlLocale) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                isScrubbing = true
                                verticalDragAccumPx = 0f
                                val startFrac = (startOffset.x / size.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    animatedProgress.snapTo(startFrac)
                                }
                                onSeekTo((startFrac * safeDuration).toLong())
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                verticalDragAccumPx += dragAmount.y
                                val precision = PlaybackQueueManager.computeIosScrubPrecision(verticalDragAccumPx, density)
                                if (precision.multiplier < 1.0f) {
                                    onScrubPrecisionChanged(if (isRtlLocale) precision.labelAr else precision.labelEn)
                                }
                                val deltaFrac = (dragAmount.x / size.width.coerceAtLeast(1).toFloat()) * precision.multiplier
                                val nextFrac = (animatedProgress.value + deltaFrac).coerceIn(0f, 1f)
                                scope.launch {
                                    animatedProgress.snapTo(nextFrac)
                                }
                                onSeekTo((nextFrac * safeDuration).toLong())
                            },
                            onDragEnd = {
                                val finalMs = (animatedProgress.value * safeDuration).toLong()
                                onSeekTo(finalMs)
                                isScrubbing = false
                                verticalDragAccumPx = 0f
                            },
                            onDragCancel = {
                                isScrubbing = false
                                verticalDragAccumPx = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val trackWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
                val thumbSizeDp = 16.dp
                val thumbWidthPx = with(LocalDensity.current) { thumbSizeDp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val centerY = size.height / 2f
                    val topY = centerY - trackHeightPx / 2f
                    val corner = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.24f),
                        topLeft = Offset(0f, topY),
                        size = Size(size.width, trackHeightPx),
                        cornerRadius = corner
                    )

                    if (loopPointAMs != null) {
                        val aX = (loopPointAMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) * size.width
                        val bX = if (loopPointBMs != null) {
                            (loopPointBMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) * size.width
                        } else aX + 4.dp.toPx()

                        drawRoundRect(
                            color = Color(0xFF34C759).copy(alpha = 0.55f),
                            topLeft = Offset(aX, topY),
                            size = Size((bX - aX).coerceAtLeast(4.dp.toPx()), trackHeightPx),
                            cornerRadius = corner
                        )
                    }

                    val fillW = (size.width * displayFraction).coerceIn(trackHeightPx, size.width)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A84FF),
                                Color(0xFF64D2FF),
                                Color.White
                            ),
                            startX = 0f,
                            endX = fillW
                        ),
                        topLeft = Offset(0f, topY),
                        size = Size(fillW, trackHeightPx),
                        cornerRadius = corner
                    )

                    bookmarks.forEach { bm ->
                        val bmX = (bm.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) * size.width
                        drawCircle(
                            color = Color(0xFFFFD60A),
                            radius = 3.5.dp.toPx(),
                            center = Offset(bmX, centerY)
                        )
                    }

                    val pulse = tapPulseAnim.value
                    if (pulse > 0.01f) {
                        val rippleX = lastTappedFraction * size.width
                        drawCircle(
                            color = Color(0xFF64D2FF).copy(alpha = (1f - pulse) * 0.65f),
                            radius = (8.dp.toPx() + 22.dp.toPx() * pulse),
                            center = Offset(rippleX, centerY)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            val xPx = (displayFraction * (trackWidthPx - thumbWidthPx)).roundToInt()
                            IntOffset(xPx, 0)
                        }
                        .size(thumbSizeDp)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFE0F2FE)
                                )
                            )
                        )
                )
            }

            Text(
                text = VideoItem.formatTimestamp(displayedRemainingMs, prefixNegative = true),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
