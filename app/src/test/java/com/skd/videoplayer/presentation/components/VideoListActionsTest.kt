package com.skd.videoplayer.presentation.components

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.domain.model.VideoEditError
import com.skd.videoplayer.domain.model.VideoEditResult
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.NameProblem
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.home.HomeViewModel
import com.skd.videoplayer.testutil.FakeConsent
import com.skd.videoplayer.testutil.FakeFavoritesRepository
import com.skd.videoplayer.testutil.FakeWatchedVideosRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The three-dot menu: favorite, rename and delete, including Android's consent round-trip. */
@OptIn(ExperimentalCoroutinesApi::class)
class VideoListActionsTest {

    private val dispatcher = StandardTestDispatcher()
    private val videos = FakeVideoRepository()
    private val favorites = FakeFavoritesRepository()
    private val clip = testVideo(7, title = "clip")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        videos.library.value = Result.success(listOf(clip))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Harness(
        val viewModel: HomeViewModel,
        val events: MutableList<VideoListEvent>,
        val state: () -> VideoListUiState,
    )

    private fun TestScope.harness(edits: FakeVideoEditRepository): Harness {
        val viewModel = HomeViewModel(
            videos,
            favorites,
            FakeWatchedVideosRepository(),
            edits,
            FilterAndSortVideosUseCase(),
            ValidateVideoNameUseCase(),
            dispatcher,
            SavedStateHandle(),
        )
        val events = mutableListOf<VideoListEvent>()
        val eager = UnconfinedTestDispatcher(testScheduler)
        backgroundScope.launch(eager) { viewModel.uiState.collect {} }
        backgroundScope.launch(eager) { viewModel.eventFlow.collect { events += it } }
        return Harness(viewModel, events) {
            advanceUntilIdle()
            viewModel.uiState.value
        }
    }

    private fun Harness.messages() =
        events.filterIsInstance<VideoListEvent.ShowMessage>().map { it.message }

    @Test
    fun `favorite toggles on and off and shows in state`() = runTest(dispatcher) {
        val h = harness(FakeVideoEditRepository())

        h.viewModel.onMenuAction(clip, VideoMenuAction.TOGGLE_FAVORITE)
        assertEquals(setOf(7L), h.state().favoriteIds)

        h.viewModel.onMenuAction(clip, VideoMenuAction.TOGGLE_FAVORITE)
        assertEquals(emptySet<Long>(), h.state().favoriteIds)
        assertEquals(
            listOf(VideoListMessage.AddedToFavorites, VideoListMessage.RemovedFromFavorites),
            h.messages(),
        )
    }

    @Test
    fun `delete skips the app dialog when Android confirms itself`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository(deleteShowsSystemConfirmation = true)
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.DELETE)

        assertNull(h.state().dialog)
        assertEquals("delete", edits.calls.single().kind)
    }

    @Test
    fun `delete asks first on older Android versions`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository(deleteShowsSystemConfirmation = false)
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.DELETE)
        assertEquals(VideoDialog.ConfirmDelete(clip), h.state().dialog)
        assertTrue(edits.calls.isEmpty())

        h.viewModel.confirmDelete()
        assertNull(h.state().dialog)
        assertEquals(listOf(VideoListMessage.Deleted), h.messages())
    }

    @Test
    fun `consent is requested, then the edit is retried once granted`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository().apply { results += VideoEditResult.ConsentRequired(FakeConsent) }
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.DELETE)
        h.state()
        assertEquals(VideoListEvent.RequestConsent(FakeConsent), h.events.single())

        h.viewModel.onConsentResult(granted = true)
        h.state()

        assertEquals(listOf(false, true), edits.calls.map { it.consentGranted })
        assertEquals(listOf(VideoListMessage.Deleted), h.messages())
    }

    @Test
    fun `declined consent does nothing further`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository().apply { results += VideoEditResult.ConsentRequired(FakeConsent) }
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.DELETE)
        h.state()
        h.viewModel.onConsentResult(granted = false)
        h.state()

        assertEquals(1, edits.calls.size)
        assertTrue(h.messages().isEmpty())
    }

    @Test
    fun `rename validates input and keeps the extension`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository()
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.RENAME)
        assertEquals(VideoDialog.Rename(clip), h.state().dialog)

        h.viewModel.submitRename("   ")
        assertEquals(VideoDialog.Rename(clip, NameProblem.BLANK), h.state().dialog)
        assertTrue(edits.calls.isEmpty())

        h.viewModel.submitRename("Beach day")
        assertNull(h.state().dialog)
        assertEquals("Beach day.mp4", edits.calls.single().fileName)
        assertEquals(listOf(VideoListMessage.Renamed("Beach day.mp4")), h.messages())
    }

    @Test
    fun `failed edit is reported`() = runTest(dispatcher) {
        val edits = FakeVideoEditRepository().apply { results += VideoEditResult.Failure(VideoEditError.NAME_TAKEN) }
        val h = harness(edits)

        h.viewModel.onMenuAction(clip, VideoMenuAction.RENAME)
        h.viewModel.submitRename("other")
        h.state()

        assertEquals(listOf(VideoListMessage.EditFailed(VideoEditError.NAME_TAKEN)), h.messages())
    }
}
