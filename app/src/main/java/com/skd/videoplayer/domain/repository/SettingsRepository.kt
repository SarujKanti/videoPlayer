package com.skd.videoplayer.domain.repository

import com.skd.videoplayer.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** User preferences that persist across app restarts. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
