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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Stable
interface Backdrop {
    val isCoordinatesDependent: Boolean
        get() = true

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: Offset
    )

    fun DrawScope.drawBackdropLayer(
        density: Density,
        coordinates: Offset,
        layerProbe: LayerTransformProbe?
    ) {
        drawBackdrop(
            density = density,
            coordinates = Offset(
                x = coordinates.x + (layerProbe?.translationX ?: 0f),
                y = coordinates.y + (layerProbe?.translationY ?: 0f)
            )
        )
    }
}

class LayerTransformProbe : GraphicsLayerScope {
    override var scaleX: Float = 1f
    override var scaleY: Float = 1f
    override var alpha: Float = 1f
    override var translationX: Float = 0f
    override var translationY: Float = 0f
    override var shadowElevation: Float = 0f
    override var rotationX: Float = 0f
    override var rotationY: Float = 0f
    override var rotationZ: Float = 0f
    override var cameraDistance: Float = 8f
    override var transformOrigin: TransformOrigin = TransformOrigin.Center
    override var shape: Shape = CircleShape
    override var clip: Boolean = false
    override var renderEffect: ComposeRenderEffect? = null
    override var ambientShadowColor: Color = Color.Black
    override var spotShadowColor: Color = Color.Black
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    override var size: Size = Size.Zero
    override var density: Float = 1f
    override var fontScale: Float = 1f

    fun reset(currentSize: Size, currentDensity: Float, currentFontScale: Float) {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationX = 0f
        translationY = 0f
        rotationZ = 0f
        size = currentSize
        density = currentDensity
        fontScale = currentFontScale
    }
}

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    val onDrawBlock: (ContentDrawScope.() -> Unit)? = null
) : Backdrop {
    var rootOffset: Offset = Offset.Zero
        internal set

    var layerCoordinates: LayoutCoordinates? = null
        internal set

    @Volatile
    var isRecording: Boolean = false
        internal set

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: Offset
    ) {
        drawBackdropLayer(density, coordinates, null)
    }

    override fun DrawScope.drawBackdropLayer(
        density: Density,
        coordinates: Offset,
        layerProbe: LayerTransformProbe?
    ) {
        if (isRecording) return

        val targetX = coordinates.x + (layerProbe?.translationX ?: 0f)
        val targetY = coordinates.y + (layerProbe?.translationY ?: 0f)
        val dx = rootOffset.x - targetX
        val dy = rootOffset.y - targetY

        withTransform({
            translate(left = dx, top = dy)
        }) {
            runCatching {
                drawLayer(graphicsLayer)
            }
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
        density: Density,
        coordinates: Offset
    ) {
        for (i in backdrops.indices) {
            with(backdrops[i]) {
                drawBackdrop(density, coordinates)
            }
        }
    }

    override fun DrawScope.drawBackdropLayer(
        density: Density,
        coordinates: Offset,
        layerProbe: LayerTransformProbe?
    ) {
        for (i in backdrops.indices) {
            with(backdrops[i]) {
                drawBackdropLayer(density, coordinates, layerProbe)
            }
        }
    }
}

