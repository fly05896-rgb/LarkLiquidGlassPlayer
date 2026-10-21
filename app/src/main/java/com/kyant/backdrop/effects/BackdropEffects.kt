package com.kyant.backdrop.effects

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.TileMode
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.createCachedLensRenderEffect

fun BackdropEffectScope.vibrancy() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val matrix = ColorMatrix().apply {
            setSaturation(1.45f)
        }
        addRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)))
    }
}

fun BackdropEffectScope.colorControls(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1f
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val invSat = 1f - saturation
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val b = 0.072f * invSat
        val t = (1f - contrast) / 2f * 255f + brightness * 255f
        val cm = ColorMatrix(
            floatArrayOf(
                (r + saturation) * contrast, g * contrast, b * contrast, 0f, t,
                r * contrast, (g + saturation) * contrast, b * contrast, 0f, t,
                r * contrast, g * contrast, (b + saturation) * contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        addRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm)))
    }
}

fun BackdropEffectScope.blur(
    radius: Float,
    edgeTreatment: TileMode = TileMode.Clamp
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.2f) {
        val safeRadius = radius.coerceAtLeast(0.5f)
        val tileMode = when (edgeTreatment) {
            TileMode.Clamp -> Shader.TileMode.CLAMP
            TileMode.Repeated -> Shader.TileMode.REPEAT
            TileMode.Mirror -> Shader.TileMode.MIRROR
            TileMode.Decal -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Shader.TileMode.DECAL else Shader.TileMode.CLAMP
            else -> Shader.TileMode.CLAMP
        }
        addRenderEffect(
            RenderEffect.createBlurEffect(
                safeRadius,
                safeRadius,
                tileMode
            )
        )
    }
}

fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float = refractionHeight,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && refractionHeight > 0.5f && refractionAmount > 0.5f) {
        val lensEffect = createCachedLensRenderEffect(
            shader = cachedShader,
            width = size.width,
            height = size.height,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
            depthEffect = depthEffect,
            chromaticAberration = chromaticAberration
        )
        if (lensEffect != null) {
            addRenderEffect(lensEffect)
        }
    }
}

fun BackdropEffectScope.effect(renderEffect: RenderEffect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        addRenderEffect(renderEffect)
    }
}
