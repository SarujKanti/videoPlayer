package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The library narrowed to the user's favorites. Updates when either side changes, so adding,
 * removing, renaming or deleting a video is reflected immediately. Favorites whose video is no
 * longer visible (deleted, or outside a "selected videos only" grant) are skipped, not erased.
 */
class GetFavoriteVideosUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Result<List<Video>>> =
        combine(videoRepository.observeVideos(), favoritesRepository.observeFavoriteIds()) { library, ids ->
            library.map { videos -> videos.filter { it.id in ids } }
        }
}
