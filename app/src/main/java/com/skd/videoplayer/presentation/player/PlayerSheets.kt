package com.skd.videoplayer.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.skd.videoplayer.presentation.components.glass
import com.skd.videoplayer.presentation.components.glassColors
import androidx.compose.ui.unit.dp

enum class PlayerSheet(val title: String) {
    SPEED("Playback speed"),
    AUDIO("Audio track"),
    SUBTITLES("Subtitles"),
}

private data class SheetRow(val label: String, val selected: Boolean, val enabled: Boolean, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsSheet(
    sheet: PlayerSheet,
    state: PlayerUiState,
    onSpeed: (Float) -> Unit,
    onAudioTrack: (TrackOption) -> Unit,
    onSubtitleTrack: (TrackOption?) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows: List<SheetRow> = when (sheet) {
        PlayerSheet.SPEED -> PlaybackSpeeds.map { speed ->
            SheetRow(speedLabel(speed) + if (speed == 1f) "  (Normal)" else "", state.speed == speed, true) {
                onSpeed(speed)
            }
        }
        PlayerSheet.AUDIO -> state.audioTracks.map { track ->
            SheetRow(track.label.unsupportedSuffix(track), track.isSelected, track.isSupported) { onAudioTrack(track) }
        }
        PlayerSheet.SUBTITLES -> listOf(SheetRow("Off", state.subtitlesOff, true) { onSubtitleTrack(null) }) +
            state.subtitleTracks.map { track ->
                SheetRow(
                    track.label.unsupportedSuffix(track),
                    track.isSelected && !state.subtitlesOff,
                    track.isSupported,
                ) { onSubtitleTrack(track) }
            }
    }

    // Sheets sit over moving video with no blur, so the frost is kept mostly opaque.
    val sheetColors = glassColors(solid = true).let {
        it.copy(fillTop = it.fillTop.copy(alpha = 0.86f), fillBottom = it.fillBottom.copy(alpha = 0.86f))
    }
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = sheetShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.glass(sheetShape, sheetColors),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                sheet.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = row.enabled) {
                            row.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            !row.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            row.selected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (row.selected) {
                        Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun String.unsupportedSuffix(track: TrackOption): String =
    if (track.isSupported) this else "$this  (not supported)"
