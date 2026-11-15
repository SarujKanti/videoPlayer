package com.skd.videoplayer.core.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    /** `1:05:09` for durations of an hour or more, otherwise `5:09`. */
    fun duration(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0) / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** Binary units (1 KB = 1024 B), one decimal place above KB. */
    fun fileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes < 1024) return "${bytes.coerceAtLeast(0)} B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
        val value = bytes / 1024.0.pow(exponent)
        val unit = units[exponent - 1]
        return if (exponent == 1) {
            String.format(locale, "%.0f %s", value, unit)
        } else {
            String.format(locale, "%.1f %s", value, unit)
        }
    }

    /** Short quality label based on the shorter side, e.g. `1080p` or `4K`; null when unknown. */
    fun qualityLabel(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val shortSide = minOf(width, height)
        return when {
            shortSide >= 4320 -> "8K"
            shortSide >= 2160 -> "4K"
            shortSide >= 1440 -> "1440p"
            shortSide >= 1080 -> "1080p"
            shortSide >= 720 -> "720p"
            shortSide >= 480 -> "480p"
            else -> "${shortSide}p"
        }
    }

    /** `1920 × 1080`; null when unknown. */
    fun resolution(width: Int, height: Int): String? =
        if (width > 0 && height > 0) "$width × $height" else null
}
