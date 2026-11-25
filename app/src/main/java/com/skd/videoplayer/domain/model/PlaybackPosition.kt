package com.skd.videoplayer.domain.model

/** A saved spot in a video the user left part-way through. */
data class PlaybackPosition(
    val positionMs: Long,
    val durationMs: Long,
)
