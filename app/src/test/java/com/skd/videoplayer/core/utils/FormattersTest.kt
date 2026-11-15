package com.skd.videoplayer.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun duration() {
        assertEquals("0:00", Formatters.duration(0))
        assertEquals("0:00", Formatters.duration(-100))
        assertEquals("5:09", Formatters.duration(309_000))
        assertEquals("1:05:09", Formatters.duration(3_909_000))
    }

    @Test
    fun fileSize() {
        assertEquals("512 B", Formatters.fileSize(512, Locale.US))
        assertEquals("2 KB", Formatters.fileSize(2048, Locale.US))
        assertEquals("1.5 MB", Formatters.fileSize(1_572_864, Locale.US))
        assertEquals("2.0 GB", Formatters.fileSize(2L * 1024 * 1024 * 1024, Locale.US))
    }

    @Test
    fun qualityLabelUsesShorterSide() {
        assertEquals("1080p", Formatters.qualityLabel(1920, 1080))
        assertEquals("1080p", Formatters.qualityLabel(1080, 1920)) // portrait
        assertEquals("4K", Formatters.qualityLabel(3840, 2160))
        assertEquals("360p", Formatters.qualityLabel(640, 360))
        assertNull(Formatters.qualityLabel(0, 0))
    }
}
