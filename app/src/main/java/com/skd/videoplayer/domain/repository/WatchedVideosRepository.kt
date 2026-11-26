package com.skd.videoplayer.domain.repository

import kotlinx.coroutines.flow.Flow

/** Remembers which library videos have been played at least once. */
interface WatchedVideosRepository {
    fun observeWatchedIds(): Flow<Set<Long>>

    /** Records the video as played. Safe to call repeatedly; the first play time is kept. */
    suspend fun markWatched(videoId: Long)
}
