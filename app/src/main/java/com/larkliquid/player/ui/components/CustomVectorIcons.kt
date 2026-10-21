package com.larkliquid.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure Vector / Canvas Animated Player Icons (`CustomVectorIcons.kt`).
 * Strictly ZERO emojis anywhere:
 * - `AnimatedFastForward2xIndicator`: Real animated flowing vector chevrons for 2.0x speed hold.
 * - `IosSkip10sCanvasIcon`: Authentic iOS circular skip icon with rotating arc and crisp "10" label.
 * - `IosPlayPauseMorphIcon`: Smoothly morphing Play triangle / Pause bars.
 * - `IosPrevNextTrackIcon`: Crisp iOS Previous (`|<<`) and Next (`>>|`) vector icons.
 */

@Composable
fun AnimatedFastForward2xIndicator(
    speedText: String = "2.0x",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FastForward2xWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ChevronWavePhase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = speedText,
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )

        Canvas(modifier = Modifier.size(width = 28.dp, height = 16.dp)) {
            val strokePx = 2.2.dp.toPx()
            val chevronWidth = size.width * 0.24f
            val centerY = size.height * 0.5f
            val halfH = size.height * 0.36f

            for (i in 0..2) {
                val localPhase = ((phase * 3f) - i + 3f) % 3f
                val alphaWave = (0.28f + 0.72f * sin((localPhase / 3f) * PI).toFloat()).coerceIn(0.22f, 1f)
                val shiftX = (phase * 3.dp.toPx())
                val startX = (i * (chevronWidth * 1.15f)) + shiftX

                val path = Path().apply {
                    moveTo(startX, centerY - halfH)
                    lineTo(startX + chevronWidth, centerY)
                    lineTo(startX, centerY + halfH)
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = alphaWave),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun IosSkip10sCanvasIcon(
    seconds: Int = 10,
    isForward: Boolean,
    iconSize: Dp = 36.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(iconSize)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 2.3.dp.toPx()
            val arcPadding = 3.5.dp.toPx()
            val arcSize = Size(size.width - arcPadding * 2, size.height - arcPadding * 2)

            if (isForward) {
                drawArc(
                    color = Color.White,
                    startAngle = -78f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(arcPadding, arcPadding),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                val tipX = size.width * 0.63f
                val tipY = arcPadding + 0.5.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(tipX - 5.dp.toPx(), tipY - 3.8.dp.toPx())
                    lineTo(tipX + 2.2.dp.toPx(), tipY)
                    lineTo(tipX - 3.8.dp.toPx(), tipY + 4.8.dp.toPx())
                }
                drawPath(
                    path = arrowPath,
                    color = Color.White,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawArc(
                    color = Color.White,
                    startAngle = -102f,
                    sweepAngle = -300f,
                    useCenter = false,
                    topLeft = Offset(arcPadding, arcPadding),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                val tipX = size.width * 0.37f
                val tipY = arcPadding + 0.5.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(tipX + 5.dp.toPx(), tipY - 3.8.dp.toPx())
                    lineTo(tipX - 2.2.dp.toPx(), tipY)
                    lineTo(tipX + 3.8.dp.toPx(), tipY + 4.8.dp.toPx())
                }
                drawPath(
                    path = arrowPath,
                    color = Color.White,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        Text(
            text = String.format(Locale.US, "%d", seconds),
            color = Color.White,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 1.5.dp)
        )
    }
}

@Composable
fun IosPlayPauseMorphIcon(
    isPlaying: Boolean,
    iconSize: Dp = 32.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 1.06f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "PlayPauseMorphScale"
    )

    Canvas(
        modifier = Modifier
            .size(iconSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (isPlaying) {
            val barWidth = 7.2.dp.toPx()
            val barHeight = size.height * 0.82f
            val topY = (size.height - barHeight) / 2f
            val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.19f, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.81f - barWidth, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = corner
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.12f)
                lineTo(size.width * 0.88f, size.height * 0.50f)
                lineTo(size.width * 0.24f, size.height * 0.88f)
                close()
            }
            drawPath(path = path, color = Color.White)
        }
    }
}

@Composable
fun IosPrevNextTrackIcon(
    isNext: Boolean,
    enabled: Boolean = true,
    iconSize: Dp = 22.dp
) {
    val tintAlpha = if (enabled) 0.96f else 0.36f
    Canvas(modifier = Modifier.size(iconSize)) {
        val color = Color.White.copy(alpha = tintAlpha)
        val w = size.width
        val h = size.height
        val barWidth = 2.6.dp.toPx()
        val barCorner = CornerRadius(1.3.dp.toPx(), 1.3.dp.toPx())

        if (isNext) {
            val firstTriangle = Path().apply {
                moveTo(w * 0.10f, h * 0.18f)
                lineTo(w * 0.52f, h * 0.50f)
                lineTo(w * 0.10f, h * 0.82f)
                close()
            }
            val secondTriangle = Path().apply {
                moveTo(w * 0.48f, h * 0.18f)
                lineTo(w * 0.86f, h * 0.50f)
                lineTo(w * 0.48f, h * 0.82f)
                close()
            }
            drawPath(firstTriangle, color = color)
            drawPath(secondTriangle, color = color)
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.86f, h * 0.18f),
                size = Size(barWidth, h * 0.64f),
                cornerRadius = barCorner
            )
        } else {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.14f - barWidth, h * 0.18f),
                size = Size(barWidth, h * 0.64f),
                cornerRadius = barCorner
            )
            val firstTriangle = Path().apply {
                moveTo(w * 0.52f, h * 0.18f)
                lineTo(w * 0.14f, h * 0.50f)
                lineTo(w * 0.52f, h * 0.82f)
                close()
            }
            val secondTriangle = Path().apply {
                moveTo(w * 0.90f, h * 0.18f)
                lineTo(w * 0.48f, h * 0.50f)
                lineTo(w * 0.90f, h * 0.82f)
                close()
            }
            drawPath(firstTriangle, color = color)
            drawPath(secondTriangle, color = color)
        }
    }
}

@Composable
fun VectorCheckIcon(
    color: Color = Color.White,
    iconSize: Dp = 16.dp
) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val strokePx = 2.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.76f)
            lineTo(size.width * 0.84f, size.height * 0.24f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
