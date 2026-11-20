package com.skd.videoplayer.presentation.favorites

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.di.DefaultDispatcher
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.domain.usecase.GetFavoriteVideosUseCase
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import com.skd.videoplayer.presentation.components.VideoListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The user's favorite videos, with the same search, sort, layout and three-dot menu as the
 * library. Removing a favorite from the menu drops it from this list straight away.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: VideoRepository,
    favoritesRepository: FavoritesRepository,
    editRepository: VideoEditRepository,
    filterAndSort: FilterAndSortVideosUseCase,
    validateName: ValidateVideoNameUseCase,
    private val getFavoriteVideos: GetFavoriteVideosUseCase,
    @DefaultDispatcher computeDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : VideoListViewModel(
    repository, favoritesRepository, editRepository, filterAndSort, validateName, computeDispatcher, savedStateHandle,
) {
    override fun source(): Flow<Result<List<Video>>> = getFavoriteVideos()
}
