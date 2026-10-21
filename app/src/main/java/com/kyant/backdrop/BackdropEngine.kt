package com.kyant.backdrop

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Hardware-Accelerated Liquid Glass Backdrop Engine (`com.kyant.backdrop`).
 * Supports single and combined multi-layer backdrops (`LayerBackdrop` + `CombinedBackdrop`),
 * AGSL SDF lens refraction with chromatic dispersion on Android 13+ (API 33+),
 * RenderEffect blur + vibrancy on Android 12+ (API 31+), and universal hardware fallback on all processors.
 */
@Stable
interface Backdrop {
    val isCoordinatesDependent: Boolean
        get() = true

    fun DrawScope.drawBackdrop(
        density: androidx.compose.ui.unit.Density,
        coordinates: Offset
    )
}

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    val onDrawBlock: (ContentDrawScope.() -> Unit)? = null
) : Backdrop {
    var rootOffset: Offset = Offset.Zero
        internal set

    override fun DrawScope.drawBackdrop(
        density: androidx.compose.ui.unit.Density,
        coordinates: Offset
    ) {
        val dx = rootOffset.x - coordinates.x
        val dy = rootOffset.y - coordinates.y
        withTransform({
            translate(left = dx, top = dy)
        }) {
            drawLayer(graphicsLayer)
        }
    }
}

@Stable
internal class CombinedBackdrop(
    val backdrops: List<Backdrop>
) : Backdrop {
    override val isCoordinatesDependent: Boolean
        get() = backdrops.any { it.isCoordinatesDependent }

    override fun DrawScope.drawBackdrop(
        density: androidx.compose.ui.unit.Density,
        coordinates: Offset
    ) {
        for (i in backdrops.indices) {
            with(backdrops[i]) {
                drawBackdrop(density, coordinates)
            }
        }
    }
}

internal class OffsetHolder(var value: Offset = Offset.Zero)

