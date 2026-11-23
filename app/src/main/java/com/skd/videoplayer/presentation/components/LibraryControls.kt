package com.skd.videoplayer.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
            .height(50.dp),
        placeholder = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
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
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * The row under the search field: how many videos are shown on the left, the current sort
 * as a visible chip and the grid/list switch on the right. Making the sort visible means the
 * user always knows why the list is in this order.
 */
@Composable
fun ListToolbar(
    countLabel: String,
    sort: VideoSort,
    onSortChange: (VideoSort) -> Unit,
    layout: LibraryLayout,
    onLayoutChange: (LibraryLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SortChip(sort, onSortChange)
        Spacer(Modifier.width(8.dp))
        LayoutSwitch(layout, onLayoutChange)
    }
}

/** Sort chip and layout switch share one height so the toolbar row lines up. */
private val ToolbarControlHeight = 36.dp

val SortField.label: String
    get() = when (this) {
        SortField.NAME -> "Name"
        SortField.DATE_ADDED -> "Date added"
        SortField.DURATION -> "Duration"
        SortField.SIZE -> "File size"
    }

@Composable
private fun SortChip(sort: VideoSort, onSortChange: (VideoSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.semantics { contentDescription = "Sort by ${sort.field.label}, ${directionLabel(sort)}" },
        ) {
            Row(
                modifier = Modifier.height(ToolbarControlHeight).padding(start = 12.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(sort.field.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (sort.ascending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
                label = ascendingLabel(sort.field),
                selected = sort.ascending,
                onClick = { onSortChange(sort.copy(ascending = true)); expanded = false },
            )
            SortMenuItem(
                label = descendingLabel(sort.field),
                selected = !sort.ascending,
                onClick = { onSortChange(sort.copy(ascending = false)); expanded = false },
            )
        }
    }
}

private fun ascendingLabel(field: SortField) = when (field) {
    SortField.NAME -> "A to Z"
    SortField.DATE_ADDED -> "Oldest first"
    SortField.DURATION -> "Shortest first"
    SortField.SIZE -> "Smallest first"
}

private fun descendingLabel(field: SortField) = when (field) {
    SortField.NAME -> "Z to A"
    SortField.DATE_ADDED -> "Newest first"
    SortField.DURATION -> "Longest first"
    SortField.SIZE -> "Largest first"
}

private fun directionLabel(sort: VideoSort) =
    if (sort.ascending) ascendingLabel(sort.field) else descendingLabel(sort.field)

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

/** Two-segment grid/list switch; the active segment is filled so the current mode is obvious. */
@Composable
private fun LayoutSwitch(layout: LibraryLayout, onLayoutChange: (LibraryLayout) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .height(ToolbarControlHeight)
            .padding(3.dp),
    ) {
        LayoutSegment(Icons.Outlined.GridView, "Grid view", layout == LibraryLayout.GRID) {
            onLayoutChange(LibraryLayout.GRID)
        }
        LayoutSegment(Icons.AutoMirrored.Outlined.ViewList, "List view", layout == LibraryLayout.LIST) {
            onLayoutChange(LibraryLayout.LIST)
        }
    }
}

@Composable
private fun LayoutSegment(icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics {
            role = Role.RadioButton
            this.selected = selected
            contentDescription = description
        },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(18.dp))
    }
}

/** Small rounded label used for "NEW" markers and similar status. */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
