package com.skd.videoplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.skd.videoplayer.data.local.VideoThumbnailFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoPlayerApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(VideoThumbnailFetcher.Factory())
                add(VideoThumbnailFetcher.Key())
            }
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.20).build()
            }
            .crossfade(true)
            .build()
}
