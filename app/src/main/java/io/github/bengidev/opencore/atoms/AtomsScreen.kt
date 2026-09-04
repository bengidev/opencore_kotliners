package io.github.bengidev.opencore.atoms

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.atoms.application.AtomsComponent
import io.github.bengidev.opencore.atoms.domain.Atom
import io.github.bengidev.opencore.atoms.domain.AtomsSection
import io.github.bengidev.opencore.home.theme.HomeTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AtomsScreen(
    component: AtomsComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.subscribeAsState()
    val palette = HomeTheme.palette
    val sections = AtomsSection.grouped(
        entries = state.filteredEntries,
        expandedGroups = state.expandedGroups,
    )
    var contextMenuAtom by remember { mutableStateOf<Atom?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Atom?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceBase)
            .statusBarsPadding(),
    ) {
        AtomsHeaderSection(
            searchQuery = state.searchQuery,
            onSearchQueryChanged = component::onSearchQueryChanged,
        )

        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.25f))

        when {
            state.entries.isEmpty() -> AtomsEmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(28.dp),
                    )
                },
                title = "No atoms yet",
                subtitle = "Your conversations will appear here.",
            )

            state.filteredEntries.isEmpty() -> AtomsEmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(28.dp),
                    )
                },
                title = "No matches",
                subtitle = "No atoms match your search.",
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header-${section.id}") {
                        Text(
                            text = section.title.removePrefix("v:").removePrefix(">:"),
                            style = HomeTheme.typography.welcomeCaption,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    itemsIndexed(
                        items = section.entries,
                        key = { _, entry -> entry.atom.id },
                    ) { index, entry ->
                        val isActive = entry.atom.id == state.activeAtomId
                        val previousActive = section.entries
                            .getOrNull(index - 1)
                            ?.atom
                            ?.id == state.activeAtomId
                        if (index > 0 && !isActive && !previousActive) {
                            HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.3f))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) {
                                        palette.surfaceSubtle
                                    } else {
                                        palette.surfaceBase.copy(alpha = 0f)
                                    },
                                )
                                .combinedClickable(
                                    onClick = { component.selectAtom(entry.atom) },
                                    onLongClick = {
                                        contextMenuAtom = entry.atom
                                        showContextMenu = true
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = entry.atom.title,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = entry.lastMessagePreview,
                                fontSize = 13.sp,
                                color = if (isActive) palette.textSecondary else palette.textTertiary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    val renameAtom = renameTarget
    if (renameAtom != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename atom") },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    component.renameAtom(renameAtom.id, renameText)
                    renameTarget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    val liveAtom = contextMenuAtom?.let { target ->
        state.filteredEntries.firstOrNull { it.atom.id == target.id }?.atom ?: target
    }
    if (showContextMenu && liveAtom != null) {
        val atom = liveAtom
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = {
                Text(atom.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AtomActionButton("Rename") {
                        renameTarget = atom
                        renameText = atom.title
                        showContextMenu = false
                    }
                    AtomActionButton(
                        label = "Delete",
                        color = palette.accentPrimary,
                    ) {
                        component.deleteAtom(atom.id)
                        showContextMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun AtomActionButton(
    label: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AtomsHeaderSection(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
) {
    val palette = HomeTheme.palette

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Atoms",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(palette.surfaceSubtle)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = palette.textPrimary,
                ),
                cursorBrush = SolidColor(palette.accentPrimary),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search atoms",
                                color = palette.textTertiary,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    }
                },
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChanged("") },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = palette.textTertiary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AtomsEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    val palette = HomeTheme.palette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textSecondary,
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = palette.textTertiary,
        )
    }
}
