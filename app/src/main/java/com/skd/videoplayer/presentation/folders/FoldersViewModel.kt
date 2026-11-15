package com.skd.videoplayer.presentation.folders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.di.DefaultDispatcher
import com.skd.videoplayer.domain.model.VideoFolder
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.GroupVideosIntoFoldersUseCase
import com.skd.videoplayer.presentation.components.LibraryError
import com.skd.videoplayer.presentation.components.toLibraryError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FoldersUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val folders: List<VideoFolder> = emptyList(),
    val error: LibraryError? = null,
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val groupIntoFolders: GroupVideosIntoFoldersUseCase,
    @DefaultDispatcher computeDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)

    private val folders = repository.observeVideos()
        .map { result -> result.map(groupIntoFolders::invoke) }
        .flowOn(computeDispatcher)

    val uiState: StateFlow<FoldersUiState> =
        combine(folders, isRefreshing) { result, refreshing ->
            FoldersUiState(
                isLoading = false,
                isRefreshing = refreshing,
                folders = result.getOrNull() ?: emptyList(),
                error = result.exceptionOrNull()?.toLibraryError(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoldersUiState())

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
}
