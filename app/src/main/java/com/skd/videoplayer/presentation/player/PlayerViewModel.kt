package com.skd.videoplayer.presentation.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.skd.videoplayer.data.local.ContentUriMetadataReader
import com.skd.videoplayer.di.ApplicationScope
import com.skd.videoplayer.domain.model.PlaybackPosition
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.repository.PlaybackPositionRepository
import com.skd.videoplayer.domain.repository.WatchedVideosRepository
import com.skd.videoplayer.domain.usecase.GetPlaybackQueueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Owns the single [ExoPlayer] for the player screen. Living in the ViewModel means the player
 * survives rotation and recomposition and is released exactly once, in [onCleared].
 */
@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getPlaybackQueue: GetPlaybackQueueUseCase,
    private val uriMetadata: ContentUriMetadataReader,
    private val playbackPositions: PlaybackPositionRepository,
    private val watchedVideos: WatchedVideosRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        // Falls back to another decoder (e.g. software) when the preferred one fails to start.
        .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
        // Requests audio focus, pausing for calls and ducking for notifications.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        // Pauses when headphones are unplugged or Bluetooth disconnects.
        .setHandleAudioBecomingNoisy(true)
        .setSeekBackIncrementMs(SEEK_STEP_MS)
        .setSeekForwardIncrementMs(SEEK_STEP_MS)
        .build()
        .apply {
            // Show embedded subtitles in the device language automatically when present.
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setPreferredTextLanguage(Locale.getDefault().language)
                .build()
        }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                _uiState.update { it.copy(error = null) }
            }
            syncFromPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(error = PlaybackErrorKind.fromErrorCode(error.errorCode), isLoading = false) }
        }

        // Pausing, leaving the screen and losing audio focus all land here.
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) savePosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) currentMediaKey?.let(::clearPosition)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // The previous video ran to its end (or looped), so it was watched in full.
            val finished = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            if (finished) currentMediaKey?.let(::clearPosition)
            currentMediaKey = mediaItem?.mediaId
            // Next/previous and auto-advance start a different video; it counts as played too.
            markWatched(currentMediaKey)
        }
    }

    /** Media id of the video on screen, used as the key for its saved position. */
    private var currentMediaKey: String? = null

    /** Where the video being asked about was left, while the resume prompt is showing. */
    private var pendingResume: Pair<String, PlaybackPosition>? = null

    private var lastPeriodicSaveMs = 0L

    init {
        player.addListener(listener)
        // Position isn't pushed by the player, so poll it while playing.
        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    syncProgress()
                    // Also save now and then, so a crash or a killed process loses little.
                    val now = System.currentTimeMillis()
                    if (now - lastPeriodicSaveMs >= PERIODIC_SAVE_INTERVAL_MS) {
                        lastPeriodicSaveMs = now
                        savePosition()
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /**
     * Starts playback for [request]. A repeated call for the same screen (rotation, activity
     * re-creation) is ignored unless [replace] is set, e.g. when a new "open with" arrives.
     */
    fun open(request: PlayerRequest, replace: Boolean = false) {
        if (!replace && player.mediaItemCount > 0) return
        // A new "open with" replaces the current video; remember where that one was left.
        if (player.mediaItemCount > 0) savePosition()
        pendingResume = null
        _uiState.update { PlayerUiState(isLoading = true, scaleMode = it.scaleMode) }
        viewModelScope.launch {
            val (items, startIndex) = when (request) {
                is PlayerRequest.Library -> {
                    val queue = getPlaybackQueue(request.videoIds, request.startId)
                    queue.videos.map(::toMediaItem) to queue.startIndex
                }
                is PlayerRequest.External -> listOf(toMediaItem(request.uri, uriMetadata.displayName(request.uri))) to 0
            }
            if (items.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = PlaybackErrorKind.EMPTY_QUEUE) }
                return@launch
            }
            val startKey = items[startIndex].mediaId
            val saved = playbackPositions.get(startKey)
            currentMediaKey = startKey
            markWatched(startKey)
            player.setMediaItems(items, startIndex, C.TIME_UNSET)
            player.prepare()
            if (saved == null) {
                player.playWhenReady = true
                _uiState.update { it.copy(isLoading = false) }
            } else {
                // Hold on the first frame until the user picks where to start.
                pendingResume = startKey to saved
                player.playWhenReady = false
                _uiState.update {
                    it.copy(isLoading = false, resumePrompt = ResumePrompt(saved.positionMs, saved.durationMs))
                }
            }
        }
    }

    /** Continues the video from where it was left last time. */
    fun resumeFromSavedPosition() {
        val (_, saved) = pendingResume ?: return
        pendingResume = null
        _uiState.update { it.copy(resumePrompt = null) }
        player.seekTo(saved.positionMs)
        player.play()
        syncProgress()
    }

    /** Plays the video from the start and forgets the old position. */
    fun startFromBeginning() {
        val (key, _) = pendingResume ?: return
        pendingResume = null
        _uiState.update { it.copy(resumePrompt = null) }
        clearPosition(key)
        player.seekTo(0)
        player.play()
    }

    fun togglePlayPause() {
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(player.currentMediaItemIndex, 0)
                player.play()
            }
            player.isPlaying -> player.pause()
            else -> {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
    }

    fun pause() = player.pause()

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, maxOf(player.duration, 0)))
        syncProgress()
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun previous() {
        savePosition()
        player.seekToPrevious()
    }

    fun next() {
        savePosition()
        player.seekToNext()
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleScaleMode() = _uiState.update { it.copy(scaleMode = it.scaleMode.next()) }

    fun setLocked(locked: Boolean) = _uiState.update { it.copy(isLocked = locked) }

    fun selectAudioTrack(option: TrackOption) = selectTrack(C.TRACK_TYPE_AUDIO, option)

    /** `null` turns subtitles off. */
    fun selectSubtitleTrack(option: TrackOption?) {
        if (option == null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            selectTrack(C.TRACK_TYPE_TEXT, option)
        }
    }

    fun retry() {
        _uiState.update { it.copy(error = null) }
        player.prepare()
        player.play()
    }

    fun skipFailedVideo() {
        _uiState.update { it.copy(error = null) }
        player.seekToNextMediaItem()
        player.prepare()
        player.play()
    }

    private fun selectTrack(trackType: Int, option: TrackOption) {
        val group = player.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    private fun syncProgress() {
        _uiState.update {
            it.copy(
                positionMs = player.currentPosition.coerceAtLeast(0),
                bufferedMs = player.bufferedPosition.coerceAtLeast(0),
                durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0,
            )
        }
    }

    private fun syncFromPlayer() {
        val tracks = player.currentTracks
        val videoSize = player.videoSize
        _uiState.update { state ->
            state.copy(
                title = player.mediaMetadata.title?.toString() ?: state.title,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                isEnded = player.playbackState == Player.STATE_ENDED,
                positionMs = player.currentPosition.coerceAtLeast(0),
                bufferedMs = player.bufferedPosition.coerceAtLeast(0),
                durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                queueIndex = player.currentMediaItemIndex,
                queueSize = player.mediaItemCount,
                speed = player.playbackParameters.speed,
                repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_ALL -> PlayerRepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> PlayerRepeatMode.ONE
                    else -> PlayerRepeatMode.OFF
                },
                shuffle = player.shuffleModeEnabled,
                // Anamorphic videos store non-square pixels; correct for the true display shape.
                videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
                videoHeight = videoSize.height,
                audioTracks = TrackOptions.from(tracks, C.TRACK_TYPE_AUDIO),
                subtitleTracks = TrackOptions.from(tracks, C.TRACK_TYPE_TEXT),
                subtitlesOff = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes ||
                    !tracks.isTypeSelected(C.TRACK_TYPE_TEXT),
                videoUnsupported = TrackOptions.hasOnlyUnsupported(tracks, C.TRACK_TYPE_VIDEO),
                audioUnsupported = TrackOptions.hasOnlyUnsupported(tracks, C.TRACK_TYPE_AUDIO),
            )
        }
    }

    private fun toMediaItem(video: Video): MediaItem =
        MediaItem.Builder()
            .setMediaId(video.id.toString())
            .setUri(video.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(video.title).build())
            .build()

    private fun toMediaItem(uri: Uri, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(uri.toString())
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()

    /**
     * Remembers where the current video is. Player values are read here on the main thread; the
     * write runs on the app scope so it still finishes when the screen closes.
     */
    private fun savePosition() {
        // While asking, the player sits at 0:00; saving that would wipe the spot being offered.
        if (pendingResume != null) return
        val key = currentMediaKey ?: return
        if (player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) return
        val positionMs = player.currentPosition.coerceAtLeast(0)
        val durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0
        appScope.launch { playbackPositions.save(key, positionMs, durationMs) }
    }

    /**
     * Records a library video as played so its "NEW" badge goes away. Library items carry the
     * numeric MediaStore id as media id; files opened from other apps use a URI and are ignored.
     */
    private fun markWatched(mediaId: String?) {
        val videoId = mediaId?.toLongOrNull() ?: return
        appScope.launch { watchedVideos.markWatched(videoId) }
    }

    private fun clearPosition(key: String) {
        appScope.launch { playbackPositions.clear(key) }
    }

    override fun onCleared() {
        savePosition()
        player.removeListener(listener)
        player.release()
    }

    companion object {
        const val SEEK_STEP_MS = 10_000L
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val PERIODIC_SAVE_INTERVAL_MS = 5_000L
    }
}
