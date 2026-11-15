package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort

/**
 * Header, search field and a pull-to-refresh grid or list of videos, with loading, empty,
 * no-results and error states. Shared by the library and folder screens.
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
    onVideoClick: ((Video) -> Unit)? = null,
    /** Extra header buttons, shown before the layout and sort buttons. */
    actions: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (navigationIcon == null) 20.dp else 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!state.isLoading && state.error == null) {
                    Text(
                        text = videoCountLabel(state.totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            actions()
            LayoutToggleButton(state.layout, onLayoutChange)
            SortMenuButton(state.sort, onSortChange)
        }

        VideoSearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null && state.totalCount == 0 -> ScrollableStateContainer {
                    ErrorState(message = state.error.message, onRetry = onRefresh)
                }
                state.totalCount == 0 -> ScrollableStateContainer {
                    MessageState(
                        icon = Icons.Outlined.VideoLibrary,
                        title = emptyTitle,
                        message = emptyMessage,
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
                else -> VideoCollection(state, onVideoClick)
            }
        }
    }
}

@Composable
private fun VideoCollection(state: VideoListUiState, onVideoClick: ((Video) -> Unit)?) {
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    // A new sort or search should start from the top rather than keep an arbitrary offset.
    LaunchedEffect(state.sort, state.query) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }
    when (state.layout) {
        LibraryLayout.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.videos, key = { it.id }, contentType = { "video" }) { video ->
                VideoGridItem(
                    video = video,
                    onClick = onVideoClick?.let { { it(video) } },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        LibraryLayout.LIST -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.videos, key = { it.id }, contentType = { "video" }) { video ->
                VideoListItem(
                    video = video,
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
