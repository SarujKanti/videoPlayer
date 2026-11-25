package com.skd.videoplayer

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skd.videoplayer.core.utils.MediaAccess
import com.skd.videoplayer.core.utils.MediaPermissions
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.navigation.AppNavigation
import com.skd.videoplayer.presentation.components.MediaPermissionGate
import com.skd.videoplayer.presentation.splash.BrandReveal
import com.skd.videoplayer.presentation.splash.SplashIconBounds
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
        // Hand over to the animated brand reveal, which starts with the logo exactly where the
        // system drew it. With animations turned off (or no icon to measure) just fade out.
        splashScreen.setOnExitAnimationListener { provider ->
            val bounds = if (animationsEnabled()) provider.iconBounds() else null
            if (bounds != null) {
                splashProvider = provider
                brandReveal = bounds
            } else {
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

            Box(Modifier.fillMaxSize()) {
                // The app composes underneath while the reveal plays, so it's ready when it fades.
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
                brandReveal?.let { start ->
                    BrandReveal(
                        start = start,
                        onFirstFrame = ::removeSystemSplash,
                        onFinished = { brandReveal = null },
                    )
                }
            }
        }
    }

    /** The system splash, kept until the brand reveal has drawn its first frame on top of it. */
    private var splashProvider: SplashScreenViewProvider? = null

    /** Set while the brand reveal is playing. Not saved: after rotation the app shows directly. */
    private var brandReveal by mutableStateOf<SplashIconBounds?>(null)

    private fun removeSystemSplash() {
        val provider = splashProvider ?: return
        splashProvider = null
        provider.remove()
        // Removing the splash re-applies the window theme, which resets the bar icon colour.
        applySystemBarStyle(darkSystemBars)
    }

    private fun SplashScreenViewProvider.iconBounds(): SplashIconBounds? {
        val icon = runCatching { iconView }.getOrNull() ?: return null
        if (icon.width == 0 || icon.height == 0) return null
        val location = IntArray(2)
        icon.getLocationInWindow(location)
        val size = minOf(icon.width, icon.height)
        // Copy exactly what the system drew, backdrop and all. Some devices draw the icon on a
        // separate surface, which copies as blank; then fall back to the logo resource.
        val snapshot = runCatching { icon.drawToBitmap() }.getOrNull()
        val radius = snapshot?.let(::visibleRadius)
        return SplashIconBounds(
            centerX = location[0] + icon.width / 2,
            centerY = location[1] + icon.height / 2,
            sizePx = size,
            snapshot = snapshot?.takeIf { radius != null }?.asImageBitmap(),
            // The logo's ring spans 48 of the 108 units of the icon grid.
            visibleRadiusPx = radius ?: (size * 24f / 108f),
        )
    }

    /** Distance from the centre to the first visible pixel along the middle row, or null if blank. */
    private fun visibleRadius(bitmap: Bitmap): Float? {
        val y = bitmap.height / 2
        val centerX = bitmap.width / 2
        for (x in 0 until centerX) {
            if (Color.alpha(bitmap.getPixel(x, y)) > VISIBLE_ALPHA) return (centerX - x).toFloat()
        }
        return null
    }

    /** Respects "Remove animations" and a 0x animator scale in developer options. */
    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

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
        const val VISIBLE_ALPHA = 24
    }
}
