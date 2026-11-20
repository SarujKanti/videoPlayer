package com.skd.videoplayer.presentation.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skd.videoplayer.presentation.components.VideoActionsHost
import com.skd.videoplayer.presentation.components.VideoBrowser

@Composable
fun FavoritesScreen(
    onPlay: (videoIds: LongArray, startId: Long) -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    VideoBrowser(
        title = "Favorites",
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onLayoutChange = viewModel::onLayoutChange,
        onRefresh = viewModel::refresh,
        modifier = modifier,
        emptyTitle = "No favorites yet",
        emptyMessage = "Tap ⋮ on any video and choose Add to favorites to keep it here.",
        emptyIcon = Icons.Outlined.FavoriteBorder,
        emptyActionLabel = "Browse library",
        onEmptyAction = onBrowseLibrary,
        // Plays through the favorites in the order shown.
        onVideoClick = { video -> onPlay(state.videos.map { it.id }.toLongArray(), video.id) },
        onMenuAction = viewModel::onMenuAction,
        snackbarHostState = snackbarHostState,
    )
    VideoActionsHost(viewModel = viewModel, dialog = state.dialog, snackbarHostState = snackbarHostState)
}
