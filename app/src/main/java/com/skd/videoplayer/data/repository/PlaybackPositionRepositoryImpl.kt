package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.PlaybackPositionDao
import com.skd.videoplayer.data.entity.PlaybackPositionEntity
import com.skd.videoplayer.domain.model.PlaybackPosition
import com.skd.videoplayer.domain.model.ResumePolicy
import com.skd.videoplayer.domain.repository.PlaybackPositionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPositionRepositoryImpl @Inject constructor(
    private val dao: PlaybackPositionDao,
) : PlaybackPositionRepository {

    override suspend fun get(mediaKey: String): PlaybackPosition? =
        dao.get(mediaKey)
            ?.takeIf { ResumePolicy.isResumable(it.positionMs, it.durationMs) }
            ?.let { PlaybackPosition(it.positionMs, it.durationMs) }

    override suspend fun save(mediaKey: String, positionMs: Long, durationMs: Long) {
        if (ResumePolicy.isResumable(positionMs, durationMs)) {
            dao.upsert(PlaybackPositionEntity(mediaKey, positionMs, durationMs, System.currentTimeMillis()))
        } else {
            dao.delete(mediaKey)
        }
    }

    override suspend fun clear(mediaKey: String) = dao.delete(mediaKey)
}
