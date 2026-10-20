package com.kyant.backdrop.shadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class Shadow(
    val radius: Dp = 24.dp,
    val offset: DpOffset = DpOffset(0.dp, 4.dp),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val alpha: Float = 1.0f
) {
    companion object {
        val Default = Shadow()
    }
}

data class InnerShadow(
    val radius: Dp = 24.dp,
    val offset: DpOffset = DpOffset(0.dp, 4.dp),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val alpha: Float = 1.0f
) {
    companion object {
        val Default = InnerShadow()
    }
}
