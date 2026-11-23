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
    fun relativeDate() {
        val day = 24L * 60 * 60 * 1000
        val now = 1_763_000_000_000L
        assertEquals("Today", Formatters.relativeDate(now - 1_000, now))
        assertEquals("Yesterday", Formatters.relativeDate(now - day - 1_000, now))
        assertEquals("3 days ago", Formatters.relativeDate(now - 3 * day, now))
        assertEquals("1 week ago", Formatters.relativeDate(now - 8 * day, now))
        assertEquals("3 weeks ago", Formatters.relativeDate(now - 22 * day, now))
        assertEquals("", Formatters.relativeDate(0, now))
        // Older than a month falls back to a calendar date.
        val old = now - 400 * day
        assertEquals(
            java.text.SimpleDateFormat("d MMM yyyy", Locale.US).format(java.util.Date(old)),
            Formatters.relativeDate(old, now, Locale.US),
        )
    }

    @Test
    fun isNewMeansAddedInTheLastTwoDays() {
        val day = 24L * 60 * 60 * 1000
        val now = 1_763_000_000_000L
        assertEquals(true, Formatters.isNew(now - day, now))
        assertEquals(false, Formatters.isNew(now - 3 * day, now))
        assertEquals(false, Formatters.isNew(0, now))
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
