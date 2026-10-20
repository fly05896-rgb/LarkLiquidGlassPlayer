package com.larkliquid.player.player

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@Immutable
data class MediaTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val mimeType: String,
    val bitrate: Int,
    val isSelected: Boolean
)

/**
 * AndroidX Media3 Track Selector & Multi-Audio/Subtitle Stream Controller (`Media3TrackSelectorEngine.kt`).
 * Built according to official `https://github.com/androidx/media` guidelines:
 * - Enumerates embedded audio tracks (Dolby AC-3, E-AC-3, AAC, Opus, FLAC) and embedded subtitle tracks.
 * - Allows instant in-player switching between multiple audio languages or embedded subtitle tracks.
 * - Configures hardware decoder tunneling and adaptive DASH/HLS bitrate parameters.
 */
object Media3TrackSelectorEngine {

    fun createConfiguredTrackSelector(
        context: Context,
        preferHardwareDecoders: Boolean = true
    ): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
                    .setTunnelingEnabled(preferHardwareDecoders)
            )
        }
    }

    fun extractAudioTracks(player: Player): List<MediaTrackOption> {
        val result = mutableListOf<MediaTrackOption>()
        val currentTracks: Tracks = player.currentTracks
        for ((groupIdx, group) in currentTracks.groups.withIndex()) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIdx in 0 until group.length) {
                    val format: Format = group.getTrackFormat(trackIdx)
                    val lang = format.language ?: "und"
                    val label = format.label ?: buildReadableAudioLabel(format, trackIdx)
                    result.add(
                        MediaTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = label,
                            language = lang,
                            mimeType = format.sampleMimeType ?: "audio/mp4a-latm",
                            bitrate = format.bitrate.coerceAtLeast(0),
                            isSelected = group.isTrackSelected(trackIdx)
                        )
                    )
                }
            }
        }
        return result
    }

    fun extractSubtitleTracks(player: Player): List<MediaTrackOption> {
        val result = mutableListOf<MediaTrackOption>()
        val currentTracks: Tracks = player.currentTracks
        for ((groupIdx, group) in currentTracks.groups.withIndex()) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (trackIdx in 0 until group.length) {
                    val format: Format = group.getTrackFormat(trackIdx)
                    val lang = format.language ?: "und"
                    val label = format.label ?: "Track ${trackIdx + 1} ($lang)"
                    result.add(
                        MediaTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = label,
                            language = lang,
                            mimeType = format.sampleMimeType ?: "text/vtt",
                            bitrate = 0,
                            isSelected = group.isTrackSelected(trackIdx)
                        )
                    )
                }
            }
        }
        return result
    }

    fun selectTrack(player: Player, option: MediaTrackOption) {
        val groups = player.currentTracks.groups
        if (option.groupIndex !in groups.indices) return
        val mediaTrackGroup = groups[option.groupIndex].mediaTrackGroup
        val override = TrackSelectionOverride(mediaTrackGroup, listOf(option.trackIndex))
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
    }

    fun setTextTracksDisabled(player: Player, disabled: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, disabled)
            .build()
    }

    private fun buildReadableAudioLabel(format: Format, index: Int): String {
        val channels = when (format.channelCount) {
            1 -> "Mono"
            2 -> "Stereo 2.0"
            6 -> "Surround 5.1"
            8 -> "Surround 7.1"
            else -> "${format.channelCount.coerceAtLeast(2)}ch"
        }
        val sampleRate = if (format.sampleRate > 0) "${format.sampleRate / 1000}kHz" else "48kHz"
        val lang = format.language?.uppercase() ?: "AUDIO #${index + 1}"
        return "$lang • $channels • $sampleRate"
    }
}
