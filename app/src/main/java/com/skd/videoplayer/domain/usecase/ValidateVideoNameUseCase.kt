package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.domain.model.Video
import javax.inject.Inject

enum class NameProblem { BLANK, INVALID_CHARACTERS, TOO_LONG, UNCHANGED }

sealed interface NameValidation {
    /** [fileName] is the full new file name, with the original extension kept. */
    data class Valid(val fileName: String) : NameValidation
    data class Invalid(val problem: NameProblem) : NameValidation
}

/**
 * Checks a new name typed by the user. The user edits only the base name; the original
 * extension is always kept so the file stays playable.
 */
class ValidateVideoNameUseCase @Inject constructor() {

    operator fun invoke(video: Video, input: String): NameValidation {
        val base = input.trim()
        val fileName = fileNameFor(base, video.displayName)
        return when {
            base.isEmpty() -> NameValidation.Invalid(NameProblem.BLANK)
            base.startsWith('.') || base.any { it in FORBIDDEN || it.isISOControl() } ->
                NameValidation.Invalid(NameProblem.INVALID_CHARACTERS)
            // File systems limit names to 255 bytes, not characters.
            fileName.toByteArray(Charsets.UTF_8).size > MAX_FILE_NAME_BYTES ->
                NameValidation.Invalid(NameProblem.TOO_LONG)
            fileName == video.displayName -> NameValidation.Invalid(NameProblem.UNCHANGED)
            else -> NameValidation.Valid(fileName)
        }
    }

    companion object {
        private const val FORBIDDEN = "/\\:*?\"<>|"
        private const val MAX_FILE_NAME_BYTES = 255

        /** The editable part of a file name: `Holiday.final.mp4` → `Holiday.final`. */
        fun baseName(displayName: String): String =
            if ('.' in displayName) displayName.substringBeforeLast('.') else displayName

        fun fileNameFor(base: String, originalDisplayName: String): String {
            val extension = originalDisplayName.substringAfterLast('.', missingDelimiterValue = "")
            return if (extension.isEmpty()) base else "$base.$extension"
        }
    }
}
