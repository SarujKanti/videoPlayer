package com.skd.videoplayer.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.skd.videoplayer.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads a human-readable name for a video opened from another app or the system file picker. */
class ContentUriMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun displayName(uri: Uri): String = withContext(ioDispatcher) {
        val fromProvider = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }
            }.getOrNull()
        } else {
            null
        }
        (fromProvider ?: uri.lastPathSegment ?: "Video").substringBeforeLast('.')
    }
}
