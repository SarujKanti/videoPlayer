package com.skd.videoplayer.presentation.player

import androidx.compose.runtime.Immutable

enum class PlayerRepeatMode { OFF, ALL, ONE }

/** How the picture fits the screen. Mapped to Media3 resize modes in the UI. */
enum class VideoScaleMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch"),
    ;

    fun next(): VideoScaleMode = entries[(ordinal + 1) % entries.size]
}

val PlaybackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** `1x`, `0.25x`, `1.5x`. */
fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

/** What the player screen renders. Gesture and control visibility stay in the UI layer. */
@Immutable
data class PlayerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    val speed: Float = 1f,
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    val shuffle: Boolean = false,
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val isLocked: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val subtitlesOff: Boolean = false,
    val videoUnsupported: Boolean = false,
    val audioUnsupported: Boolean = false,
    val error: PlaybackErrorKind? = null,
) {
    val hasVideoSize: Boolean get() = videoWidth > 0 && videoHeight > 0
    val isLandscapeVideo: Boolean get() = !hasVideoSize || videoWidth >= videoHeight
}
