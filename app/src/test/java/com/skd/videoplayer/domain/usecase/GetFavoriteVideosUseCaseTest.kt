package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import com.skd.videoplayer.testutil.testVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetFavoriteVideosUseCaseTest {

    private val videos = FakeVideoRepository()
    private val favorites = FakeFavoritesRepository()
    private val useCase = GetFavoriteVideosUseCase(videos, favorites)

    @Test
    fun `only favorite videos are returned`() = runTest {
        videos.library.value = Result.success(listOf(testVideo(1), testVideo(2), testVideo(3)))
        favorites.ids.value = setOf(1, 3)

        assertEquals(listOf(1L, 3L), useCase().first().getOrThrow().map { it.id })
    }

    @Test
    fun `favorites for videos that are no longer visible are skipped`() = runTest {
        videos.library.value = Result.success(listOf(testVideo(1)))
        favorites.ids.value = setOf(1, 99)

        assertEquals(listOf(1L), useCase().first().getOrThrow().map { it.id })
        assertEquals(setOf(1L, 99L), favorites.ids.value) // nothing was erased
    }

    @Test
    fun `follows changes to the favorite set`() = runTest {
        videos.library.value = Result.success(listOf(testVideo(1), testVideo(2)))
        favorites.ids.value = emptySet()
        assertTrue(useCase().first().getOrThrow().isEmpty())

        favorites.toggle(2)
        assertEquals(listOf(2L), useCase().first().getOrThrow().map { it.id })
    }

    @Test
    fun `library errors pass through`() = runTest {
        videos.library.value = Result.failure(SecurityException())
        favorites.ids.value = setOf(1)

        assertTrue(useCase().first().exceptionOrNull() is SecurityException)
    }
}
