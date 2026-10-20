package com.larkliquid.player.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Custom AGSL Fragment Shader for Android 13+ (API 33+) that injects dynamic liquid wave
 * refraction & chromatic dispersion when scrolling through the video list or dragging controls.
 */
private const val LIQUID_WAVE_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float scrollVelocity;
    uniform float pressProgress;
    uniform float2 touchPoint;

    half4 main(float2 coord) {
        float2 uv = coord / max(size, float2(1.0));
        
        // Edge distance factor (stronger optical bending near the curved capsule/circle edges)
        float2 centered = uv * 2.0 - 1.0;
        float edgeDist = dot(centered, centered);
        
        // Touch-driven water ripple & scroll-driven refractive wave
        float2 toTouch = coord - touchPoint;
        float distToTouch = length(toTouch);
        float touchWave = exp(-distToTouch * 0.018) * pressProgress * 6.5;
        
        float scrollWaveX = sin(uv.y * 9.42 + scrollVelocity * 0.002) * clamp(scrollVelocity * 0.008, -10.0, 10.0) * edgeDist;
        float scrollWaveY = cos(uv.x * 6.28) * clamp(abs(scrollVelocity) * 0.005, 0.0, 8.0) * edgeDist;
        
        float2 dir = distToTouch > 0.001 ? normalize(toTouch) : float2(0.0, 0.0);
        float2 displacement = dir * touchWave + float2(scrollWaveX, scrollWaveY);
        
        // Chromatic aberration (RGB channel splitting like real high-index iOS 26 crystal glass)
        float aberration = (0.8 + pressProgress * 1.6 + clamp(abs(scrollVelocity) * 0.0015, 0.0, 1.5)) * edgeDist;
        half r = content.eval(coord + displacement + float2(aberration, -aberration * 0.5)).r;
        half g = content.eval(coord + displacement).g;
        half b = content.eval(coord + displacement - float2(aberration, -aberration * 0.5)).b;
        half a = content.eval(coord + displacement).a;
        
        return half4(r, g, b, a);
    }
