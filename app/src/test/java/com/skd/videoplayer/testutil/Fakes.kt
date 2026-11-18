package com.skd.videoplayer.testutil

import com.skd.videoplayer.domain.model.UserConsentRequest
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoEditResult
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVideoRepository : VideoRepository {
    val library = MutableStateFlow<Result<List<Video>>>(Result.success(emptyList()))
    var refreshCount = 0
    override fun observeVideos(): Flow<Result<List<Video>>> = library
    override suspend fun refresh() {
        refreshCount++
    }
}

class FakeFavoritesRepository : FavoritesRepository {
    val ids = MutableStateFlow<Set<Long>>(emptySet())
    override fun observeFavoriteIds(): Flow<Set<Long>> = ids
    override suspend fun toggle(videoId: Long): Boolean {
        val added = videoId !in ids.value
        ids.value = if (added) ids.value + videoId else ids.value - videoId
        return added
    }
    override suspend fun remove(videoId: Long) {
        ids.value = ids.value - videoId
    }
}

object FakeConsent : UserConsentRequest

/** Records calls and replies with queued results; defaults to success. */
class FakeVideoEditRepository(
    override val deleteShowsSystemConfirmation: Boolean = true,
) : VideoEditRepository {
    data class Call(val kind: String, val videoId: Long, val fileName: String?, val consentGranted: Boolean)

    val calls = mutableListOf<Call>()
    val results = ArrayDeque<VideoEditResult>()

    override suspend fun delete(video: Video, consentGranted: Boolean): VideoEditResult {
        calls += Call("delete", video.id, null, consentGranted)
        return results.removeFirstOrNull() ?: VideoEditResult.Success
    }

    override suspend fun rename(video: Video, newFileName: String, consentGranted: Boolean): VideoEditResult {
        calls += Call("rename", video.id, newFileName, consentGranted)
        return results.removeFirstOrNull() ?: VideoEditResult.Success
    }
}
