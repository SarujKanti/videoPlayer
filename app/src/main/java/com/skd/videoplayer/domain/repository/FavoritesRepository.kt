package com.skd.videoplayer.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavoriteIds(): Flow<Set<Long>>

    /** Adds the video if it isn't a favorite yet, otherwise removes it. Returns the new state. */
    suspend fun toggle(videoId: Long): Boolean

    suspend fun remove(videoId: Long)
}
