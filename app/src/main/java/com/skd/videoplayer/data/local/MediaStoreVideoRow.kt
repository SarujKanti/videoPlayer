package com.skd.videoplayer.data.local

/**
 * Raw values read from one MediaStore cursor row. Kept free of Android types so the
 * mapping to the domain model can be unit tested.
 */
data class MediaStoreVideoRow(
    val id: Long,
    val displayName: String?,
    val title: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedSeconds: Long,
    val mimeType: String?,
    val bucketId: String?,
    val bucketName: String?,
    /** RELATIVE_PATH on Android 10+, or the parent directory of DATA on older versions. */
    val relativePath: String?,
)
