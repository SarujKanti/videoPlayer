package com.skd.videoplayer.data.repository

import com.skd.videoplayer.core.common.runCatchingCancellable
import com.skd.videoplayer.core.constants.MediaConstants
import com.skd.videoplayer.data.local.MediaStoreVideoDataSource
import com.skd.videoplayer.data.local.MediaStoreVideoRow
import com.skd.videoplayer.data.mapper.VideoMapper
import com.skd.videoplayer.di.ApplicationScope
import com.skd.videoplayer.di.IoDispatcher
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies raw rows and a change signal; an interface so the repository is unit-testable. */
interface VideoRowSource {
    val collectionUri: String
    suspend fun queryVideos(): List<MediaStoreVideoRow>
    fun observeChanges(): Flow<Unit>
}

class MediaStoreVideoRowSource @Inject constructor(
    private val dataSource: MediaStoreVideoDataSource,
) : VideoRowSource {
    override val collectionUri: String = dataSource.collectionUri.toString()
    override suspend fun queryVideos(): List<MediaStoreVideoRow> = dataSource.queryVideos()
    override fun observeChanges(): Flow<Unit> = dataSource.observeChanges()
}

/**
 * Holds one in-memory copy of the library that every screen shares. MediaStore is scanned on
 * first use, on an explicit refresh, and after the system reports a change.
 */
@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val source: VideoRowSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope appScope: CoroutineScope,
) : VideoRepository {

    private val library = MutableStateFlow<Result<List<Video>>?>(null)
    private val refreshMutex = Mutex()

    init {
        @OptIn(FlowPreview::class)
        source.observeChanges()
            .debounce(MediaConstants.MEDIA_CHANGE_DEBOUNCE_MS)
            .onEach { if (library.value != null) refresh() }
            .launchIn(appScope)
    }

    override fun observeVideos(): Flow<Result<List<Video>>> =
        library
            .onStart { if (library.value == null) refresh() }
            .filterNotNull()

    override suspend fun refresh() {
        refreshMutex.withLock {
            val result = runCatchingCancellable {
                withContext(ioDispatcher) {
                    source.queryVideos().map { VideoMapper.toDomain(it, source.collectionUri) }
                }
            }
            // Keep the last good library visible if a re-scan fails.
            val previous = library.value
            library.value = if (result.isFailure && previous?.isSuccess == true) previous else result
        }
    }
}
