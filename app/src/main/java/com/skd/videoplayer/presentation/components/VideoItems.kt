package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.core.utils.Formatters
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.presentation.theme.ThumbnailScrim

private fun Modifier.clickableIf(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) clickable(onClick = onClick) else this

/**
 * Badges over a thumbnail, each in its own corner so they never compete:
 * NEW top-left, favorite top-right, quality bottom-left, duration bottom-right.
 */
@Composable
private fun BoxScope.ThumbnailOverlays(
    video: Video,
    isFavorite: Boolean,
    nowMs: Long,
    showQuality: Boolean,
    inset: androidx.compose.ui.unit.Dp,
) {
    Box(Modifier.matchParentSize()) {
        if (Formatters.isNew(video.dateAddedEpochMs, nowMs)) {
            StatusPill("NEW", Modifier.align(Alignment.TopStart).padding(inset))
        }
        if (isFavorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(inset - 2.dp))
        if (showQuality) {
            Formatters.qualityLabel(video.width, video.height)?.let {
                ThumbnailBadge(it, Modifier.align(Alignment.BottomStart).padding(inset))
            }
        }
        if (video.durationMs > 0) {
            ThumbnailBadge(
                Formatters.duration(video.durationMs),
                Modifier.align(Alignment.BottomEnd).padding(inset),
            )
        }
    }
}

/** Tile for grid layouts. The three-dot menu sits at the bottom right. */
@Composable
fun VideoGridItem(
    video: Video,
    isFavorite: Boolean,
    nowMs: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onMenuAction: ((VideoMenuAction) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickableIf(onClick)
            .padding(bottom = 2.dp),
    ) {
        Box {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            )
            ThumbnailOverlays(video, isFavorite, nowMs, showQuality = true, inset = 8.dp)
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = 2.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = metadataLine(video, nowMs, includeResolution = false),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            onMenuAction?.let {
                VideoMoreMenu(isFavorite = isFavorite, onAction = it, modifier = Modifier.offset(x = 6.dp, y = (-4).dp))
            }
        }
    }
}

/** `1080p · 1.2 GB · Yesterday`, skipping anything unknown. */
private fun metadataLine(video: Video, nowMs: Long, includeResolution: Boolean): String =
    listOfNotNull(
        if (includeResolution) Formatters.qualityLabel(video.width, video.height) else null,
        Formatters.fileSize(video.sizeBytes).takeIf { video.sizeBytes > 0 },
        Formatters.relativeDate(video.dateAddedEpochMs, nowMs).takeIf { it.isNotEmpty() },
    ).joinToString("  ·  ")

/** Compact row for list layouts. The three-dot menu sits at the bottom right. */
@Composable
fun VideoListItem(
    video: Video,
    isFavorite: Boolean,
    nowMs: Long,
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
            // Quality is in the text beside the thumbnail, so it isn't repeated on the image.
            ThumbnailOverlays(video, isFavorite, nowMs, showQuality = false, inset = 6.dp)
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
                text = metadataLine(video, nowMs, includeResolution = true),
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
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
