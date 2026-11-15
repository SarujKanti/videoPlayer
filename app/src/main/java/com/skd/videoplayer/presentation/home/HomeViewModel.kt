package com.skd.videoplayer.presentation.home

import androidx.lifecycle.SavedStateHandle
import com.skd.videoplayer.di.DefaultDispatcher
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.FilterAndSortVideosUseCase
import com.skd.videoplayer.presentation.components.VideoListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The full device library. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository,
    filterAndSort: FilterAndSortVideosUseCase,
    @DefaultDispatcher computeDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : VideoListViewModel(repository, filterAndSort, computeDispatcher, savedStateHandle) {

    override fun source(): Flow<Result<List<Video>>> = repository.observeVideos()
}
