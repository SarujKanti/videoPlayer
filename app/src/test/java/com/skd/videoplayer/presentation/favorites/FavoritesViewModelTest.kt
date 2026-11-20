package com.skd.videoplayer.presentation.favorites

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.domain.model.SortField
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.GetFavoriteVideosUseCase
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.components.VideoListUiState
import com.skd.videoplayer.presentation.components.VideoMenuAction
import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeVideoEditRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import com.skd.videoplayer.testutil.testVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val videos = FakeVideoRepository()
    private val favorites = FakeFavoritesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        videos.library.value = Result.success(
            listOf(testVideo(1, title = "beta"), testVideo(2, title = "alpha"), testVideo(3, title = "gamma")),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.observe(): Pair<FavoritesViewModel, () -> VideoListUiState> {
        val viewModel = FavoritesViewModel(
            videos,
            favorites,
            FakeVideoEditRepository(),
            FilterAndSortVideosUseCase(),
            ValidateVideoNameUseCase(),
            GetFavoriteVideosUseCase(videos, favorites),
            dispatcher,
            SavedStateHandle(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel to {
            advanceUntilIdle()
            viewModel.uiState.value
        }
    }

    @Test
    fun `lists only favorites, searchable and sortable`() = runTest(dispatcher) {
        favorites.ids.value = setOf(1, 2)
        val (viewModel, state) = observe()

        assertEquals(2, state().totalCount)

        viewModel.onSortChange(VideoSort(SortField.NAME, ascending = true))
        assertEquals(listOf("alpha", "beta"), state().videos.map { it.title })

        viewModel.onQueryChange("bet")
        assertEquals(listOf("beta"), state().videos.map { it.title })
    }

    @Test
    fun `removing a favorite from the menu drops it from the list`() = runTest(dispatcher) {
        favorites.ids.value = setOf(1, 2)
        val (viewModel, state) = observe()
        val first = state().videos.first { it.id == 1L }

        viewModel.onMenuAction(first, VideoMenuAction.TOGGLE_FAVORITE)

        assertEquals(listOf(2L), state().videos.map { it.id })
    }

    @Test
    fun `no favorites gives an empty list rather than an error`() = runTest(dispatcher) {
        val (_, state) = observe()

        val current = state()
        assertTrue(current.videos.isEmpty())
        assertEquals(0, current.totalCount)
        assertEquals(null, current.error)
    }
}
