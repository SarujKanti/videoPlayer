package com.skd.videoplayer

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skd.videoplayer.core.utils.MediaAccess
import com.skd.videoplayer.core.utils.MediaPermissions
import com.skd.videoplayer.navigation.AppNavigation
import com.skd.videoplayer.presentation.components.MediaPermissionGate
import com.skd.videoplayer.presentation.theme.LumoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: it swaps the launch theme for the regular app theme.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel.awaitInitialLibrary(hasMediaAccess = hasMediaAccess())
        splashScreen.setKeepOnScreenCondition { !viewModel.isReady.value }

        // The app theme is dark regardless of the system setting, so force light system-bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            LumoraTheme {
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

    private fun hasMediaAccess(): Boolean =
        MediaPermissions.accessLevel(Build.VERSION.SDK_INT) {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        } != MediaAccess.NONE
}
