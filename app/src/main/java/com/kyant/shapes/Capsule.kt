package com.kyant.shapes

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp

/**
 * Apple iOS 26 Continuous Pill / Capsule Shape (`com.kyant.shapes.Capsule`).
 * Embedded directly in the project source tree so Gradle builds never fail on external Kotlin metadata versions.
 */
fun Capsule(): CornerBasedShape = RoundedCornerShape(percent = 50)

val ContinuousCapsule: CornerBasedShape = RoundedCornerShape(percent = 50)

fun ContinuousRoundedRectangle(
    radius: Dp,
    continuity: Float = 1.0f
): CornerBasedShape = RoundedCornerShape(radius)
