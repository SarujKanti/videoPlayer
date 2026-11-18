package com.skd.videoplayer.presentation.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.UserConsentRequest
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoEditError
import com.skd.videoplayer.domain.model.VideoEditResult
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.NameValidation
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Short confirmations and failures shown in a snackbar. */
sealed interface VideoListMessage {
    data object AddedToFavorites : VideoListMessage
    data object RemovedFromFavorites : VideoListMessage
    data object Deleted : VideoListMessage
    data class Renamed(val fileName: String) : VideoListMessage
    data class EditFailed(val error: VideoEditError) : VideoListMessage
}

/** One-off things the screen must do, as opposed to state it renders. */
sealed interface VideoListEvent {
    data class ShowMessage(val message: VideoListMessage) : VideoListEvent

    /** Launch the system confirmation, then report back through [VideoListViewModel.onConsentResult]. */
    data class RequestConsent(val request: UserConsentRequest) : VideoListEvent
}

/**
 * Search, sort, layout, refresh and the three-dot menu actions for any screen that lists
 * videos. Subclasses only decide which videos to show via [source].
 */
abstract class VideoListViewModel(
    private val repository: VideoRepository,
    private val favoritesRepository: FavoritesRepository,
    private val editRepository: VideoEditRepository,
    private val filterAndSort: FilterAndSortVideosUseCase,
    private val validateName: ValidateVideoNameUseCase,
    private val computeDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The unfiltered videos this screen shows. */
    protected abstract fun source(): Flow<Result<List<Video>>>

    private sealed interface PendingEdit {
        val video: Video
        data class Delete(override val video: Video) : PendingEdit
        data class Rename(override val video: Video, val fileName: String) : PendingEdit
    }

    private val options = MutableStateFlow(VideoListOptions(query = savedStateHandle[KEY_QUERY] ?: ""))
    private val events = Channel<VideoListEvent>(Channel.BUFFERED)

    /** The edit waiting for the user to answer a system confirmation. */
    private var pendingEdit: PendingEdit? = null

    val eventFlow: Flow<VideoListEvent> = events.receiveAsFlow()

    val uiState: StateFlow<VideoListUiState> by lazy {
        combine(source(), favoritesRepository.observeFavoriteIds(), options) { result, favorites, opts ->
            buildVideoListState(result, favorites, opts, filterAndSort)
        }
            // Filtering and sorting thousands of items stays off the main thread.
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideoListUiState())
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
        options.update { it.copy(query = value) }
    }

    fun onSortChange(value: VideoSort) = options.update { it.copy(sort = value) }

    fun onLayoutChange(value: LibraryLayout) = options.update { it.copy(layout = value) }

    fun refresh() {
        if (options.value.isRefreshing) return
        viewModelScope.launch {
            options.update { it.copy(isRefreshing = true) }
            try {
                repository.refresh()
            } finally {
                options.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onMenuAction(video: Video, action: VideoMenuAction) {
        when (action) {
            VideoMenuAction.TOGGLE_FAVORITE -> viewModelScope.launch {
                val nowFavorite = favoritesRepository.toggle(video.id)
                send(if (nowFavorite) VideoListMessage.AddedToFavorites else VideoListMessage.RemovedFromFavorites)
            }
            VideoMenuAction.RENAME -> showDialog(VideoDialog.Rename(video))
            VideoMenuAction.DELETE ->
                // Android 11+ shows its own confirmation; asking twice would be noise.
                if (editRepository.deleteShowsSystemConfirmation) {
                    runEdit(PendingEdit.Delete(video))
                } else {
                    showDialog(VideoDialog.ConfirmDelete(video))
                }
        }
    }

    fun confirmDelete() {
        val dialog = options.value.dialog as? VideoDialog.ConfirmDelete ?: return
        dismissDialog()
        runEdit(PendingEdit.Delete(dialog.video))
    }

    fun submitRename(input: String) {
        val dialog = options.value.dialog as? VideoDialog.Rename ?: return
        when (val validation = validateName(dialog.video, input)) {
            is NameValidation.Invalid -> showDialog(dialog.copy(problem = validation.problem))
            is NameValidation.Valid -> {
                dismissDialog()
                runEdit(PendingEdit.Rename(dialog.video, validation.fileName))
            }
        }
    }

    fun dismissDialog() = options.update { it.copy(dialog = null) }

    /** Called with the user's answer to a system confirmation requested via [VideoListEvent.RequestConsent]. */
    fun onConsentResult(granted: Boolean) {
        val edit = pendingEdit ?: return
        pendingEdit = null
        if (granted) runEdit(edit, consentGranted = true)
    }

    private fun runEdit(edit: PendingEdit, consentGranted: Boolean = false) {
        viewModelScope.launch {
            val result = when (edit) {
                is PendingEdit.Delete -> editRepository.delete(edit.video, consentGranted)
                is PendingEdit.Rename -> editRepository.rename(edit.video, edit.fileName, consentGranted)
            }
            when (result) {
                VideoEditResult.Success -> send(
                    when (edit) {
                        is PendingEdit.Delete -> VideoListMessage.Deleted
                        is PendingEdit.Rename -> VideoListMessage.Renamed(edit.fileName)
                    },
                )
                is VideoEditResult.ConsentRequired -> {
                    pendingEdit = edit
                    events.send(VideoListEvent.RequestConsent(result.request))
                }
                is VideoEditResult.Failure -> send(VideoListMessage.EditFailed(result.error))
            }
        }
    }

    private fun showDialog(dialog: VideoDialog) = options.update { it.copy(dialog = dialog) }

    private suspend fun send(message: VideoListMessage) = events.send(VideoListEvent.ShowMessage(message))

    private companion object {
        const val KEY_QUERY = "query"
    }
}
