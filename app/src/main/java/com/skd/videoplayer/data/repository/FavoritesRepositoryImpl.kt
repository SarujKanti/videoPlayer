package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.entity.FavoriteEntity
import com.skd.videoplayer.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
) : FavoritesRepository {

    /** Room emits on every write to the table; only changes in the id set matter to the UI. */
    override fun observeFavoriteIds(): Flow<Set<Long>> =
        dao.observeIds().map { it.toSet() }.distinctUntilChanged()

    override suspend fun toggle(videoId: Long): Boolean =
        if (dao.isFavorite(videoId)) {
            dao.delete(videoId)
            false
        } else {
            dao.insert(FavoriteEntity(videoId, System.currentTimeMillis()))
            true
        }

    override suspend fun remove(videoId: Long) = dao.delete(videoId)
}
