package com.skd.videoplayer.presentation.folders

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.di.DefaultDispatcher
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.components.VideoListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Videos inside one folder. Arguments come from [com.skd.videoplayer.navigation.FolderVideosRoute]. */
@HiltViewModel
class FolderVideosViewModel @Inject constructor(
    private val repository: VideoRepository,
    favoritesRepository: FavoritesRepository,
    editRepository: VideoEditRepository,
    filterAndSort: FilterAndSortVideosUseCase,
    validateName: ValidateVideoNameUseCase,
    @DefaultDispatcher computeDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : VideoListViewModel(
    repository, favoritesRepository, editRepository, filterAndSort, validateName, computeDispatcher, savedStateHandle,
) {
    // Type-safe navigation stores each route property under its own name.
    private val folderId: String = checkNotNull(savedStateHandle["folderId"])
    val folderName: String = savedStateHandle["folderName"] ?: ""

    override fun source(): Flow<Result<List<Video>>> =
        repository.observeVideos().map { result ->
            result.map { videos -> videos.filter { it.folderId == folderId } }
        }
}
