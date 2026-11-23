package com.skd.videoplayer.domain.model

/** The app-wide colour theme the user picked in Settings. */
enum class ThemeMode {
    /** Follow the phone's light/dark setting. */
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        val Default = SYSTEM
    }
}
