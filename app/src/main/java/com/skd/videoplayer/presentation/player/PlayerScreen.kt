package com.skd.videoplayer.presentation.player

import android.graphics.Color as AndroidColor
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.skd.videoplayer.core.utils.Formatters
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

private const val CONTROLS_TIMEOUT_MS = 3_500L
private const val MAX_ZOOM = 4f

/** Transient overlay shown while a gesture is in progress. */
private sealed interface GestureFeedback {
    data class Seek(val targetMs: Long, val deltaMs: Long, val durationMs: Long) : GestureFeedback
    data class Level(val icon: ImageVector, val fraction: Float) : GestureFeedback
    data class Message(val text: String) : GestureFeedback
}

/** Mutable values a single drag needs; kept outside Compose state to avoid recomposition per pixel. */
private class DragSession {
    var seekBaseMs = 0L
    var seekTargetMs = 0L
    var level = 0f
}

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    isInPip: Boolean,
    canEnterPip: Boolean,
    onEnterPip: () -> Unit,
    onRotate: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = rememberDeviceControls()

    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var interactions by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var feedback by remember { mutableStateOf<GestureFeedback?>(null) }
    var doubleTapZone by remember { mutableStateOf<TapZone?>(null) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }

    // Hide controls after a pause in interaction, but never while paused or a sheet is open.
    LaunchedEffect(controlsVisible, state.isPlaying, interactions, sheet) {
        if (controlsVisible && state.isPlaying && sheet == null) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(doubleTapZone) {
        if (doubleTapZone != null) {
            delay(600)
            doubleTapZone = null
        }
    }
    LaunchedEffect(feedback) {
        if (feedback is GestureFeedback.Message) {
            delay(1_200)
            feedback = null
        }
    }
    LaunchedEffect(lockHintVisible) {
        if (lockHintVisible) {
            delay(2_500)
            lockHintVisible = false
        }
    }
    LaunchedEffect(state.scaleMode) { zoom = 1f }
    // Errors and the end of the queue need the controls on screen.
    LaunchedEffect(state.error, state.isEnded) {
        if (state.error != null || state.isEnded) controlsVisible = true
    }

    val drag = remember { DragSession() }
    val gestures = remember(device) {
        PlayerGestureCallbacks(
            onTap = {
                controlsVisible = !controlsVisible
                interactions++
            },
            onDoubleTap = { zone ->
                when (zone) {
                    TapZone.LEFT -> viewModel.seekBy(-PlayerViewModel.SEEK_STEP_MS)
                    TapZone.RIGHT -> viewModel.seekBy(PlayerViewModel.SEEK_STEP_MS)
                    TapZone.CENTER -> viewModel.togglePlayPause()
                }
                doubleTapZone = zone
            },
            onDragStart = { gesture ->
                when (gesture) {
                    DragGesture.SEEK -> {
                        drag.seekBaseMs = viewModel.player.currentPosition
                        drag.seekTargetMs = drag.seekBaseMs
                    }
                    DragGesture.BRIGHTNESS -> drag.level = device?.brightness() ?: 0.5f
                    DragGesture.VOLUME -> drag.level = device?.volume() ?: 0.5f
                }
            },
            onDrag = { gesture, amount ->
                when (gesture) {
                    DragGesture.SEEK -> {
                        val duration = viewModel.uiState.value.durationMs
                        if (duration > 0) {
                            // A full-width swipe covers up to two minutes, or the whole video if shorter.
                            val span = min(duration, 120_000L)
                            drag.seekTargetMs = (drag.seekTargetMs + (amount * span).toLong()).coerceIn(0, duration)
                            feedback = GestureFeedback.Seek(
                                drag.seekTargetMs,
                                drag.seekTargetMs - drag.seekBaseMs,
                                duration,
                            )
                        }
                    }
                    DragGesture.BRIGHTNESS -> {
                        drag.level = (drag.level + amount * 1.2f).coerceIn(0f, 1f)
                        val applied = device?.setBrightness(drag.level) ?: drag.level
                        feedback = GestureFeedback.Level(Icons.Outlined.BrightnessMedium, applied)
                    }
                    DragGesture.VOLUME -> {
                        drag.level = (drag.level + amount * 1.2f).coerceIn(0f, 1f)
                        val applied = device?.setVolume(drag.level) ?: drag.level
                        feedback = GestureFeedback.Level(Icons.AutoMirrored.Outlined.VolumeUp, applied)
                    }
                }
            },
            onDragEnd = { gesture ->
                if (gesture == DragGesture.SEEK && drag.seekTargetMs != drag.seekBaseMs) {
                    viewModel.seekTo(drag.seekTargetMs)
                }
                feedback = null
            },
            onZoom = { factor ->
                zoom = (zoom * factor).coerceIn(1f, MAX_ZOOM)
                feedback = GestureFeedback.Message("Zoom ${(zoom * 100).toInt()}%")
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VideoSurface(
            player = viewModel.player,
            scaleMode = state.scaleMode,
            zoom = zoom,
            modifier = Modifier.fillMaxSize(),
        )

        if (isInPip) return@Box

        // Gesture layer sits under the controls so buttons still receive their own taps.
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (state.isLocked) {
                        Modifier.lockedTapDetector { lockHintVisible = true }
                    } else {
                        Modifier.playerGestures(enabled = state.error == null, callbacks = gestures)
                    },
                ),
        )

        DoubleTapIndicator(doubleTapZone)

        if (state.isBuffering && state.error == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp),
            )
        }

        feedback?.let {
            FeedbackPill(
                feedback = it,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 72.dp),
            )
        }

        if (state.isLocked) {
            AnimatedVisibility(
                visible = lockHintVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 24.dp),
            ) {
                Surface(
                    onClick = {
                        viewModel.setLocked(false)
                        controlsVisible = true
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tap to unlock", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            AnimatedVisibility(
                visible = controlsVisible && state.error == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                PlayerControls(
                    state = state,
                    canEnterPip = canEnterPip,
                    actions = PlayerControlActions(
                        onBack = onBack,
                        onPlayPause = { viewModel.togglePlayPause(); interactions++ },
                        onSeekBack = { viewModel.seekBy(-PlayerViewModel.SEEK_STEP_MS); interactions++ },
                        onSeekForward = { viewModel.seekBy(PlayerViewModel.SEEK_STEP_MS); interactions++ },
                        onPrevious = { viewModel.previous(); interactions++ },
                        onNext = { viewModel.next(); interactions++ },
                        onSeekTo = { viewModel.seekTo(it); interactions++ },
                        onScrub = { interactions++ },
                        onOpenSheet = { sheet = it },
                        onLock = {
                            viewModel.setLocked(true)
                            lockHintVisible = true
                        },
                        onCycleScale = {
                            viewModel.cycleScaleMode()
                            feedback = GestureFeedback.Message(viewModel.uiState.value.scaleMode.label)
                            interactions++
                        },
                        onCycleRepeat = { viewModel.cycleRepeatMode(); interactions++ },
                        onToggleShuffle = { viewModel.toggleShuffle(); interactions++ },
                        onRotate = { onRotate(); interactions++ },
                        onEnterPip = onEnterPip,
                    ),
                )
            }
        }

        state.error?.let { error ->
            PlaybackErrorOverlay(
                error = error,
                canSkip = state.hasNext,
                onRetry = viewModel::retry,
                onSkip = viewModel::skipFailedVideo,
                onClose = onBack,
            )
        }
    }

    sheet?.let { current ->
        PlayerOptionsSheet(
            sheet = current,
            state = state,
            onSpeed = viewModel::setSpeed,
            onAudioTrack = viewModel::selectAudioTrack,
            onSubtitleTrack = viewModel::selectSubtitleTrack,
            onDismiss = { sheet = null },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    player: Player,
    scaleMode: VideoScaleMode,
    zoom: Float,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setShutterBackgroundColor(AndroidColor.BLACK)
                setKeepContentOnPlayerReset(true)
                this.player = player
            }
        },
        update = { view ->
            view.resizeMode = when (scaleMode) {
                VideoScaleMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoScaleMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoScaleMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            // Scale the video surface itself so subtitles stay put while zooming.
            view.videoSurfaceView?.apply {
                scaleX = zoom
                scaleY = zoom
            }
        },
        onRelease = { it.player = null },
        modifier = modifier,
    )
}

