package com.skd.videoplayer.domain.repository

import com.skd.videoplayer.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    /**
     * Emits the device video library. The first collector triggers an initial scan;
     * later emissions follow [refresh] calls and MediaStore change notifications.
     * A failed scan emits [Result.failure] without terminating the flow.
     */
    fun observeVideos(): Flow<Result<List<Video>>>

    /** Re-scans MediaStore. Suspends until the new result has been published. */
    suspend fun refresh()
}
