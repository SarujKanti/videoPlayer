package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** A resolved playlist and the position to start from. */
data class PlaybackQueue(val videos: List<Video>, val startIndex: Int)

/**
 * Turns the ids the user saw on screen into playable videos, in the same order. Ids that no
 * longer exist (deleted since the list was shown) are dropped.
 */
class GetPlaybackQueueUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    suspend operator fun invoke(ids: List<Long>, startId: Long): PlaybackQueue {
        val library = repository.observeVideos().first().getOrNull().orEmpty()
        val byId = library.associateBy { it.id }
        val videos = ids.mapNotNull(byId::get)
        val startIndex = videos.indexOfFirst { it.id == startId }.coerceAtLeast(0)
        return PlaybackQueue(videos, startIndex)
    }
}
