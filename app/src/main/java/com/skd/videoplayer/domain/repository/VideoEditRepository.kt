package com.skd.videoplayer.domain.repository

import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoEditResult

/**
 * Changes video files on the device. When the system needs the user's approval first, the
 * result is [VideoEditResult.ConsentRequired]; call again with `consentGranted = true` once the
 * user has approved.
 */
interface VideoEditRepository {
    /** True when Android shows its own delete confirmation, so the app shouldn't ask twice. */
    val deleteShowsSystemConfirmation: Boolean

    suspend fun delete(video: Video, consentGranted: Boolean = false): VideoEditResult

    /** [newFileName] includes the extension, e.g. `Holiday.mp4`. */
    suspend fun rename(video: Video, newFileName: String, consentGranted: Boolean = false): VideoEditResult
}
