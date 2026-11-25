package com.skd.videoplayer.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.skd.videoplayer.core.utils.Formatters
import com.skd.videoplayer.presentation.components.GlassDialog

/**
 * Asks whether to pick the video up where it was left or play it from the start. Tapping
 * outside or pressing back also continues, since that is what the user most likely wants.
 */
@Composable
fun ResumeDialog(
    prompt: ResumePrompt,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
) {
    val position = Formatters.duration(prompt.positionMs)
    GlassDialog(
        onDismissRequest = onResume,
        properties = DialogProperties(dismissOnClickOutside = false),
        icon = { Icon(Icons.Outlined.History, contentDescription = null) },
        title = { Text("Continue watching?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (prompt.durationMs > 0) {
                        "You stopped at $position of ${Formatters.duration(prompt.durationMs)}."
                    } else {
                        "You stopped at $position."
                    },
                )
                if (prompt.durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (prompt.positionMs.toFloat() / prompt.durationMs).coerceIn(0f, 1f) },
                        strokeCap = StrokeCap.Round,
                        trackColor = LocalContentColor.current.copy(alpha = 0.18f),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onResume, shape = RoundedCornerShape(12.dp)) { Text("Continue") }
        },
        dismissButton = {
            OutlinedButton(onClick = onStartOver, shape = RoundedCornerShape(12.dp)) { Text("Start over") }
        },
    )
}
