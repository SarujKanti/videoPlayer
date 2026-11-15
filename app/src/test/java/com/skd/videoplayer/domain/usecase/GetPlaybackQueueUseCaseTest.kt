package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.testutil.testVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPlaybackQueueUseCaseTest {

    private fun useCaseWith(result: Result<List<Video>>) = GetPlaybackQueueUseCase(
        object : VideoRepository {
            override fun observeVideos(): Flow<Result<List<Video>>> = flowOf(result)
            override suspend fun refresh() = Unit
        },
    )

    @Test
    fun `keeps the on-screen order and starts at the tapped video`() = runTest {
        val useCase = useCaseWith(Result.success(listOf(testVideo(1), testVideo(2), testVideo(3))))

        val queue = useCase(ids = listOf(3, 1, 2), startId = 1)

        assertEquals(listOf(3L, 1L, 2L), queue.videos.map { it.id })
        assertEquals(1, queue.startIndex)
    }

    @Test
    fun `videos deleted since the list was shown are skipped`() = runTest {
        val useCase = useCaseWith(Result.success(listOf(testVideo(1), testVideo(3))))

        val queue = useCase(ids = listOf(1, 2, 3), startId = 3)

        assertEquals(listOf(1L, 3L), queue.videos.map { it.id })
        assertEquals(1, queue.startIndex)
    }

    @Test
    fun `missing start video falls back to the first item`() = runTest {
        val queue = useCaseWith(Result.success(listOf(testVideo(1)))).invoke(listOf(1, 9), startId = 9)
        assertEquals(0, queue.startIndex)
    }

    @Test
    fun `unreadable library gives an empty queue`() = runTest {
        val queue = useCaseWith(Result.failure(SecurityException())).invoke(listOf(1), startId = 1)
        assertTrue(queue.videos.isEmpty())
    }
}
