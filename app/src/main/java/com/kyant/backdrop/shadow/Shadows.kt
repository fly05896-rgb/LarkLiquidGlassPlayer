package com.kyant.backdrop.shadow

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Shadow(
    val alpha: Float = 1.0f
)

data class InnerShadow(
    val radius: Dp = 8.dp,
    val alpha: Float = 1.0f
)
