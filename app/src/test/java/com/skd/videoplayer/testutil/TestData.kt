package com.skd.videoplayer.testutil

import com.skd.videoplayer.data.local.MediaStoreVideoRow
import com.skd.videoplayer.domain.model.Video

fun testVideo(
    id: Long,
    title: String = "Video $id",
    durationMs: Long = 60_000,
    sizeBytes: Long = 1_000_000,
    dateAddedEpochMs: Long = id * 1000,
    folderId: String = "camera",
    folderName: String = "Camera",
) = Video(
    id = id,
    uri = "content://media/external/video/media/$id",
    title = title,
    displayName = "$title.mp4",
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    width = 1920,
    height = 1080,
    dateAddedEpochMs = dateAddedEpochMs,
    mimeType = "video/mp4",
    folderId = folderId,
    folderName = folderName,
    relativePath = "DCIM/$folderName/",
)

fun testRow(
    id: Long,
    displayName: String? = "clip_$id.mp4",
    title: String? = "clip_$id",
    bucketId: String? = "100",
    bucketName: String? = "Camera",
    relativePath: String? = "DCIM/Camera/",
) = MediaStoreVideoRow(
    id = id,
    displayName = displayName,
    title = title,
    durationMs = 90_000,
    sizeBytes = 5_000_000,
    width = 1280,
    height = 720,
    dateAddedSeconds = 1_700_000_000,
    mimeType = "video/mp4",
    bucketId = bucketId,
    bucketName = bucketName,
    relativePath = relativePath,
)
