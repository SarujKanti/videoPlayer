package com.skd.videoplayer.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ClosedCaptionDisabled
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.core.utils.Formatters

/** Everything the control overlay can ask for. Grouped to keep the composable signature readable. */
class PlayerControlActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekBack: () -> Unit,
    val onSeekForward: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onScrub: () -> Unit,
    val onOpenSheet: (PlayerSheet) -> Unit,
    val onLock: () -> Unit,
    val onCycleScale: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onToggleShuffle: () -> Unit,
    val onRotate: () -> Unit,
    val onEnterPip: () -> Unit,
)

private val ControlTint = Color.White
private val InactiveTint = Color.White.copy(alpha = 0.55f)

@Composable
fun PlayerControls(
    state: PlayerUiState,
    canEnterPip: Boolean,
    actions: PlayerControlActions,
) {
    Box(Modifier.fillMaxSize()) {
        // Scrims keep white controls legible on bright video.
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
        )

        TopBar(
            state = state,
            canEnterPip = canEnterPip,
            actions = actions,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )

        CenterControls(
            state = state,
            actions = actions,
            modifier = Modifier.align(Alignment.Center),
        )

        BottomBar(
            state = state,
            actions = actions,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun TopBar(
    state: PlayerUiState,
    canEnterPip: Boolean,
    actions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Close player", onClick = actions.onBack)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = ControlTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.queueSize > 1) {
                Text(
                    text = "${state.queueIndex + 1} of ${state.queueSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InactiveTint,
                )
            }
        }
        if (state.subtitleTracks.isNotEmpty()) {
            ControlIcon(
                if (state.subtitlesOff) Icons.Outlined.ClosedCaptionDisabled else Icons.Outlined.ClosedCaption,
                "Subtitles",
            ) { actions.onOpenSheet(PlayerSheet.SUBTITLES) }
        }
        if (state.audioTracks.size > 1) {
            ControlIcon(Icons.Outlined.Audiotrack, "Audio track") { actions.onOpenSheet(PlayerSheet.AUDIO) }
        }
        TextButton(onClick = { actions.onOpenSheet(PlayerSheet.SPEED) }) {
            Text(
                speedLabel(state.speed),
                color = if (state.speed == 1f) ControlTint else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (canEnterPip) {
            ControlIcon(Icons.Outlined.PictureInPictureAlt, "Picture in picture", onClick = actions.onEnterPip)
        }
    }
}

@Composable
private fun CenterControls(
    state: PlayerUiState,
    actions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ControlIcon(
            Icons.Outlined.SkipPrevious,
            "Previous video",
            size = 34.dp,
            enabled = state.hasPrevious || state.positionMs > 0,
            onClick = actions.onPrevious,
        )
        ControlIcon(Icons.Outlined.Replay10, "Back 10 seconds", size = 34.dp, onClick = actions.onSeekBack)
        Surface(
            onClick = actions.onPlayPause,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.16f),
            contentColor = ControlTint,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val (icon, label) = when {
                    state.isEnded -> Icons.Outlined.Replay to "Replay"
                    state.isPlaying -> Icons.Outlined.Pause to "Pause"
                    else -> Icons.Outlined.PlayArrow to "Play"
                }
                Icon(icon, contentDescription = label, modifier = Modifier.size(44.dp))
            }
        }
        ControlIcon(Icons.Outlined.Forward10, "Forward 10 seconds", size = 34.dp, onClick = actions.onSeekForward)
        ControlIcon(
            Icons.Outlined.SkipNext,
            "Next video",
            size = 34.dp,
            enabled = state.hasNext,
            onClick = actions.onNext,
        )
    }
}

@Composable
private fun BottomBar(
    state: PlayerUiState,
    actions: PlayerControlActions,
    modifier: Modifier = Modifier,
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(1)
    val shownPosition = scrubFraction?.let { (it * duration).toLong() } ?: state.positionMs

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        unsupportedTrackMessage(state)?.let {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeLabel(Formatters.duration(shownPosition))
            Slider(
                value = scrubFraction ?: (state.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = {
                    scrubFraction = it
                    actions.onScrub()
                },
                onValueChangeFinished = {
                    scrubFraction?.let { actions.onSeekTo((it * duration).toLong()) }
                    scrubFraction = null
                },
                enabled = state.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            TimeLabel(Formatters.duration(state.durationMs))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlIcon(Icons.Outlined.Lock, "Lock controls", onClick = actions.onLock)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions.onCycleScale) {
                    Icon(Icons.Outlined.AspectRatio, contentDescription = null, tint = ControlTint)
                    Spacer(Modifier.width(6.dp))
                    Text(state.scaleMode.label, color = ControlTint, style = MaterialTheme.typography.labelLarge)
                }
                if (state.queueSize > 1) {
                    ControlIcon(
                        Icons.Outlined.Shuffle,
                        if (state.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else InactiveTint,
                        onClick = actions.onToggleShuffle,
                    )
                }
                ControlIcon(
                    if (state.repeatMode == PlayerRepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    when (state.repeatMode) {
                        PlayerRepeatMode.OFF -> "Repeat off"
                        PlayerRepeatMode.ALL -> "Repeat all"
                        PlayerRepeatMode.ONE -> "Repeat one"
                    },
                    tint = if (state.repeatMode == PlayerRepeatMode.OFF) InactiveTint else MaterialTheme.colorScheme.primary,
                    onClick = actions.onCycleRepeat,
                )
            }
            ControlIcon(Icons.Outlined.ScreenRotation, "Rotate screen", onClick = actions.onRotate)
        }
    }
}

private fun unsupportedTrackMessage(state: PlayerUiState): String? = when {
    state.videoUnsupported && state.audioUnsupported -> "This device can't decode this video's picture or sound"
    state.videoUnsupported -> "Video codec not supported on this device — playing sound only"
    state.audioUnsupported -> "Audio format not supported on this device — playing without sound"
    else -> null
}

@Composable
private fun TimeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ControlTint,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    enabled: Boolean = true,
    tint: Color = ControlTint,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size + 24.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = Color.White.copy(alpha = 0.3f),
        ),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(size))
    }
}

@Composable
fun PlaybackErrorOverlay(
    error: PlaybackErrorKind,
    canSkip: Boolean,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(error.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClose) { Text("Close", color = Color.White) }
                if (error != PlaybackErrorKind.EMPTY_QUEUE) {
                    OutlinedButton(onClick = onRetry) { Text("Try again", color = Color.White) }
                }
                if (canSkip) {
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Next video") }
                }
            }
        }
    }
}
