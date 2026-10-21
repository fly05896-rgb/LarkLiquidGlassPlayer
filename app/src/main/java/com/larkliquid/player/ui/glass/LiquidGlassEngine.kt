package com.larkliquid.player.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * iOS 26 & AndroidLiquidGlass Bridge (`LiquidGlassEngine.kt`).
 * Built directly on top of `com.kyant.backdrop.drawBackdrop` and `InteractiveHighlight`
 * from `AndroidLiquidGlass-kmp` for authentic real-time background refraction, blur, and vibrancy.
 */
@Composable
fun Modifier.ios26LiquidGlass(
    backdrop: Backdrop,
    shape: Shape = Capsule(),
    isInteractive: Boolean = true,
    blurRadius: Dp = 10.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = false,
    surfaceTint: Color = Color.Black.copy(alpha = 0.24f),
    accentTint: Color = Color.Unspecified
): Modifier {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx().coerceAtLeast(1f))
                lens(
                    refractionHeight = refractionHeight.toPx(),
                    refractionAmount = refractionAmount.toPx(),
                    depthEffect = depthEffect,
                    chromaticAberration = chromaticAberration
                )
            },
            highlight = {
                val p = interactiveHighlight.pressProgress
                Highlight.Default.copy(alpha = (0.58f + 0.32f * p).fastCoerceIn(0f, 1f))
            },
            shadow = {
                val p = interactiveHighlight.pressProgress
                Shadow(alpha = (0.22f + 0.28f * p).fastCoerceIn(0f, 1f))
            },
            innerShadow = {
                val p = interactiveHighlight.pressProgress
                InnerShadow(
                    radius = 6.dp,
                    alpha = (0.28f + 0.30f * p).fastCoerceIn(0f, 1f)
                )
            },
            layerBlock = if (isInteractive) {
                {
                    val width = size.width.coerceAtLeast(1f)
                    val height = size.height.coerceAtLeast(1f)
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

                    val maxOffset = size.minDimension.coerceAtLeast(1f)
                    val initialDerivative = 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 4f.dp.toPx() / height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
                                (width / height).fastCoerceAtMost(1f)
                    scaleY =
                        scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
                                (height / width).fastCoerceAtMost(1f)
                }
            } else null,
            onDrawSurface = {
                if (accentTint.isSpecified) {
                    drawRect(accentTint, blendMode = BlendMode.Hue)
                    drawRect(accentTint.copy(alpha = 0.35f))
                }
                if (surfaceTint.isSpecified) {
                    drawRect(surfaceTint)
                }
                drawRect(Color.White.copy(alpha = 0.08f))
            }
        )
        .then(
            if (isInteractive) {
                Modifier
                    .then(interactiveHighlight.modifier)
                    .then(interactiveHighlight.gestureModifier)
            } else {
                Modifier
            }
        )
}

@Composable
fun LiquidCircleButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    blurRadius: Dp = 12.dp,
    surfaceTint: Color = Color.Black.copy(alpha = 0.24f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                isInteractive = true,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = false,
                depthEffect = false,
                surfaceTint = surfaceTint
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LiquidCapsulePill(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isInteractive: Boolean = true,
    blurRadius: Dp = 12.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    surfaceTint: Color = Color.Black.copy(alpha = 0.24f),
    accentTint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                isInteractive = isInteractive,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = false,
                depthEffect = false,
                surfaceTint = surfaceTint,
                accentTint = accentTint
            )
            .then(clickModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
