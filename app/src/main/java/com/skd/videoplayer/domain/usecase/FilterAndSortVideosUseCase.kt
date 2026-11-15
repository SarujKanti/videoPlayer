package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.SortField
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoSort
import javax.inject.Inject

/** Applies a case-insensitive name search and a sort order to a list of videos. */
class FilterAndSortVideosUseCase @Inject constructor() {

    operator fun invoke(videos: List<Video>, query: String, sort: VideoSort): List<Video> {
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) videos else videos.filter {
            it.title.contains(needle, ignoreCase = true) ||
                it.displayName.contains(needle, ignoreCase = true)
        }
        val comparator: Comparator<Video> = when (sort.field) {
            SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.DATE_ADDED -> compareBy { it.dateAddedEpochMs }
            SortField.DURATION -> compareBy { it.durationMs }
            SortField.SIZE -> compareBy { it.sizeBytes }
        }
        // Stable tie-breaker so equal keys keep a deterministic order.
        val withTieBreak = comparator.thenBy { it.id }
        return filtered.sortedWith(if (sort.ascending) withTieBreak else withTieBreak.reversed())
    }
}
