package com.skd.videoplayer.presentation.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Search, sort, layout and refresh handling for any screen that lists videos. Subclasses only
 * decide which videos to show via [source].
 */
abstract class VideoListViewModel(
    private val repository: VideoRepository,
    private val filterAndSort: FilterAndSortVideosUseCase,
    private val computeDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The unfiltered videos this screen shows. */
    protected abstract fun source(): Flow<Result<List<Video>>>

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val sort = MutableStateFlow(VideoSort.Default)
    private val layout = MutableStateFlow(LibraryLayout.GRID)
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<VideoListUiState> by lazy {
        combine(source(), query, sort, layout, isRefreshing) { result, q, s, l, refreshing ->
            buildVideoListState(result, q, s, l, refreshing, filterAndSort)
        }
            // Filtering and sorting thousands of items stays off the main thread.
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideoListUiState())
    }

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onSortChange(value: VideoSort) {
        sort.value = value
    }

    fun onLayoutChange(value: LibraryLayout) {
        layout.value = value
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                repository.refresh()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}
