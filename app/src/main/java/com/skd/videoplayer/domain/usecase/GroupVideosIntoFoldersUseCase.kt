package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoFolder
import javax.inject.Inject

/** Groups videos by their MediaStore bucket, sorted by folder name. */
class GroupVideosIntoFoldersUseCase @Inject constructor() {

    operator fun invoke(videos: List<Video>): List<VideoFolder> =
        videos.groupBy { it.folderId }
            .map { (folderId, items) ->
                val cover = items.maxBy { it.dateAddedEpochMs }
                VideoFolder(
                    id = folderId,
                    name = cover.folderName,
                    relativePath = cover.relativePath,
                    videoCount = items.size,
                    totalSizeBytes = items.sumOf { it.sizeBytes },
                    coverVideo = cover,
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}
