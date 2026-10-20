package com.larkliquid.player.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

enum class AudioPreset(val titleAr: String, val subtitleEn: String, val bandGainsMb: IntArray) {
    FLAT("قياسي متوازن", "iOS Natural Flat", intArrayOf(0, 0, 0, 0, 0)),
    SPATIAL_CINEMA("سينمائي محيطي", "Spatial Audio 3D", intArrayOf(500, 250, -100, 350, 600)),
    BASS_BOOST("تضخيم البيس", "Lark Super Bass", intArrayOf(850, 500, 0, 150, 300)),
    VOCAL_CLARITY("نقاء الصوت البشري", "Crystal Dialogue", intArrayOf(-200, 200, 700, 550, 200)),
    NIGHT_MODE("الوضع الليلي", "Late Night Soft", intArrayOf(-400, 100, 450, 200, -300))
}

/**
 * Hardware Audio Equalizer, Spatial Virtualizer & 200% Volume Booster Engine
 * Combines iOS 26 Spatial Audio profiles with Lark Video's 200% Volume Boost & Equalizer.
 */
class AudioEqualizerEngine {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
        }
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        }
        runCatching {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
        }
        runCatching {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
            }
        }
    }

    /**
     * Sets volume boost from 100% (1.0f) up to 200% (2.0f) using Android LoudnessEnhancer.
     */
    fun setVolumeBoostFactor(boostFactor: Float) {
        val extraGainMb = ((boostFactor - 1.0f).coerceAtLeast(0f) * 1400f).toInt()
        runCatching {
            loudnessEnhancer?.setTargetGain(extraGainMb)
        }
    }

    fun applyPreset(preset: AudioPreset, bassStrengthPercent: Float, spatialStrengthPercent: Float) {
        runCatching {
            val eq = equalizer ?: return@runCatching
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minEq = range[0].toInt()
            val maxEq = range[1].toInt()

            for (i in 0 until bands) {
                val gain = preset.bandGainsMb.getOrElse(i) { 0 }.coerceIn(minEq, maxEq).toShort()
                eq.setBandLevel(i.toShort(), gain)
            }
        }
        runCatching {
            bassBoost?.setStrength((bassStrengthPercent.coerceIn(0f, 1f) * 1000).toInt().toShort())
        }
        runCatching {
            virtualizer?.setStrength((spatialStrengthPercent.coerceIn(0f, 1f) * 1000).toInt().toShort())
        }
    }

    fun release() {
        runCatching { loudnessEnhancer?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        loudnessEnhancer = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
    }
}
