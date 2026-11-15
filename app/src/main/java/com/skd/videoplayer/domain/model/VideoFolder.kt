package com.skd.videoplayer.domain.model

data class VideoFolder(
    val id: String,
    val name: String,
    val relativePath: String?,
    val videoCount: Int,
    val totalSizeBytes: Long,
    /** Most recently added video in the folder; used as the folder thumbnail. */
    val coverVideo: Video,
)
