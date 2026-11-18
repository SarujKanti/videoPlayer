package com.skd.videoplayer.domain.model

/**
 * A system confirmation the user has to approve before an edit can happen, such as Android's
 * "Allow Lumora to delete this video?" dialog. The platform-specific payload lives in the
 * implementation so the domain layer stays free of Android types.
 */
interface UserConsentRequest

enum class VideoEditError {
    /** Another file in the same folder already has that name. */
    NAME_TAKEN,

    /** The video no longer exists. */
    NOT_FOUND,

    /** The system or the user refused write access. */
    NOT_ALLOWED,
    UNKNOWN,
}

sealed interface VideoEditResult {
    data object Success : VideoEditResult
    data class ConsentRequired(val request: UserConsentRequest) : VideoEditResult
    data class Failure(val error: VideoEditError) : VideoEditResult
}
