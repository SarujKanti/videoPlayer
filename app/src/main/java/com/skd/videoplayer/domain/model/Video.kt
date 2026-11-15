package com.skd.videoplayer.domain.model

/**
 * A video discovered on the device. Pure Kotlin so it can be used in unit tests
 * without Android framework classes. [uri] is a `content://` URI string.
 */
data class Video(
    val id: Long,
    val uri: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedEpochMs: Long,
    val mimeType: String?,
    val folderId: String,
    val folderName: String,
    val relativePath: String?,
) {
    val hasResolution: Boolean get() = width > 0 && height > 0
}
