package com.skd.videoplayer.presentation.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.skd.videoplayer.core.common.AndroidConsentRequest
import com.skd.videoplayer.domain.model.VideoEditError
import com.skd.videoplayer.domain.usecase.NameProblem
import com.skd.videoplayer.domain.usecase.ValidateVideoNameUseCase

/**
 * Shows the rename and delete dialogs, launches Android's own confirmation when an edit needs
 * it, and turns results into snackbar messages. Place once per screen that uses [VideoListViewModel].
 */
@Composable
fun VideoActionsHost(
    viewModel: VideoListViewModel,
    dialog: VideoDialog?,
    snackbarHostState: SnackbarHostState,
) {
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is VideoListEvent.ShowMessage -> {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(event.message.text)
                    }
                    is VideoListEvent.RequestConsent -> {
                        val request = event.request as? AndroidConsentRequest
                        if (request == null) {
                            viewModel.onConsentResult(false)
                        } else {
                            consentLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        is VideoDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text("Delete video?") },
            text = { Text("“${dialog.video.displayName}” will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } },
        )
        is VideoDialog.Rename -> RenameDialog(
            dialog = dialog,
            onSubmit = viewModel::submitRename,
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}

@Composable
private fun RenameDialog(
    dialog: VideoDialog.Rename,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(dialog.video.id) { ValidateVideoNameUseCase.baseName(dialog.video.displayName) }
    // Start with the whole name selected so typing replaces it.
    var value by remember(dialog.video.id) {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length)))
    }
    val extension = dialog.video.displayName.substringAfterLast('.', missingDelimiterValue = "")
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename video") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                suffix = extension.takeIf { it.isNotEmpty() }?.let { ext -> @Composable { Text(".$ext") } },
                isError = dialog.problem != null,
                supportingText = dialog.problem?.let { problem -> @Composable { Text(problem.text) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit(value.text) }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = { TextButton(onClick = { onSubmit(value.text) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val NameProblem.text: String
    get() = when (this) {
        NameProblem.BLANK -> "Enter a name"
        NameProblem.INVALID_CHARACTERS -> "Names can't start with a dot or contain / \\ : * ? \" < > |"
        NameProblem.TOO_LONG -> "That name is too long"
        NameProblem.UNCHANGED -> "That's already the name"
    }

private val VideoListMessage.text: String
    get() = when (this) {
        VideoListMessage.AddedToFavorites -> "Added to favorites"
        VideoListMessage.RemovedFromFavorites -> "Removed from favorites"
        VideoListMessage.Deleted -> "Video deleted"
        is VideoListMessage.Renamed -> "Renamed to $fileName"
        is VideoListMessage.EditFailed -> when (error) {
            VideoEditError.NAME_TAKEN -> "A video with that name already exists in this folder"
            VideoEditError.NOT_FOUND -> "That video no longer exists"
            VideoEditError.NOT_ALLOWED -> "Lumora isn't allowed to change this video"
            VideoEditError.UNKNOWN -> "Couldn't change this video"
        }
    }
