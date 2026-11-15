package com.skd.videoplayer.data.local

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Reads video rows from MediaStore and reports library changes. Call from a background dispatcher. */
@Singleton
class MediaStoreVideoDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    val collectionUri: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    private val usesRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @Suppress("DEPRECATION") // DATA is only read below Android 10.
    private val pathColumn: String =
        if (usesRelativePath) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Video.Media.DATA

    private val projection: Array<String> = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        pathColumn,
    )

    /**
     * Queries every video visible to the app, newest first. Only the columns the UI needs are
     * projected, so even libraries with thousands of entries stay small in memory.
     */
    suspend fun queryVideos(): List<MediaStoreVideoRow> {
        val cursor = resolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketIdCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val pathCol = c.getColumnIndexOrThrow(pathColumn)

            val rows = ArrayList<MediaStoreVideoRow>(c.count)
            while (c.moveToNext()) {
                // Lets a cancelled scan stop promptly on very large libraries.
                if (rows.size % 500 == 0) yield()
                rows += MediaStoreVideoRow(
                    id = c.getLong(idCol),
                    displayName = c.getStringOrNull(nameCol),
                    title = c.getStringOrNull(titleCol),
                    durationMs = c.getLongOrZero(durationCol),
                    sizeBytes = c.getLongOrZero(sizeCol),
                    width = c.getLongOrZero(widthCol).toInt(),
                    height = c.getLongOrZero(heightCol).toInt(),
                    dateAddedSeconds = c.getLongOrZero(dateCol),
                    mimeType = c.getStringOrNull(mimeCol),
                    bucketId = c.getStringOrNull(bucketIdCol),
                    bucketName = c.getStringOrNull(bucketNameCol),
                    relativePath = c.getStringOrNull(pathCol)?.let { value ->
                        if (usesRelativePath) value else File(value).parent
                    },
                )
            }
            rows
        }
    }

    /** Emits whenever MediaStore reports a change to the video collection. */
    fun observeChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(collectionUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)
    private fun Cursor.getLongOrZero(index: Int): Long = if (isNull(index)) 0L else getLong(index)
}
