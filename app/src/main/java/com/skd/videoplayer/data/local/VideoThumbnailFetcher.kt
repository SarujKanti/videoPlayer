package com.skd.videoplayer.data.local

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.Dimension
import com.skd.videoplayer.core.constants.MediaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coil request model for a MediaStore video thumbnail. */
data class VideoThumbnail(val videoId: Long, val uri: String)

/**
 * Loads thumbnails from the system thumbnail cache instead of decoding video frames, which keeps
 * scrolling smooth in large libraries. Coil keeps the decoded bitmaps in its memory cache.
 */
class VideoThumbnailFetcher(
    private val data: VideoThumbnail,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bitmap = withContext(Dispatchers.IO) { loadBitmap() } ?: return null
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun loadBitmap(): Bitmap? {
        val resolver = options.context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val width = (options.size.width as? Dimension.Pixels)?.px ?: MediaConstants.THUMBNAIL_WIDTH_PX
            val height = (options.size.height as? Dimension.Pixels)?.px ?: MediaConstants.THUMBNAIL_HEIGHT_PX
            resolver.loadThumbnail(Uri.parse(data.uri), Size(width, height), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(
                resolver,
                data.videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null,
            )
        }
    }

    class Factory : Fetcher.Factory<VideoThumbnail> {
        override fun create(data: VideoThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            VideoThumbnailFetcher(data, options)
    }

    /** Without a key Coil would skip the memory cache for this custom model. */
    class Key : Keyer<VideoThumbnail> {
        override fun key(data: VideoThumbnail, options: Options): String = "video-thumb:${data.uri}"
    }
}
