package com.skd.videoplayer.presentation.folders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun FolderVideosScreen(
    onBack: () -> Unit,
    onPlay: (videoIds: LongArray, startId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderVideosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    VideoBrowser(
        title = viewModel.folderName,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onLayoutChange = viewModel::onLayoutChange,
        onRefresh = viewModel::refresh,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
        },
        emptyTitle = "This folder is empty",
        emptyMessage = "Its videos were moved or deleted.",
        onVideoClick = { video -> onPlay(state.videos.map { it.id }.toLongArray(), video.id) },
        onMenuAction = viewModel::onMenuAction,
        snackbarHostState = snackbarHostState,
    )
    VideoActionsHost(viewModel = viewModel, dialog = state.dialog, snackbarHostState = snackbarHostState)
}
