package com.skd.videoplayer.data.mapper

import com.skd.videoplayer.core.constants.MediaConstants
import com.skd.videoplayer.testutil.testRow
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMapperTest {

    private val collection = "content://media/external/video/media"

    @Test
    fun `maps a complete row`() {
        val video = VideoMapper.toDomain(testRow(42), collection)

        assertEquals("content://media/external/video/media/42", video.uri)
        assertEquals("clip_42", video.title)
        assertEquals("clip_42.mp4", video.displayName)
        assertEquals("100", video.folderId)
        assertEquals("Camera", video.folderName)
        assertEquals(1_700_000_000_000L, video.dateAddedEpochMs)
        assertEquals(1280, video.width)
    }

    @Test
    fun `missing title falls back to file name without extension`() {
        val video = VideoMapper.toDomain(testRow(1, title = null, displayName = "holiday.final.mkv"), collection)
        assertEquals("holiday.final", video.title)
    }

    @Test
    fun `missing bucket falls back to the relative path`() {
        val video = VideoMapper.toDomain(
            testRow(1, bucketId = null, bucketName = null, relativePath = "Movies/Trips/"),
            collection,
        )
        assertEquals("Movies/Trips/", video.folderId)
        assertEquals("Trips", video.folderName)
    }

    @Test
    fun `row without any location goes to the unknown folder`() {
        val video = VideoMapper.toDomain(
            testRow(1, bucketId = null, bucketName = null, relativePath = null, displayName = null, title = null),
            collection,
        )
        assertEquals(MediaConstants.UNKNOWN_FOLDER_ID, video.folderId)
        assertEquals(MediaConstants.UNKNOWN_FOLDER_NAME, video.folderName)
        assertEquals("video_1", video.title)
    }

    @Test
    fun `negative values from broken metadata are clamped`() {
        val row = testRow(1).copy(durationMs = -5, sizeBytes = -1, width = -1)
        val video = VideoMapper.toDomain(row, "$collection/")
        assertEquals(0L, video.durationMs)
        assertEquals(0L, video.sizeBytes)
        assertEquals(false, video.hasResolution)
        assertEquals("content://media/external/video/media/1", video.uri)
    }
}
