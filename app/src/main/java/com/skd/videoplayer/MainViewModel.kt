package com.skd.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-level state. Re-scans the library when the user grants or changes media access. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
) : ViewModel() {
    fun onMediaAccessChanged() {
        viewModelScope.launch { videoRepository.refresh() }
    }
}
