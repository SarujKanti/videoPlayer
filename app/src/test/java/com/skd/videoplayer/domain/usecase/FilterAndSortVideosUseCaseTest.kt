package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.SortField
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.testutil.testVideo
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAndSortVideosUseCaseTest {

    private val useCase = FilterAndSortVideosUseCase()

    private val videos = listOf(
        testVideo(1, title = "beach day", durationMs = 30_000, sizeBytes = 300, dateAddedEpochMs = 3),
        testVideo(2, title = "Birthday", durationMs = 90_000, sizeBytes = 100, dateAddedEpochMs = 1),
        testVideo(3, title = "concert", durationMs = 60_000, sizeBytes = 200, dateAddedEpochMs = 2),
    )

    @Test
    fun `search is case-insensitive and trims whitespace`() {
        val result = useCase(videos, "  BIRTH ", VideoSort.Default)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `blank query keeps every video`() {
        assertEquals(3, useCase(videos, "   ", VideoSort.Default).size)
    }

    @Test
    fun `name sort ignores case`() {
        val result = useCase(videos, "", VideoSort(SortField.NAME, ascending = true))
        assertEquals(listOf("beach day", "Birthday", "concert"), result.map { it.title })
    }

    @Test
    fun `default sort is newest first`() {
        assertEquals(listOf(1L, 3L, 2L), useCase(videos, "", VideoSort.Default).map { it.id })
    }

    @Test
    fun `duration and size sorts respect direction`() {
        assertEquals(
            listOf(1L, 3L, 2L),
            useCase(videos, "", VideoSort(SortField.DURATION, ascending = true)).map { it.id },
        )
        assertEquals(
            listOf(1L, 3L, 2L),
            useCase(videos, "", VideoSort(SortField.SIZE, ascending = false)).map { it.id },
        )
    }

    @Test
    fun `equal keys fall back to a stable id order`() {
        val same = listOf(testVideo(5, sizeBytes = 1), testVideo(4, sizeBytes = 1))
        assertEquals(listOf(4L, 5L), useCase(same, "", VideoSort(SortField.SIZE, ascending = true)).map { it.id })
    }
}
