package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort

/**
 * Header, search, sort/layout toolbar and a pull-to-refresh grid or list of videos, with
 * loading, empty, no-results and error states. Shared by the library, folder and favorites screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoBrowser(
    title: String,
    state: VideoListUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (VideoSort) -> Unit,
    onLayoutChange: (LibraryLayout) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    emptyTitle: String = "No videos yet",
    emptyMessage: String = "Videos you record or download will appear here automatically.",
    emptyIcon: ImageVector = Icons.Outlined.VideoLibrary,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    onVideoClick: ((Video) -> Unit)? = null,
    /** Extra header buttons, shown at the end of the title row. */
    actions: @Composable RowScope.() -> Unit = {},
    /** Shows the three-dot menu on each video when set. */
    onMenuAction: ((Video, VideoMenuAction) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
) {
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val scrolled by remember(state.layout) {
        derivedStateOf { if (state.layout == LibraryLayout.GRID) gridState.isScrolled() else listState.isScrolled() }
    }
    val showToolbar = !state.isLoading && state.totalCount > 0

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = title,
                scrolled = scrolled,
                navigationIcon = navigationIcon,
                actions = actions,
                below = {
                    VideoSearchField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    )
                    if (showToolbar) {
                        ListToolbar(
                            countLabel = countLabel(state),
                            sort = state.sort,
                            onSortChange = onSortChange,
                            layout = state.layout,
                            onLayoutChange = onLayoutChange,
                        )
                    }
                },
            )

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isLoading -> VideoSkeleton(state.layout)
                    state.error != null && state.totalCount == 0 -> ScrollableStateContainer {
                        ErrorState(message = state.error.message, onRetry = onRefresh)
                    }
                    state.totalCount == 0 -> ScrollableStateContainer {
                        MessageState(
                            icon = emptyIcon,
                            title = emptyTitle,
                            message = emptyMessage,
                            actionLabel = emptyActionLabel,
                            onAction = onEmptyAction,
                        )
                    }
                    state.videos.isEmpty() -> ScrollableStateContainer {
                        MessageState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matches",
                            message = "Nothing matches “${state.query.trim()}”. Try a different name.",
                            actionLabel = "Clear search",
                            onAction = { onQueryChange("") },
                            iconTint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    else -> VideoCollection(state, gridState, listState, onVideoClick, onMenuAction)
                }
            }
        }
        snackbarHostState?.let {
            SnackbarHost(
                hostState = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

private fun LazyGridState.isScrolled() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
private fun LazyListState.isScrolled() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

/** `128 videos`, or `3 of 128 videos` while searching. */
private fun countLabel(state: VideoListUiState): String =
    if (state.query.isBlank()) {
        videoCountLabel(state.totalCount)
    } else {
        "${state.videos.size} of ${videoCountLabel(state.totalCount)}"
    }

@Composable
private fun VideoCollection(
    state: VideoListUiState,
    gridState: LazyGridState,
    listState: LazyListState,
    onVideoClick: ((Video) -> Unit)?,
    onMenuAction: ((Video, VideoMenuAction) -> Unit)?,
) {
    // One clock reading per list keeps "Yesterday"/"NEW" consistent across every item.
    val nowMs = remember(state.videos) { System.currentTimeMillis() }
    // A new sort or search should start from the top rather than keep an arbitrary offset.
    LaunchedEffect(state.sort, state.query) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }
    when (state.layout) {
        LibraryLayout.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.videos, key = { it.id }, contentType = { "video" }) { video ->
                VideoGridItem(
                    video = video,
                    isFavorite = video.id in state.favoriteIds,
                    nowMs = nowMs,
                    onMenuAction = onMenuAction?.let { handler -> { action -> handler(video, action) } },
                    onClick = onVideoClick?.let { { it(video) } },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        LibraryLayout.LIST -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.videos, key = { it.id }, contentType = { "video" }) { video ->
                VideoListItem(
                    video = video,
                    isFavorite = video.id in state.favoriteIds,
                    nowMs = nowMs,
                    onMenuAction = onMenuAction?.let { handler -> { action -> handler(video, action) } },
                    onClick = onVideoClick?.let { { it(video) } },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** Makes a non-list state scrollable so pull-to-refresh still works on it. */
@Composable
fun ScrollableStateContainer(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                verticalArrangement = Arrangement.Center,
            ) { content() }
        }
    }
}

val LibraryError.message: String
    get() = when (this) {
        LibraryError.PERMISSION_REVOKED -> "Access to your videos was removed. Allow access again to see your library."
        LibraryError.UNKNOWN -> "Your videos couldn't be loaded. Pull down or tap below to try again."
    }

fun videoCountLabel(count: Int): String = if (count == 1) "1 video" else "$count videos"
