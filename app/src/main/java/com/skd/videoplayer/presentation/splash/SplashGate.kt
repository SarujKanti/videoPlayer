package com.skd.videoplayer.presentation.splash

/**
 * Decides how long the splash screen stays up. It waits for the app to be ready to draw its
 * first real frame (the saved theme has been read) so the user never sees a half-built screen,
 * but gives up after [maxHoldMs] so a slow disk can't trap them on the splash.
 */
class SplashGate(
    private val startedAtMs: Long,
    private val maxHoldMs: Long = DEFAULT_MAX_HOLD_MS,
) {
    fun shouldKeepOnScreen(isReady: Boolean, nowMs: Long): Boolean =
        !isReady && nowMs - startedAtMs < maxHoldMs

    companion object {
        const val DEFAULT_MAX_HOLD_MS = 1_500L
    }
}
