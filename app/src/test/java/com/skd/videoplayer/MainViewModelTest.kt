package com.skd.videoplayer

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.testutil.FakeVideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `is not ready before the wait starts`() {
        val viewModel = MainViewModel(FakeVideoRepository())
        assertFalse(viewModel.isReady.value)
    }

    @Test
    fun `without media access it is ready immediately`() {
        val viewModel = MainViewModel(FakeVideoRepository())
        viewModel.awaitInitialLibrary(hasMediaAccess = false)
        assertTrue(viewModel.isReady.value)
    }

    @Test
    fun `with media access it becomes ready once the library emits`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeVideoRepository())
        viewModel.awaitInitialLibrary(hasMediaAccess = true)
        assertFalse(viewModel.isReady.value)

        runCurrent()
        assertTrue(viewModel.isReady.value)
    }

    @Test
    fun `a library that never emits is capped by the timeout`() = runTest(dispatcher) {
        val viewModel = MainViewModel(SilentVideoRepository())
        viewModel.awaitInitialLibrary(hasMediaAccess = true)

        advanceTimeBy(MainViewModel.SPLASH_MAX_DURATION_MS - 1)
        assertFalse(viewModel.isReady.value)

        advanceTimeBy(2)
        assertTrue(viewModel.isReady.value)
    }

    @Test
    fun `calling it again does not start a second wait`() = runTest(dispatcher) {
        val repository = CountingVideoRepository()
        val viewModel = MainViewModel(repository)
        viewModel.awaitInitialLibrary(hasMediaAccess = true)
        viewModel.awaitInitialLibrary(hasMediaAccess = true)
        advanceUntilIdle()

        assertEquals(1, repository.observeCount)
        assertTrue(viewModel.isReady.value)
    }

    @Test
    fun `media access change still refreshes the library`() = runTest(dispatcher) {
        val repository = FakeVideoRepository()
        val viewModel = MainViewModel(repository)
        viewModel.onMediaAccessChanged()
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)
    }

    /** Never emits, like a MediaStore scan that hangs. */
    private class SilentVideoRepository : VideoRepository {
        override fun observeVideos(): Flow<Result<List<Video>>> = MutableSharedFlow()
        override suspend fun refresh() = Unit
    }

    private class CountingVideoRepository : VideoRepository {
        private val delegate = FakeVideoRepository()
        var observeCount = 0
        override fun observeVideos(): Flow<Result<List<Video>>> {
            observeCount++
            return delegate.observeVideos()
        }
        override suspend fun refresh() = delegate.refresh()
    }
}
