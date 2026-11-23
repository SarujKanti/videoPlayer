package com.skd.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.videoplayer.domain.model.ThemeMode
import com.skd.videoplayer.domain.repository.SettingsRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Activity-level state: the app theme, re-scanning the library when the user grants or changes
 * media access, and telling the splash screen when the first screen has something to show.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Null until the saved choice is read, so the first frame never shows the wrong theme. */
    val themeMode: StateFlow<ThemeMode?> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isReady = MutableStateFlow(false)

    /** False while the splash screen should stay up. Only ever goes from false to true. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var readinessJob: Job? = null

    /**
     * Starts waiting for the first library scan so the home screen opens populated rather than
     * on a loading spinner. Without media access there is nothing to scan and the permission
     * prompt should appear straight away. The wait is capped at [SPLASH_MAX_DURATION_MS] so a
     * slow MediaStore never holds the app on the splash screen. Calling this again is a no-op.
     */
    fun awaitInitialLibrary(hasMediaAccess: Boolean) {
        if (_isReady.value || readinessJob != null) return
        if (!hasMediaAccess) {
            _isReady.value = true
            return
        }
        readinessJob = viewModelScope.launch {
            withTimeoutOrNull(SPLASH_MAX_DURATION_MS) { videoRepository.observeVideos().first() }
            _isReady.value = true
        }
    }

    fun onMediaAccessChanged() {
        viewModelScope.launch { videoRepository.refresh() }
    }

    companion object {
        /** Longest the splash screen waits for the library before showing the app anyway. */
        const val SPLASH_MAX_DURATION_MS = 1_500L
    }
}
