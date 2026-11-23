package com.skd.videoplayer

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skd.videoplayer.core.utils.MediaAccess
import com.skd.videoplayer.core.utils.MediaPermissions
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.navigation.AppNavigation
import com.skd.videoplayer.presentation.components.MediaPermissionGate
import com.skd.videoplayer.presentation.theme.LumoraTheme
import com.skd.videoplayer.presentation.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** The theme currently on screen, so the bars can be restored after the splash hand-over. */
    private var darkSystemBars = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: it swaps the launch theme for the regular app theme.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel.awaitInitialLibrary(hasMediaAccess = hasMediaAccess())
        // Also wait for the saved theme (read in milliseconds) so the first frame is never in the
        // wrong theme. DataStore falls back to defaults on read errors, so this always resolves.
        splashScreen.setKeepOnScreenCondition {
            !viewModel.isReady.value || viewModel.themeMode.value == null
        }
        // Fade the splash out instead of cutting straight to the first frame. Removing the splash
        // re-applies the window theme, which resets the system bar icon colour, so the app's own
        // choice is applied again afterwards.
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    provider.remove()
                    applySystemBarStyle(darkSystemBars)
                }
                .start()
        }

        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            // The splash covers the wait for the saved theme; the default is only a safety net.
            val mode = themeMode ?: ThemeMode.Default
            val darkTheme = mode.isDark()

            // Status and navigation bar icons must contrast with the chosen theme, not the phone's.
            DisposableEffect(darkTheme) {
                darkSystemBars = darkTheme
                applySystemBarStyle(darkTheme)
                onDispose { }
            }

            LumoraTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MediaPermissionGate(onAccessChanged = viewModel::onMediaAccessChanged) { access ->
                        AppNavigation(mediaAccess = access)
                    }
                }
            }
        }
    }

    /** Light icons on a dark theme, dark icons on a light one. */
    private fun applySystemBarStyle(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun hasMediaAccess(): Boolean =
        MediaPermissions.accessLevel(Build.VERSION.SDK_INT) {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        } != MediaAccess.NONE

    private companion object {
        const val SPLASH_EXIT_DURATION_MS = 250L
    }
}
