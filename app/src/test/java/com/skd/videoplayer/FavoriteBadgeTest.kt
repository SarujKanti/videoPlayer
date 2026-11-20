package com.skd.videoplayer

import com.skd.videoplayer.domain.usecase.GetFavoriteVideosUseCase
import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeSettingsRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import com.skd.videoplayer.testutil.testVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The Favorites tab badge counts favorites that are actually on the device. */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteBadgeTest {

    private val dispatcher = StandardTestDispatcher()
    private val videos = FakeVideoRepository()
    private val favorites = FakeFavoritesRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `badge counts visible favorites and follows changes`() = runTest(dispatcher) {
        videos.library.value = Result.success(listOf(testVideo(1), testVideo(2)))
        favorites.ids.value = setOf(1, 42) // 42 is no longer on the device
        val viewModel = MainViewModel(videos, FakeSettingsRepository(), GetFavoriteVideosUseCase(videos, favorites))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.favoriteCount.collect {} }

        advanceUntilIdle()
        assertEquals(1, viewModel.favoriteCount.value)

        favorites.toggle(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.favoriteCount.value)
    }

    @Test
    fun `unreadable library shows no badge`() = runTest(dispatcher) {
        videos.library.value = Result.failure(SecurityException())
        favorites.ids.value = setOf(1)
        val viewModel = MainViewModel(videos, FakeSettingsRepository(), GetFavoriteVideosUseCase(videos, favorites))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.favoriteCount.collect {} }

        advanceUntilIdle()
        assertEquals(0, viewModel.favoriteCount.value)
    }
}
