package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.skd.videoplayer.data.local.VideoThumbnail
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.presentation.theme.ThumbnailScrim

/**
 * Rounded video thumbnail. A film icon sits underneath the image so videos without a
 * thumbnail (or still loading) never show an empty box.
 */
@Composable
fun VideoThumbnailImage(
    video: Video,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    iconSize: Dp = 28.dp,
) {
    val model = remember(video.id, video.uri) { VideoThumbnail(video.id, video.uri) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize),
        )
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Small pill drawn on top of a thumbnail, e.g. the duration or `1080p`. */
@Composable
fun ThumbnailBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ThumbnailScrim)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
