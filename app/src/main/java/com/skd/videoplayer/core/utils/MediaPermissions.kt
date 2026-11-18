package com.skd.videoplayer.core.utils

import android.Manifest

enum class MediaAccess {
    /** Every video on the device is readable. */
    FULL,

    /** Android 14+: the user granted access to selected videos only. */
    PARTIAL,

    /** No video can be read. */
    NONE,
}

/**
 * Picks the narrowest storage permission for each Android version. The SDK level is a parameter
 * so the decision logic can be unit tested without a device.
 */
object MediaPermissions {

    private const val SDK_Q = 29
    private const val SDK_TIRAMISU = 33
    private const val SDK_UPSIDE_DOWN_CAKE = 34

    fun requiredPermissions(sdkInt: Int): Array<String> = when {
        sdkInt >= SDK_UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdkInt >= SDK_TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        sdkInt >= SDK_Q -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        // Android 9 and below have no per-file consent, so renaming and deleting need write access.
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun accessLevel(sdkInt: Int, isGranted: (String) -> Boolean): MediaAccess = when {
        sdkInt >= SDK_TIRAMISU && isGranted(Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.FULL
        sdkInt >= SDK_UPSIDE_DOWN_CAKE &&
            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL
        sdkInt < SDK_TIRAMISU && isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) -> MediaAccess.FULL
        else -> MediaAccess.NONE
    }
}
