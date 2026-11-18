package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesRepositoryImplTest {

    /** In-memory stand-in for the Room DAO with the same semantics. */
    private class FakeFavoriteDao : FavoriteDao {
        val rows = MutableStateFlow<Map<Long, FavoriteEntity>>(emptyMap())
        override fun observeIds(): Flow<List<Long>> = rows.map { it.keys.toList() }
        override suspend fun isFavorite(videoId: Long) = videoId in rows.value
        override suspend fun insert(entity: FavoriteEntity) {
            if (entity.videoId !in rows.value) rows.value = rows.value + (entity.videoId to entity)
        }
        override suspend fun delete(videoId: Long) {
            rows.value = rows.value - videoId
        }
    }

    private val dao = FakeFavoriteDao()
    private val repository = FavoritesRepositoryImpl(dao)

    @Test
    fun `toggle adds then removes`() = runTest {
        assertTrue(repository.toggle(5))
        assertEquals(setOf(5L), repository.observeFavoriteIds().first())

        assertFalse(repository.toggle(5))
        assertEquals(emptySet<Long>(), repository.observeFavoriteIds().first())
    }

    @Test
    fun `remove is safe for videos that aren't favorites`() = runTest {
        repository.toggle(1)
        repository.remove(2)
        repository.remove(1)
        assertEquals(emptySet<Long>(), repository.observeFavoriteIds().first())
    }
}
