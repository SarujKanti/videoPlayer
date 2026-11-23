package com.skd.videoplayer

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.navigation.AppNavigation
import com.skd.videoplayer.presentation.components.MediaPermissionGate
import com.skd.videoplayer.presentation.splash.SplashExitAnimation
import com.skd.videoplayer.presentation.splash.SplashGate
import com.skd.videoplayer.presentation.theme.LumoraTheme
import com.skd.videoplayer.presentation.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** The theme currently on screen, so the bars can be restored after the splash hand-over. */
    private var darkSystemBars = true

    /** Light icons on a dark theme, dark icons on a light one. */
    private fun applySystemBarStyle(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the launch theme hands over cleanly.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val splashGate = SplashGate(startedAtMs = SystemClock.uptimeMillis())
        splashScreen.setKeepOnScreenCondition {
            splashGate.shouldKeepOnScreen(viewModel.isReady, SystemClock.uptimeMillis())
        }
        // Handing over from the splash re-applies the window theme, which resets the system bar
        // icon colour, so the app's choice is applied again once the splash is gone.
        splashScreen.setOnExitAnimationListener { provider ->
            SplashExitAnimation.play(provider) { applySystemBarStyle(darkSystemBars) }
        }

        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            // The splash covers this wait; if it timed out, fall back to the default theme.
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
                        val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
                        AppNavigation(mediaAccess = access, favoriteCount = favoriteCount)
                    }
                }
            }
        }
    }
}
