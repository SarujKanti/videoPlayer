package com.skd.videoplayer.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skd.videoplayer.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Where the system splash drew its icon, in window pixels, so the reveal can start from exactly
 * that spot and the hand-over is invisible.
 *
 * Each Android version and device draws the splash icon at its own size, with or without a round
 * backdrop, so the reveal copies what was actually drawn rather than guessing.
 *
 * @param snapshot a copy of the system's icon, or null to draw the logo resource instead.
 * @param visibleRadiusPx how far the visible mark reaches from the centre, used to place the name.
 */
@Immutable
class SplashIconBounds(
    val centerX: Int,
    val centerY: Int,
    val sizePx: Int,
    val snapshot: ImageBitmap?,
    val visibleRadiusPx: Float,
)

/*
 * Timeline, as fractions of TOTAL_MS. One Animatable drives everything, and every value is read
 * inside graphicsLayer / draw lambdas, so the animation never recomposes: each frame is only a
 * redraw of a handful of layers.
 */
private const val TOTAL_MS = 1_250
private val LOGO = 0.00f..0.36f
private const val LETTERS_START = 0.16f
private const val LETTER_STAGGER = 0.045f
private const val LETTER_LENGTH = 0.28f
private val TAGLINE = 0.46f..0.70f
private val SHIMMER = 0.50f..0.82f
private val EXIT = 0.84f..1.00f

/** Material's emphasized-decelerate curve: quick start, long soft landing. */
private val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Progress of [range] at overall progress [p], clamped to 0..1 and eased. */
private fun phase(p: Float, range: ClosedFloatingPointRange<Float>, easing: Easing = EmphasizedDecelerate): Float =
    easing.transform(((p - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f))

private val LogoLift = 64.dp
private const val LOGO_END_SCALE = 0.8f
private val LetterRise = 14.dp

/**
 * Brand intro that continues the system splash: the logo glides up and settles, "Lumora" rises in
 * letter by letter, a light sweep crosses the name, then the whole layer fades into the app.
 * Tapping skips straight to the fade.
 *
 * @param onFirstFrame called once this overlay is on screen, so the system splash can be removed
 *   underneath it without a flicker.
 */
@Composable
fun BrandReveal(
    start: SplashIconBounds,
    onFirstFrame: () -> Unit,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val firstFrame by rememberUpdatedState(onFirstFrame)
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        // Two frames: the first composes and draws this layer, the second confirms it's shown.
        withFrameNanos { }
        withFrameNanos { }
        firstFrame()
        progress.animateTo(1f, tween(TOTAL_MS, easing = LinearEasing))
        finished()
    }

    val background = colorResource(R.color.splash_background)
    val accent = colorResource(R.color.splash_icon_accent)
    val dark = background.luminance() < 0.5f

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
    }
}

private val WordmarkStyle = TextStyle(
    fontSize = 42.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
)

private val TaglineStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 3.sp,
)
