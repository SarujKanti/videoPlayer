package com.skd.videoplayer.data.repository

import com.skd.videoplayer.core.constants.MediaConstants
import com.skd.videoplayer.data.local.MediaStoreVideoRow
import com.skd.videoplayer.testutil.testRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryImplTest {

    private class FakeRowSource : VideoRowSource {
        override val collectionUri = "content://media/external/video/media"
        var rows: List<MediaStoreVideoRow> = emptyList()
        var failure: Exception? = null
        var queryCount = 0
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

        override suspend fun queryVideos(): List<MediaStoreVideoRow> {
            queryCount++
            failure?.let { throw it }
            return rows
        }

        override fun observeChanges(): Flow<Unit> = changes
    }

    private val source = FakeRowSource()

    private fun TestScope.createRepository(): VideoRepositoryImpl {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return VideoRepositoryImpl(source, dispatcher, backgroundScope)
    }

    @Test
    fun `first collection scans MediaStore and maps rows`() = runTest {
        source.rows = listOf(testRow(1), testRow(2))
        val repository = createRepository()

        val result = repository.observeVideos().first()

        assertEquals(listOf(1L, 2L), result.getOrThrow().map { it.id })
        assertEquals(1, source.queryCount)
    }

    @Test
    fun `later collectors reuse the cached library`() = runTest {
        source.rows = listOf(testRow(1))
        val repository = createRepository()

        repository.observeVideos().first()
        repository.observeVideos().first()

        assertEquals(1, source.queryCount)
    }

    @Test
    fun `failed first scan is reported without throwing`() = runTest {
        source.failure = SecurityException("revoked")
        val repository = createRepository()

        val result = repository.observeVideos().first()

        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `failed rescan keeps the last good library`() = runTest {
        source.rows = listOf(testRow(1))
        val repository = createRepository()
        repository.observeVideos().first()

        source.failure = IllegalStateException("disk unmounted")
        repository.refresh()

        assertEquals(listOf(1L), repository.observeVideos().first().getOrThrow().map { it.id })
    }

    @Test
    fun `media change notifications trigger a debounced rescan`() = runTest {
        source.rows = listOf(testRow(1))
        val repository = createRepository()
        val emissions = mutableListOf<Int>()
        repository.observeVideos()
            .onEach { emissions += it.getOrThrow().size }
            .launchIn(backgroundScope)
        runCurrent()

        source.rows = listOf(testRow(1), testRow(2))
        repeat(3) { source.changes.emit(Unit) } // a burst collapses into one scan
        advanceTimeBy(MediaConstants.MEDIA_CHANGE_DEBOUNCE_MS + 1)
        runCurrent()

        assertEquals(2, source.queryCount)
        assertEquals(listOf(1, 2), emissions)
    }
}
