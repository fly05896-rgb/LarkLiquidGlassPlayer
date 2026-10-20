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
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Self-contained, hardware-accelerated implementation of the Kyant0 `com.kyant.backdrop`
 * Liquid Glass engine powered by Jetpack Compose `GraphicsLayer` and Android 13+ AGSL `RuntimeShader`.
 *
 * Fixed AGSL syntax (`0.0` / `1.0` standard floats without invalid `h` suffixes) verified against Android 13–15 SkSL/AGSL compilers.
 */
@Stable
interface Backdrop {
    val graphicsLayer: GraphicsLayer?
    val rootOffset: Offset
}

@Stable
class LayerBackdrop internal constructor(
    override val graphicsLayer: GraphicsLayer,
    val onDrawBlock: (ContentDrawScope.() -> Unit)? = null
) : Backdrop {
    override var rootOffset: Offset = Offset.Zero
        internal set
}

internal class OffsetHolder(var value: Offset = Offset.Zero)

/**
 * AGSL Signed Distance Field (SDF) Convex Liquid Glass Lens Shader (Android 13+ / API 33+).
 * Uses strict standard SkSL/AGSL float literals (no `half-literal` suffix) so it compiles cleanly on all GPUs.
 */
private const val KYANT_SDF_LENS_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float cornerRadius;
    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform int depthEffect;
    uniform int chromaticAberration;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 coord) {
        float2 halfSize = max(size * 0.5, float2(1.0));
        float2 p = coord - halfSize;
        float r = clamp(cornerRadius, 0.0, min(halfSize.x, halfSize.y));
        
        float dist = sdRoundedBox(p, halfSize, r);
        float edgeBand = max(refractionHeight, 1.0);
        
        float rim = clamp(1.0 + dist / edgeBand, 0.0, 1.0);
        float lensProfile = rim * rim * (3.0 - 2.0 * rim);
        
        float2 normal = length(p) > 0.001 ? normalize(p) : float2(0.0, 0.0);
        
        float depthScale = depthEffect != 0 ? (-0.07 * (1.0 - lensProfile)) : 0.0;
        float2 displacement = -normal * (lensProfile * refractionAmount * 0.48) + p * depthScale;
        
        float2 sampleCoord = clamp(coord + displacement, float2(0.0), max(size - 1.0, float2(1.0)));
        float rimGlow = lensProfile * 0.14;
        
        if (chromaticAberration != 0) {
            float2 chromaOffset = normal * (lensProfile * min(refractionAmount * 0.07, 5.0));
            float2 rCoord = clamp(sampleCoord + chromaOffset, float2(0.0), max(size - 1.0, float2(1.0)));
            float2 bCoord = clamp(sampleCoord - chromaOffset, float2(0.0), max(size - 1.0, float2(1.0)));
            
            float red = float(content.eval(rCoord).r);
            half4 greenAlpha = content.eval(sampleCoord);
            float green = float(greenAlpha.g);
            float blue = float(content.eval(bCoord).b);
            
            float outR = clamp(red + rimGlow, 0.0, 1.0);
            float outG = clamp(green + rimGlow, 0.0, 1.0);
            float outB = clamp(blue + rimGlow * 1.15, 0.0, 1.0);
            return half4(half(outR), half(outG), half(outB), greenAlpha.a);
        } else {
            half4 col = content.eval(sampleCoord);
            float outR = clamp(float(col.r) + rimGlow, 0.0, 1.0);
            float outG = clamp(float(col.g) + rimGlow, 0.0, 1.0);
            float outB = clamp(float(col.b) + rimGlow, 0.0, 1.0);
            return half4(half(outR), half(outG), half(outB), col.a);
        }
    }
"""

class BackdropEffectScope internal constructor(
    val size: Size,
    private val density: Float
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
    highlight: (() -> com.kyant.backdrop.highlight.Highlight?)? = null,
    shadow: (() -> com.kyant.backdrop.shadow.Shadow?)? = null,
    innerShadow: (() -> com.kyant.backdrop.shadow.InnerShadow?)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null
): Modifier {
    val foregroundLayer = rememberGraphicsLayer()
    val offsetHolder = remember { OffsetHolder() }
    val resolvedShape = shape()

    return this
        .onGloballyPositioned { coords ->
            offsetHolder.value = coords.positionInRoot()
        }
        .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
        .drawWithCache {
            val densityPx = density
            val effectScope = BackdropEffectScope(size, densityPx)
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
                            color = Color.Black.copy(alpha = 0.28f * sh.alpha),
                            topLeft = Offset(0f, 4.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f)
                        )
                    }

                    val sourceLayer = backdrop.graphicsLayer
                    if (sourceLayer != null) {
                        foregroundLayer.renderEffect = composeRenderEffect
                        foregroundLayer.record {
                            val dx = backdrop.rootOffset.x - offsetHolder.value.x
                            val dy = backdrop.rootOffset.y - offsetHolder.value.y
                            withTransform({
                                translate(left = dx, top = dy)
                            }) {
                                drawLayer(sourceLayer)
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
                                            Color.White.copy(alpha = 0.26f * inSh.alpha),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.22f * inSh.alpha)
                                        )
                                    )
                                )
                            }

                            val hl = highlight?.invoke()
                            if (hl != null && hl.alpha > 0.01f) {
                                val strokeW = 1.3.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.68f * hl.alpha),
                                            Color.White.copy(alpha = 0.12f * hl.alpha),
                                            Color.White.copy(alpha = 0.48f * hl.alpha)
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height)
                                    ),
                                    cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f),
                                    style = Stroke(width = strokeW),
                                    blendMode = BlendMode.Plus
                                )
                            }
                        }
                    } else {
                        clipPath(clipPath) {
                            onDrawSurface?.invoke(this)
                        }
                    }

                    if (exportedBackdrop != null) {
                        exportedBackdrop.rootOffset = offsetHolder.value
                        exportedBackdrop.graphicsLayer.record {
                            drawLayer(foregroundLayer)
                        }
                    }
                }

                drawContent()
            }
        }
        .clip(resolvedShape)
}

internal fun createKyantLensShader(
    width: Float,
    height: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean,
    chromaticAberration: Boolean
): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return runCatching {
        val shader = RuntimeShader(KYANT_SDF_LENS_AGSL)
        val safeW = width.coerceAtLeast(1f)
        val safeH = height.coerceAtLeast(1f)
        shader.setFloatUniform("size", safeW, safeH)
        shader.setFloatUniform("cornerRadius", min(safeW, safeH) * 0.5f)
        shader.setFloatUniform("refractionHeight", refractionHeight.coerceAtLeast(1f))
        shader.setFloatUniform("refractionAmount", refractionAmount)
        shader.setIntUniform("depthEffect", if (depthEffect) 1 else 0)
        shader.setIntUniform("chromaticAberration", if (chromaticAberration) 1 else 0)
        RenderEffect.createRuntimeShaderEffect(shader, "content")
    }.getOrNull()
}
