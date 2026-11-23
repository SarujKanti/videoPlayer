package com.skd.videoplayer.core.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * When a video was added, in the words people use: `Today`, `Yesterday`, `3 days ago`,
     * `2 weeks ago`, then a plain date such as `12 Mar 2025` beyond a month.
     */
    fun relativeDate(epochMs: Long, nowMs: Long, locale: Locale = Locale.getDefault()): String {
        if (epochMs <= 0) return ""
        val days = ((nowMs - epochMs).coerceAtLeast(0) / DAY_MS).toInt()
        return when {
            days == 0 -> "Today"
            days == 1 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 14 -> "1 week ago"
            days < 31 -> "${days / 7} weeks ago"
            else -> java.text.SimpleDateFormat("d MMM yyyy", locale).format(java.util.Date(epochMs))
        }
    }

    /** Added within the last two days; such videos get a "NEW" marker. */
    fun isNew(epochMs: Long, nowMs: Long): Boolean =
        epochMs > 0 && nowMs - epochMs in 0 until 2 * DAY_MS

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
