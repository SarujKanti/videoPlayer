package com.skd.videoplayer.presentation.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

enum class DragGesture { SEEK, BRIGHTNESS, VOLUME }

enum class TapZone { LEFT, CENTER, RIGHT }

/** Callbacks for [playerGestures]. Drag amounts are fractions of the screen size. */
class PlayerGestureCallbacks(
    val onTap: () -> Unit,
    val onDoubleTap: (TapZone) -> Unit,
    val onDragStart: (DragGesture) -> Unit,
    /** Horizontal fraction for [DragGesture.SEEK]; upward fraction for brightness and volume. */
    val onDrag: (DragGesture, Float) -> Unit,
    val onDragEnd: (DragGesture) -> Unit,
    val onZoom: (Float) -> Unit,
)

/** Which third of the screen a tap landed in. Pure so it can be unit tested. */
fun tapZoneFor(x: Float, width: Int): TapZone = when {
    width <= 0 -> TapZone.CENTER
    x < width / 3f -> TapZone.LEFT
    x > width * 2f / 3f -> TapZone.RIGHT
    else -> TapZone.CENTER
}

/** Picks the drag gesture once movement passes touch slop: horizontal seeks, vertical depends on side. */
fun dragGestureFor(totalDrag: Offset, startX: Float, width: Int): DragGesture = when {
    abs(totalDrag.x) > abs(totalDrag.y) -> DragGesture.SEEK
    startX < width / 2f -> DragGesture.BRIGHTNESS
    else -> DragGesture.VOLUME
}

/**
 * Taps, double-taps, one-finger drags (seek / brightness / volume) and two-finger pinch zoom.
 * A drag consumes its pointer events, which cancels the tap detector for that gesture.
 */
fun Modifier.playerGestures(enabled: Boolean, callbacks: PlayerGestureCallbacks): Modifier {
    if (!enabled) return this
    return this
        .pointerInput(callbacks) {
            detectTapGestures(
                onTap = { callbacks.onTap() },
                onDoubleTap = { offset -> callbacks.onDoubleTap(tapZoneFor(offset.x, size.width)) },
            )
        }
        .pointerInput(callbacks) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var gesture: DragGesture? = null
                var zooming = false
                var total = Offset.Zero
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    if (pressed >= 2 && gesture == null) {
                        zooming = true
                        val zoom = event.calculateZoom()
                        if (zoom != 1f) callbacks.onZoom(zoom)
                        event.changes.forEach { it.consume() }
                    } else if (!zooming) {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange()
                        total += delta
                        if (gesture == null && total.getDistance() > viewConfiguration.touchSlop) {
                            gesture = dragGestureFor(total, down.position.x, size.width)
                                .also(callbacks.onDragStart)
                        }
                        gesture?.let {
                            val amount = if (it == DragGesture.SEEK) {
                                delta.x / size.width
                            } else {
                                -delta.y / size.height
                            }
                            callbacks.onDrag(it, amount)
                            change.consume()
                        }
                    }
                } while (event.changes.any { it.pressed })
                gesture?.let(callbacks.onDragEnd)
            }
        }
}
