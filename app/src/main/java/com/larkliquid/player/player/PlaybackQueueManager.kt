package com.larkliquid.player.player

import androidx.compose.runtime.Immutable
import com.larkliquid.player.data.VideoItem
import kotlin.math.abs

enum class ClassQueueRepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

@Immutable
data class ScrubSpeedPrecision(
    val multiplier: Float,
    val labelAr: String,
    val labelEn: String
)

/**
 * iOS Native Video Player Queue & Precision Scrubbing Controller (`PlaybackQueueManager.kt`).
 * Implements:
 * 1. Seamless Previous / Next track navigation across the active folder, playlist, or all videos.
 * 2. Automatic transition to the next video when the current video finishes (`Player.STATE_ENDED`).
 * 3. iOS `AVPlayerViewController` vertical drag fine-scrubbing precision (High-Speed, Half-Speed, Quarter-Speed, Fine Scrubbing).
 */
object PlaybackQueueManager {

    fun resolveNextVideo(
        queue: List<VideoItem>,
        currentVideoId: Long,
        repeatMode: ClassQueueRepeatMode = ClassQueueRepeatMode.REPEAT_ALL
    ): VideoItem? {
        if (queue.isEmpty()) return null
        val currentIndex = queue.indexOfFirst { it.id == currentVideoId }
        if (currentIndex < 0) return queue.firstOrNull()

        return when {
            repeatMode == ClassQueueRepeatMode.REPEAT_ONE -> queue[currentIndex]
            currentIndex + 1 < queue.size -> queue[currentIndex + 1]
            repeatMode == ClassQueueRepeatMode.REPEAT_ALL -> queue.firstOrNull()
            else -> null
        }
    }

    fun resolvePreviousVideo(
        queue: List<VideoItem>,
        currentVideoId: Long,
        repeatMode: ClassQueueRepeatMode = ClassQueueRepeatMode.REPEAT_ALL
    ): VideoItem? {
        if (queue.isEmpty()) return null
        val currentIndex = queue.indexOfFirst { it.id == currentVideoId }
        if (currentIndex < 0) return queue.firstOrNull()

        return when {
            currentIndex - 1 >= 0 -> queue[currentIndex - 1]
            repeatMode == ClassQueueRepeatMode.REPEAT_ALL -> queue.lastOrNull()
            else -> queue.firstOrNull()
        }
    }

    /**
     * Native iOS vertical scrubbing precision: dragging finger vertically while scrubbing
     * scales the horizontal seek sensitivity from 1.0x down to 0.125x for frame-accurate seeking.
     */
    fun computeIosScrubPrecision(verticalOffsetPx: Float, density: Float): ScrubSpeedPrecision {
        val distanceDp = abs(verticalOffsetPx) / density.coerceAtLeast(1f)
        return when {
            distanceDp < 36f -> ScrubSpeedPrecision(
                multiplier = 1.0f,
                labelAr = "تمرير سريع (High-Speed Scrubbing)",
                labelEn = "High-Speed Scrubbing"
            )
            distanceDp < 82f -> ScrubSpeedPrecision(
                multiplier = 0.5f,
                labelAr = "تمرير بنصف السرعة (Half-Speed)",
                labelEn = "Half-Speed Scrubbing"
            )
            distanceDp < 132f -> ScrubSpeedPrecision(
                multiplier = 0.25f,
                labelAr = "تمرير بربع السرعة (Quarter-Speed)",
                labelEn = "Quarter-Speed Scrubbing"
            )
            else -> ScrubSpeedPrecision(
                multiplier = 0.10f,
                labelAr = "تمرير دقيق بالثانية (Fine Scrubbing)",
                labelEn = "Fine Scrubbing"
            )
        }
    }
}
