package com.skd.videoplayer.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.domain.model.LibraryLayout

/**
 * Placeholder grid or list shown while the library loads. It mirrors the real layout so the
 * screen doesn't jump when videos arrive, and a soft shimmer shows that work is happening.
 */
@Composable
fun VideoSkeleton(layout: LibraryLayout, modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    when (layout) {
        LibraryLayout.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            userScrollEnabled = false,
            modifier = modifier.fillMaxSize(),
        ) {
            items(8) {
                Column {
                    Block(brush, Modifier.fillMaxWidth().aspectRatio(16f / 10f), radius = 14.dp)
                    Spacer(Modifier.height(10.dp))
                    Block(brush, Modifier.fillMaxWidth(0.85f).height(12.dp))
                    Spacer(Modifier.height(6.dp))
                    Block(brush, Modifier.fillMaxWidth(0.5f).height(10.dp))
                }
            }
        }
        LibraryLayout.LIST -> LazyColumn(
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            userScrollEnabled = false,
            modifier = modifier.fillMaxSize(),
        ) {
            items(8) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Block(brush, Modifier.width(132.dp).aspectRatio(16f / 10f), radius = 12.dp)
                    Column(Modifier.padding(start = 14.dp, top = 6.dp)) {
                        Block(brush, Modifier.fillMaxWidth(0.9f).height(12.dp))
                        Spacer(Modifier.height(8.dp))
                        Block(brush, Modifier.fillMaxWidth(0.6f).height(10.dp))
                        Spacer(Modifier.height(8.dp))
                        Block(brush, Modifier.fillMaxWidth(0.4f).height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Block(brush: Brush, modifier: Modifier, radius: Dp = 6.dp) {
    Spacer(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(brush),
    )
}

@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainer
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1_600f,
        animationSpec = infiniteRepeatable(tween(1_300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerShift",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shift, 0f),
        end = Offset(shift + 600f, 300f),
    )
}
