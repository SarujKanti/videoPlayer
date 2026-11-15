package com.skd.videoplayer.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skd.videoplayer.presentation.components.VideoBrowser

@Composable
fun HomeScreen(
    onPlay: (videoIds: LongArray, startId: Long) -> Unit,
    onOpenUri: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // System file picker: plays any video the user chooses without needing storage permission.
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onOpenUri)
    }
    VideoBrowser(
        title = "Library",
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onLayoutChange = viewModel::onLayoutChange,
        onRefresh = viewModel::refresh,
        modifier = modifier,
        // The queue is exactly what's on screen: same search, same order.
        onVideoClick = { video -> onPlay(state.videos.map { it.id }.toLongArray(), video.id) },
        actions = {
            IconButton(onClick = { pickVideo.launch(arrayOf("video/*")) }) {
                Icon(Icons.Outlined.FileOpen, contentDescription = "Open a video file")
            }
        },
    )
}
