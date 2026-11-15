package com.skd.videoplayer.presentation.player

import androidx.compose.ui.geometry.Offset
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PlayerLogicTest {

    @Test
    fun `playback error codes map to actionable kinds`() {
        assertEquals(
            PlaybackErrorKind.FILE_NOT_FOUND,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND),
        )
        assertEquals(
            PlaybackErrorKind.UNSUPPORTED_CONTAINER,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED),
        )
        assertEquals(
            PlaybackErrorKind.UNSUPPORTED_CODEC,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES),
        )
        assertEquals(
            PlaybackErrorKind.DECODER_FAILED,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED),
        )
        assertEquals(
            PlaybackErrorKind.CORRUPT_FILE,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED),
        )
        assertEquals(
            PlaybackErrorKind.UNKNOWN,
            PlaybackErrorKind.fromErrorCode(PlaybackException.ERROR_CODE_UNSPECIFIED),
        )
    }

    @Test
    fun `audio track label combines language, channels and codec`() {
        val format = Format.Builder()
            .setLanguage("en")
            .setChannelCount(6)
            .setSampleMimeType(MimeTypes.AUDIO_AC3)
            .build()
        assertEquals("English  ·  5.1  ·  AC3", TrackOptions.label(format, 1, Locale.US))
    }

    @Test
    fun `explicit track title wins over language`() {
        val format = Format.Builder().setLabel("Director's commentary").setLanguage("en").build()
        assertEquals("Director's commentary", TrackOptions.label(format, 2, Locale.US))
    }

    @Test
    fun `track without metadata falls back to its number`() {
        val format = Format.Builder().setLanguage("und").setSampleMimeType(MimeTypes.APPLICATION_SUBRIP).build()
        assertEquals("Track 3  ·  SRT", TrackOptions.label(format, 3, Locale.US))
    }

    @Test
    fun `taps split the screen into thirds`() {
        assertEquals(TapZone.LEFT, tapZoneFor(100f, 900))
        assertEquals(TapZone.CENTER, tapZoneFor(450f, 900))
        assertEquals(TapZone.RIGHT, tapZoneFor(800f, 900))
    }

    @Test
    fun `drag direction and side pick the gesture`() {
        assertEquals(DragGesture.SEEK, dragGestureFor(Offset(40f, 5f), startX = 100f, width = 1000))
        assertEquals(DragGesture.BRIGHTNESS, dragGestureFor(Offset(3f, -40f), startX = 100f, width = 1000))
        assertEquals(DragGesture.VOLUME, dragGestureFor(Offset(3f, 40f), startX = 900f, width = 1000))
    }

    @Test
    fun `speed labels`() {
        assertEquals(listOf("0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), PlaybackSpeeds.map(::speedLabel))
    }

    @Test
    fun `scale modes cycle`() {
        assertEquals(VideoScaleMode.FILL, VideoScaleMode.FIT.next())
        assertEquals(VideoScaleMode.FIT, VideoScaleMode.STRETCH.next())
    }
}