internal class OffsetHolder(
    var value: Offset = Offset.Zero,
    var coordinates: LayoutCoordinates? = null
)

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
        float refractedDistance = circleMap(1.0 - (-sd / safeHeight)) * (-refractionAmount);
        float2 radialDir = (length(centeredCoord) > 1e-3) ? normalize(centeredCoord) : float2(0.0);
        float2 grad = normalize(normal + depthEffect * radialDir);
        float2 displacement = refractedDistance * grad;
        float2 safeMax = max(size - 1.0, float2(1.0));

        if (chromaticAberration > 0.01) {
            float dispersionIntensity = chromaticAberration * (
                (centeredCoord.x * centeredCoord.y) / max(halfSize.x * halfSize.y, 1.0)
            );
            float2 dispersedCoord = displacement * dispersionIntensity;

            half4 color = half4(0.0);

            half4 red = content.eval(clamp(coord + displacement + dispersedCoord * 1.2, float2(0.0), safeMax));
            color.r += red.r / 3.5;
            color.a += red.a / 7.0;

            half4 orange = content.eval(clamp(coord + displacement + dispersedCoord * 0.8, float2(0.0), safeMax));
            color.r += orange.r / 3.5;
            color.g += orange.g / 7.0;
            color.a += orange.a / 7.0;

            half4 yellow = content.eval(clamp(coord + displacement + dispersedCoord * 0.4, float2(0.0), safeMax));
            color.r += yellow.r / 3.5;
            color.g += yellow.g / 3.5;
            color.a += yellow.a / 7.0;

            half4 green = content.eval(clamp(coord + displacement, float2(0.0), safeMax));
            color.g += green.g / 3.5;
            color.a += green.a / 7.0;

            half4 cyan = content.eval(clamp(coord + displacement - dispersedCoord * 0.4, float2(0.0), safeMax));
            color.g += cyan.g / 3.5;
            color.b += cyan.b / 3.0;
            color.a += cyan.a / 7.0;

            half4 blue = content.eval(clamp(coord + displacement - dispersedCoord * 0.8, float2(0.0), safeMax));
            color.b += blue.b / 3.0;
            color.a += blue.a / 7.0;

            half4 purple = content.eval(clamp(coord + displacement - dispersedCoord * 1.2, float2(0.0), safeMax));
            color.r += purple.r / 7.0;
            color.b += purple.b / 3.0;
            color.a += purple.a / 7.0;

            return color;
        } else {
            float2 sampleCoord = clamp(coord + displacement, float2(0.0), safeMax);
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
    val layerProbe = remember { LayerTransformProbe() }
    val resolvedShape = shape()
    val rememberedLensShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(LIQUID_GLASS_LENS_AGSL) }.getOrNull()
        } else null
    }

    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                offsetHolder.coordinates = coords
                offsetHolder.value = coords.positionInRoot()
            }
        }
        .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
        .drawWithCache {
            val densityPx = density
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
                    layerProbe.reset(size, densityPx, fontScale)
                    layerBlock?.invoke(layerProbe)

                    val effectScope = BackdropEffectScope(size, layoutDirection, densityPx, rememberedLensShader)
                    effects?.invoke(effectScope)
                    val androidRenderEffect = effectScope.buildCombinedRenderEffect()
                    val composeRenderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && androidRenderEffect != null) {
                        androidRenderEffect.asComposeRenderEffect()
                    } else null

                    val sh = shadow?.invoke()
                    if (sh != null && sh.alpha > 0.01f) {
                        drawRoundRect(
                            color = sh.color.copy(alpha = sh.color.alpha * sh.alpha),
                            topLeft = Offset(sh.offset.x.toPx(), sh.offset.y.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f)
                        )
                    }

                    val liveCoords = offsetHolder.coordinates
                    if (liveCoords != null && liveCoords.isAttached) {
                        offsetHolder.value = liveCoords.positionInRoot()
                    }

                    foregroundLayer.renderEffect = composeRenderEffect
                    runCatching {
                        foregroundLayer.record {
                            val drawBg: DrawScope.() -> Unit = {
                                with(backdrop) {
                                    drawBackdropLayer(
                                        density = this@onDrawWithContent,
                                        coordinates = offsetHolder.value,
                                        layerProbe = if (layerBlock != null) layerProbe else null
                                    )
                                }
                            }
                            if (onDrawBackdrop != null) {
                                onDrawBackdrop(drawBg)
                            } else {
                                drawBg()
                            }
                        }
                    }

                    clipPath(clipPath) {
                        runCatching { drawLayer(foregroundLayer) }
                        onDrawSurface?.invoke(this)

                        val inSh = innerShadow?.invoke()
                        if (inSh != null && inSh.alpha > 0.01f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.28f * inSh.alpha),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.14f * inSh.alpha)
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
                                        Color.White.copy(alpha = 0.68f * hl.alpha),
                                        Color.White.copy(alpha = 0.14f * hl.alpha),
                                        Color.White.copy(alpha = 0.45f * hl.alpha)
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

                    if (exportedBackdrop != null && !exportedBackdrop.isRecording) {
                        exportedBackdrop.rootOffset = Offset(
                            x = offsetHolder.value.x + layerProbe.translationX,
                            y = offsetHolder.value.y + layerProbe.translationY
                        )
                        exportedBackdrop.isRecording = true
                        try {
                            runCatching {
                                exportedBackdrop.graphicsLayer.record {
                                    drawLayer(foregroundLayer)
                                    onDrawSurface?.invoke(this)
                                }
                            }
                        } finally {
                            exportedBackdrop.isRecording = false
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
        shader.setFloatUniform("depthEffect", if (depthEffect) 0.35f else 0f)
        shader.setFloatUniform("chromaticAberration", if (chromaticAberration) 1f else 0f)
        RenderEffect.createRuntimeShaderEffect(shader, "content")
    }.getOrNull()
}
