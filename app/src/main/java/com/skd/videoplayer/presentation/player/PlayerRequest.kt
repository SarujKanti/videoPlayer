package com.skd.videoplayer.presentation.player

import android.content.Context
import android.content.Intent
import android.net.Uri

/** What the player was asked to play. Parsed from the launching intent. */
sealed interface PlayerRequest {
    /** A queue of library videos in the order shown on screen, starting at [startId]. */
    data class Library(val videoIds: List<Long>, val startId: Long) : PlayerRequest

    /** A single file from another app or the system file picker. */
    data class External(val uri: Uri) : PlayerRequest

    companion object {
        private const val EXTRA_VIDEO_IDS = "com.skd.videoplayer.extra.VIDEO_IDS"
        private const val EXTRA_START_ID = "com.skd.videoplayer.extra.START_ID"

        /** Ids are passed instead of full objects to stay far below the Binder transaction limit. */
        fun libraryIntent(context: Context, videoIds: LongArray, startId: Long): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_IDS, videoIds)
                .putExtra(EXTRA_START_ID, startId)

        fun uriIntent(context: Context, uri: Uri): Intent =
            Intent(context, PlayerActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setDataAndType(uri, "video/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        fun from(intent: Intent): PlayerRequest? {
            val ids = intent.getLongArrayExtra(EXTRA_VIDEO_IDS)
            if (ids != null && ids.isNotEmpty()) {
                return Library(ids.toList(), intent.getLongExtra(EXTRA_START_ID, ids.first()))
            }
            return intent.data?.let(::External)
        }
    }
}