private const val LIQUID_GLASS_LENS_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float4 cornerRadii;
    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float depthEffect;
    uniform float chromaticAberration;

    float sdRoundedBox(float2 coord, float2 halfSize, float4 radii) {
        float2 r = (coord.x > 0.0) ? radii.xy : radii.zw;
        float radius = (coord.y > 0.0) ? r.y : r.x;
        float2 q = abs(coord) - halfSize + radius;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    }

    float2 gradSdRoundedBox(float2 coord, float2 halfSize, float4 radii) {
        float2 r = (coord.x > 0.0) ? radii.xy : radii.zw;
        float radius = (coord.y > 0.0) ? r.y : r.x;
        float2 q = abs(coord) - halfSize + radius;
        float2 s = sign(coord);
        if (q.x > 0.0 || q.y > 0.0) {
            return s * normalize(max(q, 1e-4));
        } else {
            return s * ((q.x > q.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
        }
    }

    float circleMap(float x) {
        float c = clamp(x, 0.0, 1.0);
        return 1.0 - sqrt(1.0 - c * c);
    }

    half4 main(float2 coord) {
        float2 halfSize = max(size * 0.5, float2(1.0));
        float2 centeredCoord = coord - halfSize;
        float sd = sdRoundedBox(centeredCoord, halfSize, cornerRadii);
        float safeHeight = max(refractionHeight, 1.0);
        if (-sd >= safeHeight) {
            return content.eval(coord);
        }
        sd = min(sd, 0.0);
        float4 maxGradRadius = float4(min(halfSize.x, halfSize.y));
        float4 gradRadius = min(cornerRadii * 1.5, maxGradRadius);
        float2 normal = gradSdRoundedBox(centeredCoord, halfSize, gradRadius);
        float refractedDistance = circleMap(1.0 - (-sd / safeHeight)) * refractionAmount;
        float2 grad = normalize(normal + depthEffect * normalize(centeredCoord));
        float2 displacement = refractedDistance * grad;
        if (chromaticAberration > 0.5) {
            float2 dR = displacement * 1.16;
            float2 dG = displacement * 1.00;
            float2 dB = displacement * 0.84;
            half4 cR = content.eval(clamp(coord + dR, float2(0.0), max(size - 1.0, float2(1.0))));
            half4 cG = content.eval(clamp(coord + dG, float2(0.0), max(size - 1.0, float2(1.0))));
            half4 cB = content.eval(clamp(coord + dB, float2(0.0), max(size - 1.0, float2(1.0))));
            return half4(cR.r, cG.g, cB.b, cG.a);
        } else {
            float2 sampleCoord = clamp(coord + displacement, float2(0.0), max(size - 1.0, float2(1.0)));
            return content.eval(sampleCoord);
        }
    }
"""

class BackdropEffectScope internal constructor(
    val size: Size,
    val layoutDirection: LayoutDirection,
    private val density: Float,
    internal val cachedShader: RuntimeShader?
) {
    internal val renderEffects = mutableListOf<RenderEffect>()

    fun Float.toPx(): Float = this * density
    fun Dp.toPx(): Float = this.value * density

    internal fun addRenderEffect(effect: RenderEffect) {
        renderEffects.add(effect)
    }

    internal fun buildCombinedRenderEffect(): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || renderEffects.isEmpty()) return null
        var current = renderEffects.first()
        for (i in 1 until renderEffects.size) {
            current = RenderEffect.createChainEffect(renderEffects[i], current)
        }
        return current
    }
}

@Composable
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape = { CircleShape },
    effects: (BackdropEffectScope.() -> Unit)? = null,
    highlight: (() -> com.kyant.backdrop.highlight.Highlight?)? = { com.kyant.backdrop.highlight.Highlight.Default },
    shadow: (() -> com.kyant.backdrop.shadow.Shadow?)? = { com.kyant.backdrop.shadow.Shadow.Default },
    innerShadow: (() -> com.kyant.backdrop.shadow.InnerShadow?)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier {
    val foregroundLayer = rememberGraphicsLayer()
    val offsetHolder = remember { OffsetHolder() }
    val resolvedShape = shape()
    val rememberedLensShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(LIQUID_GLASS_LENS_AGSL) }.getOrNull()
        } else null
    }

    return this
        .onGloballyPositioned { coords ->
            offsetHolder.value = coords.positionInRoot()
        }
        .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
        .drawWithCache {
            val densityPx = density
            val effectScope = BackdropEffectScope(size, layoutDirection, densityPx, rememberedLensShader)
            effects?.invoke(effectScope)
            val androidRenderEffect = effectScope.buildCombinedRenderEffect()
            val composeRenderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && androidRenderEffect != null) {
                androidRenderEffect.asComposeRenderEffect()
            } else null

            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            val clipPath = Path().apply {
                when (outline) {
                    is Outline.Rectangle -> addRect(outline.rect)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Generic -> addPath(outline.path)
                }
            }

            onDrawWithContent {
                if (size.width > 1f && size.height > 1f) {
                    val sh = shadow?.invoke()
                    if (sh != null && sh.alpha > 0.01f) {
                        drawRoundRect(
                            color = sh.color.copy(alpha = sh.color.alpha * sh.alpha),
                            topLeft = Offset(sh.offset.x.toPx(), sh.offset.y.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f)
                        )
                    }

                    foregroundLayer.renderEffect = composeRenderEffect
                    foregroundLayer.record {
                        val drawBg: DrawScope.() -> Unit = {
                            with(backdrop) {
                                drawBackdrop(this@onDrawWithContent, offsetHolder.value)
                            }
                        }
                        if (onDrawBackdrop != null) {
                            onDrawBackdrop(drawBg)
                        } else {
                            drawBg()
                        }
                    }

                    clipPath(clipPath) {
                        drawLayer(foregroundLayer)
                        onDrawSurface?.invoke(this)

                        val inSh = innerShadow?.invoke()
                        if (inSh != null && inSh.alpha > 0.01f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.24f * inSh.alpha),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.16f * inSh.alpha)
                                    )
                                )
                            )
                        }

                        val hl = highlight?.invoke()
                        if (hl != null && hl.alpha > 0.01f) {
                            val strokeW = hl.width.toPx().coerceAtLeast(1f)
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.55f * hl.alpha),
                                        Color.White.copy(alpha = 0.12f * hl.alpha),
                                        Color.White.copy(alpha = 0.36f * hl.alpha)
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                ),
                                cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f),
                                style = Stroke(width = strokeW)
                            )
                        }

                        onDrawFront?.invoke(this)
                    }

                    if (exportedBackdrop != null) {
                        exportedBackdrop.rootOffset = offsetHolder.value
                        exportedBackdrop.graphicsLayer.record {
                            drawLayer(foregroundLayer)
                            onDrawSurface?.invoke(this)
                        }
                    }
                }

                drawContent()
            }
        }
        .clip(resolvedShape)
}

@Composable
fun Modifier.drawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape = { CircleShape },
    effects: (BackdropEffectScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawBackdrop: (DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = shape,
    effects = effects,
    highlight = null,
    shadow = null,
    innerShadow = null,
    exportedBackdrop = exportedBackdrop,
    layerBlock = layerBlock,
    onDrawBackdrop = onDrawBackdrop,
    onDrawSurface = onDrawSurface,
    onDrawFront = onDrawFront
)

internal fun createCachedLensRenderEffect(
    shader: RuntimeShader?,
    width: Float,
    height: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Boolean = false
): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || shader == null) return null
    return runCatching {
        val safeW = width.coerceAtLeast(1f)
        val safeH = height.coerceAtLeast(1f)
        val r = min(safeW, safeH) * 0.5f
        shader.setFloatUniform("size", safeW, safeH)
        shader.setFloatUniform("cornerRadii", r, r, r, r)
        shader.setFloatUniform("refractionHeight", refractionHeight.coerceAtLeast(1f))
        shader.setFloatUniform("refractionAmount", refractionAmount)
        shader.setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        shader.setFloatUniform("chromaticAberration", if (chromaticAberration) 1f else 0f)
        RenderEffect.createRuntimeShaderEffect(shader, "content")
    }.getOrNull()
}
