package com.skd.videoplayer.core.utils

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPermissionsTest {

    @Test
    fun `requests the narrowest permission per Android version`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            MediaPermissions.requiredPermissions(28),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MediaPermissions.requiredPermissions(29),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
            MediaPermissions.requiredPermissions(33),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            MediaPermissions.requiredPermissions(34),
        )
    }

    @Test
    fun `legacy storage permission grants full access below Android 13`() {
        val granted = setOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertEquals(MediaAccess.FULL, MediaPermissions.accessLevel(30) { it in granted })
        // The legacy permission means nothing on Android 13+.
        assertEquals(MediaAccess.NONE, MediaPermissions.accessLevel(33) { it in granted })
    }

    @Test
    fun `selected-videos grant is partial access on Android 14`() {
        val granted = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        assertEquals(MediaAccess.PARTIAL, MediaPermissions.accessLevel(34) { it in granted })
    }

    @Test
    fun `full video grant wins over partial`() {
        val granted = setOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        assertEquals(MediaAccess.FULL, MediaPermissions.accessLevel(35) { it in granted })
    }

    @Test
    fun `nothing granted is no access`() {
        assertEquals(MediaAccess.NONE, MediaPermissions.accessLevel(34) { false })
        assertEquals(MediaAccess.NONE, MediaPermissions.accessLevel(24) { false })
    }
}
