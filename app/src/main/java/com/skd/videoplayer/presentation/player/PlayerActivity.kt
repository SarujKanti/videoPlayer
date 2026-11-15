package com.skd.videoplayer.presentation.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skd.videoplayer.presentation.theme.LumoraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Full-screen player window. Handles what only an Activity can: immersive mode, orientation,
 * keep-screen-on, Picture-in-Picture and pausing when the user leaves.
 */
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private var isInPip by mutableStateOf(false)

    /** Set once the user rotates manually; disables automatic orientation for this session. */
    private var manualOrientation: Int? = null

    private val supportsPip: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manualOrientation = savedInstanceState?.getInt(KEY_ORIENTATION, -1)?.takeIf { it >= 0 }
        enterImmersiveMode()

        val request = PlayerRequest.from(intent)
        if (request == null) {
            finish()
            return
        }
        viewModel.open(request)

        setContent {
            LumoraTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    isInPip = isInPip,
                    canEnterPip = supportsPip,
                    onEnterPip = ::enterPip,
                    onRotate = ::toggleOrientation,
                    onBack = ::finish,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { Triple(it.videoWidth, it.videoHeight, it.isPlaying) }
                    .distinctUntilChanged()
                    .collect { (width, height, playing) ->
                        updateKeepScreenOn(playing)
                        applyAutoOrientation(width, height)
                        updatePipParams(width, height, playing)
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PlayerRequest.from(intent)?.let { viewModel.open(it, replace = true) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        manualOrientation?.let { outState.putInt(KEY_ORIENTATION, it) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // System bars come back after dialogs or notification shade; hide them again.
        if (hasFocus) enterImmersiveMode()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ enters PiP automatically via setAutoEnterEnabled; older versions need this.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && viewModel.player.isPlaying) enterPip()
    }

    override fun onStop() {
        super.onStop()
        // Background playback arrives with the media session service; until then, stop on leave.
        viewModel.pause()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun updateKeepScreenOn(playing: Boolean) {
        if (playing) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyAutoOrientation(width: Int, height: Int) {
        if (isInPip || width <= 0 || height <= 0) return
        requestedOrientation = manualOrientation
            ?: if (width >= height) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
    }

    private fun toggleOrientation() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val target = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        manualOrientation = target
        requestedOrientation = target
    }

    private fun pipParams(width: Int, height: Int, playing: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder()
        if (width > 0 && height > 0) {
            // Android rejects ratios outside roughly 1:2.39 .. 2.39:1.
            val ratio = (width.toFloat() / height).coerceIn(1 / 2.39f, 2.39f)
            builder.setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(playing)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun updatePipParams(width: Int, height: Int, playing: Boolean) {
        if (!supportsPip || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        pipParams(width, height, playing)?.let(::setPictureInPictureParams)
    }

    private fun enterPip() {
        if (!supportsPip || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val state = viewModel.uiState.value
        pipParams(state.videoWidth, state.videoHeight, state.isPlaying)?.let {
            runCatching { enterPictureInPictureMode(it) }
        }
    }

    private companion object {
        const val KEY_ORIENTATION = "manual_orientation"
    }
}
