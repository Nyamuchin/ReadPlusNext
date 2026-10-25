package com.readplusnext.ui.motion

import androidx.compose.animation.core.CubicBezierEasing

/** Shared motion constants so every transition in the app feels like one system. */
object Motion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val PAGE_TRANSITION_MS = 500
    const val NAV_ENTER_MS = 380
    const val NAV_EXIT_MS = 260
    const val FADE_MS = 240
    const val CONTROL_MS = 200
}
