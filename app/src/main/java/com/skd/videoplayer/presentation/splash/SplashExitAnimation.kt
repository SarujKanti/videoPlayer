package com.skd.videoplayer.presentation.splash

import android.view.animation.AccelerateInterpolator
import androidx.core.splashscreen.SplashScreenViewProvider

/** A short hand-off from splash to app: the mark grows slightly while the splash fades out. */
object SplashExitAnimation {

    private const val DURATION_MS = 260L
    private const val ICON_SCALE = 1.15f

    /** [onFinished] runs once the splash view has been removed. */
    fun play(provider: SplashScreenViewProvider, onFinished: () -> Unit = {}) {
        // Some Android 12 builds don't expose the icon view; the fade still runs without it.
        runCatching { provider.iconView }.getOrNull()
            ?.animate()
            ?.scaleX(ICON_SCALE)
            ?.scaleY(ICON_SCALE)
            ?.setDuration(DURATION_MS)
            ?.setInterpolator(AccelerateInterpolator())
            ?.start()

        provider.view.animate()
            .alpha(0f)
            .setDuration(DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                provider.remove()
                onFinished()
            }
            .start()
    }
}
