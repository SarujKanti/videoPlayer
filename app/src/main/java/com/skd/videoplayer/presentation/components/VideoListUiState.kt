package com.skd.videoplayer.presentation.components

import androidx.compose.runtime.Immutable
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.NameProblem

enum class LibraryError { PERMISSION_REVOKED, UNKNOWN }

/** Actions in each video's three-dot menu. */
enum class VideoMenuAction { TOGGLE_FAVORITE, RENAME, DELETE }

/** A dialog the list screen is showing on top of the videos. */
sealed interface VideoDialog {
    data class ConfirmDelete(val video: Video) : VideoDialog
    data class Rename(val video: Video, val problem: NameProblem? = null) : VideoDialog
}

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
    val favoriteIds: Set<Long> = emptySet(),
    val dialog: VideoDialog? = null,
    val error: LibraryError? = null,
)

/** The user's current view choices, kept apart from the library data. */
data class VideoListOptions(
    val query: String = "",
    val sort: VideoSort = VideoSort.Default,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val isRefreshing: Boolean = false,
    val dialog: VideoDialog? = null,
)

fun Throwable.toLibraryError(): LibraryError =
    if (this is SecurityException) LibraryError.PERMISSION_REVOKED else LibraryError.UNKNOWN

/** Builds [VideoListUiState] from a library result, the favorites and the view options. */
fun buildVideoListState(
    result: Result<List<Video>>,
    favoriteIds: Set<Long>,
    options: VideoListOptions,
    filterAndSort: FilterAndSortVideosUseCase,
): VideoListUiState {
    val all = result.getOrNull()
    return VideoListUiState(
        isLoading = false,
        isRefreshing = options.isRefreshing,
        videos = all?.let { filterAndSort(it, options.query, options.sort) } ?: emptyList(),
        totalCount = all?.size ?: 0,
        query = options.query,
        sort = options.sort,
        layout = options.layout,
        favoriteIds = favoriteIds,
        dialog = options.dialog,
        error = result.exceptionOrNull()?.toLibraryError(),
    )
}
