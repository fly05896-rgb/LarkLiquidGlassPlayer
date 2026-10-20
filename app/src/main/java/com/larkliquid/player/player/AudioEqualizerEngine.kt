package com.larkliquid.player.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.larkliquid.player.engine.EgNativeEngine

enum class AudioPreset(val titleAr: String, val subtitleEn: String, val bandGainsMb: IntArray) {
    FLAT("قياسي متوازن", "Natural Flat", intArrayOf(0, 0, 0, 0, 0)),
    SPATIAL_CINEMA("سينمائي محيطي 3D", "Spatial Audio 3D", intArrayOf(550, 300, -50, 400, 650)),
    BASS_BOOST("تضخيم البيس الفائق", "Super Bass", intArrayOf(900, 550, 50, 200, 350)),
    VOCAL_CLARITY("نقاء الصوت والحوار", "Crystal Dialogue", intArrayOf(-200, 250, 750, 600, 250)),
    NIGHT_MODE("الوضع الليلي المريح", "Dolby Late Night", intArrayOf(-450, 100, 500, 250, -300)),
    ELECTRONIC_POP("إلكترونيك وحيوية", "Dynamic Pop & EDM", intArrayOf(700, 350, -150, 450, 750)),
    GAMING_SURROUND("ألعاب وتفاصيل محيطية", "360° Gaming Stage", intArrayOf(650, 200, 300, 650, 800))
}

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
     * Uses C++23 Machine-Code Engine (`EgNativeEngine.computeLoudnessGainMb`) to compute hardware gain.
     */
    fun setVolumeBoostFactor(boostFactor: Float) {
        val extraGainMb = EgNativeEngine.computeLoudnessGainMb(boostFactor)
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
