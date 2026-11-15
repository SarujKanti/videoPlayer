package com.skd.videoplayer.domain.model

enum class SortField { NAME, DATE_ADDED, DURATION, SIZE }

data class VideoSort(
    val field: SortField = SortField.DATE_ADDED,
    val ascending: Boolean = false,
) {
    companion object {
        val Default = VideoSort()
    }
}

enum class LibraryLayout { GRID, LIST }
