package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.core.utils.Formatters
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.presentation.theme.ThumbnailScrim

private fun Modifier.clickableIf(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(onClick = onClick) else this

/** Card-style tile for grid layouts. The three-dot menu sits at the bottom right. */
@Composable
fun VideoGridItem(
    video: Video,
    isFavorite: Boolean,
    isNew: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
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
            // NEW takes the top-start corner; the quality badge sits beside it instead of under it.
            val quality = Formatters.qualityLabel(video.width, video.height)
            if (isNew || quality != null) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isNew) NewBadge()
                    quality?.let { ThumbnailBadge(it) }
                }
            }
            if (isFavorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (video.durationMs > 0) {
                ThumbnailBadge(
                    Formatters.duration(video.durationMs),
                    Modifier.align(Alignment.BottomEnd).padding(8.dp),
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 2.dp, bottom = 2.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.fileSize(video.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            onMenuAction?.let { VideoMoreMenu(isFavorite = isFavorite, onAction = it) }
        }
    }
}

/** Compact row for list layouts. The three-dot menu sits at the bottom right. */
@Composable
fun VideoListItem(
    video: Video,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickableIf(onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
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
            if (isFavorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
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
        onMenuAction?.let {
            VideoMoreMenu(
                isFavorite = isFavorite,
                onAction = it,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}

/** Three-dot button with favorite, rename and delete. */
@Composable
fun VideoMoreMenu(
    isFavorite: Boolean,
    onAction: (VideoMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        GlassIconButton(onClick = { expanded = true }, size = 32.dp) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        GlassDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fun pick(action: VideoMenuAction) {
                expanded = false
                onAction(action)
            }
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = { pick(VideoMenuAction.TOGGLE_FAVORITE) },
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                onClick = { pick(VideoMenuAction.RENAME) },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = { pick(VideoMenuAction.DELETE) },
            )
        }
    }
}

@Composable
private fun FavoriteBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(ThumbnailScrim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.primary.takeIf { it != Color.Unspecified } ?: Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}
