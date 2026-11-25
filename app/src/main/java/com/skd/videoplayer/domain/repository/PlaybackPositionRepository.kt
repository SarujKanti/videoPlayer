package com.skd.videoplayer.domain.repository

import com.skd.videoplayer.domain.model.PlaybackPosition

/** Remembers where each video was left so it can be resumed later. */
interface PlaybackPositionRepository {

    /** The saved spot for [mediaKey], or null if the video was never left part-way through. */
    suspend fun get(mediaKey: String): PlaybackPosition?

    /**
     * Records that playback stopped at [positionMs]. Positions too close to the start or the end
     * to be worth resuming clear the saved spot instead (see [ResumePolicy]).
     */
    suspend fun save(mediaKey: String, positionMs: Long, durationMs: Long)

    suspend fun clear(mediaKey: String)
}
