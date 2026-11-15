package com.skd.videoplayer.presentation.player

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/**
 * Window brightness and media volume for the swipe gestures. Brightness only affects this
 * window and resets when the player closes; the system setting is never changed.
 */
class DeviceControls(private val activity: Activity) {

    private val audioManager = activity.getSystemService(AudioManager::class.java)
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    fun volume(): Float = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume

    /** Returns the volume actually applied, since the stream only has [maxVolume] steps. */
    fun setVolume(fraction: Float): Float {
        val index = (fraction.coerceIn(0f, 1f) * maxVolume).roundToInt()
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0) }
        return index.toFloat() / maxVolume
    }

    fun brightness(): Float {
        val window = activity.window.attributes.screenBrightness
        if (window >= 0f) return window
        return runCatching {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f)
    }

    fun setBrightness(fraction: Float): Float {
        val value = fraction.coerceIn(MIN_BRIGHTNESS, 1f)
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = value }
        return value
    }

    private companion object {
        /** A fully black screen looks like a crash, so keep a sliver of light. */
        const val MIN_BRIGHTNESS = 0.01f
    }
}

@Composable
fun rememberDeviceControls(): DeviceControls? {
    val context = LocalContext.current
    return remember(context) { (context as? Activity)?.let(::DeviceControls) }
}
