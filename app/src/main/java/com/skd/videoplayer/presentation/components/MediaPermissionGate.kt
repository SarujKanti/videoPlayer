package com.skd.videoplayer.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skd.videoplayer.core.utils.MediaAccess
import com.skd.videoplayer.core.utils.MediaPermissions

/** What the gate hands to its content: the current access level and a way to ask for more. */
class MediaAccessState(
    val access: MediaAccess,
    val requestAccess: () -> Unit,
)

/**
 * Asks for the narrowest video permission for the running Android version and shows [content]
 * once any access exists. Access is re-checked on every resume, so changes made in system
 * settings are picked up. [onAccessChanged] fires after the user answers the system dialog.
 */
@Composable
fun MediaPermissionGate(
    onAccessChanged: () -> Unit,
    content: @Composable (MediaAccessState) -> Unit,
) {
    val context = LocalContext.current
    val sdk = Build.VERSION.SDK_INT
    val permissions = remember(sdk) { MediaPermissions.requiredPermissions(sdk) }
    val currentAccess = {
        MediaPermissions.accessLevel(sdk) {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var access by remember { mutableStateOf(currentAccess()) }
    var denials by rememberSaveable { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        access = currentAccess()
        if (access == MediaAccess.NONE) denials++
        onAccessChanged()
    }

    LifecycleResumeEffect(Unit) {
        val previous = access
        access = currentAccess()
        if (previous != access) onAccessChanged()
        onPauseOrDispose { }
    }

    val request = { launcher.launch(permissions) }

    if (access == MediaAccess.NONE) {
        // After a denial the system stops showing its dialog once the rationale flag turns false.
        val activity = context.findActivity()
        val blocked = denials > 0 && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions.first())
        MessageState(
            icon = Icons.Outlined.VideoLibrary,
            title = "Your videos, all in one place",
            message = if (blocked) {
                "Video access is turned off. Open settings and allow Lumora to access videos to build your library."
            } else {
                "Lumora needs access to the videos on this device to show and play them. Nothing leaves your phone."
            },
            actionLabel = if (blocked) "Open settings" else "Allow access",
            onAction = if (blocked) ({ context.openAppSettings() }) else request,
        )
    } else {
        content(MediaAccessState(access, request))
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
