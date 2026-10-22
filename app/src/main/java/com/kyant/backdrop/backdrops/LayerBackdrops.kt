package com.kyant.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.CombinedBackdrop
import com.kyant.backdrop.LayerBackdrop

@Composable
fun rememberLayerBackdrop(
    onDraw: (ContentDrawScope.() -> Unit)? = null
): LayerBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer, onDraw) {
        LayerBackdrop(graphicsLayer = graphicsLayer, onDrawBlock = onDraw)
    }
}

@Composable
fun rememberCombinedBackdrop(
    first: Backdrop,
    second: Backdrop
): Backdrop {
    return remember(first, second) {
        CombinedBackdrop(listOf(first, second))
    }
}

@Composable
fun rememberCombinedBackdrop(
    vararg backdrops: Backdrop
): Backdrop {
    return remember(*backdrops) {
        CombinedBackdrop(backdrops.toList())
    }
}

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier {
    return this
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                backdrop.layerCoordinates = coords
                backdrop.rootOffset = coords.positionInRoot()
            }
        }
        .drawWithContent {
            if (!backdrop.isRecording) {
                backdrop.isRecording = true
                try {
                    runCatching {
                        backdrop.graphicsLayer.record {
                            val customBlock = backdrop.onDrawBlock
                            if (customBlock != null) {
                                customBlock.invoke(this@drawWithContent)
                            } else {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                } finally {
                    backdrop.isRecording = false
                }
            }
            runCatching {
                drawLayer(backdrop.graphicsLayer)
            }.onFailure {
                drawContent()
            }
        }
}
