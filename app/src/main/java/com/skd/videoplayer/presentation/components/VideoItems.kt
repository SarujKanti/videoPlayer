package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.core.utils.Formatters
import com.skd.videoplayer.domain.model.Video

private fun Modifier.clickableIf(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(onClick = onClick) else this

/** Card-style tile for grid layouts. */
@Composable
fun VideoGridItem(
    video: Video,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clickableIf(onClick)
            .padding(bottom = 4.dp),
    ) {
        Box {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            )
            Formatters.qualityLabel(video.width, video.height)?.let {
                ThumbnailBadge(it, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            if (video.durationMs > 0) {
                ThumbnailBadge(
                    Formatters.duration(video.durationMs),
                    Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            text = Formatters.fileSize(video.sizeBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}

/** Compact row for list layouts. */
@Composable
fun VideoListItem(
    video: Video,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickableIf(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            VideoThumbnailImage(
                video = video,
                cornerRadius = 12.dp,
                iconSize = 22.dp,
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(16f / 10f),
            )
            if (video.durationMs > 0) {
                ThumbnailBadge(
                    Formatters.duration(video.durationMs),
                    Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    Formatters.fileSize(video.sizeBytes),
                    Formatters.resolution(video.width, video.height),
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = video.folderName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
