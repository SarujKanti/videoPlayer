package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.WatchedVideoDao
import com.skd.videoplayer.data.entity.WatchedVideoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchedVideosRepositoryImplTest {

    /** In-memory stand-in for the Room DAO; IGNORE keeps the existing row on conflict. */
    private class FakeWatchedVideoDao : WatchedVideoDao {
        val rows = MutableStateFlow<Map<Long, WatchedVideoEntity>>(emptyMap())
        override fun observeIds(): Flow<List<Long>> = rows.map { it.keys.toList() }
        override suspend fun insert(entity: WatchedVideoEntity) {
            if (entity.videoId !in rows.value) rows.value = rows.value + (entity.videoId to entity)
        }
    }

    private val dao = FakeWatchedVideoDao()
    private val repository = WatchedVideosRepositoryImpl(dao)

    @Test
    fun `nothing is watched at first`() = runTest {
        assertEquals(emptySet<Long>(), repository.observeWatchedIds().first())
    }

    @Test
    fun `markWatched adds the video to the watched set`() = runTest {
        repository.markWatched(7)
        repository.markWatched(9)
        assertEquals(setOf(7L, 9L), repository.observeWatchedIds().first())
    }

    @Test
    fun `marking twice keeps one row and the first play time`() = runTest {
        repository.markWatched(7)
        val firstPlayedAt = dao.rows.value.getValue(7).firstPlayedAtEpochMs
        repository.markWatched(7)
        assertEquals(1, dao.rows.value.size)
        assertEquals(firstPlayedAt, dao.rows.value.getValue(7).firstPlayedAtEpochMs)
    }
}
