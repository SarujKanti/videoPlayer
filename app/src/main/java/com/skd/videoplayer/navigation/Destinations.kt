package com.skd.videoplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object FoldersRoute

@Serializable
data object FavoritesRoute

@Serializable
data object SettingsRoute

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
    FAVORITES(FavoritesRoute, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    SETTINGS(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}
