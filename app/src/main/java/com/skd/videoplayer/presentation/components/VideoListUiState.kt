package com.skd.videoplayer.presentation.components

import androidx.compose.runtime.Immutable
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase

enum class LibraryError { PERMISSION_REVOKED, UNKNOWN }

/** UI state shared by every screen that shows a searchable, sortable list of videos. */
@Immutable
data class VideoListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Videos after search and sort are applied. */
    val videos: List<Video> = emptyList(),
    /** Number of videos before the search filter, used for empty-state wording. */
    val totalCount: Int = 0,
    val query: String = "",
    val sort: VideoSort = VideoSort.Default,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val error: LibraryError? = null,
)

fun Throwable.toLibraryError(): LibraryError =
    if (this is SecurityException) LibraryError.PERMISSION_REVOKED else LibraryError.UNKNOWN

/** Builds [VideoListUiState] from a library result and the user's current view options. */
fun buildVideoListState(
    result: Result<List<Video>>,
    query: String,
    sort: VideoSort,
    layout: LibraryLayout,
    isRefreshing: Boolean,
    filterAndSort: FilterAndSortVideosUseCase,
): VideoListUiState {
    val all = result.getOrNull()
    return VideoListUiState(
        isLoading = false,
        isRefreshing = isRefreshing,
        videos = all?.let { filterAndSort(it, query, sort) } ?: emptyList(),
        totalCount = all?.size ?: 0,
        query = query,
        sort = sort,
        layout = layout,
        error = result.exceptionOrNull()?.toLibraryError(),
    )
}
