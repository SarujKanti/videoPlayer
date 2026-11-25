package com.skd.videoplayer.presentation.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import java.util.function.Consumer

/*
 * Lumora's frosted-glass look: a translucent tinted fill, a soft diagonal sheen and a hairline
 * border that is brighter at the top, like light catching the edge of a pane. Dialogs also blur
 * the screen behind them on Android 12+ devices that allow it.
 */

/** What the glass sits on, which decides how it is tinted. */
enum class GlassTone {
    /** On the app's own background; follows the light or dark theme. */
    Surface,

    /** Over video or photos; always smoked glass so white icons stay readable. */
    OnMedia,
}

@Immutable
data class GlassColors(
    val fillTop: Color,
    val fillBottom: Color,
    val sheen: Color,
    val borderTop: Color,
    val borderBottom: Color,
)

/**
 * @param solid makes the fill nearly opaque. Used when nothing behind the glass is blurred
 *   (menus, or dialogs on devices without blur), where see-through text would be hard to read.
 */
@Composable
fun glassColors(tone: GlassTone = GlassTone.Surface, solid: Boolean = false): GlassColors {
    if (tone == GlassTone.OnMedia) {
        // Smoked glass: dark enough for white icons on bright scenes, with a bright rim and sheen.
        return GlassColors(
            fillTop = Color.Black.copy(alpha = 0.28f),
            fillBottom = Color.Black.copy(alpha = 0.46f),
            sheen = Color.White.copy(alpha = 0.18f),
            borderTop = Color.White.copy(alpha = 0.45f),
            borderBottom = Color.White.copy(alpha = 0.10f),
        )
    }
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val alpha = if (solid) 0.94f else if (dark) 0.62f else 0.72f
    return if (dark) {
        GlassColors(
            fillTop = scheme.surfaceContainerHighest.copy(alpha = alpha),
            fillBottom = scheme.surfaceContainer.copy(alpha = alpha),
            sheen = Color.White.copy(alpha = 0.07f),
            borderTop = Color.White.copy(alpha = 0.24f),
            borderBottom = Color.White.copy(alpha = 0.05f),
        )
    } else {
        GlassColors(
            fillTop = Color.White.copy(alpha = alpha),
            // A slightly darker, warm bottom gives the pane depth against the off-white page.
            fillBottom = scheme.surfaceContainerHigh.copy(alpha = alpha),
            sheen = Color.White.copy(alpha = 0.6f),
            borderTop = Color.White,
            borderBottom = Color.Black.copy(alpha = 0.12f),
        )
    }
}

fun GlassColors.borderBrush(): Brush = Brush.verticalGradient(listOf(borderTop, borderBottom))

/** Paints the glass fill, sheen and edge inside [shape]. */
fun Modifier.glass(
    shape: Shape,
    colors: GlassColors,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(colors.fillTop, colors.fillBottom)))
    .background(
        Brush.linearGradient(
            0f to colors.sheen,
            0.45f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(600f, 600f),
        ),
    )
    .border(borderWidth, colors.borderBrush(), shape)

/** A round glass button for a single icon. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    tone: GlassTone = GlassTone.Surface,
    contentColor: Color = if (tone == GlassTone.OnMedia) Color.White else MaterialTheme.colorScheme.onSurface,
    content: @Composable BoxScope.() -> Unit,
) {
    // Small buttons keep a 48dp touch target around a smaller glass circle.
    Box(
        modifier = modifier
            .size(maxOf(size, MinTouchTarget))
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.4f)
                .glassShadow(CircleShape, tone)
                .glass(CircleShape, glassColors(tone)),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
        }
    }
}

private val MinTouchTarget = 48.dp

/**
 * On light backgrounds white glass needs a soft shadow to lift it off the page. Over video and
 * in dark mode the edge highlight is enough, and a shadow would show through the clear fill.
 */
@Composable
private fun Modifier.glassShadow(shape: Shape, tone: GlassTone): Modifier {
    val light = tone == GlassTone.Surface && MaterialTheme.colorScheme.background.luminance() >= 0.5f
    return if (light) {
        shadow(4.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.18f))
    } else {
        this
    }
}

/** A non-interactive glass tile behind an icon, e.g. in settings rows and empty states. */
@Composable
fun GlassIconBadge(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = glassColors().let { base ->
        if (tint == null) base else base.copy(fillTop = tint.copy(alpha = 0.28f), fillBottom = tint.copy(alpha = 0.10f))
    }
    Box(modifier.glassShadow(shape, GlassTone.Surface).glass(shape, colors), contentAlignment = Alignment.Center, content = content)
}

/** Drop-down menu with a frosted panel. Popups can't blur what's behind them, so it stays solid. */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = glassColors(solid = true)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        containerColor = colors.fillTop,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.borderBrush()),
        content = content,
    )
}

/**
 * Glass replacement for Material's AlertDialog, with the same slots. The screen behind is
 * blurred where the device supports it; otherwise the panel turns more opaque and the dim deeper.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val blurred = rememberBackdropBlur()
        val colors = glassColors(solid = !blurred)
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .glass(shape, colors)
                .padding(24.dp),
            horizontalAlignment = if (icon != null) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            if (icon != null) {
                GlassIconBadge(
                    shape = CircleShape,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) { icon() }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (title != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    ProvideTextStyle(
                        MaterialTheme.typography.headlineSmall.copy(
                            textAlign = if (icon != null) TextAlign.Center else TextAlign.Start,
                        ),
                    ) { title() }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (text != null) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Box(Modifier.fillMaxWidth()) { text() }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

/**
 * Blurs everything behind the current dialog window on Android 12+. Returns whether the blur is
 * actually showing: the user or battery saver can turn cross-window blur off at any time.
 */
@Composable
private fun rememberBackdropBlur(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return false
    val windowManager = remember(view) { view.context.getSystemService(WindowManager::class.java) }
    val radiusPx = with(LocalDensity.current) { BACKDROP_BLUR.roundToPx() }
    var enabled by remember { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }

    DisposableEffect(window, windowManager) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = radiusPx }
        val listener = Consumer<Boolean> { on ->
            enabled = on
            window.setDimAmount(if (on) DIM_WITH_BLUR else DIM_WITHOUT_BLUR)
        }
        // Called straight away with the current state, then on every change.
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

private val BACKDROP_BLUR = 24.dp
private const val DIM_WITH_BLUR = 0.25f
private const val DIM_WITHOUT_BLUR = 0.55f
