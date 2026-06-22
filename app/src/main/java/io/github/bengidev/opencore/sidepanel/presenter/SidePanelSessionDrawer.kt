package io.github.bengidev.opencore.sidepanel.presenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionComponent
import io.github.bengidev.opencore.sidepanel.application.session.SidePanelSessionState
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelSessionSection
import java.util.UUID

private const val DrawerAnimationMs = 280
private const val DrawerWidthRatio = 0.82f
private val MaxDrawerWidth = 360.dp
private const val ScrimAlpha = 0.32f

@Composable
internal fun SidePanelSessionDrawer(
    component: SidePanelSessionComponent,
    state: SidePanelSessionState,
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = minOf(screenWidth * DrawerWidthRatio, MaxDrawerWidth)

    var renameTarget by remember { mutableStateOf<SidePanelConversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    var newGroupTargetId by remember { mutableStateOf<UUID?>(null) }
    var newGroupText by remember { mutableStateOf("") }
    var contextMenuConversation by remember { mutableStateOf<SidePanelConversation?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.isSidebarVisible,
            enter = slideInHorizontally(animationSpec = tween(DrawerAnimationMs)) { -it },
            exit = slideOutHorizontally(animationSpec = tween(DrawerAnimationMs)) { -it }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.textPrimary.copy(alpha = ScrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { component.dismissSidebar() }
                        .semantics { contentDescription = "Dismiss sidebar" }
                )

                Column(
                    modifier = Modifier
                        .width(drawerWidth)
                        .fillMaxHeight()
                        .background(palette.surfacePaper)
                        .align(Alignment.CenterStart)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 24.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = component::settingsButtonTapped,
                            modifier = Modifier.testTag("sidepanel-settings-button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = palette.textPrimary
                            )
                        }
                        IconButton(onClick = component::dismissSidebar) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close history",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    SearchField(
                        query = state.historySearchQuery,
                        onQueryChanged = component::onHistorySearchQueryChanged
                    )

                    HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.25f))

                    when {
                        state.conversations.isEmpty() -> EmptyState(
                            icon = { Icon(Icons.Outlined.ChatBubbleOutline, null, tint = palette.textTertiary, modifier = Modifier.size(28.dp)) },
                            title = "No conversations yet",
                            subtitle = "Your chats will appear here."
                        )
                        state.filteredConversations.isEmpty() -> EmptyState(
                            icon = { Icon(Icons.Default.Search, null, tint = palette.textTertiary, modifier = Modifier.size(28.dp)) },
                            title = "No matches",
                            subtitle = "No conversations match your search."
                        )
                        else -> ConversationList(
                            state = state,
                            onConversationSelected = component::selectConversation,
                            onGroupHeaderToggled = component::toggleGroupHeader,
                            onConversationLongPress = { conversation ->
                                contextMenuConversation = conversation
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    val renameConversation = renameTarget
    if (renameConversation != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename conversation") },
            text = {
                TextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    component.renameConversation(renameConversation.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (newGroupTargetId != null) {
        AlertDialog(
            onDismissRequest = { newGroupTargetId = null },
            title = { Text("Create Group") },
            text = {
                TextField(
                    value = newGroupText,
                    onValueChange = { newGroupText = it },
                    singleLine = true,
                    placeholder = { Text("Group name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newGroupTargetId?.let { id ->
                        val trimmed = newGroupText.trim()
                        if (trimmed.isNotEmpty()) {
                            component.changeGroup(id, trimmed)
                        }
                    }
                    newGroupTargetId = null
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { newGroupTargetId = null }) { Text("Cancel") }
            }
        )
    }

    val liveConversation = contextMenuConversation?.let { target ->
        state.filteredConversations.firstOrNull { it.id == target.id } ?: target
    }
    if (showContextMenu && liveConversation != null) {
        val conversation = liveConversation
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConversationActionButton("Rename") {
                        renameTarget = conversation
                        renameText = conversation.title
                        showContextMenu = false
                    }
                    ConversationActionButton(if (conversation.isPinned) "Unpin" else "Pin") {
                        component.pinConversation(conversation)
                        showContextMenu = false
                    }
                    state.availableGroups.forEach { group ->
                        ConversationActionButton(
                            if (conversation.groupName == group) "Remove from $group" else "Move to $group"
                        ) {
                            val nextGroup = if (conversation.groupName == group) null else group
                            component.changeGroup(conversation.id, nextGroup)
                            showContextMenu = false
                        }
                    }
                    if (conversation.groupName != null) {
                        val currentGroup = conversation.groupName
                        ConversationActionButton("Remove from $currentGroup") {
                            component.changeGroup(conversation.id, null)
                            showContextMenu = false
                        }
                    }
                    ConversationActionButton("New Group...") {
                        newGroupTargetId = conversation.id
                        newGroupText = ""
                        showContextMenu = false
                    }
                    ConversationActionButton(
                        label = "Delete",
                        color = palette.accentPrimary
                    ) {
                        component.deleteConversation(conversation.id)
                        showContextMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ConversationActionButton(
    label: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = color
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    val palette = HomeTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(CircleShape)
            .background(palette.surfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = palette.textTertiary, modifier = Modifier.size(14.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                color = palette.textPrimary
            ),
            cursorBrush = SolidColor(palette.textPrimary),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text("Search conversations", color = palette.textTertiary, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = palette.textTertiary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    val palette = HomeTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        icon()
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textSecondary)
        Text(subtitle, fontSize = 13.sp, color = palette.textTertiary)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConversationList(
    state: SidePanelSessionState,
    onConversationSelected: (SidePanelConversation) -> Unit,
    onGroupHeaderToggled: (String) -> Unit,
    onConversationLongPress: (SidePanelConversation) -> Unit
) {
    val forceExpandGroups = state.historySearchQuery.trim().isNotEmpty()
    val sections = SidePanelSessionSection.grouped(
        conversations = state.filteredConversations,
        expandedGroups = state.expandedGroups,
        forceExpandGroups = forceExpandGroups
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        sections.forEach { section ->
            item(key = "header-${section.id}") {
                if (section.id.startsWith("group:")) {
                    val groupName = section.id.removePrefix("group:")
                    val isExpanded = state.expandedGroups.contains(groupName) || forceExpandGroups
                    GroupSectionHeader(
                        groupName = groupName,
                        isExpanded = isExpanded,
                        onClick = { onGroupHeaderToggled(groupName) }
                    )
                } else {
                    SectionHeader(title = section.title)
                }
            }
            items(section.conversations, key = { "${section.id}-${it.id}" }) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    isActive = state.activeConversationId == conversation.id,
                    isInGroup = section.id.startsWith("group:"),
                    onClick = { onConversationSelected(conversation) },
                    onLongClick = { onConversationLongPress(conversation) }
                )
            }
        }
    }
}

@Composable
private fun GroupSectionHeader(
    groupName: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val palette = HomeTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = palette.accentSoft, modifier = Modifier.size(11.dp))
        Text(
            text = groupName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textSecondary
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = HomeTheme.palette
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = palette.textTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: SidePanelConversation,
    isActive: Boolean,
    isInGroup: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = HomeTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isInGroup) 18.dp else 0.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) palette.surfaceSubtle else palette.surfacePaper.copy(alpha = 0f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("history-conversation-row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (conversation.isPinned) {
            Icon(Icons.Default.PushPin, contentDescription = null, tint = palette.textTertiary, modifier = Modifier.size(11.dp))
        }
        if (!isInGroup && conversation.groupName != null) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = palette.accentSoft, modifier = Modifier.size(11.dp))
        }
        Text(
            text = conversation.title,
            fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = SidePanelSessionSection.relativeLabel(conversation.updatedAt),
            fontSize = 12.sp,
            color = palette.textTertiary
        )
    }
}