/** While locked, any tap only reveals the unlock button. */
private fun Modifier.lockedTapDetector(onTap: () -> Unit): Modifier =
    pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }

@Composable
private fun DoubleTapIndicator(zone: TapZone?) {
    val side = zone?.takeIf { it != TapZone.CENTER } ?: return
    val left = side == TapZone.LEFT
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
                .align(if (left) Alignment.CenterStart else Alignment.CenterEnd)
                .background(
                    Brush.horizontalGradient(
                        if (left) {
                            listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.18f))
                        },
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (left) Icons.Outlined.FastRewind else Icons.Outlined.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "${PlayerViewModel.SEEK_STEP_MS / 1000} seconds",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun FeedbackPill(feedback: GestureFeedback, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.65f),
        contentColor = Color.White,
    ) {
        when (feedback) {
            is GestureFeedback.Seek -> Column(
                Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${Formatters.duration(feedback.targetMs)} / ${Formatters.duration(feedback.durationMs)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                val sign = if (feedback.deltaMs < 0) "−" else "+"
                Text(
                    "$sign${Formatters.duration(abs(feedback.deltaMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            is GestureFeedback.Level -> Row(
                Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(feedback.icon, contentDescription = null)
                LinearProgressIndicator(
                    progress = { feedback.fraction },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .width(140.dp)
                        .height(4.dp),
                )
                Text("${(feedback.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            is GestureFeedback.Message -> Text(
                feedback.text,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
