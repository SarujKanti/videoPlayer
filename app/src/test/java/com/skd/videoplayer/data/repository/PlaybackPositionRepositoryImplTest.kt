package com.skd.videoplayer.data.repository

import com.skd.videoplayer.data.dao.PlaybackPositionDao
import com.skd.videoplayer.data.entity.PlaybackPositionEntity
import com.skd.videoplayer.domain.model.PlaybackPosition
import com.skd.videoplayer.domain.model.ResumePolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPositionRepositoryImplTest {

    private class FakePlaybackPositionDao : PlaybackPositionDao {
        val rows = mutableMapOf<String, PlaybackPositionEntity>()
        override suspend fun get(mediaKey: String) = rows[mediaKey]
        override suspend fun upsert(entity: PlaybackPositionEntity) {
            rows[entity.mediaKey] = entity
        }
        override suspend fun delete(mediaKey: String) {
            rows.remove(mediaKey)
        }
    }

    private val dao = FakePlaybackPositionDao()
    private val repository = PlaybackPositionRepositoryImpl(dao)

    @Test
    fun `a position part-way through is saved and offered again`() = runTest {
        repository.save("42", positionMs = 65_000, durationMs = 600_000)

        assertEquals(PlaybackPosition(65_000, 600_000), repository.get("42"))
    }

    @Test
    fun `a newer position replaces the older one`() = runTest {
        repository.save("42", positionMs = 65_000, durationMs = 600_000)
        repository.save("42", positionMs = 120_000, durationMs = 600_000)

        assertEquals(120_000L, repository.get("42")?.positionMs)
    }

    @Test
    fun `stopping near the start or the end forgets the saved spot`() = runTest {
        repository.save("42", positionMs = 65_000, durationMs = 600_000)
        repository.save("42", positionMs = 2_000, durationMs = 600_000)
        assertNull(repository.get("42"))

        repository.save("42", positionMs = 65_000, durationMs = 600_000)
        repository.save("42", positionMs = 595_000, durationMs = 600_000)
        assertNull(repository.get("42"))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `clear removes the saved spot`() = runTest {
        repository.save("content://media/7", positionMs = 30_000, durationMs = 90_000)
        repository.clear("content://media/7")

        assertNull(repository.get("content://media/7"))
    }

    @Test
    fun `unknown videos have no saved spot`() = runTest {
        assertNull(repository.get("missing"))
    }

    @Test
    fun `resume policy skips the first seconds and the last stretch`() {
        assertFalse(ResumePolicy.isResumable(4_999, 600_000))
        assertTrue(ResumePolicy.isResumable(5_000, 600_000))
        assertTrue(ResumePolicy.isResumable(589_999, 600_000))
        assertFalse(ResumePolicy.isResumable(590_000, 600_000))
        // Unknown length: anything past the start counts.
        assertTrue(ResumePolicy.isResumable(30_000, 0))
        // Clips shorter than the margins are never worth resuming.
        assertFalse(ResumePolicy.isResumable(6_000, 12_000))
    }
}
