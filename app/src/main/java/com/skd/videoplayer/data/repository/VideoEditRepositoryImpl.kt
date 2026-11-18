package com.skd.videoplayer.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.skd.videoplayer.core.common.AndroidConsentRequest
import com.skd.videoplayer.di.IoDispatcher
import com.skd.videoplayer.domain.model.Video
import com.skd.videoplayer.domain.model.VideoEditError
import com.skd.videoplayer.domain.model.VideoEditResult
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes and renames videos through MediaStore, following each Android version's rules:
 * - 11+: system delete/write requests, which show Android's own confirmation dialog.
 * - 10: direct calls; files owned by other apps throw [RecoverableSecurityException], whose
 *   dialog grants access for a retry.
 * - 9 and below: direct file access, covered by the legacy storage permission.
 */
@Singleton
class VideoEditRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val videoRepository: VideoRepository,
    private val favoritesRepository: FavoritesRepository,
) : VideoEditRepository {

    private val resolver: ContentResolver get() = context.contentResolver
    private val sdk = Build.VERSION.SDK_INT

    override val deleteShowsSystemConfirmation: Boolean = sdk >= Build.VERSION_CODES.R

    override suspend fun delete(video: Video, consentGranted: Boolean): VideoEditResult =
        withContext(ioDispatcher) {
            val uri = Uri.parse(video.uri)
            val result = when {
                // Android performed the delete itself when the user approved its dialog.
                sdk >= Build.VERSION_CODES.R && consentGranted -> VideoEditResult.Success
                sdk >= Build.VERSION_CODES.R -> consent(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
                else -> attemptWrite(uri) {
                    if (resolver.delete(uri, null, null) > 0) {
                        VideoEditResult.Success
                    } else {
                        VideoEditResult.Failure(VideoEditError.NOT_FOUND)
                    }
                }
            }
            if (result == VideoEditResult.Success) {
                favoritesRepository.remove(video.id)
                videoRepository.refresh()
            }
            result
        }

    override suspend fun rename(video: Video, newFileName: String, consentGranted: Boolean): VideoEditResult =
        withContext(ioDispatcher) {
            val uri = Uri.parse(video.uri)
            val result = when {
                nameTaken(video, newFileName) -> VideoEditResult.Failure(VideoEditError.NAME_TAKEN)
                sdk < Build.VERSION_CODES.Q -> renameLegacy(uri, newFileName)
                else -> attemptWrite(uri) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
                        put(MediaStore.Video.Media.TITLE, ValidateVideoNameUseCase.baseName(newFileName))
                    }
                    if (resolver.update(uri, values, null, null) > 0) {
                        VideoEditResult.Success
                    } else {
                        VideoEditResult.Failure(VideoEditError.NOT_FOUND)
                    }
                }
            }
            if (result == VideoEditResult.Success) videoRepository.refresh()
            result
        }

    /** Runs a MediaStore write, turning "you need permission" errors into a consent request. */
    private inline fun attemptWrite(uri: Uri, write: () -> VideoEditResult): VideoEditResult =
        try {
            write()
        } catch (e: SecurityException) {
            when {
                sdk >= Build.VERSION_CODES.R ->
                    consent(MediaStore.createWriteRequest(resolver, listOf(uri)).intentSender)
                sdk >= Build.VERSION_CODES.Q && e is RecoverableSecurityException ->
                    consent(e.userAction.actionIntent.intentSender)
                else -> VideoEditResult.Failure(VideoEditError.NOT_ALLOWED)
            }
        } catch (e: IllegalStateException) {
            // MediaProvider reports name clashes and missing files this way on some versions.
            VideoEditResult.Failure(VideoEditError.UNKNOWN)
        } catch (e: IllegalArgumentException) {
            VideoEditResult.Failure(VideoEditError.UNKNOWN)
        }

    private fun consent(sender: IntentSender) =
        VideoEditResult.ConsentRequired(AndroidConsentRequest(sender))

    /** Another video with the same name in the same folder would make the rename fail or clobber it. */
    private fun nameTaken(video: Video, newFileName: String): Boolean {
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ? AND " +
            "${MediaStore.Video.Media.BUCKET_ID} = ? AND ${MediaStore.Video.Media._ID} != ?"
        val args = arrayOf(newFileName, video.folderId, video.id.toString())
        val collection = Uri.parse(video.uri.substringBeforeLast('/'))
        return runCatching {
            resolver.query(collection, arrayOf(MediaStore.Video.Media._ID), selection, args, null)
                ?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION") // DATA is the only way to reach the file below Android 10.
    private fun renameLegacy(uri: Uri, newFileName: String): VideoEditResult {
        val path = resolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: return VideoEditResult.Failure(VideoEditError.NOT_FOUND)
        val source = File(path)
        val target = File(source.parentFile, newFileName)
        if (target.exists()) return VideoEditResult.Failure(VideoEditError.NAME_TAKEN)
        if (!source.renameTo(target)) return VideoEditResult.Failure(VideoEditError.NOT_ALLOWED)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DATA, target.absolutePath)
            put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
            put(MediaStore.Video.Media.TITLE, ValidateVideoNameUseCase.baseName(newFileName))
        }
        resolver.update(uri, values, null, null)
        return VideoEditResult.Success
    }
}
