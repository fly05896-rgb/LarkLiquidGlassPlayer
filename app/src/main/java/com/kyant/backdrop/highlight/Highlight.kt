package com.kyant.backdrop.highlight

data class Highlight(
    val alpha: Float = 1.0f
) {
    companion object {
        val Default = Highlight(alpha = 1.0f)
    }
}
