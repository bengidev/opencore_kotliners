package io.github.bengidev.opencore.sidepanel.application.session

import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SidePanelSessionReducerTest {

    private val conversationId = UUID.randomUUID()

    private fun conversation(
        title: String = "Chat",
        isPinned: Boolean = false,
        groupName: String? = null
    ): SidePanelConversation = SidePanelConversation(
        id = conversationId,
        title = title,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        isPinned = isPinned,
        groupName = groupName
    )

    @Test
    fun sidebarToggled_flipsVisibility() {
        val opened = SidePanelSessionReducer.reduce(
            SidePanelSessionState(),
            SidePanelSessionIntent.SidebarToggled
        )
        assertTrue(opened.isSidebarVisible)

        val closed = SidePanelSessionReducer.reduce(opened, SidePanelSessionIntent.SidebarDismissed)
        assertFalse(closed.isSidebarVisible)
    }

    @Test
    fun historySearchQueryChanged_filtersInDerivedState() {
        val state = SidePanelSessionReducer.reduce(
            SidePanelSessionState(
                conversations = listOf(
                    conversation(title = "Alpha"),
                    SidePanelConversation(title = "Beta")
                )
            ),
            SidePanelSessionIntent.HistorySearchQueryChanged("alp")
        )
        assertEquals("alp", state.historySearchQuery)
        assertEquals(1, state.filteredConversations.size)
        assertEquals("Alpha", state.filteredConversations.first().title)
    }

    @Test
    fun conversationPinToggled_updatesPinnedFlag() {
        val initial = SidePanelSessionState(conversations = listOf(conversation(isPinned = false)))
        val pinned = SidePanelSessionReducer.reduce(
            initial,
            SidePanelSessionIntent.ConversationPinToggled(conversation(isPinned = false))
        )
        assertTrue(pinned.conversations.first().isPinned)
    }

    @Test
    fun conversationRenamed_trimsAndUpdatesTitle() {
        val initial = SidePanelSessionState(conversations = listOf(conversation()))
        val renamed = SidePanelSessionReducer.reduce(
            initial,
            SidePanelSessionIntent.ConversationRenamed(conversationId, "  New title  ")
        )
        assertEquals("New title", renamed.conversations.first().title)
    }

    @Test
    fun conversationDeleted_removesConversation() {
        val initial = SidePanelSessionState(conversations = listOf(conversation()))
        val deleted = SidePanelSessionReducer.reduce(
            initial,
            SidePanelSessionIntent.ConversationDeleted(conversationId)
        )
        assertTrue(deleted.conversations.isEmpty())
    }
}
