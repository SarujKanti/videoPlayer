package com.skd.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.domain.repository.SettingsRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.GetFavoriteVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity-level state: the app theme, and re-scanning the library when the user grants or
 * changes media access.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    settingsRepository: SettingsRepository,
    getFavoriteVideos: GetFavoriteVideosUseCase,
) : ViewModel() {

    /** Null until the saved choice is read, so the first frame never shows the wrong theme. */
    val themeMode: StateFlow<ThemeMode?> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Favorites that are actually on the device, for the tab badge. Only collected once media
     * access is granted, so it never scans the library early.
     */
    val favoriteCount: StateFlow<Int> =
        getFavoriteVideos()
            .map { it.getOrNull()?.size ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onMediaAccessChanged() {
        viewModelScope.launch { videoRepository.refresh() }
    }
}
