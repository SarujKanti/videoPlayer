package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skd.videoplayer.presentation.theme.NewBadgeRed

/** Small red "NEW" pill shown on thumbnails of videos that were never played. */
@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        text = "NEW",
        style = TextStyle(
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        ),
        color = Color.White,
        modifier = modifier
            .semantics { contentDescription = "New, not played yet" }
            .clip(RoundedCornerShape(6.dp))
            .background(NewBadgeRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
