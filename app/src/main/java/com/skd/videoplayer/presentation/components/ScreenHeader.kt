package com.skd.videoplayer.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Title bar shared by every top-level screen so spacing and type match across tabs. When the
 * content underneath is scrolled, the bar takes a tonal surface and a hairline divider so it
 * reads as separate from what slides beneath it.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrolled: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    below: (@Composable () -> Unit)? = null,
) {
    val background by animateColorAsState(
        targetValue = if (scrolled) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.background,
        animationSpec = tween(220),
        label = "headerBackground",
    )
    Column(modifier.fillMaxWidth().background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (navigationIcon == null) 20.dp else 4.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            actions()
        }
        below?.invoke()
        HorizontalDivider(
            color = if (scrolled) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else Color.Transparent,
        )
    }
}
