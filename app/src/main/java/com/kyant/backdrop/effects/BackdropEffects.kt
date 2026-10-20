package com.kyant.backdrop.effects

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.createKyantLensShader

fun BackdropEffectScope.vibrancy() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val matrix = ColorMatrix().apply {
            setSaturation(1.48f)
        }
        addRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)))
    }
}

fun BackdropEffectScope.blur(radius: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val safeRadius = radius.coerceAtLeast(0.5f)
        addRenderEffect(
            RenderEffect.createBlurEffect(
                safeRadius,
                safeRadius,
                Shader.TileMode.MIRROR
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val lensEffect = createKyantLensShader(
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