"""

/**
 * Interactive Fluid Physics controller for iOS 26 Liquid Glass elements.
 * Handles:
 * 1. Upward elevation lift (`liftHeight`) when pressed ("لما ادوس عليها ترفع لفوق")
 * 2. Magnetic fluid tracking (`offset`) that follows the user's finger smoothly ("وتكون مائية تمشي مع ايدي بكل سلاسة وحركة")
 * 3. Directional squash-and-stretch droplet deformation (`scaleX`, `scaleY`)
 * 4. Specular highlight tracking (`touchPosition`)
 */
@Stable
class LiquidInteractivePhysics(
    private val scope: CoroutineScope,
    val liftHeightDp: Dp = 10.dp,
    val maxScaleBoost: Float = 0.18f
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
                    dampingRatio = 0.48f,
                    stiffness = 380f
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
                    dampingRatio = 0.52f,
                    stiffness = 300f
                )
            )
        }
        scope.launch {
            _dragOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = 280f
                )
            )
        }
    }
}

@Composable
fun rememberLiquidInteractivePhysics(
    liftHeightDp: Dp = 10.dp,
    maxScaleBoost: Float = 0.18f
): LiquidInteractivePhysics {
    val scope = rememberCoroutineScope()
    return remember(scope, liftHeightDp, maxScaleBoost) {
        LiquidInteractivePhysics(scope, liftHeightDp, maxScaleBoost)
    }
}

/**
 * Core Modifier that turns any Composable into an authentic iOS 26 Liquid Glass element
 * using Kyant0's `drawBackdrop` (`lens`, `vibrancy`, `blur`, `Highlight`, `InnerShadow`, `Shadow`)
 * combined with our interactive fluid physics and optional AGSL wave shader.
 */
@Composable
fun Modifier.ios26LiquidGlass(
    backdrop: Backdrop,
    shape: Shape = Capsule(),
    physics: LiquidInteractivePhysics? = null,
    blurRadius: Dp = 3.5.dp,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 32.dp,
    chromaticAberration: Boolean = true,
    depthEffect: Boolean = true,
    scrollVelocity: Float = 0f,
    surfaceTint: Color = Color.White.copy(alpha = 0.10f),
    accentTint: Color = Color.Unspecified
): Modifier {
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(LIQUID_WAVE_AGSL)
        } else null
    }

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
                val p = physics?.pressProgress ?: 0f
                vibrancy()
                blur((blurRadius.toPx() * (1f - 0.25f * p)).coerceAtLeast(1f))
                lens(
                    refractionHeight = refractionHeight.toPx() * (1f + 0.35f * p),
                    refractionAmount = refractionAmount.toPx() * (1f + 0.45f * p),
                    depthEffect = depthEffect,
                    chromaticAberration = chromaticAberration
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null && (p > 0.01f || abs(scrollVelocity) > 5f)) {
                    runtimeShader.setFloatUniform("size", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                    runtimeShader.setFloatUniform("scrollVelocity", scrollVelocity)
                    runtimeShader.setFloatUniform("pressProgress", p)
                    val tp = physics?.touchPosition ?: Offset(size.width / 2f, size.height / 2f)
                    runtimeShader.setFloatUniform("touchPoint", tp.x, tp.y)
                    effect(RenderEffect.createRuntimeShaderEffect(runtimeShader, "content"))
                }
            },
            highlight = {
                val p = physics?.pressProgress ?: 0f
                Highlight.Default.copy(alpha = (0.72f + 0.28f * p).fastCoerceIn(0f, 1f))
            },
            shadow = {
                val p = physics?.pressProgress ?: 0f
                Shadow(alpha = (0.35f + 0.45f * p).fastCoerceIn(0f, 1f))
            },
            innerShadow = {
                val p = physics?.pressProgress ?: 0f
                InnerShadow(
                    radius = 6.dp + (6.dp * p),
                    alpha = (0.45f + 0.45f * p).fastCoerceIn(0f, 1f)
                )
            },
            layerBlock = if (physics != null) {
                {
                    val width = size.width.coerceAtLeast(1f)
                    val height = size.height.coerceAtLeast(1f)
                    val p = physics.pressProgress
                    val baseScale = lerp(1f, 1f + physics.maxScaleBoost, p)

                    val maxOffset = size.minDimension.coerceAtLeast(36f)
                    val dragSensitivity = 0.085f
                    val offset = physics.dragOffset

                    // Smooth magnetic follow on X & Y + upward liquid lift on press
                    val upwardLiftPx = physics.liftHeightDp.toPx() * p
                    translationX = maxOffset * tanh(dragSensitivity * offset.x / maxOffset)
                    translationY = -upwardLiftPx + maxOffset * tanh(dragSensitivity * offset.y / maxOffset)

                    // Water-droplet directional stretch when dragged
                    val maxDragScale = 6.dp.toPx() / height
                    val offsetAngle = atan2(offset.y, offset.x)
                    val stretchX = maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
                            (width / height).fastCoerceAtMost(1f)
                    val stretchY = maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
                            (height / width).fastCoerceAtMost(1f)

                    scaleX = baseScale + stretchX
                    scaleY = baseScale + stretchY
                }
            } else null,
            onDrawSurface = {
                val p = physics?.pressProgress ?: 0f
                if (accentTint.isSpecified) {
                    drawRect(accentTint, blendMode = BlendMode.Hue)
                    drawRect(accentTint.copy(alpha = 0.35f + 0.2f * p))
                }
                drawRect(surfaceTint.copy(alpha = (surfaceTint.alpha + 0.08f * p).fastCoerceIn(0f, 0.45f)))
            }
        )
        .drawWithContent {
            drawContent()
            // Specular crystal rim reflection (matches the bright top-left & bottom-right edge in the iOS 26 screenshots)
            val p = physics?.pressProgress ?: 0f
            val strokeWidth = (1.1.dp + 0.6.dp * p).toPx()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f + 0.30f * p),
                        Color.White.copy(alpha = 0.12f),
                        Color(0xFF88CCFF).copy(alpha = 0.20f + 0.20f * p),
                        Color.White.copy(alpha = 0.42f + 0.25f * p)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                style = Stroke(width = strokeWidth),
                blendMode = BlendMode.Plus
            )
        }
}

/**
 * Circular iOS 26 Liquid Glass Button (Used for Play/Pause, 15s/10s Skip, and Close X button).
 * Features strong convex SDF lens refraction so the video background bends through the button!
 */
@Composable
fun LiquidCircleButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    liftHeightDp: Dp = 12.dp,
    refractionHeight: Dp = 20.dp,
    refractionAmount: Dp = 42.dp,
    blurRadius: Dp = 2.dp,
    surfaceTint: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable BoxScope.() -> Unit
) {
    val physics = rememberLiquidInteractivePhysics(
        liftHeightDp = liftHeightDp,
        maxScaleBoost = 0.22f
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
                chromaticAberration = true,
                depthEffect = true,
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

/**
 * Capsule Pill iOS 26 Liquid Glass Container (Used for Top-Left PiP/AirPlay pill,
 * Top-Right Volume pill, Bottom-Right Audio/Subtitles pill, and Bottom Timeline Scrubber).
 */
@Composable
fun LiquidCapsulePill(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isInteractive: Boolean = true,
    liftHeightDp: Dp = 9.dp,
    blurRadius: Dp = 3.5.dp,
    refractionHeight: Dp = 15.dp,
    refractionAmount: Dp = 30.dp,
    scrollVelocity: Float = 0f,
    surfaceTint: Color = Color.White.copy(alpha = 0.09f),
    accentTint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val physics = if (isInteractive) {
        rememberLiquidInteractivePhysics(
            liftHeightDp = liftHeightDp,
            maxScaleBoost = 0.12f
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
                chromaticAberration = true,
                depthEffect = true,
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
