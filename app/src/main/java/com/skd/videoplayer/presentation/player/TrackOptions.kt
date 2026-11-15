package com.skd.videoplayer.presentation.player

import androidx.compose.runtime.Immutable
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import java.util.Locale

/** One selectable audio or subtitle track. Indices point into [Tracks.getGroups]. */
@Immutable
data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean,
    val isSupported: Boolean,
)

object TrackOptions {

    fun from(tracks: Tracks, trackType: Int): List<TrackOption> {
        val options = mutableListOf<TrackOption>()
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != trackType) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                options += TrackOption(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    label = label(group.getTrackFormat(trackIndex), options.size + 1),
                    isSelected = group.isTrackSelected(trackIndex),
                    isSupported = group.isTrackSupported(trackIndex),
                )
            }
        }
        return options
    }

    /** True when the file has tracks of [trackType] but this device can decode none of them. */
    fun hasOnlyUnsupported(tracks: Tracks, trackType: Int): Boolean =
        tracks.containsType(trackType) && !tracks.isTypeSupported(trackType)

    /** e.g. `English · 5.1 · AC3`, falling back to `Track 2` when the file has no metadata. */
    fun label(format: Format, number: Int, displayLocale: Locale = Locale.getDefault()): String {
        val name = format.label?.takeIf { it.isNotBlank() }
            ?: languageName(format.language, displayLocale)
            ?: "Track $number"
        val details = listOfNotNull(
            channelLabel(format.channelCount),
            codecLabel(format.sampleMimeType),
        ).filterNot { name.contains(it, ignoreCase = true) }
        return (listOf(name) + details).joinToString("  ·  ")
    }

    private fun languageName(code: String?, displayLocale: Locale): String? {
        if (code.isNullOrBlank() || code == "und" || code == "mul") return null
        val name = Locale.forLanguageTag(code).getDisplayLanguage(displayLocale)
        return name.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
            ?.replaceFirstChar { it.titlecase(displayLocale) }
            ?: code
    }

    private fun channelLabel(channels: Int): String? = when (channels) {
        Format.NO_VALUE, 0 -> null
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$channels ch"
    }

    private fun codecLabel(mimeType: String?): String? = when (mimeType) {
        null -> null
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_AC3 -> "AC3"
        MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC3"
        MimeTypes.AUDIO_AC4 -> "AC4"
        MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
        MimeTypes.AUDIO_TRUEHD -> "TrueHD"
        MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_MPEG_L2 -> "MP3"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_RAW -> "PCM"
        MimeTypes.APPLICATION_SUBRIP -> "SRT"
        MimeTypes.TEXT_SSA -> "SSA"
        MimeTypes.TEXT_VTT -> "WebVTT"
        MimeTypes.APPLICATION_TTML -> "TTML"
        MimeTypes.APPLICATION_PGS -> "PGS"
        MimeTypes.APPLICATION_VOBSUB -> "VobSub"
        MimeTypes.APPLICATION_CEA608, MimeTypes.APPLICATION_CEA708 -> "CC"
        MimeTypes.APPLICATION_DVBSUBS -> "DVB"
        else -> null
    }
}
