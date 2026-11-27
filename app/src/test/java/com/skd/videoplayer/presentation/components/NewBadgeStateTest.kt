package com.skd.videoplayer.presentation.components

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.home.HomeViewModel
import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeVideoEditRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import com.skd.videoplayer.testutil.FakeWatchedVideosRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewBadgeStateTest {

    private val dispatcher = StandardTestDispatcher()
    private val videos = FakeVideoRepository()
    private val watched = FakeWatchedVideosRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        videos.library.value = Result.success(listOf(testVideo(1), testVideo(2), testVideo(3)))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.state(): () -> VideoListUiState {
        val viewModel = HomeViewModel(
            videos,
            FakeFavoritesRepository(),
            watched,
            FakeVideoEditRepository(),
            FilterAndSortVideosUseCase(),
            ValidateVideoNameUseCase(),
            dispatcher,
            SavedStateHandle(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return {
            advanceUntilIdle()
            viewModel.uiState.value
        }
    }

    @Test
    fun `every video is new until it has been watched`() = runTest(dispatcher) {
        val state = state()()
        assertTrue(state.videos.all { state.isNew(it) })
    }

    @Test
    fun `only videos outside the watched set are new`() = runTest(dispatcher) {
        watched.ids.value = setOf(2L)
        val state = state()()
        assertEquals(setOf(2L), state.watchedIds)
        assertEquals(setOf(1L, 3L), state.videos.filter { state.isNew(it) }.map { it.id }.toSet())
    }

    @Test
    fun `badge disappears once the video is marked watched`() = runTest(dispatcher) {
        val current = state()
        assertTrue(current().isNew(testVideo(1)))
        watched.markWatched(1)
        assertFalse(current().isNew(testVideo(1)))
        assertTrue(current().isNew(testVideo(2)))
    }

    @Test
    fun `nothing is new before the library has loaded`() {
        assertTrue(VideoListUiState().videos.isEmpty())
    }
}
