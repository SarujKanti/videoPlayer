package com.skd.videoplayer.data.mapper

import com.skd.videoplayer.core.constants.MediaConstants
import com.skd.videoplayer.data.local.MediaStoreVideoRow
import com.skd.videoplayer.domain.model.Video

object VideoMapper {

    fun toDomain(row: MediaStoreVideoRow, collectionUri: String): Video {
        val displayName = row.displayName?.takeIf { it.isNotBlank() } ?: "video_${row.id}"
        val title = row.title?.takeIf { it.isNotBlank() } ?: displayName.substringBeforeLast('.')
        val relativePath = row.relativePath?.takeIf { it.isNotBlank() }
        val folderName = row.bucketName?.takeIf { it.isNotBlank() }
            ?: relativePath?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: MediaConstants.UNKNOWN_FOLDER_NAME
        val folderId = row.bucketId?.takeIf { it.isNotBlank() }
            ?: relativePath
            ?: MediaConstants.UNKNOWN_FOLDER_ID
        return Video(
            id = row.id,
            uri = "${collectionUri.trimEnd('/')}/${row.id}",
            title = title,
            displayName = displayName,
            durationMs = row.durationMs.coerceAtLeast(0),
            sizeBytes = row.sizeBytes.coerceAtLeast(0),
            width = row.width.coerceAtLeast(0),
            height = row.height.coerceAtLeast(0),
            dateAddedEpochMs = row.dateAddedSeconds.coerceAtLeast(0) * 1000,
            mimeType = row.mimeType,
            folderId = folderId,
            folderName = folderName,
            relativePath = relativePath,
        )
    }
}
