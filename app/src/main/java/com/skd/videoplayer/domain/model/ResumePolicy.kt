package com.skd.videoplayer.domain.model

/** Decides when a stopped position is worth offering to resume. */
object ResumePolicy {
    /** Stopping within the first few seconds means the user barely started; don't ask. */
    const val MIN_POSITION_MS = 5_000L

    /** Stopping this close to the end counts as finished, e.g. during the credits. */
    const val END_MARGIN_MS = 10_000L

    fun isResumable(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs < MIN_POSITION_MS) return false
        // Unknown length (some streams and broken files): any position past the start counts.
        if (durationMs <= 0) return true
        return positionMs < durationMs - END_MARGIN_MS
    }
}
