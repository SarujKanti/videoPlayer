package com.skd.videoplayer.domain.usecase

import com.skd.videoplayer.testutil.testVideo
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateVideoNameUseCaseTest {

    private val validate = ValidateVideoNameUseCase()
    private val video = testVideo(1, title = "Holiday") // displayName = Holiday.mp4

    @Test
    fun `valid name keeps the original extension and is trimmed`() {
        assertEquals(NameValidation.Valid("Beach day.mp4"), validate(video, "  Beach day "))
    }

    @Test
    fun `blank, unchanged and illegal names are rejected`() {
        assertEquals(NameValidation.Invalid(NameProblem.BLANK), validate(video, "  "))
        assertEquals(NameValidation.Invalid(NameProblem.UNCHANGED), validate(video, "Holiday"))
        assertEquals(NameValidation.Invalid(NameProblem.INVALID_CHARACTERS), validate(video, "a/b"))
        assertEquals(NameValidation.Invalid(NameProblem.INVALID_CHARACTERS), validate(video, "what?"))
        assertEquals(NameValidation.Invalid(NameProblem.INVALID_CHARACTERS), validate(video, ".hidden"))
    }

    @Test
    fun `length limit counts bytes, not characters`() {
        // 85 three-byte characters = 255 bytes, plus ".mp4" goes over the limit.
        assertEquals(NameValidation.Invalid(NameProblem.TOO_LONG), validate(video, "語".repeat(85)))
        assertEquals(NameValidation.Valid("${"a".repeat(251)}.mp4"), validate(video, "a".repeat(251)))
    }

    @Test
    fun `base name and file name helpers`() {
        assertEquals("Holiday.final", ValidateVideoNameUseCase.baseName("Holiday.final.mkv"))
        assertEquals("noext", ValidateVideoNameUseCase.baseName("noext"))
        assertEquals("new.mkv", ValidateVideoNameUseCase.fileNameFor("new", "old.mkv"))
        assertEquals("new", ValidateVideoNameUseCase.fileNameFor("new", "old"))
    }
}
