package com.skd.videoplayer.core.constants

object MediaConstants {
    const val UNKNOWN_FOLDER_ID = "unknown"
    const val UNKNOWN_FOLDER_NAME = "Other videos"

    /** Coalesces bursts of MediaStore change notifications (e.g. while a recording is saved). */
    const val MEDIA_CHANGE_DEBOUNCE_MS = 1_000L

    /** Size requested for list/grid thumbnails when Coil has no measured size. */
    const val THUMBNAIL_WIDTH_PX = 512
    const val THUMBNAIL_HEIGHT_PX = 288
}
