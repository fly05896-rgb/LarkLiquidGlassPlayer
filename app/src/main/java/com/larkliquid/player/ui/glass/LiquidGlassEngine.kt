package com.larkliquid.player.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.tanh

/**
 * Pure Natural Crystal Glass Engine (`LiquidGlassEngine.kt`).
 * - Designed for 120Hz zero-lag performance on Snapdragon 8 Elite (Adreno 830) & MediaTek Helio G99 Ultra (Mali-G57).
 * - Removes rainbow/prismatic color fringes (`chromaticAberration = false`) and uses clean, natural frosted glass
 *   with a subtle white specular rim matching native iOS video player overlays.
 */
@Stable
class LiquidInteractivePhysics(
    private val scope: CoroutineScope,
    val liftHeightDp: Dp = 6.dp,
    val maxScaleBoost: Float = 0.08f
) {
    private val _pressProgress = Animatable(0f)
    val pressProgress: Float get() = _pressProgress.value

    private val _dragOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    val dragOffset: Offset get() = _dragOffset.value

    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    var velocityX by mutableFloatStateOf(0f)
        private set

    fun onPressStart(initialPos: Offset) {
        touchPosition = initialPos
        scope.launch {
            _pressProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 480f
                )
            )
        }
    }

    fun onDragBy(delta: Offset, currentPos: Offset) {
        touchPosition = currentPos
        velocityX = delta.x
        scope.launch {
            _dragOffset.snapTo(_dragOffset.value + delta)
        }
    }

    fun onRelease() {
        velocityX = 0f
        scope.launch {
            _pressProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = 420f
                )
            )
        }
        scope.launch {
            _dragOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 380f
                )
            )
        }
    }
}

@Composable
fun rememberLiquidInteractivePhysics(
    liftHeightDp: Dp = 6.dp,
    maxScaleBoost: Float = 0.08f
): LiquidInteractivePhysics {
    val scope = rememberCoroutineScope()
    return remember(scope, liftHeightDp, maxScaleBoost) {
        LiquidInteractivePhysics(scope, liftHeightDp, maxScaleBoost)
    }
}

@Composable
fun Modifier.ios26LiquidGlass(
    backdrop: Backdrop,
    shape: Shape = Capsule(),
    physics: LiquidInteractivePhysics? = null,
    blurRadius: Dp = 12.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 16.dp,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = false,
    scrollVelocity: Float = 0f,
    surfaceTint: Color = Color.Black.copy(alpha = 0.28f),
    accentTint: Color = Color.Unspecified
): Modifier {
    val interactiveGestureModifier = if (physics != null) {
        Modifier.pointerInput(physics) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                physics.onPressStart(down.position)
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            physics.onDragBy(delta, change.position)
                        }
                    }
                } finally {
                    physics.onRelease()
                }
            }
        }
    } else {
        Modifier
    }

    return this
        .then(interactiveGestureModifier)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx().coerceAtLeast(1f))
                lens(
                    refractionHeight = refractionHeight.toPx(),
                    refractionAmount = refractionAmount.toPx(),
                    depthEffect = false,
                    chromaticAberration = false
                )
            },
            highlight = {
                val p = physics?.pressProgress ?: 0f
                Highlight.Default.copy(alpha = (0.52f + 0.25f * p).fastCoerceIn(0f, 1f))
            },
            shadow = {
                val p = physics?.pressProgress ?: 0f
                Shadow(alpha = (0.24f + 0.26f * p).fastCoerceIn(0f, 1f))
            },
            innerShadow = {
                val p = physics?.pressProgress ?: 0f
                InnerShadow(
                    radius = 5.dp,
                    alpha = (0.30f + 0.25f * p).fastCoerceIn(0f, 1f)
                )
            },
            layerBlock = if (physics != null) {
                {
                    val p = physics.pressProgress
                    val baseScale = lerp(1f, 1f + physics.maxScaleBoost, p)
                    val maxOffset = size.minDimension.coerceAtLeast(28f)
                    val dragSensitivity = 0.06f
                    val offset = physics.dragOffset
                    val upwardLiftPx = physics.liftHeightDp.toPx() * p

                    translationX = maxOffset * tanh(dragSensitivity * offset.x / maxOffset)
                    translationY = -upwardLiftPx + maxOffset * tanh(dragSensitivity * offset.y / maxOffset)
                    scaleX = baseScale
                    scaleY = baseScale
                }
            } else null,
            onDrawSurface = {
                val p = physics?.pressProgress ?: 0f
                // Clean natural glass base without rainbow hues
                drawRect(surfaceTint)
                drawRect(Color.White.copy(alpha = (0.14f + 0.08f * p).fastCoerceIn(0f, 0.35f)))
                if (accentTint.isSpecified) {
                    drawRect(accentTint.copy(alpha = 0.18f + 0.10f * p))
                }
            }
        )
        .drawWithContent {
            drawContent()
            val p = physics?.pressProgress ?: 0f
            val strokeWidth = 1.dp.toPx()
            val radius = size.minDimension / 2f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f + 0.20f * p),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.26f + 0.14f * p)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = strokeWidth)
            )
        }
}

@Composable
fun LiquidCircleButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    liftHeightDp: Dp = 6.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 16.dp,
    blurRadius: Dp = 14.dp,
    surfaceTint: Color = Color.Black.copy(alpha = 0.28f),
    content: @Composable BoxScope.() -> Unit
) {
    val physics = rememberLiquidInteractivePhysics(
        liftHeightDp = liftHeightDp,
        maxScaleBoost = 0.10f
    )

    Box(
        modifier = modifier
            .size(size)
            .ios26LiquidGlass(
                backdrop = backdrop,
                shape = CircleShape,
                physics = physics,
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
    liftHeightDp: Dp = 5.dp,
    blurRadius: Dp = 14.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 16.dp,
    scrollVelocity: Float = 0f,
    surfaceTint: Color = Color.Black.copy(alpha = 0.28f),
    accentTint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val physics = if (isInteractive) {
        rememberLiquidInteractivePhysics(
            liftHeightDp = liftHeightDp,
            maxScaleBoost = 0.06f
        )
    } else null

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
                physics = physics,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = false,
                depthEffect = false,
                scrollVelocity = scrollVelocity,
                surfaceTint = surfaceTint,
                accentTint = accentTint
            )
            .then(clickModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
