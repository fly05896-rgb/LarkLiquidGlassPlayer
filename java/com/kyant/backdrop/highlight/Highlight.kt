package com.kyant.backdrop.highlight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Highlight(
    val width: Dp = 0.75.dp,
    val blurRadius: Dp = 0.5.dp,
    val alpha: Float = 1.0f,
    val style: HighlightStyle = HighlightStyle.Default
) {
    companion object {
        val Default = Highlight(alpha = 1.0f)
        val Ambient = Highlight(alpha = 0.65f, style = HighlightStyle.Ambient)
        val Plain = Highlight(alpha = 0.85f, style = HighlightStyle.Plain)
    }
}

interface HighlightStyle {
    companion object {
        val Default: HighlightStyle = object : HighlightStyle {}
        val Ambient: HighlightStyle = object : HighlightStyle {}
        fun Plain(color: Color = Color.White): HighlightStyle = object : HighlightStyle {}
        val Plain: HighlightStyle = Plain()
    }
}
