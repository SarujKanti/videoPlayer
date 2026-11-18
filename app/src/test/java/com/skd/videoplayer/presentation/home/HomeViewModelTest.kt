package com.skd.videoplayer.presentation.home

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.SortField
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.components.LibraryError
import com.skd.videoplayer.presentation.components.VideoListUiState
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeVideoRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() =
        HomeViewModel(
            repository,
            FakeFavoritesRepository(),
            FakeVideoEditRepository(),
            FilterAndSortVideosUseCase(),
            ValidateVideoNameUseCase(),
            dispatcher,
            SavedStateHandle(),
        )

    /** Keeps uiState subscribed (it uses WhileSubscribed) and returns the latest value. */
    private fun TestScope.observe(viewModel: HomeViewModel): () -> VideoListUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return {
            advanceUntilIdle()
            viewModel.uiState.value
        }
    }

    @Test
    fun `starts loading, then shows the library`() = runTest(dispatcher) {
        repository.library.value = Result.success(listOf(testVideo(1), testVideo(2)))
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        val state = observe(viewModel)()

        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        assertEquals(listOf(2L, 1L), state.videos.map { it.id }) // newest first
    }

    @Test
    fun `search, sort and layout update the state`() = runTest(dispatcher) {
        repository.library.value = Result.success(
            listOf(testVideo(1, title = "Zebra"), testVideo(2, title = "apple"), testVideo(3, title = "Mango")),
        )
        val viewModel = createViewModel()
        val state = observe(viewModel)

        viewModel.onSortChange(VideoSort(SortField.NAME, ascending = true))
        assertEquals(listOf("apple", "Mango", "Zebra"), state().videos.map { it.title })

        viewModel.onQueryChange("an")
        assertEquals(listOf("Mango"), state().videos.map { it.title })
        assertEquals(3, state().totalCount)

        viewModel.onLayoutChange(LibraryLayout.LIST)
        assertEquals(LibraryLayout.LIST, state().layout)
    }

    @Test
    fun `library updates flow through`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = observe(viewModel)
        assertEquals(0, state().totalCount)

        repository.library.value = Result.success(listOf(testVideo(7)))

        assertEquals(listOf(7L), state().videos.map { it.id })
    }

    @Test
    fun `security failure maps to a permission error`() = runTest(dispatcher) {
        repository.library.value = Result.failure(SecurityException())
        val state = observe(createViewModel())()

        assertEquals(LibraryError.PERMISSION_REVOKED, state.error)
        assertTrue(state.videos.isEmpty())
    }

    @Test
    fun `refresh calls the repository and clears the refreshing flag`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val state = observe(viewModel)

        viewModel.refresh()

        assertFalse(state().isRefreshing)
        assertNull(state().error)
        assertEquals(1, repository.refreshCount)
    }
}
