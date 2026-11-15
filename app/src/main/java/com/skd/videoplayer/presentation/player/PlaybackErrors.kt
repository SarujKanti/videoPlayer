package com.skd.videoplayer.presentation.player

import androidx.media3.common.PlaybackException

enum class PlaybackErrorKind(val title: String, val message: String) {
    FILE_NOT_FOUND(
        "Video not found",
        "This file was moved or deleted.",
    ),
    NO_PERMISSION(
        "Can't open this video",
        "Lumora isn't allowed to read this file. Open it again from your library or file manager.",
    ),
    UNSUPPORTED_CONTAINER(
        "Format not supported",
        "This file type can't be played on this device.",
    ),
    UNSUPPORTED_CODEC(
        "Codec not supported",
        "This device has no decoder for this video's format or resolution.",
    ),
    DECODER_FAILED(
        "Playback failed",
        "The video decoder stopped unexpectedly. Try again, or switch to another video.",
    ),
    CORRUPT_FILE(
        "File looks damaged",
        "This video couldn't be read. It may be incomplete or corrupted.",
    ),
    EMPTY_QUEUE(
        "Nothing to play",
        "The selected videos are no longer available.",
    ),
    UNKNOWN(
        "Playback failed",
        "Something went wrong while playing this video.",
    ),
    ;

    companion object {
        /** Maps a Media3 [PlaybackException.errorCode] to something the user can act on. */
        fun fromErrorCode(code: Int): PlaybackErrorKind = when (code) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> FILE_NOT_FOUND

            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> NO_PERMISSION

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            -> UNSUPPORTED_CONTAINER

            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            -> UNSUPPORTED_CODEC

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            -> DECODER_FAILED

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            -> CORRUPT_FILE

            else -> UNKNOWN
        }
    }
}
