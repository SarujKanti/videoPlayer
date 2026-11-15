package com.skd.videoplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object FoldersRoute

/** Property names are read back by [com.skd.videoplayer.presentation.folders.FolderVideosViewModel]. */
@Serializable
data class FolderVideosRoute(val folderId: String, val folderName: String)

/** Destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LIBRARY(LibraryRoute, "Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    FOLDERS(FoldersRoute, "Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
}
