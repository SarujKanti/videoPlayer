package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.WatchedVideoDao
import com.skd.videoplayer.data.entity.WatchedVideoEntity
import com.skd.videoplayer.domain.repository.WatchedVideosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchedVideosRepositoryImpl @Inject constructor(
    private val dao: WatchedVideoDao,
) : WatchedVideosRepository {

    override fun observeWatchedIds(): Flow<Set<Long>> =
        dao.observeIds().map { it.toSet() }.distinctUntilChanged()

    override suspend fun markWatched(videoId: Long) =
        dao.insert(WatchedVideoEntity(videoId, System.currentTimeMillis()))
}
