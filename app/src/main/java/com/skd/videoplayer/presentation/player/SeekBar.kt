package com.skd.videoplayer.presentation.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp

/**
 * Slim video seek bar: a 4dp track showing played and buffered progress, and a round thumb
 * that grows while dragging. Tap anywhere to jump; drag to scrub. The touch area is much taller
 * than the visible track so it's easy to grab.
 */
@Composable
fun SeekBar(
    fraction: Float,
    bufferedFraction: Float,
    enabled: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val thumbRadius by animateDpAsState(if (dragging) 9.dp else 6.dp, label = "thumbRadius")
    val latestScrub by rememberUpdatedState(onScrub)
    val latestFinish by rememberUpdatedState(onScrubFinished)
    val played = MaterialTheme.colorScheme.primary
    val buffered = Color.White.copy(alpha = 0.45f)
    val track = Color.White.copy(alpha = 0.22f)

    val input = if (enabled) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    latestScrub((offset.x / size.width).coerceIn(0f, 1f))
                    latestFinish()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        latestScrub((offset.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        dragging = false
                        latestFinish()
                    },
                    onDragCancel = {
                        dragging = false
                        latestFinish()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        latestScrub((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                )
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        latestScrub(target.coerceIn(0f, 1f))
                        latestFinish()
                        true
                    }
                }
            }
            .then(input),
    ) {
        val trackHeight = 4.dp.toPx()
        val top = (size.height - trackHeight) / 2
        val radius = CornerRadius(trackHeight / 2)
        drawRoundRect(track, Offset(0f, top), Size(size.width, trackHeight), radius)
        drawRoundRect(buffered, Offset(0f, top), Size(size.width * bufferedFraction, trackHeight), radius)
        drawRoundRect(played, Offset(0f, top), Size(size.width * fraction, trackHeight), radius)
        if (enabled) {
            drawCircle(played, thumbRadius.toPx(), Offset(size.width * fraction, size.height / 2))
        }
    }
}
