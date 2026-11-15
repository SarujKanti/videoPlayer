package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.testutil.testVideo
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupVideosIntoFoldersUseCaseTest {

    private val useCase = GroupVideosIntoFoldersUseCase()

    @Test
    fun `groups by folder with counts, sizes and newest cover`() {
        val videos = listOf(
            testVideo(1, folderId = "cam", folderName = "Camera", sizeBytes = 10, dateAddedEpochMs = 1),
            testVideo(2, folderId = "cam", folderName = "Camera", sizeBytes = 20, dateAddedEpochMs = 5),
            testVideo(3, folderId = "dl", folderName = "downloads", sizeBytes = 7),
        )

        val folders = useCase(videos)

        assertEquals(listOf("Camera", "downloads"), folders.map { it.name })
        val camera = folders.first()
        assertEquals(2, camera.videoCount)
        assertEquals(30L, camera.totalSizeBytes)
        assertEquals(2L, camera.coverVideo.id)
    }

    @Test
    fun `empty library has no folders`() {
        assertEquals(emptyList<Any>(), useCase(emptyList()))
    }
}
