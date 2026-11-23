package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.skd.videoplayer.domain.model.LibraryLayout
import com.skd.videoplayer.domain.model.SortField
import com.skd.videoplayer.domain.model.VideoSort

@Composable
fun VideoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search videos",
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun LayoutToggleButton(layout: LibraryLayout, onLayoutChange: (LibraryLayout) -> Unit) {
    GlassIconButton(
        onClick = {
            onLayoutChange(if (layout == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID)
        },
    ) {
        if (layout == LibraryLayout.GRID) {
            Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Show as list")
        } else {
            Icon(Icons.Outlined.GridView, contentDescription = "Show as grid")
        }
    }
}

val SortField.label: String
    get() = when (this) {
        SortField.NAME -> "Name"
        SortField.DATE_ADDED -> "Date added"
        SortField.DURATION -> "Duration"
        SortField.SIZE -> "File size"
    }

@Composable
fun SortMenuButton(sort: VideoSort, onSortChange: (VideoSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        GlassIconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort videos")
        }
        GlassDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortField.entries.forEach { field ->
                SortMenuItem(
                    label = field.label,
                    selected = sort.field == field,
                    onClick = {
                        // Names read best A→Z; everything else defaults to largest/newest first.
                        val ascending = if (sort.field == field) sort.ascending else field == SortField.NAME
                        onSortChange(VideoSort(field, ascending))
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            SortMenuItem(
                label = if (sort.field == SortField.NAME) "A to Z" else "Smallest / oldest first",
                selected = sort.ascending,
                onClick = { onSortChange(sort.copy(ascending = true)); expanded = false },
            )
            SortMenuItem(
                label = if (sort.field == SortField.NAME) "Z to A" else "Largest / newest first",
                selected = !sort.ascending,
                onClick = { onSortChange(sort.copy(ascending = false)); expanded = false },
            )
        }
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingIcon = {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        onClick = onClick,
    )
}
