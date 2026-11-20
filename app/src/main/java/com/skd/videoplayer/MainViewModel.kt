package com.skd.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.domain.repository.SettingsRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    /** Null until the saved choice is read, so the first frame never shows the wrong theme. */
    val themeMode: StateFlow<ThemeMode?> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onMediaAccessChanged() {
        viewModelScope.launch { videoRepository.refresh() }
    }
}
